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

package org.siglus.siglusapi.web;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.siglus.siglusapi.dto.SiglusShipmentDraftDto;
import org.siglus.siglusapi.service.SiglusShipmentDraftService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/shipmentDrafts")
@RequiredArgsConstructor
public class SiglusShipmentDraftController {

  private final SiglusShipmentDraftService siglusShipmentDraftService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public SiglusShipmentDraftDto createShipmentDraft(@RequestBody ShipmentDraftDto draftDto) {
    // CreateShipmentDraftRequest
    return siglusShipmentDraftService.createShipmentDraft(draftDto.getOrder().getId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteShipmentDraft(@PathVariable UUID id) {
    siglusShipmentDraftService.deleteShipmentDraft(id);
  }

  @PutMapping("/{id}")
  public ShipmentDraftDto updateShipmentDraft(@PathVariable UUID id, @RequestBody ShipmentDraftDto draftDto) {
    return siglusShipmentDraftService.updateShipmentDraft(id, draftDto);
  }

  @GetMapping("/{id}")
  public SiglusShipmentDraftDto getShipmentDraft(@PathVariable UUID id) {
    return siglusShipmentDraftService.getShipmentDraft(id);
  }

  @GetMapping
  public List<SiglusShipmentDraftDto> getShipmentDrafts(@RequestParam UUID orderId) {
    return siglusShipmentDraftService.getShipmentDraftByOrderId(orderId);
  }
}
