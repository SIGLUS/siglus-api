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

package org.openlmis.notification.service;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.openlmis.notification.i18n.MessageKeys.PERMISSION_MISSING;
import static org.openlmis.notification.i18n.MessageKeys.PERMISSION_MISSING_GENERIC;
import static org.openlmis.notification.testutils.OAuth2AuthenticationDataBuilder.SERVICE_CLIENT_ID;

import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.notification.service.referencedata.RightDto;
import org.openlmis.notification.service.referencedata.UserDto;
import org.openlmis.notification.service.referencedata.UserReferenceDataService;
import org.openlmis.notification.testutils.OAuth2AuthenticationDataBuilder;
import org.openlmis.notification.testutils.RightDataBuilder;
import org.openlmis.notification.testutils.UserDataBuilder;
import org.openlmis.notification.util.AuthenticationHelper;
import org.openlmis.notification.web.MissingPermissionException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class PermissionServiceTest {

  private static final String USERS_MANAGE = "USERS_MANAGE";

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private SecurityContext securityContext;

  @InjectMocks
  private PermissionService permissionService;

  private UserDto userDto;
  private RightDto rightDto;
  private OAuth2Authentication serviceAuthentication;
  private OAuth2Authentication userAuthentication;

  @Before
  public void setUp() {
    SecurityContextHolder.setContext(securityContext);
    serviceAuthentication = new OAuth2AuthenticationDataBuilder().buildServiceAuthentication();
    userAuthentication = new OAuth2AuthenticationDataBuilder().buildUserAuthentication();
    userDto = new UserDataBuilder().build();
    rightDto = new RightDataBuilder().build();
    ReflectionTestUtils.setField(permissionService, "serviceTokenClientId", SERVICE_CLIENT_ID);
  }

  @Test
  public void shouldAllowOtherServiceToGetUserContactDetails() {
    when(securityContext.getAuthentication()).thenReturn(serviceAuthentication);

    permissionService.canManageUserContactDetails(UUID.randomUUID());
  }

  @Test
  public void shouldAllowUserWithUsersManageRightToGetUserContactDetails() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(authenticationHelper.getRight(eq(USERS_MANAGE))).thenReturn(rightDto);
    when(userReferenceDataService.hasRight(userDto.getId(), rightDto.getId(), null, null, null))
        .thenReturn(new ResultDto<>(true));

    permissionService.canManageUserContactDetails(UUID.randomUUID());
  }

  @Test
  public void shouldAllowUserWithoutUsersManageRightToGetOwnUserContactDetails() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(authenticationHelper.getRight(eq(USERS_MANAGE))).thenReturn(rightDto);
    when(userReferenceDataService.hasRight(userDto.getId(), rightDto.getId(), null, null, null))
        .thenReturn(new ResultDto<>(false));

    permissionService.canManageUserContactDetails(userDto.getId());
  }

  @Test
  public void shouldNotAllowUserWithoutUsersManageRightToGetUserContactDetails() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(authenticationHelper.getRight(eq(USERS_MANAGE))).thenReturn(rightDto);
    when(userReferenceDataService.hasRight(userDto.getId(), rightDto.getId(), null, null, null))
        .thenReturn(new ResultDto<>(false));

    expectException();

    permissionService.canManageUserContactDetails(UUID.randomUUID());
  }

  @Test
  public void shouldAllowOtherServiceToManageUserSubscriptions() {
    when(securityContext.getAuthentication()).thenReturn(serviceAuthentication);

    permissionService.canManageUserSubscriptions(UUID.randomUUID());
  }

  @Test
  public void shouldAllowUserWithUsersManageRightToManageUserSubscriptions() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(authenticationHelper.getRight(eq(USERS_MANAGE))).thenReturn(rightDto);
    when(userReferenceDataService.hasRight(userDto.getId(), rightDto.getId(), null, null, null))
        .thenReturn(new ResultDto<>(true));

    permissionService.canManageUserSubscriptions(UUID.randomUUID());
  }

  @Test
  public void shouldAllowUserWithoutUsersManageRightToManageOwnUserSubscriptions() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(authenticationHelper.getRight(eq(USERS_MANAGE))).thenReturn(rightDto);
    when(userReferenceDataService.hasRight(userDto.getId(), rightDto.getId(), null, null, null))
        .thenReturn(new ResultDto<>(false));

    permissionService.canManageUserSubscriptions(userDto.getId());
  }

  @Test
  public void shouldNotAllowUserWithoutUsersManageRightToManageOtherUserSubscriptions() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(authenticationHelper.getRight(eq(USERS_MANAGE))).thenReturn(rightDto);
    when(userReferenceDataService.hasRight(userDto.getId(), rightDto.getId(), null, null, null))
        .thenReturn(new ResultDto<>(false));

    expectException();

    permissionService.canManageUserSubscriptions(UUID.randomUUID());
  }

  @Test
  public void shouldNotAllowToSendNotificationForUsers() {
    when(securityContext.getAuthentication()).thenReturn(userAuthentication);
    expectGenericException();

    permissionService.canSendNotification();
  }

  @Test
  public void shouldAllowToSendNotificationForServiceLevelToken() {
    when(securityContext.getAuthentication()).thenReturn(serviceAuthentication);
    permissionService.canSendNotification();
  }

  private void expectException() {
    exception.expect(MissingPermissionException.class);
    exception.expect(hasProperty("params", arrayContaining(USERS_MANAGE)));
    exception.expectMessage(PERMISSION_MISSING);
  }

  private void expectGenericException() {
    exception.expect(MissingPermissionException.class);
    exception.expectMessage(PERMISSION_MISSING_GENERIC);
  }

}
