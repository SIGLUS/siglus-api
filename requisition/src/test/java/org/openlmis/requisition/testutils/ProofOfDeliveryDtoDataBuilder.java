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

package org.openlmis.requisition.testutils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.ProofOfDeliveryLineItemDto;
import org.openlmis.requisition.dto.ProofOfDeliveryStatus;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class ProofOfDeliveryDtoDataBuilder implements DtoDataBuilder<ProofOfDeliveryDto> {

  private ObjectReferenceDto shipment;
  private ProofOfDeliveryStatus status;
  private List<ProofOfDeliveryLineItemDto> lineItems;
  private String receivedBy;
  private String deliveredBy;
  private LocalDate receivedDate;

  /**
   * Builder for {@link ProofOfDeliveryDto}.
   */
  public ProofOfDeliveryDtoDataBuilder() {
    this.shipment = new ObjectReferenceDtoDataBuilder().buildAsDto();
    this.status = ProofOfDeliveryStatus.INITIATED;
    this.lineItems = new ArrayList<>();
    this.receivedBy = "someone";
    this.deliveredBy = "human";
    this.receivedDate = LocalDate.now();
  }

  @Override
  public ProofOfDeliveryDto buildAsDto() {
    return new ProofOfDeliveryDto(
        shipment,
        status,
        lineItems,
        receivedBy,
        deliveredBy,
        receivedDate
    );
  }
}
