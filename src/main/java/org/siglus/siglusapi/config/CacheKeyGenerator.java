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

package org.siglus.siglusapi.config;

import java.lang.reflect.Method;
import java.util.Map;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Component;

@Component
public class CacheKeyGenerator implements KeyGenerator {

  private static final String DOT = ".";

  @Override
  public Object generate(Object target, Method method, Object... params) {
    if (params.length == 0) {
      return SimpleKey.EMPTY;
    }
    Object param = params[0];
    StringBuilder builder = new StringBuilder();
    // className + methodName + params
    builder.append(target.getClass().getSimpleName())
        .append(DOT)
        .append(method.getName())
        .append(DOT);
    if (param instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) param;
      if (map.isEmpty()) {
        return builder.toString();
      }
      map.keySet().forEach(key -> builder.append(key).append("-").append(map.get(key)).append(DOT));
      return builder.toString();
    }
    return new SimpleKey(builder.toString(), params);
  }
}
