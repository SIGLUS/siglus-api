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
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class ApprovedProductReferenceDataService extends
    BaseReferenceDataService<ApprovedProductDto> {

  @Override
  protected String getUrl() {
    return "/api/facilities/";
  }

  @Override
  protected Class<ApprovedProductDto> getResultClass() {
    return ApprovedProductDto.class;
  }

  @Override
  protected Class<ApprovedProductDto[]> getArrayResultClass() {
    return ApprovedProductDto[].class;
  }

  /**
   * Retrieves all facility approved products from the reference data service, based on the
   * provided facility and full supply flag.
   *
   * @param facilityId id of the facility
   * @param programId  id of the program
   * @return a collection of approved products matching the search criteria
   */
  public ApproveProductsAggregator getApprovedProducts(UUID facilityId, UUID programId) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);
    params.set("size", Integer.MAX_VALUE);

    Page<ApprovedProductDto> page = getPage(facilityId + "/approvedProducts", params);
    List<ApprovedProductDto> content = page.getContent();

    return new ApproveProductsAggregator(content, programId);
  }
}
