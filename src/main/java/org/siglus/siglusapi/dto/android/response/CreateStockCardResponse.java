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

import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.dto.android.InvalidProduct;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.springframework.http.HttpStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockCardResponse {

  private Integer status;

  private String message;

  private String messageInPortuguese;

  private String details;

  public static CreateStockCardResponse from(ValidatedStockCards validatedStockCards) {
    StringBuilder message = new StringBuilder("Sync [");
    StringBuilder messageInPortuguese = new StringBuilder("Erro na sincronização do produto [");
    StringBuilder details = new StringBuilder("Sync [");
    String messageInvalidCodes = "";
    String allInvalidCodes = "";
    String errorMessages = "";
    if(!validatedStockCards.getInvalidProducts().isEmpty()) {
      messageInvalidCodes = validatedStockCards.getInvalidProducts().stream().limit(3)
          .map(InvalidProduct::getProductCode).collect(
              Collectors.joining(","));
      allInvalidCodes = validatedStockCards.getInvalidProducts().stream().map(InvalidProduct::getProductCode)
          .collect(Collectors.joining(","));
      errorMessages = validatedStockCards.getInvalidProducts().stream().map(InvalidProduct::getErrorMessage)
          .collect(Collectors.joining("\n"));
    }
    message.append(messageInvalidCodes).append("] error.");
    messageInPortuguese.append(messageInvalidCodes).append("].");
    details.append(allInvalidCodes).append("] error.\n").append(errorMessages);
    return CreateStockCardResponse.builder()
        .message(message.toString())
        .messageInPortuguese(messageInPortuguese.toString())
        .details(details.toString()).status(HttpStatus.CREATED.value())
        .build();
  }

}
