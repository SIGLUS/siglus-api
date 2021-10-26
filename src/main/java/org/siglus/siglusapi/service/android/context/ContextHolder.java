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

package org.siglus.siglusapi.service.android.context;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContextHolder {

  private static final ThreadLocal<Map<Class<?>, Context>> HOLDER = new ThreadLocal<>();

  @SuppressWarnings("unchecked")
  public static <T> T getContext(Class<T> contextType) {
    Map<Class<?>, Context> contexts = HOLDER.get();
    if (contexts == null) {
      throw new IllegalStateException("Not init");
    }
    Context target = contexts.get(contextType);
    if (target == null) {
      throw new IllegalStateException("Can't find the context: " + contextType.getName());
    }
    return (T) target;
  }

  public static void clearContext() {
    HOLDER.remove();
  }

  public static void attachContext(Context context) {
    Map<Class<?>, Context> contexts = HOLDER.get();
    if (contexts == null) {
      contexts = new HashMap<>();
      HOLDER.set(contexts);
    }
    contexts.put(context.getClass(), context);
  }


}
