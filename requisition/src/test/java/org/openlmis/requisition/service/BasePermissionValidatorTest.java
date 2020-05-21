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

package org.openlmis.requisition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.service.OAuth2AuthenticationDataBuilder.SERVICE_CLIENT_ID;
import static org.openlmis.requisition.service.OAuth2AuthenticationDataBuilder.asApiKey;
import static org.openlmis.requisition.service.OAuth2AuthenticationDataBuilder.asClient;
import static org.openlmis.requisition.service.OAuth2AuthenticationDataBuilder.asService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.util.ReflectionTestUtils;

abstract class BasePermissionValidatorTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  AuthenticationHelper authenticationHelper;

  @InjectMocks
  RightAssignmentPermissionValidator validator;

  private SecurityContext securityContext;

  private OAuth2Authentication trustedClient;
  private OAuth2Authentication userClient;
  private OAuth2Authentication apiKeyClient;

  @Before
  public void setUp() {
    securityContext = mock(SecurityContext.class);
    SecurityContextHolder.setContext(securityContext);

    trustedClient = asService();
    userClient = asClient("admin");
    apiKeyClient = asApiKey();

    when(securityContext.getAuthentication()).thenReturn(userClient);

    ReflectionTestUtils.setField(validator, "serviceTokenClientId", SERVICE_CLIENT_ID);
  }

  @Test
  public void serviceLevelTokensShouldHaveAllThePermissions() {
    when(securityContext.getAuthentication()).thenReturn(trustedClient);

    // General permission
    assertThat(validator.hasPermission(getDetailsForGeneralPermission()).isSuccess()).isTrue();

    // Supervision permission
    assertThat(validator.hasPermission(getDetailsForSupervisionPermission()).isSuccess()).isTrue();

    // Fulfillment permission
    assertThat(validator.hasPermission(getDetailsForFulfillmentPermission()).isSuccess()).isTrue();
  }

  @Test
  public void serviceLevelTokensShouldNotHaveAnyPermissionIfIdsDoesNotMatch() {
    when(securityContext.getAuthentication()).thenReturn(trustedClient);
    ReflectionTestUtils.setField(validator, "serviceTokenClientId", "test");

    // General permission
    assertThat(validator.hasPermission(getDetailsForGeneralPermission()).isSuccess()).isFalse();

    // Supervision permission
    assertThat(validator.hasPermission(getDetailsForSupervisionPermission()).isSuccess()).isFalse();

    // Fulfillment permission
    assertThat(validator.hasPermission(getDetailsForFulfillmentPermission()).isSuccess()).isFalse();
  }

  @Test
  public void apiKeyTokensShouldNotHaveAllThePermissions() {
    when(securityContext.getAuthentication()).thenReturn(apiKeyClient);

    // General permission
    assertThat(validator.hasPermission(getDetailsForGeneralPermission()).isSuccess()).isFalse();

    // Supervision permission
    assertThat(validator.hasPermission(getDetailsForSupervisionPermission()).isSuccess()).isFalse();

    // Fulfillment permission
    assertThat(validator.hasPermission(getDetailsForFulfillmentPermission()).isSuccess()).isFalse();
  }

  abstract PermissionValidationDetails getDetailsForGeneralPermission();

  abstract PermissionValidationDetails getDetailsForSupervisionPermission();

  abstract PermissionValidationDetails getDetailsForFulfillmentPermission();


}
