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

import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.PendingNotification;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.util.NotificationDataBuilder;

public class PendingNotificationDataBuilder {

  private Notification notification = new NotificationDataBuilder().build();
  private NotificationChannel channel = NotificationChannel.SMS;

  public PendingNotification build() {
    return new PendingNotification(notification, channel);
  }

  public PendingNotificationDataBuilder withChannel(NotificationChannel channel) {
    this.channel = channel;
    return this;
  }

  public PendingNotificationDataBuilder withNotification(Notification notification) {
    this.notification = notification;
    return this;
  }

  /**
   * Creates new instance of {@link PendingNotification} with passed values from the builder.
   */
  public PendingNotification buildForEmailChannel(Notification notification) {
    return this
      .withNotification(notification)
      .withChannel(NotificationChannel.EMAIL)
      .build();
  }

}
