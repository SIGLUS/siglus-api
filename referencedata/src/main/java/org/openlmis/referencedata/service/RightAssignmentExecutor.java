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

import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class RightAssignmentExecutor {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(RightAssignmentService.class);

  @Value("${rightAssignments.thread.corePool}")
  private int corePoolSize;

  @Value("${rightAssignments.thread.maxPool}")
  private int maxPoolSize;

  @Value("${rightAssignments.queue.capacity}")
  private int queueCapacity;

  @Value("${rightAssignments.thread.timeout}")
  private int threadTimeout;

  /**
   * Executor for right assignment regeneration task.
   * Restricts async parameters such as thread pool size. queue capacity or thread timeout.
   */
  @Bean
  @Qualifier("rightAssignmentTaskExecutor")
  public ThreadPoolTaskExecutor rightAssignmentTaskExecutor() {
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
    threadPoolTaskExecutor.setRejectedExecutionHandler((runnable, executor) ->
        XLOGGER.error("Thread pool for Right Assignment Regeneration exceeded"));
    threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
    threadPoolTaskExecutor.setQueueCapacity(queueCapacity);
    threadPoolTaskExecutor.setKeepAliveSeconds(threadTimeout);

    return threadPoolTaskExecutor;
  }
}
