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

import static java.util.Arrays.asList;
import static org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys.ERROR_INVALID_PARAMS;
import static org.openlmis.referencedata.util.messagekeys.ProcessingPeriodMessageKeys.ERROR_SCHEDULE_ID_SINGLE_PARAMETER;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.web.SearchParams;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
public final class ProcessingPeriodSearchParams {

  private static final String PROGRAM_ID = "programId";
  private static final String FACILITY_ID = "facilityId";
  private static final String PROCESSING_SCHEDULE_ID = "processingScheduleId";
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String ID = "id";

  private static final List<String> ALL_PARAMETERS =
      asList(PROGRAM_ID, FACILITY_ID, PROCESSING_SCHEDULE_ID, START_DATE, END_DATE, ID);

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object.
   */
  public ProcessingPeriodSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets program id query parameter.
   * If param value has incorrect UUID format {@link ValidationMessageException} will be thrown.
   *
   * @return UUID value of program id or null if params doesn't contain "programId" key.
   */
  public UUID getProgramId() {
    if (!queryParams.containsKey(PROGRAM_ID)) {
      return null;
    }
    return queryParams.getUuid(PROGRAM_ID);
  }

  /**
   * Gets facility id query parameter.
   * If param value has incorrect UUID format {@link ValidationMessageException} will be thrown.
   *
   * @return UUID value of facility id or null if params doesn't contain "facilityId" key.
   */
  public UUID getFacilityId() {
    if (!queryParams.containsKey(FACILITY_ID)) {
      return null;
    }
    return queryParams.getUuid(FACILITY_ID);
  }

  /**
   * Gets processing schedule id query parameter.
   * If param value has incorrect UUID format {@link ValidationMessageException} will be thrown.
   *
   * @return UUID value of schedule id or null if params doesn't contain "processingScheduleId" key.
   */
  public UUID getProcessingScheduleId() {
    if (!queryParams.containsKey(PROCESSING_SCHEDULE_ID)) {
      return null;
    }
    return queryParams.getUuid(PROCESSING_SCHEDULE_ID);
  }

  /**
   * Gets start date query parameter.
   * If param value has incorrect ISO format {@link ValidationMessageException} will be thrown.
   *
   * @return LocalDate value of start date or null if params doesn't contain "startDate" key.
   */
  public LocalDate getStartDate() {
    if (!queryParams.containsKey(START_DATE)) {
      return null;
    }
    return queryParams.getLocalDate(START_DATE);
  }

  /**
   * Gets end date query parameter.
   * If param value has incorrect ISO format {@link ValidationMessageException} will be thrown.
   *
   * @return LocalDate value of end date or null if params doesn't contain "endDate" key.
   */
  public LocalDate getEndDate() {
    if (!queryParams.containsKey(END_DATE)) {
      return null;
    }
    return queryParams.getLocalDate(END_DATE);
  }

  /**
   * Gets {@link Set} of {@link UUID} for "id" key from params.
   *
   * @return List of ids from params, empty if there is no "id" param
   */
  public Set<UUID> getIds() {
    if (!queryParams.containsKey(ID)) {
      return Collections.emptySet();
    }
    return queryParams.getUuids(ID);
  }

  /**
   * Validates if this search params contains a valid parameters.
   */
  public void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_INVALID_PARAMS));
    }
    if (queryParams.containsKey(PROGRAM_ID)
        && queryParams.containsKey(FACILITY_ID)
        && queryParams.containsKey(PROCESSING_SCHEDULE_ID)) {
      throw new ValidationMessageException(ERROR_SCHEDULE_ID_SINGLE_PARAMETER);
    }
  }
}
