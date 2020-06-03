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

package org.openlmis.fulfillment;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;

@NoArgsConstructor
@AllArgsConstructor
public class FileColumnBuilder {

  @Wither
  private UUID id = UUID.randomUUID();

  @Wither
  private Boolean openLmisField;

  @Wither
  private String dataFieldLabel;

  @Wither
  private String columnLabel;

  @Wither
  private Boolean include;

  @Wither
  private Integer position;

  @Wither
  private String format;

  @Wither
  private String nested;

  @Wither
  private String keyPath;

  @Wither
  private String related;

  @Wither
  private String relatedKeyPath;

  @Wither
  private FileTemplate fileTemplate;

  /**
   * Returns a new FileColumn object.
   *
   * @return FileColumn
   */
  public FileColumn build() {
    FileColumn column = new FileColumn(openLmisField, dataFieldLabel, columnLabel, include,
        position, format, nested, keyPath, related, relatedKeyPath, fileTemplate);
    column.setId(id);
    return column;
  }


}
