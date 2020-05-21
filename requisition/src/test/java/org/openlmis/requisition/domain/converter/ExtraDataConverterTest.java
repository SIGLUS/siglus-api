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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ExtraDataConverterTest {

  private ExtraDataConverter converter;

  private Map<String, Object> extraData;
  private String extraDataAsString;

  private String expectedDatabaseValue;
  private Map<String, String> expectedEntityAttribute;

  private boolean throwException;

  /**
   * Constructor for parameterized test.
   */
  public ExtraDataConverterTest(Map<String, Object> extraData, String extraDataAsString,
      String expectedDatabaseValue, Map<String, String> expectedEntityAttribute,
      boolean throwException) {
    this.extraData = extraData;
    this.extraDataAsString = extraDataAsString;

    this.expectedDatabaseValue = expectedDatabaseValue;
    this.expectedEntityAttribute = expectedEntityAttribute;

    ObjectMapper objectMapper = spy(ObjectMapper.class);
    this.converter = new ExtraDataConverter(objectMapper);

    this.throwException = throwException;

    if (throwException) {
      doThrowException(objectMapper);
    }
  }

  /**
   * Data for tests.
   */
  @Parameters(name = "extraData = {0}, extraDataAsString = {1},"
      + "expectedDatabaseValue = {2}, expectedEntityAttribute = {3}")
  public static Collection<Object[]> data() {
    List<Object[]> data = Lists.newArrayList();

    // converter should handle null values
    data.add(new Object[]{
        null, null,
        null, Maps.newHashMap(),
        false});

    // converter should handle empty values
    data.add(new Object[]{
        Maps.newHashMap(), "",
        null, Maps.newHashMap(),
        false});
    data.add(new Object[]{
        Maps.newHashMap(), "     ",
        null, Maps.newHashMap(),
        false});

    // converter should handle normal values
    data.add(new Object[]{
        ImmutableMap.of("a", "b"), "{\"c\":\"d\"}",
        "{\"a\":\"b\"}", ImmutableMap.of("c", "d"),
        false});

    // converter should handle array or collection values
    String resourceId = UUID.randomUUID().toString();
    data.add(new Object[]{
        ImmutableMap.of("e", Lists.newArrayList(1, 2, 3), "f", Lists.newArrayList(resourceId)),
        "{\"g\":[\"" + resourceId + "\"],\"h\":[4,5,6]}",
        "{\"e\":[1,2,3],\"f\":[\"" + resourceId + "\"]}",
        ImmutableMap.of("g", Lists.newArrayList(resourceId), "h", Lists.newArrayList(4, 5, 6)),
        false});

    // converter should return default values if exception occurs
    data.add(new Object[]{
        ImmutableMap.of("e", "f"), "{\"g\":\"h\"}",
        null, Maps.newHashMap(),
        true});

    return data;
  }

  @Test
  public void shouldConvertToDatabaseColumn() {
    assertThat(converter.convertToDatabaseColumn(extraData))
        .isEqualTo(expectedDatabaseValue);
  }

  @Test
  public void shouldConvertToEntityAttribute() {
    assertThat(converter.convertToEntityAttribute(extraDataAsString))
        .isEqualTo(expectedEntityAttribute);
  }

  @Test
  public void shouldBeAbleToBackToOriginalValue() {
    // assumeThat causes the following test will not
    // be executed if the throwException has been set.
    Assume.assumeThat(throwException, is(false));

    String databaseColumn = converter.convertToDatabaseColumn(extraData);
    Map<String, Object> newExtraData = converter.convertToEntityAttribute(databaseColumn);

    if (null == extraData) {
      assertThat(newExtraData).isNotNull().isEmpty();
    } else {
      assertThat(newExtraData).isEqualTo(extraData);
    }

    Map<String, Object> entityAttribute = converter.convertToEntityAttribute(extraDataAsString);
    String newExtraDataAsString = converter.convertToDatabaseColumn(entityAttribute);

    if (isBlank(extraDataAsString)) {
      assertThat(newExtraDataAsString).isNull();
    } else {
      assertThat(newExtraDataAsString).isEqualTo(extraDataAsString);
    }
  }

  private static void doThrowException(ObjectMapper mapper) {
    try {
      doThrow(mock(JsonProcessingException.class))
          .when(mapper)
          .writeValueAsString(anyMapOf(String.class, String.class));

      doThrow(new IOException())
          .when(mapper)
          .readValue(anyString(), any(TypeReference.class));
    } catch (IOException exp) {
      throw new IllegalStateException(exp);
    }
  }
}
