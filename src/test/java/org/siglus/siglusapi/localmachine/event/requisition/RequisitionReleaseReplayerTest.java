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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.localmachine.event.requisition.web.RequisitionReleaseEvent;
import org.siglus.siglusapi.localmachine.event.requisition.web.RequisitionReleaseReplayer;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class RequisitionReleaseReplayerTest {

  @InjectMocks
  private RequisitionReleaseReplayer requisitionReleaseReplayer;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;


  private final String requisitionNumber = "requisitionNumber";
  private final UUID supplyingDepotId = UUID.randomUUID();
  private final UUID authorId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccessWhenOrderReleaseEventReceived() {
    // given
    RequisitionReleaseEvent requisitionReleaseEvent = mockOrderReleaseEvent();
    when(requisitionRepository.findOne(any(UUID.class))).thenReturn(new Requisition());
    when(requisitionExtensionRepository.findByRequisitionNumber(any())).thenReturn(new RequisitionExtension());

    // when
    requisitionReleaseReplayer.replay(requisitionReleaseEvent);

    // then
    verify(requisitionRepository, times(1)).findOne(any(UUID.class));
  }

  private RequisitionReleaseEvent mockOrderReleaseEvent() {
    return RequisitionReleaseEvent
        .builder()
        .authorId(authorId)
        .supplyingDepotId(supplyingDepotId)
        .requisitionNumber(requisitionNumber)
        .build();
  }
}
