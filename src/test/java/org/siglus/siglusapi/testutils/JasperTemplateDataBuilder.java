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

package org.siglus.siglusapi.testutils;

import java.util.ArrayList;
import java.util.List;
import org.openlmis.requisition.domain.JasperTemplate;
import org.openlmis.requisition.domain.JasperTemplateParameter;
import org.siglus.siglusapi.testutils.api.DataBuilder;

public class JasperTemplateDataBuilder implements DataBuilder<JasperTemplate> {
  private String name;
  private byte[] data;
  private List<JasperTemplateParameter> templateParameters;
  private String type;
  private String description;

  /**
   * Builder for {@link JasperTemplateDataBuilder}.
   */
  public JasperTemplateDataBuilder() {
    this.name = "name";
    this.data = new byte[0];
    this.templateParameters = new ArrayList<>();
    this.type = "type";
    this.description = "description";
  }

  /**
   * Create new instance of {@link JasperTemplate}.
   */
  @Override
  public JasperTemplate build() {
    return new JasperTemplate(name, data, templateParameters, type, description);
  }

  public JasperTemplateDataBuilder withName(String name) {
    this.name = name;
    return this;
  }
}
