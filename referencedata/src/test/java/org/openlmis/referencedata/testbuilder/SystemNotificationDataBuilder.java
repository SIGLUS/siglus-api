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

package org.openlmis.referencedata.testbuilder;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.openlmis.referencedata.domain.SupplyPartner;
import org.openlmis.referencedata.domain.SystemNotification;
import org.openlmis.referencedata.domain.User;

public class SystemNotificationDataBuilder {
  private UUID id = UUID.randomUUID();
  private String title = RandomStringUtils.randomAlphanumeric(5);
  private String message = "This is the OpenLMIS system notification.";
  private ZonedDateTime startDate = ZonedDateTime.now().minusDays(10);
  private ZonedDateTime expiryDate = ZonedDateTime.now().plusDays(10);
  private ZonedDateTime createdDate = ZonedDateTime.now();
  private User author = new UserDataBuilder().buildAsNew();
  private boolean active = true;

  public SystemNotification buildAsNew() {
    return new SystemNotification(title, message, startDate, expiryDate, createdDate,
        active, author);
  }

  /**
   * Builds an instance of {@link SupplyPartner}.
   */
  public SystemNotification build() {
    SystemNotification systemNotification = buildAsNew();
    systemNotification.setId(id);

    return systemNotification;
  }

  public SystemNotificationDataBuilder withAuthor(User user) {
    this.author = user;
    return this;
  }

  public SystemNotificationDataBuilder asInactive() {
    this.active = false;
    return this;
  }

  public SystemNotificationDataBuilder withExpiryDate(ZonedDateTime expiryDate) {
    this.expiryDate = expiryDate;
    return this;
  }

  public SystemNotificationDataBuilder withoutExpiryDate() {
    this.expiryDate = null;
    return this;
  }

  public SystemNotificationDataBuilder withStartDate(ZonedDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  public SystemNotificationDataBuilder withoutStartDate() {
    this.startDate = null;
    return this;
  }
}
