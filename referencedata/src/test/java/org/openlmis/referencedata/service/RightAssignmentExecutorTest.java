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

package org.openlmis.referencedata.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

public class RightAssignmentExecutorTest {

  @Test
  public void shouldReturnProperThreadPoolTaskExecutor() {
    RightAssignmentExecutor executor = new RightAssignmentExecutor();
    ReflectionTestUtils.setField(executor, "corePoolSize", 1);
    ReflectionTestUtils.setField(executor, "maxPoolSize", 1);
    ReflectionTestUtils.setField(executor, "queueCapacity", 1);
    ReflectionTestUtils.setField(executor, "threadTimeout", 60);

    ThreadPoolTaskExecutor threadPoolTaskExecutor = executor.rightAssignmentTaskExecutor();

    assertEquals(1, threadPoolTaskExecutor.getCorePoolSize());
    assertEquals(1, threadPoolTaskExecutor.getMaxPoolSize());
    assertEquals(1, ReflectionTestUtils.getField(threadPoolTaskExecutor, "queueCapacity"));
    assertEquals(60, threadPoolTaskExecutor.getKeepAliveSeconds());
  }
}
