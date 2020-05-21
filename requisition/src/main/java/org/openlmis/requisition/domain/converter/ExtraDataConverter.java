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

package org.openlmis.requisition.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class ExtraDataConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExtraDataConverter.class);
  private static final TypeReference<Map<String, Object>> TYPE_REF =
      new TypeReference<Map<String, Object>>() {
      };

  private final ObjectMapper objectMapper;

  public ExtraDataConverter() {
    this(new ObjectMapper());
  }

  ExtraDataConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String convertToDatabaseColumn(@NotNull Map<String, Object> extraData) {
    if (CollectionUtils.isEmpty(extraData)) {
      return null;
    }

    try {
      return objectMapper.writeValueAsString(extraData);
    } catch (JsonProcessingException ex) {
      LOGGER.error("Can't convert extraData to database column", ex);
      return null;
    }
  }

  @Override
  @NotNull
  public Map<String, Object> convertToEntityAttribute(@NotNull String extraDataDataAsString) {
    if (StringUtils.isBlank(extraDataDataAsString)) {
      return Maps.newHashMap();
    }

    try {
      // IMPORTANT: any type of collection (array, list, set) will be converted to list.
      return objectMapper.readValue(extraDataDataAsString, TYPE_REF);
    } catch (IOException ex) {
      LOGGER.error("Can't convert string to extraData", ex);
      return Maps.newHashMap();
    }
  }
}
