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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.requisition.domain.RequisitionTemplate;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BasicRequisitionTemplateDto extends BaseRequisitionTemplateDto
    implements RequisitionTemplate.Exporter {

  private boolean populateStockOnHandFromStockCards;
  private String name;

  private Map<String, BasicRequisitionTemplateColumnDto> columnsMap;

  /**
   * Create new list of RequisitionTemplateDto based on given list of {@link RequisitionTemplate}.
   *
   * @param templates list of {@link RequisitionTemplate}
   * @return new list of RequisitionTemplateDto.
   */
  public static Iterable<BasicRequisitionTemplateDto> newInstance(
      Iterable<RequisitionTemplate> templates) {

    List<BasicRequisitionTemplateDto> requisitionTemplateDtos = new ArrayList<>();
    templates.forEach(t -> requisitionTemplateDtos.add(newInstance(t)));
    return requisitionTemplateDtos;
  }

  /**
   * Create new instance of RequisitionTemplateDto based on given {@link RequisitionTemplate}.
   *
   * @param requisitionTemplate instance of Template
   * @return new instance of RequisitionTemplateDto.
   */
  public static BasicRequisitionTemplateDto newInstance(
      RequisitionTemplate requisitionTemplate) {
    if (requisitionTemplate == null) {
      return null;
    }
    BasicRequisitionTemplateDto requisitionTemplateDto = new BasicRequisitionTemplateDto();
    requisitionTemplate.export(requisitionTemplateDto);
    requisitionTemplateDto.setColumnsMap(
        BasicRequisitionTemplateColumnDto.newInstance(requisitionTemplate.viewColumns()));

    return requisitionTemplateDto;
  }

  @Override
  public void setProgramId(UUID programId) {
    // nothing to do
  }

  @Override
  public void setFacilityTypeIds(Set<UUID> facilityTypeIds) {
    // nothing to do
  }
}
