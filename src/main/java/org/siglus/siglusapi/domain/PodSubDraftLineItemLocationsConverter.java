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

package org.siglus.siglusapi.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class PodSubDraftLineItemLocationsConverter
    implements AttributeConverter<List<PodSubDraftLineItemLocation>, String> {

  private static final TypeReference<List<PodSubDraftLineItemLocation>> TYPE_REF =
      new TypeReference<List<PodSubDraftLineItemLocation>>() {
      };

  private final ObjectMapper objectMapper;

  public PodSubDraftLineItemLocationsConverter() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper = mapper;
  }

  PodSubDraftLineItemLocationsConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String convertToDatabaseColumn(List<PodSubDraftLineItemLocation> locations) {
    try {
      return objectMapper.writeValueAsString(locations);
    } catch (JsonProcessingException ex) {
      log.error("Can't convert locations to database column", ex);
      return null;
    }
  }

  @Override
  @NotNull
  public List<PodSubDraftLineItemLocation> convertToEntityAttribute(@NotNull String responseAsString) {
    if (StringUtils.isBlank(responseAsString)) {
      return new ArrayList<>();
    }
    try {
      return objectMapper.readValue(responseAsString, TYPE_REF);
    } catch (IOException ex) {
      log.error("Can't convert string to locations", ex);
      return new ArrayList<>();
    }
  }
}
