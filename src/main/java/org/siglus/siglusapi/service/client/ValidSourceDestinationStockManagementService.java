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

import static org.siglus.common.constant.CacheConstants.SIGLUS_DESTINATIONS;
import static org.siglus.common.constant.CacheConstants.SIGLUS_SOURCES;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.domain.sourcedestination.SourceDestinationAssignment;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ValidSourceDestinationStockManagementService extends
    BaseStockManagementService<ValidSourceDestinationDto> {

  @Override
  protected String getUrl() {
    return "/api/";
  }

  @Override
  protected Class<ValidSourceDestinationDto> getResultClass() {
    return ValidSourceDestinationDto.class;
  }

  @Override
  protected Class<ValidSourceDestinationDto[]> getArrayResultClass() {
    return ValidSourceDestinationDto[].class;
  }

  @Cacheable(value = SIGLUS_DESTINATIONS, keyGenerator = "org.siglus.siglusapi.config.CacheKeyGenerator")
  public Collection<ValidSourceDestinationDto> getValidDestinations(UUID programId, UUID facilityId) {
    Map<String, Object> params = new HashMap<>();
    params.put("programId", programId);
    params.put("facilityId", facilityId);
    return findAll("validDestinations", params);
  }

  public void assignDestination(SourceDestinationAssignment assignment) {
    postResult("validDestinations", assignment, getResultClass());
  }

  @Cacheable(value = SIGLUS_SOURCES, keyGenerator = "org.siglus.siglusapi.config.CacheKeyGenerator")
  public Collection<ValidSourceDestinationDto> getValidSources(UUID programId, UUID facilityId) {
    Map<String, Object> params = new HashMap<>();
    params.put("programId", programId);
    params.put("facilityId", facilityId);
    return findAll("validSources", params);
  }

  public void assignSource(SourceDestinationAssignment assignment) {
    postResult("validSources", assignment, getResultClass());
  }
}

