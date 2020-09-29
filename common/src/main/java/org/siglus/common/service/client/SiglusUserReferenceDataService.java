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

package org.siglus.common.service.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.RequestParameters;
import org.siglus.common.util.referencedata.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SiglusUserReferenceDataService extends BaseReferenceDataService<UserDto>  {

  public static final String HOME_FACILITY_ID = "homeFacilityId";

  @Override
  protected String getUrl() {
    return "/api/users/";
  }

  @Override
  protected Class<UserDto> getResultClass() {
    return UserDto.class;
  }

  @Override
  protected Class<UserDto[]> getArrayResultClass() {
    return UserDto[].class;
  }

  /**
   * Get user's permission strings (a list of strings that outlines the permissions of the user).
   *
   * @param userId id of user
   * @return a set of permission strings.
   */
  public Collection<String> getPermissionStrings(UUID userId) {
    return findAll(userId + "/permissionStrings", String[].class);
  }

  public Page<UserDto> getUserInfo(UUID homeFacilityId) {
    Pageable noPagination = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
        Pagination.NO_PAGINATION);
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put(HOME_FACILITY_ID, homeFacilityId.toString());
    return getPage("search", RequestParameters.init().setPage(noPagination),
        requestBody, HttpMethod.POST, getResultClass(), false);
  }

}
