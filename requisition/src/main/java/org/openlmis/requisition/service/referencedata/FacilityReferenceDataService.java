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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class FacilityReferenceDataService extends BaseReferenceDataService<FacilityDto> {

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

  /**
   * This method retrieves Facilities for given ids.
   *
   * @param facilityIds list of facility ids.
   * @return List of FacilityDtos with similar ids.
   */
  public List<FacilityDto> search(Set<UUID> facilityIds) {
    return getPage(RequestParameters.init().set("id", facilityIds)).getContent();
  }

  /**
   * This method retrieves Facilities with facilityName similar with name parameter or
   * facilityCode similar with code parameter.
   *
   * @param code Field with string to find similar code.
   * @param name Filed with string to find similar name.
   * @return List of FacilityDtos with similar code or name.
   */
  public List<MinimalFacilityDto> search(String code, String name, UUID zoneId, boolean recurse) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("code", code);
    requestBody.put("name", name);
    requestBody.put("recurse", recurse);
    if (null != zoneId) {
      requestBody.put("zoneId", zoneId);
    }
    return getBasicFacilityPage("search", RequestParameters.init(), requestBody).getContent();
  }

  protected Page<MinimalFacilityDto> getBasicFacilityPage(String resourceUrl,
                                                          RequestParameters parameters,
                                                          Object payload) {
    return getPage(resourceUrl, parameters, payload, HttpMethod.POST, MinimalFacilityDto.class);
  }
}
