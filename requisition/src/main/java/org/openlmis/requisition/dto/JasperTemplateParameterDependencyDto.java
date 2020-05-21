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

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.requisition.domain.JasperTemplateParameterDependency;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JasperTemplateParameterDependencyDto implements
    JasperTemplateParameterDependency.Importer, JasperTemplateParameterDependency.Exporter {

  private UUID id;
  private String dependency;
  private String placeholder;

  /**
   * Create new instance of JasperTemplateParameterDependencyDto based on given {@link
   * JasperTemplateParameterDependency}.
   *
   * @param dependency instance of parameter dependency
   * @return new instance of JasperTemplateParameterDependencyDto.
   */
  public static JasperTemplateParameterDependencyDto newInstance(
      JasperTemplateParameterDependency dependency) {
    JasperTemplateParameterDependencyDto dto = new JasperTemplateParameterDependencyDto();
    dependency.export(dto);
    return dto;
  }
}