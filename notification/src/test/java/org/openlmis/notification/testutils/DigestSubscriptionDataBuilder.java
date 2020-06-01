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

package org.openlmis.notification.testutils;

import java.util.UUID;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.util.UserContactDetailsDataBuilder;

public class DigestSubscriptionDataBuilder {

  private UUID id = UUID.randomUUID();
  private UserContactDetails userContactDetails = new UserContactDetailsDataBuilder().build();
  private DigestConfiguration digestConfiguration = new DigestConfigurationDataBuilder().build();
  private String cronExpression = "* 0/15 * * * *";
  private NotificationChannel preferredChannel = NotificationChannel.EMAIL;
  private Boolean useDigest = false;

  public DigestSubscriptionDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public DigestSubscriptionDataBuilder withUserContactDetails(UserContactDetails contactDetails) {
    this.userContactDetails = contactDetails;
    return this;
  }

  public DigestSubscriptionDataBuilder withDigestConfiguration(DigestConfiguration configuration) {
    this.digestConfiguration = configuration;
    return this;
  }

  public DigestSubscriptionDataBuilder withCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
    return this;
  }

  public DigestSubscriptionDataBuilder withPreferredChannel(NotificationChannel preferredChannel) {
    this.preferredChannel = preferredChannel;
    return this;
  }

  public DigestSubscriptionDataBuilder withUseDigest(Boolean useDigest) {
    this.useDigest = useDigest;
    return this;
  }

  public DigestSubscription buildAsNew() {
    return DigestSubscription.create(userContactDetails, digestConfiguration, cronExpression,
        preferredChannel, useDigest);
  }

  /**
   * Creates new instance of {@link DigestConfiguration} with passed values from the builder, the id
   * field is also set.
   */
  public DigestSubscription build() {
    DigestSubscription built = buildAsNew();
    built.setId(id);

    return built;
  }
}
