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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.domain.ExtraDataConverter.TYPE_REFERENCE;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

public class ExtraDataConverterTest {
  private static final String EXPECTED_JSON = "{\"external\":\"true\"}";
  private static final Map<String, String> EXPECTED_MAP = ImmutableMap.of("external", "true");

  private ObjectMapper objectMapper = spy(new ObjectMapper());
  private ExtraDataConverter converter = new ExtraDataConverter(objectMapper);

  @Test
  public void shouldConvertToDatabaseColumn() {
    assertThat(converter.convertToDatabaseColumn(EXPECTED_MAP), is(EXPECTED_JSON));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionIfMapCannotBeConvertedToString() throws JsonProcessingException {
    when(objectMapper.writeValueAsString(any(Object.class)))
        .thenThrow(new JsonParseException(null, "test"));

    converter.convertToDatabaseColumn(EXPECTED_MAP);
  }

  @Test
  public void shouldConvertToEntityAttribute() {
    assertThat(converter.convertToEntityAttribute(EXPECTED_JSON), is(EXPECTED_MAP));
  }

  @Test
  public void shouldReturnNullForNullValue() {
    assertThat(converter.convertToEntityAttribute(null), is(nullValue()));
  }

  @Test
  public void shouldReturnNullIfStringContainsNullSequence() {
    assertThat(converter.convertToEntityAttribute("null"), is(nullValue()));
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionIfStringCannotBeConvertedToMap() throws IOException {
    when(objectMapper.readValue(EXPECTED_JSON, TYPE_REFERENCE))
        .thenThrow(new IOException("test"));

    converter.convertToEntityAttribute(EXPECTED_JSON);
  }
}
