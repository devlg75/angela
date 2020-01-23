package com.terracottatech.qa.angela.client.net;

import com.terracottatech.qa.angela.agent.Agent;
import com.terracottatech.qa.angela.client.util.IgniteClientHelper;
import com.terracottatech.qa.angela.common.net.Disruptor;
import com.terracottatech.qa.angela.common.net.DisruptorState;
import com.terracottatech.qa.angela.common.tcconfig.ServerSymbolicName;
import com.terracottatech.qa.angela.common.tcconfig.TerracottaServer;
import com.terracottatech.qa.angela.common.topology.InstanceId;
import com.terracottatech.qa.angela.common.topology.Topology;
import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Disrupt traffic between set of servers.(i.e active and passives)
 */
public class ServerToServerDisruptor implements Disruptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerToServerDisruptor.class);
  //servers to be linked to serve this disruption
  private final Map<ServerSymbolicName, Collection<ServerSymbolicName>> linkedServers;
  private final Ignite ignite;
  private final InstanceId instanceId;
  private final Topology topology;
  private final Consumer<Disruptor> closeHook;
  private volatile DisruptorState state;

  ServerToServerDisruptor(Ignite ignite, InstanceId instanceId, Topology topology, Map<ServerSymbolicName, Collection<ServerSymbolicName>> linkedServers, Consumer<Disruptor> closeHook) {
    this.ignite = ignite;
    this.instanceId = instanceId;
    this.topology = topology;
    this.linkedServers = linkedServers;
    this.closeHook = closeHook;
    this.state = DisruptorState.UNDISRUPTED;
  }


  @Override
  public void disrupt() {
    if (state != DisruptorState.UNDISRUPTED) {
      throw new IllegalStateException("Illegal state before disrupt:" + state);
    }

    LOGGER.info("blocking {}", this);
    //invoke disruption remotely on each linked servers.
    Map<ServerSymbolicName, TerracottaServer> topologyServers = new HashMap<>();
    for (TerracottaServer svr : topology.getServers()) {
      topologyServers.put(svr.getServerSymbolicName(), svr);
    }
    for (Map.Entry<ServerSymbolicName, Collection<ServerSymbolicName>> entry : linkedServers.entrySet()) {
      TerracottaServer server = topologyServers.get(entry.getKey());
      Collection<TerracottaServer> otherServers = Collections.unmodifiableCollection(entry.getValue()
          .stream()
          .map(topologyServers::get)
          .collect(Collectors.toList()));
      IgniteClientHelper.executeRemotely(ignite, server.getHostname(), blockRemotely(instanceId, server, otherServers));
    }

    state = DisruptorState.DISRUPTED;
  }

  @Override
  public void undisrupt() {
    if (state != DisruptorState.DISRUPTED) {
      throw new IllegalStateException("Illegal state before undisrupt:" + state);
    }

    LOGGER.info("undisrupting {}", this);
    Map<ServerSymbolicName, TerracottaServer> topologyServers = new HashMap<>();
    for (TerracottaServer svr : topology.getServers()) {
      topologyServers.put(svr.getServerSymbolicName(), svr);
    }
    for (Map.Entry<ServerSymbolicName, Collection<ServerSymbolicName>> entry : linkedServers.entrySet()) {
      TerracottaServer server = topologyServers.get(entry.getKey());
      Collection<TerracottaServer> otherServers = Collections.unmodifiableCollection(entry.getValue()
          .stream()
          .map(topologyServers::get)
          .collect(Collectors.toList()));
      IgniteClientHelper.executeRemotelyAsync(ignite, server.getHostname(), undisruptRemotely(instanceId, server, otherServers)).get();
    }
    state = DisruptorState.UNDISRUPTED;
  }


  @Override
  public void close() {
    if (state == DisruptorState.DISRUPTED) {
      undisrupt();
    }
    if (state == DisruptorState.UNDISRUPTED) {
      //remote server links will be closed when servers are stopped.
      closeHook.accept(this);
      state = DisruptorState.CLOSED;
    }
  }

  Map<ServerSymbolicName, Collection<ServerSymbolicName>> getLinkedServers() {
    return linkedServers;
  }

  private static IgniteRunnable blockRemotely(InstanceId instanceId, TerracottaServer server, Collection<TerracottaServer> otherServers) {
    return (IgniteRunnable)() -> Agent.controller.disrupt(instanceId, server, otherServers);
  }

  private static IgniteRunnable undisruptRemotely(InstanceId instanceId, TerracottaServer server, Collection<TerracottaServer> otherServers) {
    return (IgniteRunnable)() -> Agent.controller.undisrupt(instanceId, server, otherServers);
  }

  @Override
  public String toString() {
    return "ServerToServerDisruptor{" +
           "linkedServers=" + linkedServers.entrySet()
               .stream()
               .map(e -> e.getKey().getSymbolicName() + "->" + e.getValue()
                   .stream()
                   .map(ServerSymbolicName::getSymbolicName)
                   .collect(Collectors.joining(",", "[", "]")))
               .collect(Collectors.joining(",", "{", "}")) +
           '}';
  }

}