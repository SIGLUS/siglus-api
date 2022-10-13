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

import java.io.IOException;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.web.util.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;

@RunWith(MockitoJUnitRunner.class)
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
  @Mock
  private SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

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
  public void shouldPostConfirmPodSuccess() throws IOException {
    // given
    ProofOfDeliveryDto proofOfDeliveryDto = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER.readValue("{\"shipment\": {\"href\": " +
        "\"null/api/shipments/\", \"order\": {\"externalId\": null, \"emergency\": null, \"facility\": {\"code\": " +
        "\"\", \"name\": \"\", \"description\": \"\", \"active\": null, \"goLiveDate\": \"2022-10-12\", " +
        "\"goDownDate\": \"2022-10-12\", \"comment\": \"\", \"enabled\": null, \"openLmisAccessible\": null, " +
        "\"supportedPrograms\": [{\"code\": \"\", \"name\": \"\", \"description\": \"\", \"active\": null, " +
        "\"periodsSkippable\": null, \"showNonFullSupplyTab\": null, \"supportLocallyFulfilled\": {}, \"id\": null}]," +
        " \"geographicZone\": {\"code\": \"\", \"name\": \"\", \"level\": {\"code\": \"\", \"name\": \"\", " +
        "\"levelNumber\": 1, \"id\": null}, \"parent\": null, \"id\": null}, \"operator\": {\"code\": \"\", \"name\":" +
        " \"\", \"id\": null}, \"type\": {\"code\": \"\", \"name\": \"\", \"description\": \"\", \"displayOrder\": 1," +
        " \"active\": null, \"id\": null}, \"id\": null}, \"processingPeriod\": {\"processingSchedule\": {\"code\": " +
        "\"\", \"description\": \"\", \"modifiedDate\": null, \"name\": \"\", \"id\": null}, \"name\": \"\", " +
        "\"description\": \"\", \"startDate\": \"2022-10-12\", \"endDate\": \"2022-10-12\", \"id\": null}, " +
        "\"createdDate\": null, \"program\": null, \"requestingFacility\": {}, \"supplyingFacility\": {}, " +
        "\"lastUpdatedDate\": null, \"requisitionNumber\": \"\", \"id\": null, \"href\": \"\"}}}", ProofOfDeliveryDto.class);
    // when
    notificationService.postConfirmPod(userId, proofOfDeliveryDto);
  }

}

