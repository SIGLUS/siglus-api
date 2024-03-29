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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionLineItemRequest {

  @NotBlank
  private String productCode;

  @NotNull
  @Min(value = 0)
  private Integer beginningBalance;

  @NotNull
  @Min(value = 0)
  private Integer totalReceivedQuantity;

  @NotNull
  @Min(value = 0)
  private Integer totalConsumedQuantity;

  private Integer totalLossesAndAdjustments;

  @NotNull
  @Min(value = 0)
  private Integer stockOnHand;

  @Min(value = 0)
  private Integer requestedQuantity;

  @Min(value = 0)
  private Integer authorizedQuantity;

  private LocalDate expirationDate;
}

