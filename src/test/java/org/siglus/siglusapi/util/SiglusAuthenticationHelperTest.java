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

package org.siglus.siglusapi.util;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.DetailedRoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.FacilitySupplierLevelRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusAuthenticationHelperTest {
  private final SecurityContext securityContext = mock(SecurityContext.class);
  private final Authentication authentication = mock(OAuth2Authentication.class);
  @Mock private SiglusUserReferenceDataService referenceDataService;
  @Mock private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Mock private FacilitySupplierLevelRepository facilitySupplierLevelRepository;
  private SiglusAuthenticationHelper authenticationHelper;

  @Before
  public void setup() {
    SecurityContextHolder.setContext(securityContext);
    given(securityContext.getAuthentication()).willReturn(authentication);
    authenticationHelper = new SiglusAuthenticationHelper(
        referenceDataService,
        facilitySupplierLevelRepository,
        siglusFacilityReferenceDataService);
  }

  @Test
  public void shouldReturnFalseWhenCheckIfMigrationUserGivenUserNotHaveMigrateDataAuthority() {
    assertThat(authenticationHelper.isTheDataMigrationUser()).isFalse();
  }

  @Test
  public void shouldReturnUsernameWhenGetUsernameGivenUserIdIsNotNull() {
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto();
    userDto.setUsername("username");
    given(referenceDataService.getUserDetailDto(userId)).willReturn(userDto);

    assertThat(authenticationHelper.getUserNameByUserId(userId)).isEqualTo(userDto.getUsername());
  }

  @Test
  public void shouldReturnEmptyStrWhenGetUsernameGivenUserIdIsNull() {
    assertThat(authenticationHelper.getUserNameByUserId(null)).isEqualTo("");
  }

  @SneakyThrows
  @Test
  public void shouldCanMergeOrDeleteSubDraftsGivenRoleIdInWhitelisted() {
    String roleId = UUID.randomUUID().toString();
    SiglusAuthenticationHelper authenticationHelper = configureCurrentUserRoleId(roleId);
    ReflectionTestUtils.setField(authenticationHelper, "role2WareHouseManager", roleId);

    assertThat(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).isTrue();
  }

  @Test
  public void shouldReturnNullWhenGetUserRightsAndRolesGivenCurrentUserIdIsEmpty() {
    SiglusAuthenticationHelper authenticationHelper = mock(SiglusAuthenticationHelper.class);
    given(authenticationHelper.getCurrentUserId()).willReturn(Optional.empty());
    given(authenticationHelper.getUserRightsAndRoles()).willCallRealMethod();

    Collection<DetailedRoleAssignmentDto> userRightsAndRoles =
        authenticationHelper.getUserRightsAndRoles();

    assertThat(userRightsAndRoles).isNull();
  }

  @Test
  public void shouldReturnUserRightsAndRolesWhenGetUserRightsAndRolesGivenCurrentUserIdNotEmpty() {
    UUID userId = UUID.randomUUID();
    given(authentication.getPrincipal()).willReturn(userId);
    Set<DetailedRoleAssignmentDto> detailedRoleAssignmentDtos =
        singleton(mock(DetailedRoleAssignmentDto.class));
    given(referenceDataService.getUserRightsAndRoles(userId))
        .willReturn(detailedRoleAssignmentDtos);

    Collection<DetailedRoleAssignmentDto> userRightsAndRoles =
        authenticationHelper.getUserRightsAndRoles();

    assertThat(userRightsAndRoles).isEqualTo(detailedRoleAssignmentDtos);
  }

  @Test
  public void shouldReturnNullWhenGetCurrentUserGivenCurrentUserIdIsEmpty() {
    SiglusAuthenticationHelper authenticationHelper = mock(SiglusAuthenticationHelper.class);
    given(authenticationHelper.getCurrentUserId()).willReturn(Optional.empty());
    given(authenticationHelper.getCurrentUser()).willCallRealMethod();

    UserDto user = authenticationHelper.getCurrentUser();

    assertThat(user).isNull();
  }

  @Test
  public void shouldReturnUserWhenGetCurrentUserGivenCurrentUserIdNotEmpty() {
    UUID userId = UUID.randomUUID();
    given(authentication.getPrincipal()).willReturn(userId);
    UserDto currentUser = mock(UserDto.class);
    given(referenceDataService.findOne(userId)).willReturn(currentUser);

    UserDto user = authenticationHelper.getCurrentUser();

    assertThat(user).isEqualTo(currentUser);
  }

  @Test
  public void shouldReturnEmptyWhenGetCurrentUserIdGivenPrincipalIsNull() {
    assertThat(authenticationHelper.getCurrentUserId()).isEqualTo(Optional.empty());
  }

  @Test
  public void shouldReturnEmptyWhenGetCurrentUserIdGivenPrincipalIsTrustedClient() {
    given(authentication.getPrincipal()).willReturn("trusted-client");

    assertThat(authenticationHelper.getCurrentUserId()).isEqualTo(Optional.empty());
  }

  @Test
  public void shouldReturnUserIdWhenGetCurrentUserIdGivenPrincipalNormalUserId() {
    given(authentication.getPrincipal()).willReturn(UUID.randomUUID());

    assertThat(authenticationHelper.getCurrentUserId().get())
        .isEqualTo(authentication.getPrincipal());
  }

  private SiglusAuthenticationHelper configureCurrentUserRoleId(String roleId) {
    SiglusAuthenticationHelper authenticationHelper = mock(SiglusAuthenticationHelper.class);
    DetailedRoleAssignmentDto detailedRoleAssignmentDto = mock(DetailedRoleAssignmentDto.class);
    RoleDto roleDto = new RoleDto();
    roleDto.setId(UUID.fromString(roleId));
    given(detailedRoleAssignmentDto.getRole()).willReturn(roleDto);
    given(authenticationHelper.getUserRightsAndRoles())
        .willReturn(singleton(detailedRoleAssignmentDto));
    given(authenticationHelper.isTheCurrentUserCanMergeOrDeleteSubDrafts()).willCallRealMethod();
    return authenticationHelper;
  }
}
