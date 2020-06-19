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

package org.siglus.siglusapi.service.client;

import static org.siglus.siglusapi.constant.FieldConstants.EXPAND;
import static org.siglus.siglusapi.constant.FieldConstants.ORDER_ID;

import java.util.Set;
import java.util.UUID;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SiglusShipmentDraftFulfillmentService extends
    BaseFulfillmentService<ShipmentDraftDto> {

  @Override
  protected String getUrl() {
    return "/api/shipmentDrafts/";
  }

  @Override
  protected Class<ShipmentDraftDto> getResultClass() {
    return ShipmentDraftDto.class;
  }

  @Override
  protected Class<ShipmentDraftDto[]> getArrayResultClass() {
    return ShipmentDraftDto[].class;
  }

  public ShipmentDraftDto updateShipmentDraft(UUID id, ShipmentDraftDto draftDto) {
    return put(id.toString(), draftDto, getResultClass(), true);
  }

  public Page<ShipmentDraftDto> searchShipmentDrafts(UUID orderId, Pageable pageable) {
    RequestParameters queryParams = RequestParameters.init()
        .set(ORDER_ID, orderId)
        .setPage(pageable);
    return getPage("", queryParams, null, HttpMethod.GET, getResultClass(), true);
  }

  public ShipmentDraftDto searchShipmentDraft(UUID id, Set<String> expand) {
    RequestParameters queryParams = RequestParameters.init().set(EXPAND, expand);
    return findOne(id.toString(), queryParams, true);
  }

  public void deleteShipmentDraft(UUID id) {
    delete(id.toString(), true);
  }
}
