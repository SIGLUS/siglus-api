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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.service.PermissionService;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class OperatePermissionServiceTest {

  @InjectMocks
  OperatePermissionService operatePermissionService;

  @Mock
  PermissionService permissionService;

  private RequisitionV2Dto dto;


  @Before
  public void prepare() {
    dto = new RequisitionV2Dto();
    dto.setId(UUID.randomUUID());
    ObjectReferenceDto program = new ObjectReferenceDto(UUID.randomUUID(), "", "");
    dto.setProgram(program);
    ObjectReferenceDto facility = new ObjectReferenceDto(UUID.randomUUID(), "", "");
    dto.setFacility(facility);
    dto.setStatus(RequisitionStatus.INITIATED);
    dto.setSupervisoryNode(UUID.randomUUID());
  }

  @Test
  public void shouldReturnTrueWhenStatusSubmittableAndCanSubmit() {
    // given
    dto.setStatus(RequisitionStatus.INITIATED);

    //when
    ValidationResult validationResult = new ValidationResult();
    when(permissionService.canSubmitRequisition(any(Requisition.class)))
        .thenReturn(validationResult);

    // then
    assertEquals(true, operatePermissionService.isEditable(dto));
    assertEquals(true, operatePermissionService.canSubmit(dto));
  }

  @Test
  public void shouldReturnTrueWhenStatusSubmittedAndCanAuthorize() {
    // given
    dto.setStatus(RequisitionStatus.SUBMITTED);

    //when
    ValidationResult validationResult = new ValidationResult();
    when(permissionService.canAuthorizeRequisition(any(Requisition.class)))
        .thenReturn(validationResult);

    // then
    assertEquals(true, operatePermissionService.isEditable(dto));
  }

  @Test
  public void shouldReturnTrueWhenStatusAuthorizedAndCanApprove() {
    // given
    dto.setStatus(RequisitionStatus.AUTHORIZED);

    //when
    ValidationResult validationResult = new ValidationResult();
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(validationResult);

    // then
    assertEquals(true, operatePermissionService.isEditable(dto));
  }

  @Test
  public void shouldReturnFalseWhenStatusRejected() {
    // given
    dto.setStatus(RequisitionStatus.RELEASED);

    //when
    ValidationResult validationResult = new ValidationResult();
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(validationResult);

    // then
    assertEquals(false, operatePermissionService.isEditable(dto));
  }

  @Test
  public void shouldReturnFalseWhenStatusAuthorizedAndCanSubmit() {
    // given
    dto.setStatus(RequisitionStatus.AUTHORIZED);

    // then
    assertEquals(false, operatePermissionService.canSubmit(dto));
  }

}
