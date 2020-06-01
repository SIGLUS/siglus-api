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

package org.openlmis.notification.web.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.NotificationMessage;
import org.openlmis.notification.service.NotificationChannel;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class NotificationDto implements Notification.Exporter, Notification.Importer {

  private UUID userId;

  @JsonProperty("messages")
  private Map<String, MessageDto> messageMap = new HashMap<>();

  private Boolean important;

  private ZonedDateTime createdDate;
  
  public void addMessage(String key, MessageDto message) {
    messageMap.put(key, message);
  }

  @Override
  public void setMessages(List<NotificationMessage> messages) {
    this.messageMap = new HashMap<>();
    for (NotificationMessage message : messages) {
      MessageDto messageDto = new MessageDto(message.getSubject(),
          message.getBody(), message.getTag());
      this.messageMap.put(message.getChannel().toString().toLowerCase(), messageDto);
    }
  }
  
  @JsonIgnore
  @Override
  public List<NotificationMessage> getMessages() {
    List<NotificationMessage> messageList = new ArrayList<>();
    for (Map.Entry<String, MessageDto> entry : messageMap.entrySet()) {
      String key = entry.getKey();
      MessageDto messageDto = entry.getValue();
      messageList.add(new NotificationMessage(NotificationChannel.fromString(key),
          messageDto.getBody(), messageDto.getSubject(), messageDto.getTag()));
    }
    return messageList;
  }
}
