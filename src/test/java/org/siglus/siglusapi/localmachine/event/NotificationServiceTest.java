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

package org.siglus.siglusapi.localmachine.event;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class NotificationServiceTest {

  @InjectMocks
  private NotificationService notificationService;

  @Mock
  private SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private OrderExternalRepository orderExternalRepository;
  @Mock
  private RequisitionRepository requisitionRepository;

  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();

  @Test
  public void shouldPostInternalApprovalSuccess() {
    // given
    final BasicRequisitionDto r = new BasicRequisitionDto();
    r.setFacility(new MinimalFacilityDto());
    r.setProgram(new ProgramDto());
    r.setProcessingPeriod(new BasicProcessingPeriodDto());
    // when
    notificationService.postInternalApproval(userId, r, UUID.randomUUID());
  }

  @Test
  public void shouldPostFulfillmentSuccess() {
    // given
    final Order order = new Order();
    when(requisitionRepository.findOne(any(UUID.class))).thenReturn(new Requisition());
    // when
    notificationService.postFulfillment(userId, UUID.randomUUID(), order);
  }

  @Test
  public void shouldPostRejectSuccess() {
    // given
    final BasicRequisitionDto r = new BasicRequisitionDto();
    r.setFacility(new MinimalFacilityDto());
    r.setProgram(new ProgramDto());
    r.setProcessingPeriod(new BasicProcessingPeriodDto());
    // when
    notificationService.postReject(userId, r);
  }

  @Test
  public void shouldPostConfirmPodSuccess() {
    // given
    Order orderDto = new Order(UUID.randomUUID());
    // when
    notificationService.postConfirmPod(userId, UUID.randomUUID(), orderDto);
  }

}

