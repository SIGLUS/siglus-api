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

package org.siglus.siglusapi.migration;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.openlmis.referencedata.domain.Orderable.TRADE_ITEM;

import com.google.common.collect.ImmutableMap;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;

@Data
@AllArgsConstructor
@Builder
public class AdditionalOrderable {

  private UUID programId;
  private UUID orderableId;
  private String productCode;
  private UUID tradeItemId;

  public OrderableDto toPartialOrderableDto() {
    ProgramOrderableDto programOrderable = new ProgramOrderableDto();
    programOrderable.setProgramId(programId);
    ImmutableMap<String, String> identifiers = ImmutableMap.of(TRADE_ITEM, tradeItemId.toString());
    OrderableDto orderableDto = new OrderableDto(singleton(programOrderable), new DispensableDto(), identifiers,
        emptyMap());
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(productCode);
    return orderableDto;
  }
}
