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

package org.siglus.siglusapi.localmachine.event.requisition;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEvent;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedReplayer;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.android.RequisitionCreateService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;


@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class AndroidRequisitionSyncedReplayerTest {

  @InjectMocks
  private AndroidRequisitionSyncedReplayer androidRequisitionSyncedReplayer;
  @Mock
  private SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  @Mock
  private RequisitionCreateService requisitionCreateService;
  @Mock
  private SiglusRequisitionRepository requisitionRepository;
  @Mock
  private SiglusProgramService siglusProgramService;
  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private SyncUpHashRepository syncUpHashRepository;

  private final UUID requisitionId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccess() {
    // given
    final RequisitionCreateRequest req = new RequisitionCreateRequest();
    final AndroidRequisitionSyncedEvent event = new AndroidRequisitionSyncedEvent(facilityId, userId, requisitionId,
        req, "MTB.xxx.202211.", 3);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(UUID.randomUUID());
    when(siglusProgramService.getProgramByCode(any())).thenReturn(Optional.of(programDto));
    UUID processingPeriodId = UUID.randomUUID();
    when(requisitionCreateService.getPeriodId(any())).thenReturn(processingPeriodId);
    when(requisitionRepository.findOneByFacilityIdAndProgramIdAndProcessingPeriodId(any(), any(), any()))
        .thenReturn(null);
    // when
    androidRequisitionSyncedReplayer.replay(event);

  }

}
