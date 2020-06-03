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

package org.openlmis.fulfillment.service.referencedata;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.fulfillment.service.ResultDto;
import org.openlmis.fulfillment.service.ServiceResponse;
import org.openlmis.fulfillment.service.request.RequestParameters;
import org.openlmis.fulfillment.util.BooleanUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class UserReferenceDataService extends BaseReferenceDataService<UserDto> {

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
   * This method retrieves a user with given name.
   *
   * @param name the name of user.
   * @return UserDto containing user's data, or null if such user was not found.
   */
  public UserDto findUser(String name) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("username", name);

    Page<UserDto> users = getPage("search", RequestParameters.init(), payload);
    return users.getContent().isEmpty() ? null : users.getContent().get(0);
  }

  /**
   * Check if user has a right with certain criteria.
   *
   * @param user     id of user to check for right
   * @param right    right to check
   * @param program  program to check (for supervision rights, can be {@code null})
   * @param facility facility to check (for supervision rights, can be {@code null})
   * @return an instance of {@link ResultDto} with result depending on if user has the right.
   */
  public ResultDto<Boolean> hasRight(UUID user, UUID right, UUID program, UUID facility,
                                     UUID warehouse) {
    ResultDto result = get(ResultDto.class, user + "/hasRight", RequestParameters.init()
        .set("rightId", right).set("programId", program)
        .set("facilityId", facility).set("warehouseId", warehouse));

    return new ResultDto<>(BooleanUtils.toBoolean(result.getResult()));
  }

  public ServiceResponse<List<String>> getPermissionStrings(UUID user, String etag) {
    return tryFindAll(user + "/permissionStrings", String[].class, etag);
  }

  /**
   * Finds users by their ids.
   *
   * @param ids ids to look for.
   * @return a page of users
   */
  public List<UserDto> findByIds(Collection<UUID> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    return getPage(RequestParameters.init().set("id", ids)).getContent();
  }
}
