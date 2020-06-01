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

package org.openlmis.notification.web;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.notification.testutils.ToStringTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SuppressWarnings("PMD.TooManyMethods")
public class SearchParamsTest {
  private static final String WRONG_FORMAT = "wrong-format";
  private static final String WRONG_FORMAT_TOO_FEW_CHARACTER = "a337ec45-31a0-4f2b-9b2e-a1";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

  @Test
  public void containsKeyShouldReturnTrueIfKeyWasAdded() {
    String someParam = "someParam";
    map.add(someParam, RandomStringUtils.random(5));

    SearchParams searchParams = new SearchParams(map);

    assertTrue(searchParams.containsKey(someParam));
  }

  @Test
  public void containsKeyShouldReturnFalseIfKeyWasNotAdded() {
    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey("notExist"));
  }

  @Test
  public void shouldRemoveAccessTokenParamWhenCreateObject() {
    String accessToken = "access_token";
    map.add(accessToken, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(accessToken));
  }

  @Test
  public void shouldRemovePageParamWhenCreateObject() {
    String page = "page";
    map.add(page, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(page));
  }

  @Test
  public void shouldRemoveSizeParamWhenCreateObject() {
    String size = "size";
    map.add(size, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(size));
  }

  @Test
  public void shouldRemoveSortParamWhenCreateObject() {
    String sort = "sort";
    map.add(sort, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(sort));
  }

  @Test
  public void shouldGetUuidsFromStrings() {
    String key = "id";
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();
    map.add(key, id1.toString());
    map.add(key, id2.toString());
    map.add(key, id3.toString());

    SearchParams searchParams = new SearchParams(map);

    assertThat(searchParams.getUuids(key), hasItems(id1, id2, id3));
  }

  @Test
  public void shouldThrowExceptionIfIdHasWrongFormatOnGetUuids() {
    exception.expect(ValidationException.class);

    String key = "id";

    map.add(key, UUID.randomUUID().toString());
    map.add(key, UUID.randomUUID().toString());
    map.add(key, WRONG_FORMAT_TOO_FEW_CHARACTER);
    map.add(key, WRONG_FORMAT);

    SearchParams searchParams = new SearchParams(map);
    searchParams.getUuids(key);
  }

  @Test
  public void shouldGetUuidFromString() {
    String key = "id";
    UUID id = UUID.randomUUID();
    map.add(key, id.toString());

    SearchParams searchParams = new SearchParams(map);

    assertEquals(id, searchParams.getUuid(key));
  }

  @Test
  public void shouldThrowExceptionIfIdHasWrongFormatOnGetUuid() {
    exception.expect(ValidationException.class);

    String key = "id";
    map.add(key, WRONG_FORMAT);

    SearchParams searchParams = new SearchParams(map);
    searchParams.getUuid(key);
  }


  @Test
  public void shouldGetZonedDateTimeFromString() {
    String key = "dateTime";
    ZonedDateTime dateTime = ZonedDateTime.now();
    map.add(key, dateTime.toString());

    SearchParams searchParams = new SearchParams(map);

    assertEquals(dateTime, searchParams.getZonedDateTime(key));
  }

  @Test
  public void shouldThrowExceptionIfDateHasWrongFormat() {
    exception.expect(ValidationException.class);

    String key = "date";
    map.add(key, WRONG_FORMAT);

    SearchParams searchParams = new SearchParams(map);
    searchParams.getZonedDateTime(key);
  }

  @Test
  public void shouldThrowExceptionIfZonedDateTimeHasWrongFormat() {
    exception.expect(ValidationException.class);

    String key = "dateTime";
    map.add(key, WRONG_FORMAT);

    SearchParams searchParams = new SearchParams(map);
    searchParams.getZonedDateTime(key);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("key", "value");
    SearchParams params = new SearchParams(map);

    ToStringTestUtils.verify(SearchParams.class, params,
        "PAGE", "SIZE", "SORT", "ZONE_ID", "ACCESS_TOKEN");
  }
}
