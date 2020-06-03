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

package org.openlmis.fulfillment.web.stockmanagement;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.naming.VvmStatus;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public final class StockEventLineItemDto
    implements ShipmentLineItem.Exporter, ProofOfDeliveryLineItem.Exporter {
  public static final String VVM_STATUS = "vvmStatus";
  public static final String QUANTITY_REJECTED = "quantityRejected";
  public static final String REJECTION_REASON_ID = "rejectionReasonId";

  private UUID orderableId;
  private UUID lotId;
  private Integer quantity;
  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")
  private LocalDate occurredDate;
  private UUID destinationId;
  private UUID reasonId;
  private UUID sourceId;
  private Map<String, String> extraData;

  @Override
  @JsonIgnore
  public void setId(UUID id) {
    // nothing to do here
  }

  @Override
  @JsonIgnore
  public void setQuantityAccepted(Integer quantityAccepted) {
    quantity = quantityAccepted;
  }

  @Override
  @JsonIgnore
  public void setUseVvm(Boolean useVvm) {
    // nothing to do here
  }

  @Override
  @JsonIgnore
  public void setVvmStatus(VvmStatus vvmStatus) {
    putExtraData(VVM_STATUS, vvmStatus);
  }

  @Override
  @JsonIgnore
  public void setQuantityRejected(Integer quantityRejected) {
    putExtraData(QUANTITY_REJECTED, quantityRejected);
  }

  @Override
  @JsonIgnore
  public void setRejectionReasonId(UUID rejectionReasonId) {
    putExtraData(REJECTION_REASON_ID, rejectionReasonId);
  }

  @Override
  @JsonIgnore
  public void setNotes(String notes) {
    // nothing to do here
  }

  @Override
  @JsonIgnore
  public void setQuantityShipped(Long quantityShipped) {
    quantity = Math.toIntExact(quantityShipped);
  }

  @Override
  @JsonIgnore
  public void setOrderable(OrderableDto orderable) {
    // nothing to do here
  }

  private void putExtraData(String key, Object value) {
    if (null != value) {
      extraData = Optional.ofNullable(extraData).orElse(new HashMap<>());
      extraData.put(key, value.toString());
    }
  }
}
