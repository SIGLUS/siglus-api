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

package org.siglus.siglusapi.localmachine.cdc;

import static org.siglus.siglusapi.constant.FieldConstants.DOT;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableChangeEvent {

  private String schemaVersion;
  private String schemaName;
  private String tableName;
  private List<String> columns;
  private List<RowChangeEvent> rowChangeEvents;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class RowChangeEvent {

    private boolean isDeletion;
    private List<Object> values;
  }

  public String getTableFullName() {
    return schemaName + DOT + tableName;
  }
}
