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

import java.util.Collection;
import java.util.UUID;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class SupervisingUsersReferenceDataService extends UserReferenceDataService {

  @Override
  protected String getUrl() {
    return "/api/supervisoryNodes/";
  }

  /**
   * Get a list of supervising users for a certain supervisory node, program and right.
   *
   * @param supervisoryNode the UUID of the supervisory node.
   * @param right the UUID of the right.
   * @param program the UUID of the program.
   * @return a collection of supervising users.
   */
  public Collection<UserDto> findAll(UUID supervisoryNode, UUID right, UUID program) {
    return findAll(supervisoryNode + "/supervisingUsers",
        RequestParameters.init().set("rightId", right).set("programId", program));
  }
}
