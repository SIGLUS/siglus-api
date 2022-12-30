/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.localmachine.cdc;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkTaskContext;

public class JdbcSinkerContext implements SinkTaskContext {

  @Override
  public Map<String, String> configs() {
    return null;
  }

  @Override
  public void offset(Map<TopicPartition, Long> offsets) {}

  @Override
  public void offset(TopicPartition tp, long offset) {}

  @Override
  public void timeout(long timeoutMs) {}

  @Override
  public Set<TopicPartition> assignment() {
    return Collections.emptySet();
  }

  @Override
  public void pause(TopicPartition... partitions) {}

  @Override
  public void resume(TopicPartition... partitions) {}

  @Override
  public void requestCommit() {}
}
