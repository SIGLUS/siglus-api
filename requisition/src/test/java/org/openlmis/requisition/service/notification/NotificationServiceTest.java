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

package org.openlmis.requisition.service.notification;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.AuthService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {
  private static final String ACCESS_TOKEN = "token";
  private static final String MAIL_SUBJECT = "subject";
  private static final String MAIL_CONTENT = "Email content";
  private static final String SMS_CONTENT = "SMS content";
  private static final String MAIL_TAG = "tag";
  private static final String BASE_URL = "http://localhost";

  @Mock
  private AuthService authService;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private NotificationService notificationService;

  @Before
  public void before() {
    when(authService.obtainAccessToken()).thenReturn(ACCESS_TOKEN);

    notificationService.setRestTemplate(restTemplate);
    ReflectionTestUtils.setField(notificationService, "notificationUrl", BASE_URL);
  }

  @Test
  public void shouldNotifyUser() throws Exception {
    UserDto user = DtoGenerator.of(UserDto.class);

    notificationService.notify(user, MAIL_SUBJECT, MAIL_CONTENT, SMS_CONTENT, MAIL_TAG);

    ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

    verify(restTemplate).postForObject(eq(
        new URI(BASE_URL + "/api/notifications")),
        captor.capture(), eq(Object.class));

    assertEquals(singletonList("Bearer " + ACCESS_TOKEN), captor.getValue().getHeaders()
            .get(HttpHeaders.AUTHORIZATION));
  }
}
