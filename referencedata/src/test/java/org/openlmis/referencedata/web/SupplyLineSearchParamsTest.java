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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.openlmis.referencedata.util.messagekeys.SupplyLineMessageKeys.ERROR_SEARCH_INVALID_PARAMS;

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
import org.springframework.util.MultiValueMap;

@SuppressWarnings("PMD.TooManyMethods")
public class SupplyLineSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String PROGRAM_ID = "programId";
  private static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";
  private static final String SUPPLYING_FACILITY_ID = "supplyingFacilityId";
  private static final String EXPAND = "expand";

  private MultiValueMap<String, Object> queryMap;

  @Before
  public void setUp() {
    queryMap = new LinkedMultiValueMap<>();
  }

  @Test
  public void shouldGetSupplyingFacilityIdsFromParametersIfKeyIsPresent() {
    final UUID supplyingFacilityId1 = UUID.randomUUID();
    final UUID supplyingFacilityId2 = UUID.randomUUID();

    queryMap.add(SUPPLYING_FACILITY_ID, supplyingFacilityId1.toString());
    queryMap.add(SUPPLYING_FACILITY_ID, supplyingFacilityId2.toString());

    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getSupplyingFacilityIds(),
        hasItems(supplyingFacilityId1, supplyingFacilityId2));
  }

  @Test
  public void shouldGetEmptySetIfMapHasNoSupplyingFacilityIdKey() {
    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getSupplyingFacilityIds(), hasSize(0));
  }

  @Test
  public void shouldGetProgramIdFromParametersIfKeyIsPresent() {
    UUID programId = UUID.randomUUID();
    queryMap.add(PROGRAM_ID, programId.toString());

    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getProgramId(), equalTo(programId));
  }

  @Test
  public void shouldGetNullIfMapHasNoProgramIdKey() {
    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getProgramId(), nullValue());
  }

  @Test
  public void shouldGetSupervisoryNodeIdFromParametersIfKeyIsPresent() {
    UUID supervisoryNodeId = UUID.randomUUID();
    queryMap.add(SUPERVISORY_NODE_ID, supervisoryNodeId.toString());

    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getSupervisoryNodeId(), equalTo(supervisoryNodeId));
  }

  @Test
  public void shouldGetNullIfMapHasNoSupervisoryNodeIdKey() {
    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getSupervisoryNodeId(), nullValue());
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_SEARCH_INVALID_PARAMS);

    queryMap.add("some-param", "some-value");
    new SupplyLineSearchParams(queryMap);
  }

  @Test
  public void shouldGetExpandFromParametersIfKeyIsPresent() {
    final String expand1 = "expand1";
    final String expand2 = "expand2";

    queryMap.add(EXPAND, expand1);
    queryMap.add(EXPAND, expand2);

    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getExpand(), hasItems(expand1, expand2));
  }

  @Test
  public void shouldGetEmptySetIfMapHasNoExpandKey() {
    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    assertThat(params.getExpand(), hasSize(0));
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SupplyLineSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    queryMap.add(SUPERVISORY_NODE_ID, UUID.randomUUID().toString());
    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    ToStringTestUtils.verify(SupplyLineSearchParams.class, params,
        "PROGRAM_ID", "SUPERVISORY_NODE_ID", "SUPPLYING_FACILITY_ID", "ALL_PARAMETERS", "EXPAND");
  }
}
