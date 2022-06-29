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

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_INVALID_PARAMS;
import static org.springframework.http.HttpStatus.OK;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/stockMovement")
public class SiglusStockMovementController {

  @Autowired
  SiglusStockCardService siglusStockCardService;

  @GetMapping("/getMovement")
  public ResponseEntity<List<StockMovementResDto>> getStockMovement(
      @RequestParam(required = false) UUID facilityId,
      @RequestParam(required = false) UUID orderableId,
      @RequestParam(value = "startTime", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      @Nullable LocalDate since,
      @RequestParam(value = "endTime", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      @Nullable LocalDate tillExclusive) {
    List<StockMovementResDto> productMovements =
        siglusStockCardService.getProductMovements(facilityId, orderableId, since, tillExclusive);
    if (facilityId == null && orderableId == null) {
      throw new ValidationMessageException(ERROR_INVALID_PARAMS);
    }
    return new ResponseEntity<>(productMovements, OK);
  }
}
