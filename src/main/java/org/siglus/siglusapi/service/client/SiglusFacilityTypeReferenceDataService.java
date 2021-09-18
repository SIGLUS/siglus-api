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

package org.siglus.siglusapi.service.client;

import java.util.List;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusFacilityTypeReferenceDataService extends
    BaseReferenceDataService<FacilityTypeDto> {

  @Override
  protected String getUrl() {
    return "/api/facilityTypes/";
  }

  @Override
  protected Class<FacilityTypeDto> getResultClass() {
    return FacilityTypeDto.class;
  }

  @Override
  protected Class<FacilityTypeDto[]> getArrayResultClass() {
    return FacilityTypeDto[].class;
  }

  public List<FacilityTypeDto> searchAllFacilityTypes() {
    Pageable noPagination = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
        PaginationConstants.NO_PAGINATION);
    return getPage(RequestParameters.init().setPage(noPagination)).getContent();
  }

  public void saveFacilityType(FacilityTypeDto dto) {
    put(dto.getId().toString(), dto, Void.class, false);
  }

  public FacilityTypeDto createFacilityType(FacilityTypeDto dto) {
    return postResult("", dto, FacilityTypeDto.class);
  }

}
