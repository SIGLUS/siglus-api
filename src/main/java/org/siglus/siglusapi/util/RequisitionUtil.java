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

package org.siglus.siglusapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.dto.SimpleRequisitionDto;

public class RequisitionUtil {

  private RequisitionUtil() {
  }

  public static Map<String, Object> getRequisitionExtraData(SimpleRequisitionDto simpleRequisitionDto) {
    if (Objects.isNull(simpleRequisitionDto) || StringUtils.isBlank(simpleRequisitionDto.getExtraData())) {
      return null;
    }
    return jsonStringToMap(simpleRequisitionDto.getExtraData());
  }

  @SneakyThrows
  private static Map<String, Object> jsonStringToMap(String jsonString) {
    return new ObjectMapper().readValue(jsonString, Map.class);
  }

}
