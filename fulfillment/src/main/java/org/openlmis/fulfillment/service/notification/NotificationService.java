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

package org.openlmis.fulfillment.service.notification;

import static org.openlmis.fulfillment.service.notification.NotificationChannelDto.EMAIL;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.openlmis.fulfillment.service.AuthService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.request.RequestHeaders;
import org.openlmis.fulfillment.service.request.RequestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private AuthService authService;

  @Value("${notification.url}")
  private String notificationUrl;

  private RestOperations restTemplate = new RestTemplate();

  /**
   * Send an email notification.
   *
   * @param user    receiver of the notification
   * @param subject subject of the email
   * @param content content of the email
   * @return true if success, false if failed.
   */
  public boolean notify(UserDto user, String subject, String content) {
    NotificationDto request = buildNotification(user, subject, content);
    logger.debug("Sending request:"
        + "\n subject:" + request.getMessages().get(EMAIL.toString()).getSubject()
        + "\n content:" + request.getMessages().get(EMAIL.toString()).getBody()
        + "\n to: " + request.getUserId());

    String url = notificationUrl + "/api/notifications";
    try {
      RequestHeaders headers;
      headers = RequestHeaders.init().setAuth(authService.obtainAccessToken());
      URI uri = RequestHelper.createUri(url);
      HttpEntity<NotificationDto> entity = RequestHelper.createEntity(request, headers);

      restTemplate.postForObject(uri, entity, Object.class);
    } catch (HttpStatusCodeException ex) {
      logger.error(
          "Unable to send notification. Error code: {}, response message: {}",
          ex.getStatusCode(), ex.getResponseBodyAsString()
      );
      return false;
    }

    return true;
  }

  private NotificationDto buildNotification(UserDto user, String subject, String content) {
    Map<String, MessageDto> messages = new HashMap<>();
    messages.put(EMAIL.toString(), new MessageDto(subject, content));

    return new NotificationDto(user.getId(), messages);
  }
}
