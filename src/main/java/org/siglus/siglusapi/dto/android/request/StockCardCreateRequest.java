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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.constraint.stockcard.FacilityApprovedProduct;
import org.siglus.siglusapi.dto.android.constraint.stockcard.KitProductEmptyLots;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductConsistentWithOwnLots;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductPositiveInitStockOnHand;
import org.siglus.siglusapi.dto.android.constraint.stockcard.RequestValidEventTime;
import org.siglus.siglusapi.dto.android.constraint.stockcard.SupportReasonName;
import org.siglus.siglusapi.dto.android.group.DatabaseCheckGroup;
import org.siglus.siglusapi.dto.android.group.SelfCheckGroup;

@Data
@NoArgsConstructor
@RequestValidEventTime(groups = SelfCheckGroup.class)
@ProductConsistentWithOwnLots(groups = SelfCheckGroup.class)
@ProductPositiveInitStockOnHand(groups = SelfCheckGroup.class)
@FacilityApprovedProduct(groups = DatabaseCheckGroup.class)
@SupportReasonName(groups = DatabaseCheckGroup.class)
@KitProductEmptyLots(groups = DatabaseCheckGroup.class)
public class StockCardCreateRequest implements StockCardAdjustment {

  @NotBlank
  private String productCode;

  @Min(0)
  @NotNull
  // after the adjustment
  private Integer stockOnHand;

  @NotNull
  private Integer quantity;

  @NotBlank
  private String type;

  @NotNull
  private LocalDate occurredDate;

  @JsonProperty("processedDate")
  @NotNull
  private Instant recordedAt;

  private String documentationNo;

  private Integer requested;

  @JsonInclude(Include.NON_NULL)
  private String reasonName;

  private String signature;

  private Boolean isInitInventory;

  @Valid
  @JsonProperty("lotEventList")
  @NotNull
  @Nonnull
  private List<StockCardLotEventRequest> lotEvents;

  @Override
  @JsonIgnore
  public EventTime getEventTime() {
    return EventTime.fromRequest(occurredDate, recordedAt);
  }

  @JsonIgnore
  public ProductMovementKey getProductMovementKey() {
    return ProductMovementKey.of(productCode, getEventTime());
  }

  @JsonIgnore
  public String getSyncUpProperties() {
    return recordedAt.toString() + productCode + stockOnHand + quantity + type;
  }

  public boolean isInitInventory() {
    return isInitInventory != null && isInitInventory;
  }

}
