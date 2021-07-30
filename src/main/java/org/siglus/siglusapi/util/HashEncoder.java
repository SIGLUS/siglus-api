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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class HashEncoder {

  private static final String HASH_ALGORITHM = "SHA-512";

  public static String hash(String content) {
    if (content == null) {
      return null;
    }
    try {
      MessageDigest msgDigest = MessageDigest.getInstance(HASH_ALGORITHM);
      msgDigest.update(content.getBytes(StandardCharsets.UTF_8));
      byte[] rawByte = msgDigest.digest();
      byte[] encodedBytes = Base64.encodeBase64(rawByte);
      return base64ToBase62(new String(encodedBytes));
    } catch (NoSuchAlgorithmException e) {
      log.error(e.getMessage());
      return null;
    }
  }

  private static String base64ToBase62(String base64) {
    StringBuilder buf = new StringBuilder(base64.length() * 2);
    for (int i = 0; i < base64.length(); i++) {
      char ch = base64.charAt(i);
      switch (ch) {
        case 'i':
          buf.append("ii");
          break;
        case '+':
          buf.append("ip");
          break;
        case '/':
          buf.append("is");
          break;
        case '=':
          buf.append("ie");
          break;
        case '\n':
          // Strip out
          break;
        default:
          buf.append(ch);
      }
    }
    return buf.toString();
  }
}
