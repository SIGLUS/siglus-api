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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.siglus.siglusapi.domain.HfCmm;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class FacilityCmmNativeRepository extends BaseNativeRepository {

  private final ObjectMapper json;
  private final NamedParameterJdbcTemplate namedJdbc;

  public void batchCreateHfCmms(List<HfCmm> hfcmms) {
    String sql = "INSERT INTO siglusintegration.hf_cmms"
        + "(id, facilitycode, productcode, periodbegin, periodend, cmm, lastupdated) "
        + "VALUES (:id, :facilityCode, :productCode, :periodBegin, :periodEnd, :cmm, :lastUpdated)";
    namedJdbc.batchUpdate(sql, toParams(hfcmms));
  }

  @Override
  protected SqlParameterSource[] toParams(Collection<?> entities) {
    return entities.stream().map(e -> new ToJsonBeanPropertySqlParameterSource(e, json))
        .toArray(SqlParameterSource[]::new);
  }

  private static class ToJsonBeanPropertySqlParameterSource extends BeanPropertySqlParameterSource {

    private final ObjectMapper objectMapper;

    public ToJsonBeanPropertySqlParameterSource(Object object, ObjectMapper objectMapper) {
      super(object);
      this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
      Object value = super.getValue(paramName);
      if (value instanceof Map) {
        return objectMapper.writeValueAsString(value);
      }
      return value;
    }
  }

}
