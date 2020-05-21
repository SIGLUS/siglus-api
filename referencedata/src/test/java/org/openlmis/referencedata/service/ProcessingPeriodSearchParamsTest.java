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

package org.openlmis.referencedata.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys.ERROR_INVALID_PARAMS;
import static org.openlmis.referencedata.util.messagekeys.ProcessingPeriodMessageKeys.ERROR_SCHEDULE_ID_SINGLE_PARAMETER;

import java.time.LocalDate;
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
public class ProcessingPeriodSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private static final String PROGRAM_ID = "programId";
  private static final String FACILITY_ID = "facilityId";
  private static final String PROCESSING_SCHEDULE_ID = "processingScheduleId";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String ID = "id";

  private LinkedMultiValueMap<String, Object> queryMap;

  private UUID id = UUID.randomUUID();
  private String dateString = "2018-07-02";
  private LocalDate date = LocalDate.of(2018, 7, 2);

  @Before
  public void setUp() {
    queryMap = new LinkedMultiValueMap<>();
  }

  @Test
  public void shouldGetProgramIdValueFromParameters() {
    queryMap.add(PROGRAM_ID, id.toString());
    queryMap.add(FACILITY_ID, "");
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertEquals(id, params.getProgramId());
  }

  @Test
  public void shouldGetFacilityIdValueFromParameters() {
    queryMap.add(PROGRAM_ID, "");
    queryMap.add(FACILITY_ID, id.toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertEquals(id, params.getFacilityId());
  }

  @Test
  public void shouldGetProcessingScheduleIdValueFromParameters() {
    queryMap.add(PROCESSING_SCHEDULE_ID, id.toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertEquals(id, params.getProcessingScheduleId());
  }

  @Test
  public void shouldGetNullIfMapHasNoProcessingScheduleIdProperty() {
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertNull(params.getProcessingScheduleId());
  }

  @Test
  public void shouldGetStartDateValueFromParameters() {
    queryMap.add(START_DATE, dateString);
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertEquals(date, params.getStartDate());
  }

  @Test
  public void shouldGetNullIfMapHasNoStartDateProperty() {
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertNull(params.getStartDate());
  }

  @Test
  public void shouldGetEndDateValueFromParameters() {
    queryMap.add(END_DATE, dateString);
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertEquals(date, params.getEndDate());
  }

  @Test
  public void shouldGetNullIfMapHasNoEndDateProperty() {
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertNull(params.getEndDate());
  }

  @Test
  public void shouldGetIdsFromParameters() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    queryMap.add(ID, id1.toString());
    queryMap.add(ID, id2.toString());
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertThat(params.getIds(), hasItems(id1, id2));
  }

  @Test
  public void shouldGetNullIfMapHasNoIdProperty() {
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);

    assertEquals(0, params.getIds().size());
  }

  @Test
  public void shouldThrowExceptionIfProgramIdFacilityIdAndScheduleIdAreSet() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_SCHEDULE_ID_SINGLE_PARAMETER);

    queryMap.add(PROGRAM_ID, "program");
    queryMap.add(FACILITY_ID, "facility");
    queryMap.add(PROCESSING_SCHEDULE_ID, "schedule");

    new ProcessingPeriodSearchParams(queryMap);
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_INVALID_PARAMS);

    queryMap.add("some-param", "some-value");
    new ProcessingPeriodSearchParams(queryMap);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(ProcessingPeriodSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in DTO
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    queryMap.add(PROGRAM_ID, "some-program");
    queryMap.add(FACILITY_ID, "some-facility");
    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(queryMap);
    ToStringTestUtils.verify(ProcessingPeriodSearchParams.class, params, "PROGRAM_ID",
        "FACILITY_ID", "PROCESSING_SCHEDULE_ID", "START_DATE", "END_DATE", "ID", "ALL_PARAMETERS");
  }

}
