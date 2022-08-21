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

package org.siglus.siglusapi.util;

import java.util.ArrayList;
import java.util.List;

public class CustomListSortHelper {

  public static <T> List<List<T>> averageAssign(List<T> source, int n) {
    List<List<T>> result = new ArrayList<>();
    int remaider = source.size() % n;
    int number = source.size() / n;
    int offset = 0;
    for (int i = 0; i < n; i++) {
      List<T> value = null;
      if (remaider > 0) {
        value = source.subList(i * number + offset, (i + 1) * number + offset + 1);
        remaider--;
        offset++;
      } else {
        value = source.subList(i * number + offset, (i + 1) * number + offset);
      }
      result.add(value);
    }
    return result;
  }

}
