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

import java.io.Closeable;

/**
 * @author Mathieu Carbou
 */
public interface PortAllocation extends Closeable {

  static PortAllocation fixed(int basePort, int count) {
    return new PortAllocation() {
      @Override
      public int getBasePort() {
        return basePort;
      }

      @Override
      public int getPortCount() {
        return count;
      }
    };
  }

  /**
   * @return The first free port reserved by the implementation.
   */
  int getBasePort();

  /**
   * @return The number of port reserved by the implementation
   */
  int getPortCount();

  @Override
  default void close() {}
}
