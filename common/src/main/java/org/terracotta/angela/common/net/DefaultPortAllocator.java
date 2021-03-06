/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Angela.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */
package org.terracotta.angela.common.net;

import org.terracotta.utilities.test.net.PortManager;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mathieu Carbou
 */
public class DefaultPortAllocator implements PortAllocator {

  private final PortManager portManager = PortManager.getInstance();
  private final Collection<PortManager.PortRef> reservations = new CopyOnWriteArrayList<>();

  @Override
  public PortReservation reserve(int portCounts) {
    List<PortManager.PortRef> portRefs = portManager.reservePorts(portCounts);
    reservations.addAll(portRefs);
    return new PortReservation() {
      int i = 0;

      @Override
      public Integer next() {
        if (i >= portRefs.size()) {
          throw new NoSuchElementException();
        }
        return portRefs.get(i++).port();
      }

      @Override
      public boolean hasNext() {
        return i < portRefs.size();
      }

      @Override
      public void close() {
        for (PortManager.PortRef portRef : portRefs) {
          if (reservations.remove(portRef)) {
            portRef.close();
          }
        }
      }
    };
  }

  @Override
  public void close() {
    while (!reservations.isEmpty() && !Thread.currentThread().isInterrupted()) {
      for (PortManager.PortRef portRef : reservations) {
        if (reservations.remove(portRef)) {
          portRef.close();
        }
      }
    }
  }
}