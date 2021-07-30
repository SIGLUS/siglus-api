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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HashEncoderTest {

  @Test
  public void shouldHashContentBySha512AndUtf8EncodedByBase62() {
    // given
    String content = "Admin123";
    String hashedContent =
        "TQskzK3iiLfbRVHeM1muvBCiiKriibfl6lh8ipo91hb74G3OvsybvkzpPI4S3KIeWTXAiiwlUU0iiSxWii4wSuS8mokSAieie";

    // when
    String result = HashEncoder.hash(content);

    // then
    assertThat(result, is(equalTo(hashedContent)));
    assertEquals(hashedContent, result);
  }

  @Test
  public void shouldNotContainCharactersNotPermittedByBase62WhenReturningHashContentBySha512AndUtf8() {
    // given
    String content = "Admin123+=/\n";

    // when
    String result = HashEncoder.hash(content);

    // then
    assertFalse(result.contains("/"));
    assertFalse(result.contains("="));
    assertFalse(result.contains("+"));
  }

  @Test
  public void shouldReturnNullWhenContentIsNull() {
    // when
    String result = HashEncoder.hash(null);

    // then
    assertNull(result);
  }

}
