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

import static java.util.Collections.emptySet;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.openlmis.referencedata.web.SupervisoryNodeSearchParams.CODE_PARAM;
import static org.openlmis.referencedata.web.SupervisoryNodeSearchParams.FACILITY_ID;
import static org.openlmis.referencedata.web.SupervisoryNodeSearchParams.ID;
import static org.openlmis.referencedata.web.SupervisoryNodeSearchParams.NAME_PARAM;
import static org.openlmis.referencedata.web.SupervisoryNodeSearchParams.PROGRAM_ID;
import static org.openlmis.referencedata.web.SupervisoryNodeSearchParams.ZONE_ID;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.springframework.util.LinkedMultiValueMap;

@SuppressWarnings({"PMD.TooManyMethods"})
public class SupervisoryNodeSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldGetCodeValueFromParameters() {
    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    queryMap.add(CODE_PARAM, "some-code");
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryMap);

    assertEquals("some-code", params.getCode());
  }

  @Test
  public void shouldAssignNullIfCodeIsAbsentInParameters() {
    SupervisoryNodeSearchParams params =
        new SupervisoryNodeSearchParams(new LinkedMultiValueMap<>());

    assertNull(params.getCode());
  }

  @Test
  public void shouldGetNameValueFromParameters() {
    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    queryMap.add(NAME_PARAM, "some-name");
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryMap);

    assertEquals("some-name", params.getName());
  }

  @Test
  public void shouldAssignNullIfNameIsAbsentInParameters() {
    SupervisoryNodeSearchParams params =
        new SupervisoryNodeSearchParams(new LinkedMultiValueMap<>());

    assertNull(params.getName());
  }

  @Test
  public void shouldGetZoneIdValueFromParameters() {
    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    UUID zoneId = UUID.randomUUID();
    queryMap.add(ZONE_ID, zoneId.toString());
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryMap);

    assertEquals(zoneId, params.getZoneId());
  }

  @Test
  public void shouldAssignNullIfZoneIdIsAbsentInParameters() {
    SupervisoryNodeSearchParams params =
        new SupervisoryNodeSearchParams(new LinkedMultiValueMap<>());

    assertNull(params.getZoneId());
  }

  @Test
  public void shouldGetFacilityIdValueFromParameters() {
    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    UUID facilityId = UUID.randomUUID();
    queryMap.add(FACILITY_ID, facilityId.toString());
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryMap);

    assertEquals(facilityId, params.getFacilityId());
  }

  @Test
  public void shouldAssignNullIfFacilityIdIsAbsentInParameters() {
    SupervisoryNodeSearchParams params =
        new SupervisoryNodeSearchParams(new LinkedMultiValueMap<>());

    assertNull(params.getFacilityId());
  }

  @Test
  public void shouldGetProgramIdValueFromParameters() {
    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    UUID programId = UUID.randomUUID();
    queryMap.add(PROGRAM_ID, programId.toString());
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryMap);

    assertEquals(programId, params.getProgramId());
  }

  @Test
  public void shouldAssignNullIfProgramIdIsAbsentInParameters() {
    SupervisoryNodeSearchParams params =
        new SupervisoryNodeSearchParams(new LinkedMultiValueMap<>());

    assertNull(params.getProgramId());
  }

  @Test
  public void shouldGetIdsFromParameters() {
    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    UUID id1 = UUID.randomUUID();
    queryMap.add(ID, id1.toString());
    UUID id2 = UUID.randomUUID();
    queryMap.add(ID, id2.toString());
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryMap);

    assertEquals(asSet(id1, id2), params.getIds());
  }

  @Test
  public void shouldAssignEmptySetIfIdsAreAbsentInParameters() {
    SupervisoryNodeSearchParams params =
        new SupervisoryNodeSearchParams(new LinkedMultiValueMap<>());

    assertEquals(emptySet(), params.getIds());
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_INVALID_PARAMS);

    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();
    queryMap.add("someParameter", "some-value");
    new SupervisoryNodeSearchParams(queryMap);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SupervisoryNodeSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in search params object
        .withIgnoredFields("queryParams")
        .verify();
  }


}
