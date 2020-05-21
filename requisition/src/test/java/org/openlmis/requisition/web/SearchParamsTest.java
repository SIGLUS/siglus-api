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

package org.openlmis.requisition.web;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import be.joengenduvel.java.verifiers.ToStringVerifier;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.requisition.dto.ToStringContractTest;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SuppressWarnings("PMD.TooManyMethods")
public class SearchParamsTest extends ToStringContractTest<SearchParams> {

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
  public void isEmptyShouldReturnTrueIfNoValueMapIsProvided() {
    SearchParams searchParams = new SearchParams();

    assertTrue(searchParams.isEmpty());
  }

  @Test
  public void isEmptyShouldReturnTrueIfValueMapIsNull() {
    SearchParams searchParams = new SearchParams(null);

    assertTrue(searchParams.isEmpty());
  }

  @Test
  public void shouldRemoveAccessTokenParamWhenCreatingObject() {
    String accessToken = "access_token";
    map.add(accessToken, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(accessToken));
  }

  @Test
  public void shouldRemovePageParamWhenCreatingObject() {
    String page = "page";
    map.add(page, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(page));
  }

  @Test
  public void shouldRemoveSizeParamWhenCreatingObject() {
    String size = "size";
    map.add(size, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(size));
  }

  @Test
  public void shouldRemoveSortParamWhenCreatingObject() {
    String sort = "sort";
    map.add(sort, UUID.randomUUID().toString());

    SearchParams searchParams = new SearchParams(map);

    assertFalse(searchParams.containsKey(sort));
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
  public void shouldThrowExceptionIfIdHasWrongFormat() {
    exception.expect(ValidationMessageException.class);

    String key = "id";
    map.add(key, "wrong-format");

    SearchParams searchParams = new SearchParams(map);
    searchParams.getUuid(key);
  }

  @Test
  public void shouldGetLocalDateFromString() {
    String key = "date";
    LocalDate date = LocalDate.now();
    map.add(key, date.toString());

    SearchParams searchParams = new SearchParams(map);

    assertEquals(date, searchParams.getLocalDate(key));
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
    exception.expect(ValidationMessageException.class);

    String key = "date";
    map.add(key, "wrong-format");

    SearchParams searchParams = new SearchParams(map);
    searchParams.getLocalDate(key);
    searchParams.getZonedDateTime(key);
  }

  @Test
  public void shouldThrowExceptionIfZonedDateTimeHasWrongFormat() {
    exception.expect(ValidationMessageException.class);

    String key = "dateTime";
    map.add(key, "wrong-format");

    SearchParams searchParams = new SearchParams(map);
    searchParams.getZonedDateTime(key);
  }

  @Test
  public void shouldGetBooleanFromString() {
    String key = "boolean";
    map.add(key, "true");

    SearchParams searchParams = new SearchParams(map);

    assertTrue(searchParams.getBoolean(key));
  }

  @Test
  public void shouldThrowExceptionIfBooleanHasWrongFormat() {
    exception.expect(ValidationMessageException.class);

    String key = "boolean";
    map.add(key, "aaaaa");

    SearchParams searchParams = new SearchParams(map);
    searchParams.getBoolean(key);
  }

  @Override
  protected Class<SearchParams> getTestClass() {
    return SearchParams.class;
  }

  @Override
  protected void prepare(ToStringVerifier<SearchParams> verifier) {
    verifier.ignore("PAGE", "SIZE", "SORT", "ZONE_ID", "ACCESS_TOKEN");
  }

}
