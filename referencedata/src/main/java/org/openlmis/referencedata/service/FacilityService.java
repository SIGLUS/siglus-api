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

import static org.apache.commons.collections4.MapUtils.isNotEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;
import org.openlmis.referencedata.web.FacilitySearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class FacilityService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacilityService.class);

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private FacilityTypeRepository facilityTypeRepository;

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  @Autowired
  private GeographicZoneService geographicZoneService;

  private ObjectMapper mapper = new ObjectMapper();

  /**
   * Method returns all facilities with matched parameters. When no valid params are given,
   * returns all facilities
   *
   * @param params request parameters (code, name, zone, type, recurse) and JSON extraData.
   *               May be null or empty
   * @param pageable object used to encapsulate the pagination related values: page, size and sort.
   * @return Page of facilities. All facilities will be returned when map is null or empty
   */
  public Page<Facility> searchFacilities(FacilitySearchParams params, Pageable pageable) {
    Profiler profiler = new Profiler("FACILITY_SERVICE_SEARCH");
    profiler.setLogger(LOGGER);

    profiler.start("CHECK_IF_GEO_ZONE_EXISTS");
    if (null != params.getZoneId() && !geographicZoneRepository.exists(params.getZoneId())) {
      throw new ValidationMessageException(GeographicZoneMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("CHECK_IF_FACILITY_TYPE_EXISTS");
    if (params.getFacilityTypeCode() != null
        && !facilityTypeRepository.existsByCode(params.getFacilityTypeCode())) {
      throw new ValidationMessageException(FacilityTypeMessageKeys.ERROR_NOT_FOUND);
    }

    Page<Facility> facilities = findFacilities(params, pageable, profiler);

    profiler.stop().log();
    return facilities;
  }

  private Page<Facility> findFacilities(FacilitySearchParams params, Pageable pageable,
      Profiler profiler) {
        
    profiler.start("GET_GEOGRAPHIC_ZONES");
    Set<UUID> zones = Sets.newHashSet();
    if (null != params.getZoneId()) {
      zones.add(params.getZoneId());
      if (params.isRecurse()) {
        profiler.start("GET_ALL_ZONES_IN_HIERARCHY");
        zones.addAll(geographicZoneService.getAllZonesInHierarchy(params.getZoneId()));
      }
    }

    profiler.start("PARSE_EXTRA_DATA");
    String extraDataString = null;
    if (isNotEmpty(params.getExtraData())) {
      try {
        extraDataString = mapper.writeValueAsString(params.getExtraData());
      } catch (JsonProcessingException jpe) {
        LOGGER.debug("Cannot serialize extra data query request body into JSON");
        extraDataString = null;
      }
    }

    profiler.start("SEARCH_FOR_FACILITIES");
    return facilityRepository.search(params, zones, extraDataString, pageable);
  }

}
