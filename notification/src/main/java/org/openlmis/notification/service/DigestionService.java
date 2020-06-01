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

package org.openlmis.notification.service;

import static org.openlmis.notification.service.DigestFilter.SEND_NOW_POSTPONE_CHANNEL;
import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.CHANNEL_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.TAG_HEADER;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.persistence.EntityManager;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.domain.PostponeMessage;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.openlmis.notification.repository.PostponeMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@MessageEndpoint
public class DigestionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DigestionService.class);

  static final String AGGREGATE_POSTPONE_CHANNEL = "notificationToSend.sendNow.postpone.aggregate";

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Autowired
  private DigestConfigurationRepository digestConfigurationRepository;

  @Autowired
  private DigestSubscriptionRepository digestSubscriptionRepository;

  @Autowired
  private PostponeMessageRepository postponeMessageRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private BeanFactory beanFactory;

  private Table<UUID, String, SourcePollingChannelAdapter> adapters;

  public DigestionService() {
    this(HashBasedTable.create());
  }

  @VisibleForTesting
  DigestionService(Table<UUID, String, SourcePollingChannelAdapter> adapters) {
    this.adapters = adapters;
  }

  /**
   * Handle postpone notifications.
   */
  @ServiceActivator(inputChannel = SEND_NOW_POSTPONE_CHANNEL)
  public final void handleMessage(@Payload NotificationMessage message,
      @Header(CHANNEL_HEADER) NotificationChannel channel,
      @Header(RECIPIENT_HEADER) UUID recipient, @Header(TAG_HEADER) String tag) {
    DigestConfiguration configuration = digestConfigurationRepository.findByTag(tag);

    if (null == configuration) {
      LOGGER.warn("Digest configuration for tag {} does not exist", tag);
      return;
    }

    DigestSubscription subscription = digestSubscriptionRepository.findBy(recipient, configuration);

    if (null == subscription) {
      LOGGER.warn("Digest subscription for user {} and tag {} does not exist", recipient, tag);
      return;
    }

    String sendTime = subscription.getCronExpression();

    postponeMessageRepository.saveAndFlush(
        new PostponeMessage(configuration, message.getBody(),
            message.getSubject(), recipient, channel));

    if (!adapters.contains(recipient, tag)) {
      setPollingAdapter(channel, configuration, recipient, sendTime);
    }
  }

  @VisibleForTesting
  void setPollingAdapter(NotificationChannel channel, DigestConfiguration configuration,
      UUID recipient, String cronExpression) {
    SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();

    adapter.setOutputChannelName(AGGREGATE_POSTPONE_CHANNEL);

    adapter.setSource(new PostponeMessageRetriever(entityManager,
        channel, configuration.getId(), recipient));

    adapter.setAdviceChain(Lists.newArrayList(new TransactionInterceptor(
        transactionManager, new MatchAlwaysTransactionAttributeSource())));

    adapter.setTrigger(new CronTrigger(cronExpression, TimeZone.getTimeZone(timeZoneId)));
    adapter.setBeanFactory(beanFactory);

    adapter.afterPropertiesSet();

    adapter.start();

    adapters.put(recipient, configuration.getTag(), adapter);
  }

  /**
   * Drops all existing polling adapters for the given user.
   */
  public void dropExistingPollingAdapters(UUID userId) {
    Map<String, SourcePollingChannelAdapter> row = adapters.row(userId);

    row.values().forEach(AbstractEndpoint::stop);
    row.clear();
  }

}
