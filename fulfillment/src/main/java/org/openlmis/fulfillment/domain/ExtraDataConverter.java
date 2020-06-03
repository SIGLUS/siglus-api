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

package org.openlmis.fulfillment.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.validation.constraints.NotNull;

public class ExtraDataConverter implements AttributeConverter<Map<String, String>, String> {
  static final TypeReference<Map<String, String>> TYPE_REFERENCE =
      new TypeReference<Map<String, String>>() {
      };

  private final ObjectMapper objectMapper;

  public ExtraDataConverter() {
    this(new ObjectMapper());
  }

  ExtraDataConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  @NotNull
  public String convertToDatabaseColumn(Map<String, String> extraData) {
    try {
      return objectMapper.writeValueAsString(extraData);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String extraDataAsJson) {
    try {
      return null != extraDataAsJson && !"null".equalsIgnoreCase(extraDataAsJson)
          ? objectMapper.readValue(extraDataAsJson, TYPE_REFERENCE)
          : null;
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
