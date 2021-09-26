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

package org.siglus.siglusapi.dto.android.response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.constant.ValidatorConstants;
import org.siglus.siglusapi.dto.android.InvalidProduct;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockCardResponse {

  private Integer status;

  private String details;

  private List<String> errorProductCodes;

  public static CreateStockCardResponse from(ValidatedStockCards validatedStockCards) {
    StringBuilder details = new StringBuilder();
    Set<String> invalidProductCodes = new HashSet<>();
    if (!CollectionUtils.isEmpty(validatedStockCards.getInvalidProducts())) {
      invalidProductCodes = validatedStockCards.getInvalidProducts().stream()
          .map(InvalidProduct::getProductCode).collect(Collectors.toSet());
      String allInvalidCodes = String.join(",", invalidProductCodes);
      String errorMessages = validatedStockCards.getInvalidProducts().stream().map(InvalidProduct::getErrorMessage)
          .collect(Collectors.joining("\n"));
      details.append("Sync [").append(allInvalidCodes).append("] error.").append(errorMessages);
    }
    return CreateStockCardResponse.builder()
        .errorProductCodes(new ArrayList<>(invalidProductCodes))
        .details(details.toString())
        .status(HttpStatus.CREATED.value())
        .build();
  }

}
