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

package org.siglus.siglusapi.service.android;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEmitter;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.UnusedPrivateField")
public class MeCreateRequisitionServiceTest {

  @InjectMocks
  private MeCreateRequisitionService meCreateRequisitionService;

  @Mock
  private RequisitionCreateService requisitionCreateService;
  @Mock
  private AndroidRequisitionSyncedEmitter androidRequisitionSyncedEmitter;

  @Test
  public void shouldSuccessWhenCreateRequisition() {
    // given
    RequisitionCreateRequest request = new RequisitionCreateRequest();
    UUID requisitionId = UUID.randomUUID();
    when(requisitionCreateService.createRequisition(request)).thenReturn(requisitionId);
    // when
    UUID createdRequisitionId = meCreateRequisitionService.createRequisition(request);
    // then
    assertThat(createdRequisitionId).isEqualTo(requisitionId);
  }
}
