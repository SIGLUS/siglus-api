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

package org.siglus.common.dto.referencedata;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.siglus.common.util.referencedata.messagekeys.OrderableMessageKeys.ERROR_INVALID_PARAMS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;
import org.siglus.common.exception.referencedata.ValidationMessageException;
import org.siglus.common.util.referencedata.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

@ToString
public class QueryOrderableSearchParams {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String PROGRAM_CODE = "program";
  private static final String ID = "id";

  private static final List<String> ALL_PARAMETERS = Collections.unmodifiableList(Arrays.asList(
      ID, CODE, NAME, PROGRAM_CODE));

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object. Remove parameters that should be managed by
   * {@link Pageable}
   */
  public QueryOrderableSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets code.
   *
   * @return String value of code or null if params doesn't contain "code" param. Empty string
   *         for null request param value.
   */
  public String getCode() {
    if (!queryParams.containsKey(CODE)) {
      return null;
    }

    return defaultIfBlank(queryParams.getFirst(CODE), EMPTY);
  }

  /**
   * Gets name.
   *
   * @return String value of name or null if params doesn't contain "name" param. Empty string
   *         for null request param value.
   */
  public String getName() {
    if (!queryParams.containsKey(NAME)) {
      return null;
    }

    return defaultIfBlank(queryParams.getFirst(NAME), EMPTY);
  }

  /**
   * Gets program code.
   *
   * @return {@link Code} value of program code or null if params doesn't contain "programCode"
   *                      param. Empty Code for request param that has no value.
   */
  public String getProgramCode() {
    if (!queryParams.containsKey(PROGRAM_CODE)) {
      return null;
    }

    return defaultIfBlank(queryParams.getFirst(PROGRAM_CODE), EMPTY);
  }

  public Set<Pair<UUID, Long>> getIdentityPairs() {
    return Collections.emptySet();
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
   * Check if all params are empty.
   */
  public boolean isEmpty() {
    return queryParams.isEmpty();
  }

  private void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_INVALID_PARAMS));
    }
  }
}
