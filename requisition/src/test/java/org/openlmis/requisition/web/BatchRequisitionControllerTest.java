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

package org.openlmis.requisition.web;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.ReleasableRequisitionBatchDtoDataBuilder;
import org.openlmis.requisition.utils.AuthenticationHelper;

public class BatchRequisitionControllerTest {

  @Mock
  RequisitionService requisitionService;

  @Mock
  AuthenticationHelper authenticationHelper;

  @Mock
  PermissionService permissionService;

  @InjectMocks
  BatchRequisitionController batchRequisitionController;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    UserDto currentUser = DtoGenerator.of(UserDto.class);
    when(authenticationHelper.getCurrentUser()).thenReturn(currentUser);
  }

  @Test
  public void batchReleaseRequisitionsWithOrderWhenUserHasPermission() {
    doReturn(ValidationResult.success())
        .when(permissionService).canConvertToOrder(anyList());
    when(requisitionService.convertToOrder(any(), any()))
        .thenReturn(new ArrayList<>());
    when(requisitionService.releaseWithoutOrder(any()))
        .thenReturn(new ArrayList<>());

    ReleasableRequisitionBatchDto releasableBatchDto =
        new ReleasableRequisitionBatchDtoDataBuilder()
            .withCreateOrder(true)
            .buildAsDto();
    batchRequisitionController.batchReleaseRequisitions(releasableBatchDto);

    verify(requisitionService, atLeastOnce()).convertToOrder(any(), any());
    verify(requisitionService, never()).releaseWithoutOrder(any());
  }

  @Test
  public void batchReleaseRequisitionsWithoutOrderWhenUserHasPermission() {
    doReturn(ValidationResult.success())
        .when(permissionService).canConvertToOrder(anyList());
    when(requisitionService.convertToOrder(any(), any()))
        .thenReturn(new ArrayList<>());
    when(requisitionService.releaseWithoutOrder(any()))
        .thenReturn(new ArrayList<>());

    ReleasableRequisitionBatchDto releasableBatchDto =
        new ReleasableRequisitionBatchDtoDataBuilder()
            .withCreateOrder(false)
            .buildAsDto();
    batchRequisitionController.batchReleaseRequisitions(releasableBatchDto);

    verify(requisitionService, never()).convertToOrder(any(), any());
    verify(requisitionService, atLeastOnce()).releaseWithoutOrder(any());
  }

}