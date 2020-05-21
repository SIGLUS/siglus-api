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

import static java.util.Arrays.asList;
import static org.openlmis.referencedata.util.messagekeys.SupplyLineMessageKeys.ERROR_SEARCH_INVALID_PARAMS;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
public final class SupplyLineSearchParams {

  private static final String PROGRAM_ID = "programId";
  private static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";
  private static final String SUPPLYING_FACILITY_ID = "supplyingFacilityId";
  private static final String EXPAND = "expand";

  private static final List<String> ALL_PARAMETERS =
      asList(PROGRAM_ID, SUPERVISORY_NODE_ID, SUPPLYING_FACILITY_ID, EXPAND);

  private SearchParams queryParams;

  /**
   * Wraps map of query params with an object.
   */
  public SupplyLineSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Returns a set of supplying facility IDs from the request parameters.
   *
   * @return Set of supplying facility ids from params,
   *         empty if there is no "supplyingFacilityId" param
   */
  public Set<UUID> getSupplyingFacilityIds() {
    return queryParams.getUuids(SUPPLYING_FACILITY_ID);
  }

  /**
   * Returns program id.
   * If param value has incorrect format {@link ValidationMessageException} will be thrown.
   *
   * @return UUID value of program id or null if params doesn't contain "programId" key.
   */
  public UUID getProgramId() {
    return queryParams.getUuid(PROGRAM_ID);
  }

  /**
   * Returns supervisory node id.
   * If param value has incorrect format {@link ValidationMessageException} will be thrown.
   *
   * @return UUID value of supervisory node id
   *         or null if params doesn't contain "supervisoryNodeId" key.
   */
  public UUID getSupervisoryNodeId() {
    return queryParams.getUuid(SUPERVISORY_NODE_ID);
  }

  /**
   * Returns supervisory node id.
   * If param value has incorrect format {@link ValidationMessageException} will be thrown.
   *
   * @return UUID value of supervisory node id
   *         or null if params doesn't contain "supervisoryNodeId" key.
   */
  public Set<String> getExpand() {
    return queryParams.getStrings(EXPAND);
  }

  /**
   * Checks if query params are valid. Returns false if any provided param is not on supported list.
   */
  public void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_SEARCH_INVALID_PARAMS));
    }
  }
}
