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

import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Schema.Type;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class SinkerConvertObjectUtil {

  private static final Map<String, Converter> typeToConvertUtils = new LinkedHashMap<>();

  static {
    typeToConvertUtils.put(Type.INT8.getName(), new Int8Converter());
    typeToConvertUtils.put(Type.INT16.getName(), new Int16Converter());
    typeToConvertUtils.put(Type.INT32.getName(), new Int32Converter());
    typeToConvertUtils.put(Type.INT64.getName(), new Int64Converter());
    typeToConvertUtils.put(Type.FLOAT32.getName(), new FloatConverter());
    typeToConvertUtils.put(Type.FLOAT64.getName(), new DoubleConverter());
    typeToConvertUtils.put(Type.BOOLEAN.getName(), new BooleanConverter());
    typeToConvertUtils.put(Type.STRING.getName(), new StringConverter());
    typeToConvertUtils.put(Type.BYTES.getName(), new BytesConverter());
    typeToConvertUtils.put(Decimal.LOGICAL_NAME, new DecimalConverter());
  }

  public Object convertObjectByType(Schema schema, Object value) {
    if (value == null) {
      return null;
    }
    String type = schema.name() != null ? schema.name() : schema.type().getName();
    Converter converter = typeToConvertUtils.get(type);
    return converter == null ? value : converter.convert(value);
  }

  public static class Int8Converter implements Converter {
    @Override
    public Object convert(Object value) {
      return Byte.valueOf(value.toString());
    }
  }

  public static class Int16Converter implements Converter {
    @Override
    public Object convert(Object value) {
      return Short.valueOf(value.toString());
    }
  }

  public static class Int32Converter implements Converter {
    @Override
    public Object convert(Object value) {
      return Integer.valueOf(value.toString());
    }
  }

  public static class Int64Converter implements Converter {
    @Override
    public Object convert(Object value) {
      return Long.valueOf(value.toString());
    }
  }

  public static class FloatConverter implements Converter {
    @Override
    public Object convert(Object value) {
      return Float.valueOf(value.toString());
    }
  }

  public static class DoubleConverter implements Converter {
    @Override
    public Object convert(Object value) {
      return Double.valueOf(value.toString());
    }
  }

  public static class BooleanConverter implements Converter {

    @Override
    public Object convert(Object value) {
      return Boolean.valueOf(value.toString());
    }
  }

  public static class StringConverter implements Converter {
    @Override
    public Object convert(Object value) {
      return String.valueOf(value);
    }
  }

  public static class BytesConverter implements Converter {
    @Override
    public Object convert(Object value) {
      return toByteArray(value);
    }

    private static byte[] toByteArray(Object obj) {
      if (obj instanceof String) {
        return Base64.getDecoder().decode(obj.toString());
      }
      return (byte[]) obj;
    }
  }


  public static class DecimalConverter implements Converter {
    @Override
    public Object convert(Object value) {
      return new BigDecimal(value.toString());
    }
  }

}
