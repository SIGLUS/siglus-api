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

package org.openlmis.requisition.testutils;

import java.util.HashMap;
import java.util.Map;
import org.openlmis.requisition.dto.BasicRequisitionTemplateColumnDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class BasicRequisitionTemplateDtoDataBuilder implements
    DtoDataBuilder<BasicRequisitionTemplateDto> {

  private boolean populateStockOnHandFromStockCards;
  private String name;
  private Map<String, BasicRequisitionTemplateColumnDto> columnsMap;

  /**
   * Builder for {@link BasicRequisitionTemplateDto}.
   */
  public BasicRequisitionTemplateDtoDataBuilder() {
    populateStockOnHandFromStockCards = false;
    name = "some name";
    columnsMap = new HashMap<>();
  }

  @Override
  public BasicRequisitionTemplateDto buildAsDto() {
    BasicRequisitionTemplateDto basicRequisitionTemplateDto = new BasicRequisitionTemplateDto();
    basicRequisitionTemplateDto.setPopulateStockOnHandFromStockCards(
        populateStockOnHandFromStockCards);
    basicRequisitionTemplateDto.setName(name);
    basicRequisitionTemplateDto.setColumnsMap(columnsMap);
    return basicRequisitionTemplateDto;
  }
}
