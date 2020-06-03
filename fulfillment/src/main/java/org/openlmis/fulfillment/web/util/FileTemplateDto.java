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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.TemplateType;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public final class FileTemplateDto implements FileTemplate.Importer,
    FileTemplate.Exporter {

  @Getter
  @Setter
  private UUID id;

  @Getter
  @Setter
  private String filePrefix;

  @Getter
  @Setter
  private Boolean headerInFile;

  @Setter
  private List<FileColumnDto> fileColumns;

  @Getter
  @Setter
  private TemplateType templateType;

  @Override
  public List<FileColumn.Importer> getFileColumns() {
    return new ArrayList<>(
        Optional.ofNullable(fileColumns).orElse(Collections.emptyList())
    );
  }

  /**
   * Create new list of FileTemplateDto based on given list of {@link FileTemplate}.
   * @param templates instance of FileTemplate
   * @return new instance of FileTemplateDto.
   */
  public static Iterable<FileTemplateDto> newInstance(Iterable<FileTemplate> templates) {

    List<FileTemplateDto> fileTemplateDtos = new ArrayList<>();
    templates.forEach(t -> fileTemplateDtos.add(newInstance(t)));
    return fileTemplateDtos;
  }

  /**
   * Create new instance of FileTemplateDto based on given {@link FileTemplate}.
   * @param fileTemplate instance of FileTemplate
   * @return new instance of FileTemplateDto.
   */
  public static FileTemplateDto newInstance(FileTemplate fileTemplate) {
    FileTemplateDto fileTemplateDto = new FileTemplateDto();
    fileTemplate.export(fileTemplateDto);

    if (fileTemplate.getFileColumns() != null) {
      fileTemplateDto.setFileColumns(fileTemplate.getFileColumns()
          .stream().map(FileColumnDto::newInstance).collect(Collectors.toList()));
    }
    return fileTemplateDto;
  }
}
