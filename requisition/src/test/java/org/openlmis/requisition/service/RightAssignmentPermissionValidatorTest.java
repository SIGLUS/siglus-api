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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ResultDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;

public class RightAssignmentPermissionValidatorTest
    extends BasePermissionValidatorTest {

  private static final String RIGHT_NAME = "RIGHT_NAME";

  private static final UUID FACILITY_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID WAREHOUSE_ID = UUID.randomUUID();

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @InjectMocks
  private RightAssignmentPermissionValidator validator;

  @Mock
  private Requisition requisition;

  private UserDto user = DtoGenerator.of(UserDto.class);
  private RightDto right = DtoGenerator.of(RightDto.class);

  private UUID requisitionId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID supervisoryNodeId = UUID.randomUUID();

  @Before
  public void setUp() {
    super.setUp();

    when(requisition.getId()).thenReturn(requisitionId);
    when(requisition.getProgramId()).thenReturn(programId);
    when(requisition.getFacilityId()).thenReturn(facilityId);
    when(requisition.getSupplyingFacilityId()).thenReturn(facilityId);
    when(requisition.getStatus()).thenReturn(RequisitionStatus.SUBMITTED);
    when(requisition.getSupervisoryNodeId()).thenReturn(supervisoryNodeId);

    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(authenticationHelper.getRight(RIGHT_NAME)).thenReturn(right);
  }

  @Test
  public void userShouldHavePermissionIfUserHasCorrectGeneralRightAssignment() {
    // given
    given(userReferenceDataService.hasRight(user.getId(), right.getId(), null, null, null))
        .willReturn(new ResultDto<>(true));

    // when
    ValidationResult result = validator.hasPermission(getDetailsForGeneralPermission());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void userShouldNotHavePermissionIfUserHasIncorrectGeneralRightAssignment() {
    // given
    given(userReferenceDataService.hasRight(user.getId(), right.getId(), null, null, null))
        .willReturn(new ResultDto<>(false));

    // when
    ValidationResult result = validator.hasPermission(getDetailsForGeneralPermission());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void userShouldHavePermissionIfUserHasCorrectSupervisionRightAssignment() {
    // given
    given(userReferenceDataService.hasRight(user.getId(), right.getId(),
        PROGRAM_ID, FACILITY_ID, null))
        .willReturn(new ResultDto<>(true));

    // when
    ValidationResult result = validator.hasPermission(getDetailsForSupervisionPermission());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void userShouldNotHavePermissionIfUserHasIncorrectSupervisionRightAssignment() {
    // given
    given(userReferenceDataService.hasRight(user.getId(), right.getId(),
        PROGRAM_ID, FACILITY_ID, null))
        .willReturn(new ResultDto<>(false));

    // when
    ValidationResult result = validator.hasPermission(getDetailsForSupervisionPermission());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Test
  public void userShouldHavePermissionIfUserHasCorrectFulfillmentRightAssignment() {
    // given
    given(userReferenceDataService.hasRight(user.getId(), right.getId(), null, null, WAREHOUSE_ID))
        .willReturn(new ResultDto<>(true));

    // when
    ValidationResult result = validator.hasPermission(getDetailsForFulfillmentPermission());

    // then
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  public void userShouldNotHavePermissionIfUserHasIncorrectFulfillmentRightAssignment() {
    // given
    given(userReferenceDataService.hasRight(user.getId(), right.getId(), null, null, WAREHOUSE_ID))
        .willReturn(new ResultDto<>(false));

    // when
    ValidationResult result = validator.hasPermission(getDetailsForFulfillmentPermission());

    // then
    assertThat(result.isSuccess()).isFalse();
  }

  @Override
  PermissionValidationDetails getDetailsForGeneralPermission() {
    return new RightAssignmentPermissionValidationDetails(RIGHT_NAME);
  }

  @Override
  PermissionValidationDetails getDetailsForSupervisionPermission() {
    return new RightAssignmentPermissionValidationDetails(RIGHT_NAME, FACILITY_ID, PROGRAM_ID);
  }

  @Override
  PermissionValidationDetails getDetailsForFulfillmentPermission() {
    return new RightAssignmentPermissionValidationDetails(RIGHT_NAME, WAREHOUSE_ID);
  }
}
