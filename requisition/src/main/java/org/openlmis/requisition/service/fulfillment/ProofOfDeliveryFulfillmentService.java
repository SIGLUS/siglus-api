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

package org.openlmis.requisition.service.fulfillment;

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class ProofOfDeliveryFulfillmentService extends BaseFulfillmentService<ProofOfDeliveryDto> {

  @Override
  protected String getUrl() {
    return "/api/proofsOfDelivery/";
  }

  @Override
  protected Class<ProofOfDeliveryDto> getResultClass() {
    return ProofOfDeliveryDto.class;
  }

  @Override
  protected Class<ProofOfDeliveryDto[]> getArrayResultClass() {
    return ProofOfDeliveryDto[].class;
  }

  /**
   * Finds proofs of delivery related with the given order.
   */
  public List<ProofOfDeliveryDto> getProofOfDeliveries(UUID orderId) {
    return getPage(RequestParameters.init().set("orderId", orderId)).getContent();
  }

}
