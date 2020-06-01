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

package org.openlmis.notification.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.service.NotificationChannel;

public class NotificationDataBuilder {

  private UUID id = UUID.randomUUID();
  private UUID userId;
  private List<NotificationMessage> messages;
  private Boolean important;

  /**
   * Default constructor.
   */
  public NotificationDataBuilder() {
    userId = UUID.randomUUID();
    messages = new ArrayList<>();
    important = false;
  }

  public NotificationDataBuilder withUserId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public NotificationDataBuilder withMessage(NotificationMessage message) {
    this.messages.add(message);
    return this;
  }

  public NotificationDataBuilder withMessage(NotificationChannel channel, String body) {
    return withMessage(channel, body, null);
  }

  public NotificationDataBuilder withMessage(NotificationChannel channel, String body,
      String subject) {
    return withMessage(channel, body, subject, null);
  }

  public NotificationDataBuilder withMessage(NotificationChannel channel, String body,
      String subject, String tag) {
    return withMessage(new NotificationMessage(channel, body, subject, tag));
  }

  public NotificationDataBuilder withEmptyMessage(NotificationChannel channel) {
    return withMessage(channel, "");
  }
  
  public NotificationDataBuilder withImportant(boolean important) {
    this.important = important;
    return this;
  }

  /**
   * Build a new notification based on parameters from the builder without id field.
   * 
   * @return new notification
   */
  public Notification buildAsNew() {
    return new Notification(userId, messages, important);
  }

  /**
   * Build a new notification based on parameters from the builder with id field.
   *
   * @return new notification
   */
  public Notification build() {
    Notification built = buildAsNew();
    built.setId(id);

    return built;
  }
}
