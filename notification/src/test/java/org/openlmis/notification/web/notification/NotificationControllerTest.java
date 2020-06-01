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

import static org.mockito.BDDMockito.given;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_USER_CONTACT_DETAILS_NOT_FOUND;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_USER_NOT_ACTIVE_OR_NOT_FOUND;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.NotificationRepository;
import org.openlmis.notification.repository.PendingNotificationRepository;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.service.PermissionService;
import org.openlmis.notification.service.referencedata.UserDto;
import org.openlmis.notification.service.referencedata.UserReferenceDataService;
import org.openlmis.notification.testutils.UserDataBuilder;
import org.openlmis.notification.util.NotificationDataBuilder;
import org.openlmis.notification.util.UserContactDetailsDataBuilder;
import org.openlmis.notification.web.NotFoundException;
import org.openlmis.notification.web.ValidationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

@SuppressWarnings("PMD.UnusedPrivateField")
public class NotificationControllerTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private NotificationDtoValidator notificationValidator;

  @Mock
  private UserContactDetailsRepository userContactDetailsRepository;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private PendingNotificationRepository pendingNotificationRepository;

  @InjectMocks
  private NotificationController controller;

  private UserContactDetails contactDetails = new UserContactDetailsDataBuilder().build();
  private Notification notification = new NotificationDataBuilder()
      .withUserId(contactDetails.getReferenceDataUserId())
      .withMessage(NotificationChannel.EMAIL, "subject", "body")
      .build();

  private UserDto userDto = new UserDataBuilder().build();
  private NotificationDto notificationDto = new NotificationDto();

  private BindingResult bindingResult = new BeanPropertyBindingResult(
      notificationDto, "notification");

  @Before
  public void setUp() {
    notification.export(notificationDto);

    given(userContactDetailsRepository.findOne(notification.getUserId()))
        .willReturn(contactDetails);
    given(userReferenceDataService.findOne(contactDetails.getReferenceDataUserId()))
        .willReturn(userDto);
  }

  @Test
  public void shouldThrowExceptionWhenContactDetailsDoesNotExist() {
    // given
    exception.expect(NotFoundException.class);
    exception.expectMessage(ERROR_USER_CONTACT_DETAILS_NOT_FOUND);

    given(userContactDetailsRepository.findOne(notification.getUserId())).willReturn(null);

    // when
    controller.sendNotification(notificationDto, bindingResult);

    // then
    // the exception should be thrown
  }

  @Test
  public void shouldThrowExceptionWhenUserDoesNotExist() {
    // given
    exception.expect(ValidationException.class);
    exception.expectMessage(ERROR_USER_NOT_ACTIVE_OR_NOT_FOUND);

    given(userReferenceDataService.findOne(contactDetails.getReferenceDataUserId()))
        .willReturn(null);

    // when
    controller.sendNotification(notificationDto, bindingResult);

    // then
    // the exception should be thrown
  }

  @Test
  public void shouldThrowExceptionWhenUserIsNotActive() {
    // given
    exception.expect(ValidationException.class);
    exception.expectMessage(ERROR_USER_NOT_ACTIVE_OR_NOT_FOUND);

    userDto.setActive(false);

    // when
    controller.sendNotification(notificationDto, bindingResult);

    // then
    // the exception should be thrown
  }
}
