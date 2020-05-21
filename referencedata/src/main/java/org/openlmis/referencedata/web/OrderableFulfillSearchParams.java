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

import static org.openlmis.referencedata.util.messagekeys.OrderableFulFillMessageKeys.ERROR_IDS_CANNOT_BY_PROVIDED_TOGETHER_WITH_FACILITY_ID_AND_PROGRAM_ID;
import static org.openlmis.referencedata.util.messagekeys.OrderableFulFillMessageKeys.ERROR_INVALID_PARAMS;
import static org.openlmis.referencedata.util.messagekeys.OrderableFulFillMessageKeys.ERROR_PROVIDED_FACILITY_ID_WITHOUT_PROGRAM_ID;
import static org.openlmis.referencedata.util.messagekeys.OrderableFulFillMessageKeys.ERROR_PROVIDED_PROGRAM_ID_WITHOUT_FACILITY_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
public final class OrderableFulfillSearchParams {
  public static final String ID = "id";
  public static final String FACILITY_ID = "facilityId";
  public static final String PROGRAM_ID = "programId";

  private static final List<String> ALL_PARAMETERS = Collections.unmodifiableList(Arrays.asList(
      FACILITY_ID, PROGRAM_ID, ID));

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object. Remove parameters that should be managed by
   * {@link Pageable}
   */
  public OrderableFulfillSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets FacilityTypeId.
   *
   * @return UUID value of facilityId or null if params doesn't contain "facilityId" param.
   */
  public UUID getFacilityId() {
    if (!queryParams.containsKey(FACILITY_ID)) {
      return null;
    }
    return queryParams.getUuid(FACILITY_ID);
  }

  /**
   * Gets ProgramId.
   *
   * @return UUID value of programId or null if params doesn't contain "programId" param.
   */
  public UUID getProgramId() {
    if (!queryParams.containsKey(PROGRAM_ID)) {
      return null;
    }
    return queryParams.getUuid(PROGRAM_ID);
  }

  /**
   * Gets and collection of {@link UUID} for "ids" key from params.
   */
  public Set<UUID> getIds() {
    if (!queryParams.containsKey(ID)) {
      return Collections.emptySet();
    }
    return queryParams.getUuids(ID);
  }

  /**
   * Checks if provided params which allows to search by facilityId and programId.
   */
  public boolean isSearchByFacilityIdAndProgramId() {
    return getFacilityId() != null && getProgramId() != null;
  }

  private void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_INVALID_PARAMS));
    }
    if (queryParams.containsKey(FACILITY_ID) && !queryParams.containsKey(PROGRAM_ID)) {
      throw new ValidationMessageException(
          new Message(ERROR_PROVIDED_FACILITY_ID_WITHOUT_PROGRAM_ID));
    }
    if (!queryParams.containsKey(FACILITY_ID) && queryParams.containsKey(PROGRAM_ID)) {
      throw new ValidationMessageException(
          new Message(ERROR_PROVIDED_PROGRAM_ID_WITHOUT_FACILITY_ID));
    }
    if (queryParams.containsKey(ID)
        && (queryParams.containsKey(FACILITY_ID) || queryParams.containsKey(PROGRAM_ID))) {
      throw new ValidationMessageException(
          new Message(ERROR_IDS_CANNOT_BY_PROVIDED_TOGETHER_WITH_FACILITY_ID_AND_PROGRAM_ID));
    }
  }
}
