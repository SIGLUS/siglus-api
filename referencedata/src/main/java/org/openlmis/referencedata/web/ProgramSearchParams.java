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
import static org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys.ERROR_INVALID_PARAMS;

import java.util.Collections;
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
public final class ProgramSearchParams {

  private static final String ID = "id";
  private static final String NAME = "name";

  private static final List<String> ALL_PARAMETERS = asList(ID, NAME);

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object.
   */
  public ProgramSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets {@link String} for "name" key from params.
   *
   * @return String value of name or null if params doesn't contain "name" key.
   */
  public String getName() {
    if (!queryParams.containsKey(NAME)) {
      return null;
    }
    return queryParams.getFirst(NAME);
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
   * Checks if query params are valid. Returns false if any provided param is not on supported list.
   */
  public void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_INVALID_PARAMS));
    }
  }
}
