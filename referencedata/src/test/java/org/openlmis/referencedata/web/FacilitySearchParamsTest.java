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

package org.openlmis.referencedata.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys.ERROR_INVALID_PARAMS;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.springframework.util.LinkedMultiValueMap;

@SuppressWarnings("PMD.TooManyMethods")
public class FacilitySearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String FACILITY_TYPE_CODE = "type";
  private static final String ZONE_ID = "zoneId";
  private static final String RECURSE = "recurse";
  private static final String EXTRA_DATA = "extraData";

  private LinkedMultiValueMap<String, Object> queryMap;

  @Before
  public void setUp() {
    queryMap = new LinkedMultiValueMap<>();
  }

  @Test
  public void shouldGetCodeValueFromParameters() {
    queryMap.add(CODE, "code");
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertEquals("code", params.getCode());
  }

  @Test
  public void shouldGetNullIfMapHasNoCodeProperty() {
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertNull(params.getCode());
  }

  @Test
  public void shouldGetNameValueFromParameters() {
    queryMap.add(NAME, "name");
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertEquals("name", params.getName());
  }

  @Test
  public void shouldGetNullIfMapHasNoNameProperty() {
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertNull(params.getName());
  }

  @Test
  public void shouldGetTypeValueFromParameters() {
    queryMap.add(FACILITY_TYPE_CODE, "type");
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertEquals("type", params.getFacilityTypeCode());
  }

  @Test
  public void shouldGetNullIfMapHasNoTypeProperty() {
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertNull(params.getFacilityTypeCode());
  }

  @Test
  public void shouldGetZoneIdValueFromParameters() {
    UUID id = UUID.randomUUID();
    queryMap.add(ZONE_ID, id.toString());
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertEquals(id, params.getZoneId());
  }

  @Test
  public void shouldGetNullIfMapHasNoZoneIdProperty() {
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertNull(params.getZoneId());
  }

  @Test
  public void shouldGetRecurseValueFromParameters() {
    queryMap.add(RECURSE, true);
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertTrue(params.isRecurse());

    queryMap.set(RECURSE, false);
    params = new FacilitySearchParams(queryMap);

    assertFalse(params.isRecurse());
  }

  @Test
  public void shouldGetNullIfMapHasNoRecurseProperty() {
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertFalse(params.isRecurse());
  }

  @Test
  public void shouldGetExtraDataValueFromParameters() {
    Map<String, String> map = ImmutableMap.of("key", "value");
    queryMap.add(EXTRA_DATA, map);
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertEquals(map, params.getExtraData());
  }

  @Test
  public void shouldGetNullIfMapHasNoExtraDataProperty() {
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    assertNull(params.getExtraData());
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_INVALID_PARAMS);

    queryMap.add("some-param", "some-value");
    new FacilitySearchParams(queryMap);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(FacilitySearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    queryMap.add(NAME, "some-name");
    FacilitySearchParams params = new FacilitySearchParams(queryMap);

    ToStringTestUtils.verify(FacilitySearchParams.class, params,
        "CODE", "NAME", "FACILITY_TYPE_CODE", "ZONE_ID", "RECURSE", "EXTRA_DATA",
        "ALL_PARAMETERS", "ID");
  }
}
