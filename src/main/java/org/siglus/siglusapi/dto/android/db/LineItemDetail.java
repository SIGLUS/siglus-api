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

package org.siglus.siglusapi.dto.android.db;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class LineItemDetail {

  private final StockEvent stockEvent;
  private final StockCard stockCard;
  private final EventTime eventTime;
  private final Integer quantity;
  private final UUID sourceId;
  private final UUID destinationId;
  private final UUID reasonId;
  private final String documentNumber;
  private final boolean isInitInventory;

  public static LineItemDetail of(StockEvent stockEvent, StockCard stockCard, MovementType type,
      StockCardAdjustment request, boolean isInit, UUID newSourceId) {
    Integer quantity;
    if (type == MovementType.PHYSICAL_INVENTORY) {
      quantity = request.getStockOnHand();
    } else {
      quantity = Math.abs(request.getQuantity());
    }
    UUID programId = stockCard.getProgramId();
    String reason = request.getReasonName();
    UUID sourceId = newSourceId == null ? type.getSourceId(programId, reason) : newSourceId;
    UUID destinationId = type.getDestinationId(programId, reason);
    UUID reasonId = type.getAdjustmentReasonId(programId, reason);
    if (type == MovementType.RECEIVE) {
      reasonId = MovementType.ADJUSTMENT.getAdjustmentReasonId(programId, FieldConstants.CAPITAL_RECEIVE);
    } else if (type == MovementType.ISSUE || type == MovementType.UNPACK_KIT) {
      reasonId = MovementType.ADJUSTMENT.getAdjustmentReasonId(programId, FieldConstants.CAPITAL_ISSUE);
    }
    String documentationNo = request.getDocumentationNo();
    return new LineItemDetail(stockEvent, stockCard, request.getEventTime(), quantity, sourceId, destinationId,
        reasonId, documentationNo, isInit);
  }

  public java.sql.Date getOccurredDate() {
    return Date.valueOf(eventTime.getOccurredDate());
  }

  public Timestamp getServerProcessAt() {
    return Timestamp.from(stockEvent.getProcessedAt());
  }

  public String getSignature() {
    return stockEvent.getSignature();
  }

  public UUID getUserId() {
    return stockEvent.getUserId();
  }

  public Map<String, String> getExtraData() {
    Map<String, String> extraData = new LinkedHashMap<>();
    if (stockCard.getLotCode() != null) {
      extraData.put("lotCode", stockCard.getLotCode());
    }
    if (stockCard.getExpirationDate() != null) {
      extraData.put("expirationDate", stockCard.getExpirationDate().toString());
    }
    if (eventTime.getRecordedAt() != null) {
      extraData.put("originEventTime", eventTime.getRecordedAt().toString());
    }
    if (isInitInventory) {
      extraData.put("isInitInventory", "true");
    }
    return extraData;
  }

}
