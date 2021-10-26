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

package org.siglus.siglusapi.repository;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collection;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseNativeRepository {

  protected SqlParameterSource[] toParams(Collection<?> entities) {
    return entities.stream().map(BeanPropertySqlParameterSource::new).toArray(SqlParameterSource[]::new);
  }

  protected UUID readId(ResultSet rs) {
    return readUuid(rs, "id");
  }

  @SneakyThrows
  protected UUID readUuid(ResultSet rs, String columnName) {
    return rs.getObject(columnName, UUID.class);
  }

  @SneakyThrows
  protected String readAsString(ResultSet rs, String columnName) {
    ResultSetMetaData rsMetaData = rs.getMetaData();
    int numberOfColumns = rsMetaData.getColumnCount();
    for (int i = 1; i < numberOfColumns + 1; i++) {
      if (columnName.equals(rsMetaData.getColumnName(i))) {
        return rs.getString(columnName);
      }
    }
    return null;
  }

  @SneakyThrows
  protected Integer readAsInt(ResultSet rs, String columnName) {
    int value = rs.getInt(columnName);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }

  @SneakyThrows
  protected java.sql.Date readAsDate(ResultSet rs, String columnName) {
    return rs.getDate(columnName);
  }

}
