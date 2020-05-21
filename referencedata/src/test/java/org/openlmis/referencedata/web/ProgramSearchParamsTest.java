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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys.ERROR_INVALID_PARAMS;

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

public class ProgramSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String ID = "id";
  private static final String NAME = "name";

  private LinkedMultiValueMap<String, Object> queryMap;

  @Before
  public void setUp() {
    queryMap = new LinkedMultiValueMap<>();
  }

  @Test
  public void shouldGetNameValueFromParameters() {
    queryMap.add(NAME, "name");
    ProgramSearchParams params = new ProgramSearchParams(queryMap);

    assertEquals("name", params.getName());
  }

  @Test
  public void shouldGetNullIfMapHasNoNameProperty() {
    ProgramSearchParams params = new ProgramSearchParams(queryMap);

    assertNull(params.getName());
  }

  @Test
  public void shouldGetIdsFromParameters() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    queryMap.add(ID, id1.toString());
    queryMap.add(ID, id2.toString());
    ProgramSearchParams params = new ProgramSearchParams(queryMap);

    assertThat(params.getIds(), hasItems(id1, id2));
  }

  @Test
  public void shouldGetNullIfMapHasNoIdProperty() {
    ProgramSearchParams params = new ProgramSearchParams(queryMap);

    assertEquals(0, params.getIds().size());
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_INVALID_PARAMS);

    queryMap.add("some-param", "some-value");
    new ProgramSearchParams(queryMap);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(ProgramSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    queryMap.add(NAME, "some-name");
    ProgramSearchParams params = new ProgramSearchParams(queryMap);

    ToStringTestUtils.verify(ProgramSearchParams.class, params, "NAME", "ID", "ALL_PARAMETERS");
  }
}
