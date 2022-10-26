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

package org.siglus.siglusapi.web;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionsProcessingStatusDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.localmachine.event.requisition.web.RequisitionReleaseEmitter;
import org.siglus.siglusapi.service.BatchReleaseRequisitionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class SiglusBatchRequisitionControllerTest {

  @InjectMocks
  private SiglusBatchRequisitionController controller;

  @Mock
  private BatchReleaseRequisitionService batchReleaseRequisitionService;

  private ReleasableRequisitionBatchDto releasableRequisitionBatchDto;

  @Mock
  private RequisitionReleaseEmitter requisitionReleaseEmitter;

  @Mock
  private SiglusAuthenticationHelper siglusAuthenticationHelper;

  private final UUID userId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID supplyingDepotId = UUID.randomUUID();

  @Before
  public void prepare() {
    releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
    releasableRequisitionBatchDto.setCreateOrder(false);
    ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
    releasableRequisitionDto.setRequisitionId(requisitionId);
    releasableRequisitionDto.setSupplyingDepotId(supplyingDepotId);
    releasableRequisitionBatchDto.setRequisitionsToRelease(Lists.newArrayList(releasableRequisitionDto));
    UserDto user = new UserDto();
    user.setId(userId);
    when(siglusAuthenticationHelper.getCurrentUser()).thenReturn(user);
  }

  @Test
  public void shouldCallOpenlmisControllerWhenBatchReleaseRequisitions() {
    // given
    RequisitionsProcessingStatusDto dto = new RequisitionsProcessingStatusDto();
    when(batchReleaseRequisitionService.getRequisitionsProcessingStatusDtoResponse(any()))
        .thenReturn(new ResponseEntity<>(dto, HttpStatus.OK));

    // when
    controller.batchReleaseRequisitions(releasableRequisitionBatchDto);

    // then
    verify(requisitionReleaseEmitter, times(1)).emit(any(), any());
    verify(batchReleaseRequisitionService).getRequisitionsProcessingStatusDtoResponse(releasableRequisitionBatchDto);
  }

}