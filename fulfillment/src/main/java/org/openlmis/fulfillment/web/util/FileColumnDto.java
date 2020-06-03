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

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.fulfillment.domain.FileColumn;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public final class FileColumnDto implements FileColumn.Importer, FileColumn.Exporter {

  private UUID id;
  private Boolean openLmisField;
  private String dataFieldLabel;
  private String columnLabel;
  private Boolean include;
  private Integer position;
  private String format;
  private String nested;
  private String keyPath;
  private String related;
  private String relatedKeyPath;

  /**
   * Create new instance of FileColumnDto based on given {@link FileColumn}.
   *
   * @param fileColumn instance of Template
   * @return new instance of TemplateDto.
   */
  public static FileColumnDto newInstance(FileColumn fileColumn) {
    FileColumnDto fileColumnDto = new FileColumnDto();
    fileColumn.export(fileColumnDto);
    return fileColumnDto;
  }
}
