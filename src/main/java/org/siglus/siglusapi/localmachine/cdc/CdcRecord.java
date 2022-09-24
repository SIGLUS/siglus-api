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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZonedDateTime;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cdc_records", schema = "localmachine")
public class CdcRecord {
  @Id
  @Column(name = "lsn")
  private Long id;

  @Column(name = "tablename")
  private String table;

  @Column(name = "schemaname")
  private String schema;

  private Long txId;
  private String operationCode;
  private ZonedDateTime capturedAt;

  @SuppressWarnings("JpaAttributeTypeInspection")
  @Convert(converter = PayloadConverter.class)
  private Map<String, Object> payload;

  public String tableId() {
    return schema + "." + table;
  }

  public static class PayloadConverter implements AttributeConverter<Map<String, Object>, byte[]> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Override
    public byte[] convertToDatabaseColumn(Map<String, Object> attribute) {
      return objectMapper.writeValueAsBytes(attribute);
    }

    @SneakyThrows
    @Override
    public Map<String, Object> convertToEntityAttribute(byte[] dbData) {
      return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
    }
  }
}
