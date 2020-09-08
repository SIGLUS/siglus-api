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

package org.siglus.siglusapi.service.mapper;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomUtils.nextBoolean;
import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.domain.NotificationType;
import org.siglus.siglusapi.dto.NotificationDto;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class NotificationMapperTest {

  private static final int NOT_TOO_LONG = 10;

  @InjectMocks
  private NotificationMapper mapper;

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(mapper, "timeZoneId", "UTC");
  }

  @Test
  public void shouldCallRepoWhenSearchNotifications() {
    // given
    Notification notification = new Notification();
    notification.setId(randomUUID());
    notification.setRefId(randomUUID());
    notification.setEmergency(nextBoolean());
    notification.setRefStatus(NotificationStatus.IN_APPROVAL);
    notification.setOperatorId(randomUUID());
    notification.setSourceFacilityName(random(NOT_TOO_LONG));
    notification.setType(NotificationType.STATUS_UPDATE);
    notification.setCreateDate(LocalDateTime.now());
    ProcessingPeriodDto period = new ProcessingPeriodDto();

    // when
    ZonedDateTime submitDate = ZonedDateTime.now();
    NotificationDto notificationDto = mapper.from(notification, period, submitDate);

    // then
    assertEquals(notification.getId(), notificationDto.getId());
    assertEquals(notification.getEmergency(), notificationDto.getEmergencyFlag());
    assertEquals(notification.getSourceFacilityName(), notificationDto.getSourceFacilityName());
    assertEquals(notification.getRefId(), notificationDto.getRefId());
    assertEquals(notification.getRefStatus(), notificationDto.getStatus());
    assertEquals(notification.getType(), NotificationType.STATUS_UPDATE);
    assertEquals(period, notificationDto.getProcessingPeriod());
    assertEquals(submitDate, notificationDto.getRequisitionSubmitDate());
  }

}
