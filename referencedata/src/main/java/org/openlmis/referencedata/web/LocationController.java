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

package org.openlmis.referencedata.web;

import static org.openlmis.referencedata.util.Pagination.handlePage;

import java.util.ArrayList;
import java.util.List;
import org.openlmis.referencedata.fhir.FhirLocation;
import org.openlmis.referencedata.fhir.LocationFactory;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationController extends BaseController {

  public static final String RESOURCE_PATH = "/Location";

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private LocationFactory locationFactory;

  /**
   * Gets FHIR location.
   */
  @GetMapping(RESOURCE_PATH)
  @ResponseStatus(HttpStatus.OK)
  public List<FhirLocation> getLocations() {
    List<FhirLocation> list = new ArrayList<>();

    handlePage(
        geographicZoneRepository::findAll,
        zone -> list.add(locationFactory.createFor(zone))
    );
    handlePage(
        facilityRepository::findAll,
        facility -> list.add(locationFactory.createFor(facility))
    );

    return list;
  }

}
