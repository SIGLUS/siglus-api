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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import javax.persistence.AttributeConverter;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;

@Slf4j
public class ProductMovementConverter implements AttributeConverter<ProductMovementResponse, String> {

  private static final TypeReference<ProductMovementResponse> TYPE_REF =
      new TypeReference<ProductMovementResponse>() {
      };

  private final ObjectMapper objectMapper;

  public ProductMovementConverter() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.objectMapper = mapper;
  }

  ProductMovementConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String convertToDatabaseColumn(@NotNull ProductMovementResponse request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      log.error("Can't convert requisition request to database column", ex);
      return null;
    }
  }

  @Override
  @NotNull
  public ProductMovementResponse convertToEntityAttribute(@NotNull String requestAsString) {
    if (StringUtils.isBlank(requestAsString)) {
      return new ProductMovementResponse();
    }
    try {
      return objectMapper.readValue(requestAsString, TYPE_REF);
    } catch (IOException ex) {
      log.error("Can't convert string to product movement response", ex);
      return new ProductMovementResponse();
    }
  }
}
