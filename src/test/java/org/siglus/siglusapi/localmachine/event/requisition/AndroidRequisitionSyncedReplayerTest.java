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

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEvent;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedReplayer;
import org.siglus.siglusapi.service.android.RequisitionCreateService;
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

  private final UUID requisitionId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccess() {
    // given
    final RequisitionCreateRequest req = new RequisitionCreateRequest();
    final AndroidRequisitionSyncedEvent event = new AndroidRequisitionSyncedEvent(facilityId, userId, requisitionId,
        req, "MTB.xxx.202211.", 3);
    // when
    androidRequisitionSyncedReplayer.replay(event);

  }

}
