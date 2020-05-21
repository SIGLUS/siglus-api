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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.UuidUtil;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.springframework.util.MultiValueMap;

@ToString
@EqualsAndHashCode(exclude = "queryParams")
@NoArgsConstructor
public final class SupervisoryNodeSearchParams {

  static final String FACILITY_ID = "facilityId";
  static final String PROGRAM_ID = "programId";
  static final String ZONE_ID = "zoneId";
  static final String NAME_PARAM = "name";
  static final String CODE_PARAM = "code";
  static final String ID = "id";

  @Getter
  @Setter
  private String name;

  @Getter
  @Setter
  private String code;

  @Getter
  @Setter
  private UUID facilityId;

  @Getter
  @Setter
  private UUID programId;

  @Getter
  @Setter
  private UUID zoneId;

  @Getter
  @Setter
  private Set<UUID> ids;

  private SearchParams queryParams;

  /**
   * Retrieves query parameters from multi value map and assigns them to properties.
   * If in the map there are parameters unrecognizable by this class exception will be thrown.
   */
  SupervisoryNodeSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    if (!isValid()) {
      throw new ValidationMessageException(
          SupervisoryNodeMessageKeys.ERROR_INVALID_PARAMS);
    }

    this.name = getSingleStringValue(queryMap, NAME_PARAM);
    this.code = getSingleStringValue(queryMap, CODE_PARAM);
    this.facilityId = getSingleUuidValue(queryMap, FACILITY_ID);
    this.programId = getSingleUuidValue(queryMap, PROGRAM_ID);
    this.zoneId = getSingleUuidValue(queryMap, ZONE_ID);
    this.ids = UuidUtil.getIds(queryMap);
  }

  /**
   * Constructs new instance of {@code SupervisoryNodeSearchParams}.
   */
  public SupervisoryNodeSearchParams(String name, String code, UUID facilityId, UUID programId,
      UUID zoneId, Set<UUID> ids) {
    queryParams = new SearchParams();
    this.name = name;
    this.code = code;
    this.facilityId = facilityId;
    this.programId = programId;
    this.zoneId = zoneId;
    this.ids = ids;
  }

  private boolean isValid() {
    return Collections.unmodifiableList(
        Arrays.asList(ID, CODE_PARAM, NAME_PARAM, FACILITY_ID, PROGRAM_ID, ZONE_ID))
        .containsAll(queryParams.keySet());
  }

  private UUID getSingleUuidValue(MultiValueMap<String, Object> params, String paramName) {
    return UuidUtil
        .fromString(getSingleStringValue(params, paramName))
        .orElse(null);
  }

  private String getSingleStringValue(MultiValueMap<String, Object> params, String paramName) {
    return (String) getSingleValue(params, paramName);
  }

  private Object getSingleValue(MultiValueMap<String, Object> params, String paramName) {
    if (!params.containsKey(paramName)) {
      return null;
    }
    return params.getFirst(paramName);
  }
}
