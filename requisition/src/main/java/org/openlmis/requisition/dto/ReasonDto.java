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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason.Exporter;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason.Importer;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ReasonDto extends BaseDto implements Exporter, Importer {
  private String name;
  private String description;
  private ReasonType reasonType;
  private ReasonCategory reasonCategory;
  private Boolean isFreeTextAllowed;
  private Boolean hidden;

  /**
   * Create new list of ReasonDto based on given list of {@link StockAdjustmentReason}.
   *
   * @param reasons list of {@link StockAdjustmentReason}
   * @return new list of ReasonDto.
   */
  public static List<ReasonDto> newInstance(Iterable<StockAdjustmentReason> reasons) {
    if (reasons == null) {
      return Collections.emptyList();
    }
    List<ReasonDto> reasonDtos = new ArrayList<>();
    reasons.forEach(r -> reasonDtos.add(newInstance(r)));
    return reasonDtos;
  }

  /**
   * Create new instance of RequisitionTemplateDto based on given {@link RequisitionTemplate}.
   *
   * @param reason instance of Template
   * @return new instance of RequisitionTemplateDto.
   */
  public static ReasonDto newInstance(StockAdjustmentReason reason) {
    if (reason == null) {
      return null;
    }
    ReasonDto reasonDto = new ReasonDto();
    reason.export(reasonDto);

    return reasonDto;
  }

}
