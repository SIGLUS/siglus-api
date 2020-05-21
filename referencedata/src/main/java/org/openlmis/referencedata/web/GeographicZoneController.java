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

import com.vividsolutions.jts.geom.Point;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.dto.GeographicZoneDto;
import org.openlmis.referencedata.dto.GeographicZoneSimpleDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.fhir.FhirClient;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.service.GeographicZoneBuilder;
import org.openlmis.referencedata.service.GeographicZoneService;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;
import org.openlmis.referencedata.validate.GeographicZoneValidator;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class GeographicZoneController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(GeographicZoneController.class);

  public static final String RESOURCE_PATH = "/geographicZones";

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  @Autowired
  private GeographicZoneService geographicZoneService;

  @Autowired
  private GeographicZoneBuilder geographicZoneBuilder;

  @Autowired
  private GeographicZoneValidator geographicZoneValidator;

  @Autowired
  private FhirClient fhirClient;

  /**
   * Allows creating new geographicZones.
   *
   * @param geographicZoneDto A geographicZone bound to the request body.
   * @return the created geographicZone.
   */
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public GeographicZoneDto createGeographicZone(@RequestBody GeographicZoneDto geographicZoneDto,
      BindingResult bindingResult) {
    Profiler profiler = new Profiler("CREATE_GEO_ZONE");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.GEOGRAPHIC_ZONES_MANAGE_RIGHT, profiler);

    profiler.start("VALIDATE_GEO_ZONE_DTO");
    geographicZoneValidator.validate(geographicZoneDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    XLOGGER.debug("Creating new geographicZone");
    profiler.start("BUILD_GEO_ZONE_FROM_DTO");
    GeographicZone geographicZone = geographicZoneBuilder.build(geographicZoneDto);
    // Ignore provided id
    geographicZone.setId(null);

    profiler.start("SAVE_TO_DB");
    GeographicZone zone = geographicZoneRepository.save(geographicZone);

    profiler.start("SYNC_FHIR_RESOURCE");
    fhirClient.synchronizeGeographicZone(zone);

    GeographicZoneDto dto = toDto(zone, profiler);

    profiler.stop().log();

    return dto;
  }


  /**
   * Get all geographic zones.
   *
   * @return GeographicZones.
   */
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<GeographicZoneSimpleDto> getAllGeographicZones(Pageable pageable) {
    Profiler profiler = new Profiler("GET_ALL_GEO_ZONES");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_ALL");
    Page<GeographicZone> page = geographicZoneRepository.findAll(pageable);
    List<GeographicZoneSimpleDto> dtos = toSimpleDto(page.getContent(), profiler);
    Page<GeographicZoneSimpleDto> response = toPage(dtos, pageable,
        page.getTotalElements(), profiler);

    profiler.stop().log();
    return response;
  }

  /**
   * Allows updating geographicZones.
   *
   * @param geographicZoneId UUID of geographicZone which we want to update.
   * @param geographicZoneDto A geographicZone bound to the request body.
   * @return the ResponseEntity containing the updated geographicZone.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GeographicZoneDto updateGeographicZone(@PathVariable("id") UUID geographicZoneId,
      @RequestBody GeographicZoneDto geographicZoneDto,
      BindingResult bindingResult) {
    Profiler profiler = new Profiler("UPDATE_GEO_ZONE");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.GEOGRAPHIC_ZONES_MANAGE_RIGHT, profiler);

    profiler.start("VALIDATE_GEO_ZONE_DTO");
    geographicZoneValidator.validate(geographicZoneDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    profiler.start("BUILD_GEO_ZONE_FROM_DTO");
    GeographicZone geoZoneToSave = geographicZoneBuilder.build(geographicZoneDto);
    geoZoneToSave.setId(geographicZoneId);

    XLOGGER.debug("Updating geographicZone");
    profiler.start("SAVE");
    GeographicZone zone = geographicZoneRepository.save(geoZoneToSave);

    profiler.start("SYNC_FHIR_RESOURCE");
    fhirClient.synchronizeGeographicZone(zone);

    GeographicZoneDto dto = toDto(zone, profiler);

    profiler.stop().log();

    return dto;
  }

  /**
   * Get chosen geographicZone.
   *
   * @param geographicZoneId UUID of geographicZone which we want to get.
   * @return the geographicZone.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GeographicZoneDto getGeographicZone(@PathVariable("id") UUID geographicZoneId) {
    Profiler profiler = new Profiler("GET_GEO_ZONE");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_ONE_BY_ID");
    GeographicZone geographicZone = geographicZoneRepository.findOne(geographicZoneId);

    if (geographicZone == null) {
      profiler.stop().log();
      throw new NotFoundException(GeographicZoneMessageKeys.ERROR_NOT_FOUND);
    }

    GeographicZoneDto dto = toDto(geographicZone, profiler);

    profiler.stop().log();

    return dto;
  }

  /**
   * Allows deleting geographicZone.
   *
   * @param geographicZoneId UUID of geographicZone which we want to delete
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteGeographicZone(@PathVariable("id") UUID geographicZoneId) {
    Profiler profiler = new Profiler("DELETE_GEO_ZONE");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.GEOGRAPHIC_ZONES_MANAGE_RIGHT, profiler);

    profiler.start("CHECK_IF_GEO_ZONE_EXISTS");
    boolean exists = geographicZoneRepository.exists(geographicZoneId);

    if (!exists) {
      profiler.stop().log();
      throw new NotFoundException(GeographicZoneMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("DELETE_INSTANCE");
    geographicZoneRepository.delete(geographicZoneId);

    profiler.stop().log();
  }

  /**
   * Retrieves all geographic zones to which a location belongs.
   *
   * @param location GeoJSON point specifying a location
   * @return List of wanted geographic zones to which the location belongs.
   */
  @RequestMapping(value = RESOURCE_PATH + "/byLocation", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<GeographicZoneSimpleDto> findGeographicZonesByLocation(
      @RequestBody Point location) {
    Profiler profiler = new Profiler("FIND_GEO_ZONES_BY_LOCATION");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.GEOGRAPHIC_ZONES_MANAGE_RIGHT, profiler);

    profiler.start("FIND_IN_DB");
    List<GeographicZone> foundGeoZones = geographicZoneRepository.findByLocation(location);
    List<GeographicZoneSimpleDto> dtos = toSimpleDto(foundGeoZones, profiler);

    profiler.stop().log();

    return dtos;
  }

  /**
   * Retrieves page of Geographic Zones matching given parameters.
   *
   * @param queryParams request parameters (code, name, parent, levelNumber).
   * @param pageable object used to encapsulate the pagination related values: page, size and sort.
   * @return Page of matched geographic zones.
   */
  @RequestMapping(value = RESOURCE_PATH + "/search", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<GeographicZoneSimpleDto> search(@RequestBody Map<String, Object> queryParams,
      Pageable pageable) {
    Profiler profiler = new Profiler("SEARCH_GEO_ZONES");
    profiler.setLogger(XLOGGER);

    profiler.start("SEARCH_BY_PARAMS");
    Page<GeographicZone> page = geographicZoneService.search(queryParams, pageable);
    List<GeographicZoneSimpleDto> dtos = toSimpleDto(page.getContent(), profiler);
    Page<GeographicZoneSimpleDto> response = toPage(dtos, pageable,
        page.getTotalElements(), profiler);

    profiler.stop().log();
    return response;
  }

  /**
   * Get the audit information related to geographic zone.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getGeographicZoneAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    Profiler profiler = new Profiler("GET_AUDIT_LOG_OF_GEO_ZONE");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.GEOGRAPHIC_ZONES_MANAGE_RIGHT, profiler);

    profiler.start("CHECK_IF_GEO_ZONE_EXISTS");
    boolean exists = geographicZoneRepository.exists(id);

    if (!exists) {
      profiler.stop().log();
      throw new NotFoundException(GeographicZoneMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("GET_AUDIT_LOG");
    ResponseEntity<String> response = getAuditLogResponse(
        GeographicZone.class, id, author, changedPropertyName, page, returnJson
    );

    profiler.stop().log();
    return response;
  }

  private GeographicZoneDto toDto(GeographicZone geographicZone, Profiler profiler) {
    profiler.start("EXPORT_GEO_ZONE_TO_DTO");
    GeographicZoneDto dto = new GeographicZoneDto();
    geographicZone.export(dto);

    return dto;
  }

  private List<GeographicZoneSimpleDto> toSimpleDto(List<GeographicZone> geographicZones,
      Profiler profiler) {
    profiler.start("EXPORT_GEO_ZONES_TO_SIMPLE_DTOS");
    return geographicZones
        .stream()
        .map(this::toSimpleDto)
        .collect(Collectors.toList());
  }

  private GeographicZoneSimpleDto toSimpleDto(GeographicZone geographicZone) {
    GeographicZoneSimpleDto dto = new GeographicZoneSimpleDto();
    geographicZone.export(dto);

    return dto;
  }
}
