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

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.openlmis.notification.service.ChannelFilter.READY_TO_SEND_CHANNEL;
import static org.openlmis.notification.service.NotificationToSendRetriever.IMPORTANT_HEADER;
import static org.openlmis.notification.service.NotificationToSendRetriever.RECIPIENT_HEADER;
import static org.openlmis.notification.service.NotificationTransformer.TAG_HEADER;

import java.util.UUID;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.repository.DigestSubscriptionRepository;
import org.openlmis.notification.service.referencedata.TogglzFeatureDto;
import org.openlmis.notification.service.referencedata.TogglzReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.messaging.handler.annotation.Header;

@MessageEndpoint
public class DigestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DigestFilter.class);

  static final String SEND_NOW_PREPARE_CHANNEL = "notificationToSend.sendNow.prepare";
  public static final String SEND_NOW_POSTPONE_CHANNEL = "notificationToSend.sendNow.postpone";

  static final String CONSOLIDATE_NOTIFICATIONS = "CONSOLIDATE_NOTIFICATIONS";

  @Autowired
  private TogglzReferenceDataService togglzReferenceDataService;

  @Autowired
  private DigestConfigurationRepository digestConfigurationRepository;

  @Autowired
  private DigestSubscriptionRepository digestSubscriptionRepository;

  /**
   * Checks if the notification should be sent now or postpone for later.
   */
  @Router(inputChannel = READY_TO_SEND_CHANNEL)
  public String route(@Header(RECIPIENT_HEADER) UUID recipient,
      @Header(value = IMPORTANT_HEADER, required = false) Boolean important,
      @Header(value = TAG_HEADER, required = false) String tag) {
    if (isTrue(important)) {
      LOGGER.debug("The important flag is set");
      return SEND_NOW_PREPARE_CHANNEL;
    }

    if (!isFeatureActive()) {
      LOGGER.warn("Digest feature is disabled");
      return SEND_NOW_PREPARE_CHANNEL;
    }

    if (isBlank(tag)) {
      LOGGER.warn("Message has no tag header");
      return SEND_NOW_PREPARE_CHANNEL;
    }

    DigestConfiguration configuration = digestConfigurationRepository.findByTag(tag);

    if (null == configuration) {
      LOGGER.info("Digest configuration for tag {} does not exist", tag);
      return SEND_NOW_PREPARE_CHANNEL;
    }

    DigestSubscription subscription = digestSubscriptionRepository.findBy(recipient, configuration);

    if (null == subscription) {
      LOGGER.info("A notification for a user {} with {} tag will be sent now", recipient, tag);
      return SEND_NOW_PREPARE_CHANNEL;
    }

    if (!subscription.getUseDigest()) {
      LOGGER.info("A notification for a user {} with {} tag will be sent now", recipient, tag);
      return SEND_NOW_PREPARE_CHANNEL;
    }

    LOGGER.info("A notification for a user {} with {} tag will be postpone", recipient, tag);
    return SEND_NOW_POSTPONE_CHANNEL;
  }

  private boolean isFeatureActive() {
    return togglzReferenceDataService
        .findAll()
        .stream()
        .filter(feature -> CONSOLIDATE_NOTIFICATIONS.equals(feature.getName()))
        .findFirst()
        .map(TogglzFeatureDto::isEnabled)
        .orElse(false);
  }

}
