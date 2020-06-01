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

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.openlmis.notification.domain.PendingNotification.PendingNotificationId;
import org.openlmis.notification.service.NotificationChannel;

@Getter
@Entity
@Table(name = "pending_notifications")
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "notification")
@NamedQueries({
    @NamedQuery(name = "PendingNotification.getPendingNotifications",
        query = "SELECT p"
            + " FROM PendingNotification p"
            + " INNER JOIN FETCH p.notification"
            + " ORDER BY p.createdDate ASC")
})
public class PendingNotification implements Identifiable<PendingNotificationId> {

  public static final String GET_PENDING_NOTIFICATIONS_NAMED_QUERY =
      "PendingNotification.getPendingNotifications";

  @EmbeddedId
  private PendingNotificationId id;

  @MapsId("notificationId")
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "notificationId", unique = true, nullable = false)
  private Notification notification;

  @Column(columnDefinition = "timestamp with time zone", nullable = false)
  @Getter
  private ZonedDateTime createdDate;

  /**
   * Creates a new instance based on passed parameters.
   */
  public PendingNotification(Notification notification, NotificationChannel channel) {
    this.id = new PendingNotificationId(notification.getId(), channel);
    this.notification = notification;
    this.createdDate = ZonedDateTime.now();
  }

  public UUID getNotificationId() {
    return id.notificationId;
  }

  public NotificationChannel getChannel() {
    return id.channel;
  }

  @Embeddable
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static final class PendingNotificationId implements Serializable {

    private UUID notificationId;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private NotificationChannel channel;
  }

}
