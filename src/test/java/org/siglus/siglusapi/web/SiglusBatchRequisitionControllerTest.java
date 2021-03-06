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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ReleasableRequisitionBatchDto;
import org.openlmis.requisition.dto.RequisitionsProcessingStatusDto;
import org.openlmis.requisition.web.BatchRequisitionController;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class SiglusBatchRequisitionControllerTest {

  @InjectMocks
  private SiglusBatchRequisitionController controller;

  @Mock
  private BatchRequisitionController actualController;

  private ReleasableRequisitionBatchDto releasableRequisitionBatchDto;

  @Mock
  @SuppressWarnings("unused")
  private SiglusNotificationService notificationService;

  @Before
  public void prepare() {
    releasableRequisitionBatchDto = new ReleasableRequisitionBatchDto();
  }

  @Test
  public void shouldCallOpenlmisControllerWhenBatchReleaseRequisitions() {
    RequisitionsProcessingStatusDto dto = new RequisitionsProcessingStatusDto();
    when(actualController.batchReleaseRequisitions(any()))
        .thenReturn(new ResponseEntity<>(dto, HttpStatus.OK));
    controller.batchReleaseRequisitions(releasableRequisitionBatchDto);

    verify(actualController).batchReleaseRequisitions(releasableRequisitionBatchDto);
  }

}