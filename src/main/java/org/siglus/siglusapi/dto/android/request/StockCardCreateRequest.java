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

package org.siglus.siglusapi.dto.android.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.siglus.siglusapi.dto.android.constraints.ConsistentStockCard;
import org.siglus.siglusapi.dto.android.constraints.ConsistentStockOnHand;

@Data
@ConsistentStockCard
@ConsistentStockOnHand
public class StockCardCreateRequest implements StockCardAdjustment {

  @NotBlank
  private String productCode;

  @Min(0)
  @NotNull
  @JsonProperty("SOH")
  // after the adjustment
  private Integer stockOnHand;

  @NotNull
  private Integer quantity;

  @NotBlank
  private String type;

  @NotNull
  @JsonProperty("occurred")
  private LocalDate occurredDate;

  @NotNull
  @JsonProperty("processeddate")
  private Instant processedDate;

  private String referenceNumber;

  private String signature;

  // TODO check order by time ascending
  // TODO check consistent by lot
  @Valid
  @NotEmpty
  @JsonProperty("lotEventList")
  private List<StockCardLotEventRequest> lotEvents;
}
