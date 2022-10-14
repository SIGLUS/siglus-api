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

package org.siglus.siglusapi.web.withoutlocation;

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_EVENT_ORDERABLE_INVALID;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.util.UUID;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.util.UuidUtil;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/stockCards")
public class SiglusStockCardWithoutLocationController {

  private static final String PRODUCT_ID = "id";

  @Autowired
  private SiglusStockCardService siglusStockCardService;

  @GetMapping("/orderable")
  public ResponseEntity<StockCardDto> searchStockCard(
      @RequestParam MultiValueMap<String, String> parameters) {

    UUID orderableId = UuidUtil.fromString(parameters.getFirst(PRODUCT_ID)).orElse(null);
    if (orderableId == null) {
      throw new ValidationMessageException(ERROR_EVENT_ORDERABLE_INVALID);
    }
    StockCardDto stockCardDto = siglusStockCardService.findStockCardByOrderable(orderableId);
    if (stockCardDto == null) {
      return new ResponseEntity<>(NOT_FOUND);
    } else {
      return new ResponseEntity<>(stockCardDto, OK);
    }
  }

  @GetMapping("/{stockCardId}")
  public ResponseEntity<StockCardDto> searchStockCardById(
      @PathVariable("stockCardId") UUID stockCardId) {
    StockCardDto stockCardDto = siglusStockCardService.findStockCardById(stockCardId);
    if (stockCardDto == null) {
      return new ResponseEntity<>(NOT_FOUND);
    } else {
      return new ResponseEntity<>(stockCardDto, OK);
    }
  }
}
