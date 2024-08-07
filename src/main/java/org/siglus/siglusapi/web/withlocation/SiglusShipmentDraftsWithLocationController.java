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

package org.siglus.siglusapi.web.withlocation;

import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.siglus.siglusapi.dto.SiglusShipmentDraftDto;
import org.siglus.siglusapi.service.SiglusShipmentDraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/shipmentDraftsWithLocation")
public class SiglusShipmentDraftsWithLocationController {
  @Autowired
  private SiglusShipmentDraftService siglusShipmentDraftService;

  @GetMapping("/{id}")
  public SiglusShipmentDraftDto getShipmentDraftByLocation(@PathVariable UUID id) {
    List<SiglusShipmentDraftDto> drafts = siglusShipmentDraftService.getShipmentDraftByOrderId(id);
    if (ObjectUtils.isEmpty(drafts)) {
      throw new IllegalArgumentException("No shipment draft found with order id: " + id);
    }
    return drafts.get(0);
  }

  @PutMapping("/{id}")
  public ShipmentDraftDto updateShipmentDraftByLocation(@PathVariable UUID id,
      @RequestBody ShipmentDraftDto draftDto) {
    return siglusShipmentDraftService.updateShipmentDraft(id, draftDto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteShipmentDraftByLocation(@PathVariable UUID id) {
    siglusShipmentDraftService.deleteShipmentDraft(id);
  }
}
