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

import static org.siglus.siglusapi.constant.CacheConstants.CACHE_KEY_GENERATOR;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_FACILITY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.dto.FacilityDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class SiglusFacilityReferenceDataService extends BaseReferenceDataService<FacilityDto> {

  public static final String FACILITY_CODE = "code";

  @Override
  protected String getUrl() {
    return "/api/facilities/";
  }

  @Override
  protected Class<FacilityDto> getResultClass() {
    return FacilityDto.class;
  }

  @Override
  protected Class<FacilityDto[]> getArrayResultClass() {
    return FacilityDto[].class;
  }

  @Override
  public List<FacilityDto> findAll() {
    return getPage(RequestParameters.init()).getContent();
  }

  public Page<FacilityDto> getFacilityByCode(String code) {
    Pageable noPagination = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
        PaginationConstants.NO_PAGINATION);
    Map<String, String> requestBody = new HashMap<>();
    requestBody.put(FACILITY_CODE, code);
    return getPage("search", RequestParameters.init().setPage(noPagination),
        requestBody, HttpMethod.POST, getResultClass(), false);
  }

  public void saveFacility(FacilityDto dto) {
    put(dto.getId().toString(), dto, Void.class, false);
  }

  public FacilityDto createFacility(FacilityDto dto) {
    return postResult("", dto, getResultClass());
  }

  @Cacheable(value = SIGLUS_FACILITY, keyGenerator = CACHE_KEY_GENERATOR)
  public FacilityDto getFacilityById(UUID facilityId) {
    return findOne(facilityId);
  }
}
