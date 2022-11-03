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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.connect.data.SchemaBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SinkerConvertObjectUtilTest {

  @InjectMocks
  private SinkerConvertObjectUtil util;

  @Test
  public void shouldReturnNullIfConvertValueIsNull() {
    assertThat(util.convertObjectByType(SchemaBuilder.BYTES_SCHEMA, null)).isNull();
  }

  @Test
  public void shouldReturnOriginalValueIfConvertIsNull() {
    assertThat(util.convertObjectByType(SchemaBuilder.INT8_SCHEMA, 2).getClass()).isEqualTo(Byte.class);
    assertThat(util.convertObjectByType(SchemaBuilder.INT16_SCHEMA, 2).getClass()).isEqualTo(Short.class);
    assertThat(util.convertObjectByType(SchemaBuilder.INT32_SCHEMA, "2")).isEqualTo(2);
    assertThat(util.convertObjectByType(SchemaBuilder.INT64_SCHEMA, "2")).isEqualTo(2L);
    assertThat(util.convertObjectByType(SchemaBuilder.FLOAT32_SCHEMA, 2.22).getClass()).isEqualTo(Float.class);
    assertThat(util.convertObjectByType(SchemaBuilder.FLOAT64_SCHEMA, 2.3333).getClass()).isEqualTo(Double.class);
    assertThat(util.convertObjectByType(SchemaBuilder.BOOLEAN_SCHEMA, "true")).isEqualTo(true);
    assertThat(util.convertObjectByType(SchemaBuilder.STRING_SCHEMA, 2)).isEqualTo("2");
    assertThat(util.convertObjectByType(SchemaBuilder.BYTES_SCHEMA, "dGVzdDQ=").getClass()).isEqualTo(byte[].class);
  }

}
