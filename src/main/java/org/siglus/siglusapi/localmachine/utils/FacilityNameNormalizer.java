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

package org.siglus.siglusapi.localmachine.utils;

import java.io.CharArrayWriter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public class FacilityNameNormalizer {

  private static final Map<Character, Character> charToNormalizedChar = new HashMap<>();

  private static final char UNDERSCORE = '_';
  private static final int FACILITY_NAME_MAX_LENGTH = 75;

  static {
    charToNormalizedChar.put('á', 'a');
    charToNormalizedChar.put('â', 'a');
    charToNormalizedChar.put('ã', 'a');
    charToNormalizedChar.put('à', 'a');
    charToNormalizedChar.put('é', 'e');
    charToNormalizedChar.put('ê', 'e');
    charToNormalizedChar.put('è', 'e');
    charToNormalizedChar.put('í', 'i');
    charToNormalizedChar.put('ì', 'i');
    charToNormalizedChar.put('ó', 'o');
    charToNormalizedChar.put('ô', 'o');
    charToNormalizedChar.put('õ', 'o');
    charToNormalizedChar.put('ò', 'o');
    charToNormalizedChar.put('ú', 'u');
    charToNormalizedChar.put('ù', 'u');
    charToNormalizedChar.put('ç', 'c');
    charToNormalizedChar.put('Á', 'A');
    charToNormalizedChar.put('Â', 'A');
    charToNormalizedChar.put('Ã', 'A');
    charToNormalizedChar.put('À', 'A');
    charToNormalizedChar.put('É', 'E');
    charToNormalizedChar.put('Ê', 'E');
    charToNormalizedChar.put('È', 'E');
    charToNormalizedChar.put('Í', 'I');
    charToNormalizedChar.put('Ì', 'I');
    charToNormalizedChar.put('Ó', 'O');
    charToNormalizedChar.put('Ô', 'O');
    charToNormalizedChar.put('Õ', 'O');
    charToNormalizedChar.put('Ò', 'O');
    charToNormalizedChar.put('Ú', 'U');
    charToNormalizedChar.put('Ù', 'U');
    charToNormalizedChar.put('Ç', 'C');
    charToNormalizedChar.put(' ', UNDERSCORE);
    charToNormalizedChar.put('*', UNDERSCORE);
    charToNormalizedChar.put('\\', UNDERSCORE);
    charToNormalizedChar.put('/', UNDERSCORE);
    charToNormalizedChar.put('?', UNDERSCORE);
    charToNormalizedChar.put('|', UNDERSCORE);
    charToNormalizedChar.put('&', UNDERSCORE);
    charToNormalizedChar.put(',', UNDERSCORE);
    charToNormalizedChar.put('.', UNDERSCORE);
  }

  private FacilityNameNormalizer() {}

  public static synchronized String normalize(String facilityName) {
    if (StringUtils.isEmpty(facilityName)) {
      return facilityName;
    }
    CharArrayWriter writer = new CharArrayWriter(facilityName.length());
    boolean previousIsUnderscore = false;
    for (char ch : facilityName.toCharArray()) {
      Character normalizedChar = charToNormalizedChar.getOrDefault(ch, ch);
      if (previousIsUnderscore && normalizedChar == UNDERSCORE) {
        continue;
      }
      previousIsUnderscore = normalizedChar == UNDERSCORE;
      writer.append(normalizedChar);
    }
    String normalizedName = String.valueOf(writer.toCharArray());
    if (normalizedName.length() > FACILITY_NAME_MAX_LENGTH) {
      return normalizedName.substring(0, FACILITY_NAME_MAX_LENGTH);
    }
    return normalizedName;
  }
}
