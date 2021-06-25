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
import java.time.LocalDate;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.siglus.siglusapi.dto.android.constraints.stockcard.PositiveInitStockOnHand;

@Data
@PositiveInitStockOnHand
public class StockCardLotEventRequest implements StockCardAdjustment {

  @NotBlank
  private String lotNumber;

  @NotNull
  private LocalDate expirationDate;

  @Min(0)
  @NotNull
  @JsonProperty("SOH")
  private Integer stockOnHand;

  @NotNull
  private Integer quantity;

  private String reasonName;

}
