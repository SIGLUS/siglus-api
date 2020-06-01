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
import org.apache.commons.lang3.RandomStringUtils;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.PostponeMessage;
import org.openlmis.notification.service.NotificationChannel;

public class PostponeMessageDataBuilder {

  private UUID id = UUID.randomUUID();
  private DigestConfiguration configuration = new DigestConfigurationDataBuilder().build();
  private String body = RandomStringUtils.randomAlphabetic(300);
  private String subject = RandomStringUtils.randomAlphabetic(50);
  private UUID userId = UUID.randomUUID();
  private NotificationChannel channel = NotificationChannel.EMAIL;

  public PostponeMessageDataBuilder withConfiguration(DigestConfiguration configuration) {
    this.configuration = configuration;
    return this;
  }

  public PostponeMessageDataBuilder withUserId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public PostponeMessage buildAsNew() {
    return new PostponeMessage(configuration, body, subject, userId, channel);
  }

  /**
   * Creates new instance of {@link PostponeMessage} with passed values from the builder, the id
   * field is also set.
   */
  public PostponeMessage build() {
    PostponeMessage built = buildAsNew();
    built.setId(id);

    return built;
  }


}
