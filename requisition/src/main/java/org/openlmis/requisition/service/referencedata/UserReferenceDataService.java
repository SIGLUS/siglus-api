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

package org.openlmis.requisition.service.referencedata;

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.ResultDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.RequestParameters;
import org.openlmis.requisition.service.ServiceResponse;
import org.springframework.stereotype.Service;

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
   * Searches users by the given right ID, program ID, supervisory node ID and warehouse ID.
   *
   * @param right  (required) the right UUID
   * @param program  (optional) the program UUID
   * @param supervisoryNode  (optional) the supervisory node UUID
   * @param warehouse  (optional) the warehouse UUID
   * @return the list of all matching users
   */
  public List<UserDto> findUsers(UUID right, UUID program, UUID supervisoryNode, UUID warehouse) {
    RequestParameters parameters = RequestParameters.init()
            .set("rightId", right)
            .set("programId", program)
            .set("supervisoryNodeId", supervisoryNode)
            .set("warehouseId", warehouse);

    return findAll("/rightSearch", parameters);
  }

  /**
   * Check if user has a right with certain criteria.
   *
   * @param user     id of user to check for right
   * @param right    right to check
   * @param program  program to check (for supervision rights, can be {@code null})
   * @param facility facility to check (for supervision rights, can be {@code null})
   * @return an instance of {@link ResultDto} with true or false depending on if user has the
   *         right.
   */
  public ResultDto<Boolean> hasRight(UUID user, UUID right, UUID program, UUID facility,
                                     UUID warehouse) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("rightId", right)
        .set("programId", program)
        .set("facilityId", facility)
        .set("warehouseId", warehouse);
    
    return getResult(user + "/hasRight", parameters, Boolean.class);
  }

  /**
   * Get user's permission strings (a list of strings that outlines the permissions of the user).
   *
   * @param user id of user
   * @return a set of permission strings.
   */
  public List<String> getPermissionStrings(UUID user) {
    return findAll(user + "/permissionStrings", String[].class);
  }

  public ServiceResponse<List<String>> getPermissionStrings(UUID user, String etag) {
    return tryFindAll(user + "/permissionStrings", String[].class, etag);
  }
}
