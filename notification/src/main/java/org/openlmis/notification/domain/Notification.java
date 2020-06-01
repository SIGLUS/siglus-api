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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

  @Column(nullable = false)
  @Getter
  private UUID userId;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "notification", orphanRemoval = true,
      fetch = FetchType.EAGER)
  @Getter
  private List<NotificationMessage> messages;

  @Getter
  private Boolean important;
  
  @Column(columnDefinition = "timestamp with time zone", nullable = false)
  @Getter
  private ZonedDateTime createdDate;

  /**
   * Default constructor.
   * 
   * @param userId user id
   * @param messages messages list
   * @param important important flag
   */
  public Notification(UUID userId, List<NotificationMessage> messages, Boolean important) {
    this.userId = userId;
    this.messages = messages;
    this.messages.forEach(notificationMessage -> notificationMessage.setNotification(this));
    this.important = important;
    this.createdDate = ZonedDateTime.now();
  }

  /**
   * Construct new notification based on an importer (DTO).
   *
   * @param importer importer (DTO) to use
   * @return new notification
   */
  public static Notification newInstance(Importer importer) {
    return new Notification(importer.getUserId(), importer.getMessages(), importer.getImportant());
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setUserId(userId);
    exporter.setMessages(messages);
    exporter.setImportant(important);
    exporter.setCreatedDate(createdDate);
  }
  
  public interface Importer {
    
    UUID getUserId();
    
    List<NotificationMessage> getMessages();
    
    Boolean getImportant();
  }

  public interface Exporter {

    void setUserId(UUID userId);

    void setMessages(List<NotificationMessage> messages);
    
    void setImportant(Boolean important);
    
    void setCreatedDate(ZonedDateTime createdDate);
  }
}
