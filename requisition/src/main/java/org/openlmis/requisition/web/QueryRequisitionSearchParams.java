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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_INVALID_REQUISITION_STATUS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SEARCH_INVALID_PARAMS;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.utils.Message;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
final class QueryRequisitionSearchParams implements RequisitionSearchParams {

  private static final String FACILITY = "facility";
  private static final String PROGRAM = "program";
  private static final String INITIATED_DATE_FROM = "initiatedDateFrom";
  private static final String INITIATED_DATE_TO = "initiatedDateTo";
  private static final String PROCESSING_PERIOD = "processingPeriod";
  private static final String SUPERVISORY_NODE = "supervisoryNode";
  private static final String REQUISITION_STATUS = "requisitionStatus";
  private static final String EMERGENCY = "emergency";
  private static final String MODIFIED_DATE_FROM = "modifiedDateFrom";
  private static final String MODIFIED_DATE_TO = "modifiedDateTo";

  private static final List<String> ALL_PARAMETERS = asList(FACILITY, PROGRAM, INITIATED_DATE_FROM,
      INITIATED_DATE_TO, MODIFIED_DATE_FROM, MODIFIED_DATE_TO, PROCESSING_PERIOD,
      SUPERVISORY_NODE, REQUISITION_STATUS, EMERGENCY);

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object.
   */
  QueryRequisitionSearchParams(MultiValueMap<String, String> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets {@link UUID} for "facility" key from params.
   *
   * @return UUID value of facility id or null if params doesn't contain "facility" key.
   */
  @Override
  public UUID getFacility() {
    if (!queryParams.containsKey(FACILITY)) {
      return null;
    }
    return queryParams.getUuid(FACILITY);
  }

  /**
   * Gets {@link UUID} for "program" key from params.
   *
   * @return UUID value of program id or null if params doesn't contain "program" key.
   */
  @Override
  public UUID getProgram() {
    if (!queryParams.containsKey(PROGRAM)) {
      return null;
    }
    return queryParams.getUuid(PROGRAM);
  }

  /**
   * Gets {@link LocalDate} for "initiatedDateFrom" key from params.
   *
   * @return LocalDate value of initiatedDateFrom
   *          or null if params doesn't contain "initiatedDateFrom" key.
   */
  @Override
  public LocalDate getInitiatedDateFrom() {
    if (!queryParams.containsKey(INITIATED_DATE_FROM)) {
      return null;
    }
    return queryParams.getLocalDate(INITIATED_DATE_FROM);
  }

  /**
   * Gets {@link LocalDate} for "initiatedDateTo" key from params.
   *
   * @return LocalDate value of initiatedDateTo
   *          or null if params doesn't contain "initiatedDateTo" key.
   */
  @Override
  public LocalDate getInitiatedDateTo() {
    if (!queryParams.containsKey(INITIATED_DATE_TO)) {
      return null;
    }
    return queryParams.getLocalDate(INITIATED_DATE_TO);
  }

  /**
   * Gets {@link UUID} for "processingPeriod" key from params.
   *
   * @return UUID value of processingPeriod id
   *          or null if params doesn't contain "processingPeriod" key.
   */
  @Override
  public UUID getProcessingPeriod() {
    if (!queryParams.containsKey(PROCESSING_PERIOD)) {
      return null;
    }
    return queryParams.getUuid(PROCESSING_PERIOD);
  }

  /**
   * Gets {@link UUID} for "supervisoryNode" key from params.
   *
   * @return UUID value of supervisoryNode id
   *          or null if params doesn't contain "supervisoryNode" key.
   */
  @Override
  public UUID getSupervisoryNode() {
    if (!queryParams.containsKey(SUPERVISORY_NODE)) {
      return null;
    }
    return queryParams.getUuid(SUPERVISORY_NODE);
  }

  /**
   * Gets {@link RequisitionStatus} for "requisitionStatus" key from params.
   *
   * @return Enum value of Requisition status
   *          or null if params doesn't contain "requisitionStatus" key.
   */
  @Override
  public Set<RequisitionStatus> getRequisitionStatuses() {
    if (!queryParams.containsKey(REQUISITION_STATUS)) {
      return Collections.emptySet();
    }
    Collection<String> values = queryParams.get(REQUISITION_STATUS);
    try {
      return values.stream()
          .map(RequisitionStatus::valueOf)
          .collect(toSet());
    } catch (IllegalArgumentException cause) {
      throw new ValidationMessageException(
          new Message(ERROR_INVALID_REQUISITION_STATUS, values), cause);
    }
  }

  /**
   * Gets value for emergency parameter.
   * If param value has incorrect format {@link ValidationMessageException} will be thrown.
   *
   * @return Boolean value of emergency flag or null if params doesn't contain "emergency" key.
   */
  @Override
  public Boolean getEmergency() {
    if (!queryParams.containsKey(EMERGENCY)) {
      return null;
    }
    return queryParams.getBoolean(EMERGENCY);
  }

  /**
   * Checks if query params are valid. Returns false if any provided param is not on supported list.
   */
  public void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_SEARCH_INVALID_PARAMS));
    }
  }

  /**
   * Gets {@link ZonedDateTime} for "modifiedDateFrom" key from params.
   *
   * @return ZonedDateTime value of modifiedDateFrom
   *          or null if params doesn't contain "modifiedDateFrom" key.
   */
  public ZonedDateTime getModifiedDateFrom() {
    if (!queryParams.containsKey(MODIFIED_DATE_FROM)) {
      return null;
    }
    return queryParams.getZonedDateTime(MODIFIED_DATE_FROM);
  }

  /**
   * Gets {@link ZonedDateTime} for "modifiedDateTo" key from params.
   *
   * @return ZonedDateTime value of modifiedDateTo
   *          or null if params doesn't contain "modifiedDateTo" key.
   */
  public ZonedDateTime getModifiedDateTo() {
    if (!queryParams.containsKey(MODIFIED_DATE_TO)) {
      return null;
    }
    return queryParams.getZonedDateTime(MODIFIED_DATE_TO);
  }
}
