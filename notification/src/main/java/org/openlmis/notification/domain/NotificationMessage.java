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

package org.openlmis.notification.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.openlmis.notification.service.NotificationChannel;

@Entity
@Table(name = "notification_messages",
    uniqueConstraints = @UniqueConstraint(name = "unq_notification_messages_notificationid_channel",
        columnNames = {"notificationId", "channel"}))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true, exclude = "notification")
public class NotificationMessage extends BaseEntity {

  @ManyToOne
  @Type(type = UUID_TYPE)
  @JoinColumn(name = "notificationId", nullable = false)
  @Setter
  private Notification notification;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION, nullable = false)
  @Enumerated(value = EnumType.STRING)
  private NotificationChannel channel;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION, nullable = false)
  private String body;

  @Column(columnDefinition = TEXT_COLUMN_DEFINITION)
  private String subject;

  @Getter
  private String tag;

  /**
   * Default constructor.
   *
   * @param channel channel
   * @param body body
   */
  public NotificationMessage(NotificationChannel channel, String body) {
    this(channel, body, null);
  }

  /**
   * Constructor which includes subject.
   *
   * @param channel channel
   * @param body body
   * @param subject subject
   */
  public NotificationMessage(NotificationChannel channel, String body, String subject) {
    this(channel, body, subject, null);
  }

  public NotificationMessage(NotificationChannel channel, String body,
      String subject, String tag) {
    this(null, channel, body, subject, tag);
  }
}
