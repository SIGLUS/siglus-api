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

import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PodLotLineRequest {
  private static String OLD_RECEIVED_LESS_QUANTITIES_THAN_EXPECTED_REASON = "Recebido a menos";
  private static String NEW_RECEIVED_LESS_QUANTITIES_THAN_EXPECTED_REASON = "Quantidade recebida a menos (no lote)";

  private LotBasicRequest lot;

  @NotNull
  private Integer shippedQuantity;

  @NotNull
  private Integer acceptedQuantity;

  private String rejectedReason;

  private String notes;

  public String getRejectedReason() {
    if (OLD_RECEIVED_LESS_QUANTITIES_THAN_EXPECTED_REASON.equals(rejectedReason)) {
      return NEW_RECEIVED_LESS_QUANTITIES_THAN_EXPECTED_REASON;
    }
    return rejectedReason;
  }
}
