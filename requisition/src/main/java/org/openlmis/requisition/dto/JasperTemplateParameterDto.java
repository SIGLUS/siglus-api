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

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.domain.JasperTemplateParameter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JasperTemplateParameterDto implements JasperTemplateParameter.Importer,
    JasperTemplateParameter.Exporter {

  private UUID id;
  private String name;
  private String displayName;
  private String defaultValue;
  private String dataType;
  private String selectExpression;
  private String selectMethod;
  private String selectBody;
  private String selectProperty;
  private String displayProperty;
  private String description;
  private Boolean required;
  private List<String> options;
  private List<JasperTemplateParameterDependencyDto> dependencies;

  /**
   * Create new instance of JasperTemplateParameterDto based on given {@link
   * JasperTemplateParameter}.
   *
   * @param jasperTemplateParameter instance of Template
   * @return new instance of JasperTemplateDto.
   */
  public static JasperTemplateParameterDto newInstance(
      JasperTemplateParameter jasperTemplateParameter) {
    JasperTemplateParameterDto jasperTemplateParameterDto = new JasperTemplateParameterDto();
    jasperTemplateParameter.export(jasperTemplateParameterDto);
    return jasperTemplateParameterDto;
  }
}
