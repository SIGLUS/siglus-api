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

package org.siglus.siglusapi.localmachine.event.order;

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
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.siglus.siglusapi.localmachine.event.order.release.OrderReleaseEvent;
import org.siglus.siglusapi.localmachine.event.order.release.OrderReleaseReplayer;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class OrderReleaseReplayerTest {

  @InjectMocks
  private OrderReleaseReplayer orderReleaseReplayer;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private StatusChangeRepository statusChangeRepository;


  private final UUID requisitionId = UUID.randomUUID();
  private final UUID supplyingDepotId = UUID.randomUUID();
  private final UUID authorId = UUID.randomUUID();

  @Test
  public void shouldDoReplaySuccessWhenOrderReleaseEventReceived() {
    // given
    OrderReleaseEvent orderReleaseEvent = mockOrderReleaseEvent();
    when(requisitionRepository.findOne(requisitionId)).thenReturn(new Requisition());

    // when
    orderReleaseReplayer.replay(orderReleaseEvent);

    // then
    verify(statusChangeRepository, times(1)).save(any(StatusChange.class));
  }

  private OrderReleaseEvent mockOrderReleaseEvent() {
    return OrderReleaseEvent
        .builder()
        .authorId(authorId)
        .supplyingDepotId(supplyingDepotId)
        .requisitionId(requisitionId)
        .build();
  }
}
