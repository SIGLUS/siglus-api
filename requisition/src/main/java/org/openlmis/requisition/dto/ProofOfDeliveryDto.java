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

package org.openlmis.requisition.dto;

import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.CONFIRMED;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ProofOfDeliveryDto extends BaseDto {
  private ObjectReferenceDto shipment;
  private ProofOfDeliveryStatus status;
  private List<ProofOfDeliveryLineItemDto> lineItems;
  private String receivedBy;
  private String deliveredBy;
  private LocalDate receivedDate;

  @JsonIgnore
  public boolean isSubmitted() {
    return CONFIRMED == status;
  }

  /**
   * Finds a correct line item based on product id.
   */
  public ProofOfDeliveryLineItemDto findLineByProductId(UUID productId) {
    if (null == lineItems) {
      return null;
    }

    return lineItems
        .stream()
        .filter(e -> null != e.getOrderable())
        .filter(e -> Objects.equals(productId, e.getOrderable().getId()))
        .findFirst()
        .orElse(null);
  }
}
