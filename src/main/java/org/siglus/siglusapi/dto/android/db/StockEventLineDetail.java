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

import static org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder.getContext;

import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class StockEventLineDetail {

  private final EventTime eventTime;
  private final Integer quantity;
  private final UUID sourceId;
  private final UUID destinationId;
  private final UUID reasonId;
  private final String documentNumber;

  public static StockEventLineDetail of(String productCode, MovementType type, StockCardAdjustment request) {
    Integer quantity;
    if (type == MovementType.PHYSICAL_INVENTORY) {
      quantity = request.getStockOnHand();
    } else {
      quantity = request.getQuantity();
    }
    UUID programId = getContext().getProgramId(productCode).orElseThrow(IllegalStateException::new);
    String reason = request.getReasonName();
    UUID sourceId = type.getSourceId(programId, reason);
    UUID destinationId = type.getDestinationId(programId, reason);
    UUID reasonId = type.getAdjustmentReasonId(programId, reason);
    String documentationNo = request.getDocumentationNo();
    return new StockEventLineDetail(request.getEventTime(), quantity, sourceId, destinationId, reasonId,
        documentationNo);
  }

}
