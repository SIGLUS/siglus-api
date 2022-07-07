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

package org.siglus.siglusapi.migration;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;
import javax.validation.Valid;
import org.hibernate.validator.constraints.NotEmpty;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.response.CreateStockCardResponse;
import org.siglus.siglusapi.migration.DataMigrationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/data-migration")
@Validated
public class DataMigrationController {

  private final DataMigrationService dataMigrationService;

  public DataMigrationController(DataMigrationService service) {
    this.dataMigrationService = service;
  }

  @PostMapping("/{facilityId}/stock-cards")
  @ResponseStatus(CREATED)
  public CreateStockCardResponse createStockCards(
      @PathVariable String facilityId,
      @RequestBody @Valid @NotEmpty List<StockCardCreateRequest> requests) {
    return dataMigrationService.createStockCards(facilityId, requests);
  }

  @PostMapping("/{facilityId}/cmms")
  @ResponseStatus(CREATED)
  public void createOrUpdateCmms(
      @PathVariable String facilityId,
      @RequestBody @Valid List<HfCmmDto> hfCmmDtos) {
    dataMigrationService.createOrUpdateCmms(facilityId, hfCmmDtos);
  }
}
