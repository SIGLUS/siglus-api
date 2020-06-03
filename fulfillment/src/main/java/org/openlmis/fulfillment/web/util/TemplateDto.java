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

package org.openlmis.fulfillment.web.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.domain.TemplateParameter;

@AllArgsConstructor
@NoArgsConstructor
public class TemplateDto implements Template.Importer, Template.Exporter {

  @Getter
  @Setter
  private UUID id;

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String type;

  @Getter
  @Setter
  private String description;

  @Getter
  @Setter
  private byte[] data;

  @Setter
  private List<TemplateParameterDto> templateParameters;

  @Override
  public List<TemplateParameter.Importer> getTemplateParameters() {
    return new ArrayList<>(
        Optional.ofNullable(templateParameters).orElse(Collections.emptyList())
    );
  }

  /**
   * Create new list of TemplateDto based on given list of {@link Template}.
   * @param templates list of {@link Template}
   * @return new list of TemplateDto.
   */
  public static Iterable<TemplateDto> newInstance(Iterable<Template> templates) {

    List<TemplateDto> templateDtos = new ArrayList<>();
    templates.forEach(t -> templateDtos.add(newInstance(t)));
    return templateDtos;
  }

  /**
   * Create new instance of TemplateDto based on given {@link Template}.
   * @param template instance of Template
   * @return new instance of TemplateDto.
   */
  public static TemplateDto newInstance(Template template) {
    if (template == null) {
      return null;
    }
    TemplateDto templateDto = new TemplateDto();
    template.export(templateDto);

    if (template.getTemplateParameters() != null) {
      templateDto.setTemplateParameters(template.getTemplateParameters().stream()
          .map(TemplateParameterDto::newInstance).collect(Collectors.toList()));
    }
    return templateDto;
  }
}
