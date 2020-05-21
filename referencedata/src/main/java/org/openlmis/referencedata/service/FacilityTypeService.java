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

package org.openlmis.referencedata.service;

import static org.apache.commons.collections4.MapUtils.isEmpty;

import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.util.UuidUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class FacilityTypeService {

  protected static final String ACTIVE = "active";

  @Autowired
  private FacilityTypeRepository facilityTypeRepository;

  /**
   * Method returns all facility types with matched parameters.
   * When no valid parameters are given, returns all facility types.
   *
   * @param requestParams multi map with request parameters,
   *                      can contain multiple id and active parameters
   * @param pageable      pagination and sorting parameters
   * @return List of facilities. All facilities will be returned when map is null or empty
   */
  public Page<FacilityType> search(MultiValueMap<String, Object> requestParams, Pageable pageable) {
    if (!isEmpty(requestParams)) {
      Set<UUID> ids = UuidUtil.getIds(requestParams);
      String activeParameter = (String) requestParams.getFirst(ACTIVE);
      Boolean active = null != activeParameter ? Boolean.valueOf(activeParameter) : null;

      if (!ids.isEmpty() && active != null) {
        return facilityTypeRepository.findByIdInAndActive(ids, active, pageable);
      } else if (!ids.isEmpty()) {
        return facilityTypeRepository.findByIdIn(ids, pageable);
      } else if (active != null) {
        return facilityTypeRepository.findByActive(active, pageable);
      }
    }

    return facilityTypeRepository.findAll(pageable);
  }
}
