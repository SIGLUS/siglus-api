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

package org.siglus.siglusapi.service.scheduledtask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;


@RunWith(MockitoJUnitRunner.class)
public class SiglusRequisitionAutoCloseServiceTest {
  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @InjectMocks
  private SiglusRequisitionAutoCloseService autoCloseService;

  private final UUID targetPeriodId = UUID.randomUUID();
  private final UUID targetPreviousPeriodId = UUID.randomUUID();
  private final UUID targetLaterPeriodId = UUID.randomUUID();
  private final LocalDate targetPeriodEndDate = LocalDate.of(2023, 8, 20);
  private final LocalDate targetPreviousPeriodEndDate = LocalDate.of(2023, 7, 20);
  private final LocalDate targetLaterPeriodEndDate = LocalDate.of(2023, 9, 20);

  @Test
  public void shouldCloseRequisitionWhenItHasLaterRequisition() {
    // given
    when(periodReferenceDataService.searchByProgramAndFacility(any(), any()))
      .thenReturn(buildPeriodDtos());

    // when
    boolean shouldClose = autoCloseService.needCloseRequisition(
        buildTargetRequisition(),
        buildLaterGroupRequisitions()
    );

    // then
    assertTrue(shouldClose);
  }

  @Test
  public void shouldNotCloseRequisitionWhenItOnlyHasPreviousRequisition() {
    // given
    when(periodReferenceDataService.searchByProgramAndFacility(any(), any()))
      .thenReturn(buildPeriodDtos());

    // when
    boolean shouldClose = autoCloseService.needCloseRequisition(
        buildTargetRequisition(),
        buildPreviousGroupRequisitions()
    );

    // then
    assertFalse(shouldClose);
  }

  @Test
  public void shouldNotCloseRequisitionWhenItHasLaterRequisitionButNoPeriodOrBeforeApprovalStatus() {
    // given
    when(periodReferenceDataService.searchByProgramAndFacility(any(), any()))
      .thenReturn(buildPeriodDtos());

    // when
    boolean shouldClose = autoCloseService.needCloseRequisition(
        buildTargetRequisition(),
        buildLaterGroupNoPeriodOrNoStatusRequisitions()
    );

    // then
    assertFalse(shouldClose);
  }

  private List<Requisition> buildPreviousGroupRequisitions() {
    Requisition requisition = new Requisition();
    requisition.setProcessingPeriodId(targetPreviousPeriodId);
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    return Arrays.asList(requisition, buildTargetRequisition());
  }

  private List<Requisition> buildLaterGroupRequisitions() {
    Requisition requisition = new Requisition();
    requisition.setProcessingPeriodId(targetLaterPeriodId);
    requisition.setStatus(RequisitionStatus.RELEASED);
    return Arrays.asList(requisition, buildTargetRequisition());
  }

  private List<Requisition> buildLaterGroupNoPeriodOrNoStatusRequisitions() {
    Requisition requisition1 = new Requisition();
    requisition1.setProcessingPeriodId(targetLaterPeriodId);
    requisition1.setStatus(RequisitionStatus.AUTHORIZED);
    Requisition requisition2 = new Requisition();
    requisition2.setStatus(RequisitionStatus.RELEASED);
    return Arrays.asList(requisition1, requisition2, buildTargetRequisition());
  }

  private Requisition buildTargetRequisition() {
    Requisition targetRequisition = new Requisition();
    targetRequisition.setProcessingPeriodId(targetPeriodId);
    targetRequisition.setStatus(RequisitionStatus.IN_APPROVAL);
    return targetRequisition;
  }

  private Collection<ProcessingPeriodDto> buildPeriodDtos() {
    ProcessingPeriodDto periodDto1 = new ProcessingPeriodDto();
    periodDto1.setId(targetPeriodId);
    periodDto1.setEndDate(targetPeriodEndDate);

    ProcessingPeriodDto periodDto2 = new ProcessingPeriodDto();
    periodDto2.setId(targetPreviousPeriodId);
    periodDto2.setEndDate(targetPreviousPeriodEndDate);

    ProcessingPeriodDto periodDto3 = new ProcessingPeriodDto();
    periodDto3.setId(targetLaterPeriodId);
    periodDto3.setEndDate(targetLaterPeriodEndDate);
    return Arrays.asList(periodDto1, periodDto2, periodDto3);
  }
}
