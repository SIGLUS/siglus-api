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
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class SupplyLineReferenceDataService extends BaseReferenceDataService<SupplyLineDto> {

  @Override
  protected String getUrl() {
    return "/api/supplyLines/";
  }

  @Override
  protected Class<SupplyLineDto> getResultClass() {
    return SupplyLineDto.class;
  }

  @Override
  protected Class<SupplyLineDto[]> getArrayResultClass() {
    return SupplyLineDto[].class;
  }

  /**
   * Retrieves supply lines from reference data service by supplying facility ids and program id.
   *
   * @param supplyingFacilityIds UUIDs of supplying facility
   * @param programId            UUID of program
   * @return A list of supply lines matching search criteria
   */
  public List<SupplyLineDto> search(Set<UUID> supplyingFacilityIds, UUID programId) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("programId", programId)
        .set("supplyingFacilityId", supplyingFacilityIds);

    return search(parameters);
  }

  /**
   * Retrieves supply lines from reference data service by program and supervisory node.
   *
   * @param programId         UUID of the program
   * @param supervisoryNodeId UUID of the supervisory node
   * @return A list of supply lines matching search criteria
   */
  public List<SupplyLineDto> search(UUID programId, UUID supervisoryNodeId) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("programId", programId)
        .set("supervisoryNodeId", supervisoryNodeId);

    return search(parameters);
  }

  private List<SupplyLineDto> search(RequestParameters parameters) {
    return getPage(parameters).getContent();
  }
}
