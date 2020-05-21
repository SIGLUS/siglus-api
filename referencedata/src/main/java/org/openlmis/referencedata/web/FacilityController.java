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

import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.dto.ApprovedProductDto;
import org.openlmis.referencedata.dto.BasicFacilityDto;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.MinimalFacilityDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.fhir.FhirClient;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeApprovedProductRepository;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.service.FacilityBuilder;
import org.openlmis.referencedata.service.FacilityService;
import org.openlmis.referencedata.service.RightAssignmentService;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.validate.FacilityValidator;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@Controller
@Transactional
@SuppressWarnings({"PMD.TooManyMethods"})
public class FacilityController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(FacilityController.class);

  public static final String RESOURCE_PATH = "/facilities";

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private FacilityTypeApprovedProductRepository facilityTypeApprovedProductRepository;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private FacilityService facilityService;

  @Autowired
  private FacilityValidator facilityValidator;

  @Autowired
  private RightAssignmentService rightAssignmentService;

  @Autowired
  private FhirClient fhirClient;

  @Autowired
  private FacilityBuilder facilityBuilder;

  /**
   * Allows creating new facilities. If the id is specified, it will be ignored.
   *
   * @param facilityDto A facility bound to the request body.
   * @return created facility.
   */
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public FacilityDto createFacility(@RequestBody FacilityDto facilityDto,
      BindingResult bindingResult) {
    Profiler profiler = new Profiler("CREATE_FACILITY");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT, profiler);

    profiler.start("VALIDATE_FACILITY_DTO");
    facilityValidator.validate(facilityDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    XLOGGER.debug("Creating new facility");
    profiler.start("BUILD_FACILITY_FROM_DTO");
    facilityDto.setId(null);
    Facility newFacility = facilityBuilder.build(facilityDto);

    profiler.start("SAVE");
    newFacility = facilityRepository.save(newFacility);
    XLOGGER.debug("Created new facility with id: ", facilityDto.getId());

    profiler.start("SYNC_FHIR_RESOURCE");
    fhirClient.synchronizeFacility(newFacility);

    FacilityDto dto = toDto(newFacility, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Get all facilities with minimal representation (id, name).
   *
   * @param pageable A Pageable object that allows client to optionally add "page" (page number) and
   *                 "size" (page size) query parameters to the request.
   * @param active True if only active facilities should be returned.
   * @return Facilities.
   */
  @RequestMapping(value = RESOURCE_PATH + "/minimal", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<MinimalFacilityDto> getMinimalFacilities(
      @RequestParam(required = false) Boolean active,
      Pageable pageable) {
    Profiler profiler = new Profiler("GET_MINIMAL_FACILITIES");
    profiler.setLogger(XLOGGER);

    Page<Facility> facilities;

    if (active != null) {
      profiler.start("FIND_BY_ACTIVE");
      facilities = facilityRepository.findByActive(active, pageable);
    } else {
      profiler.start("FIND_ALL");
      facilities = facilityRepository.findAll(pageable);
    }

    Page<MinimalFacilityDto> minimalFacilities = toMinimalDto(facilities, profiler, pageable);

    profiler.stop().log();
    return minimalFacilities;
  }


  /**
   * Get the audit information related to facilities.
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
  public ResponseEntity<String> getFacilitiesAuditLog(
          @PathVariable("id") UUID id,
          @RequestParam(name = "author", required = false, defaultValue = "") String author,
          @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
                        String changedPropertyName,
          //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
          @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
                        boolean returnJson,
          Pageable page) {
    Profiler profiler = new Profiler("GET_AUDIT_LOG");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT, profiler);

    //Return a 404 if the specified facility can't be found
    findFacility(id, profiler);

    profiler.start("GET_AUDIT_LOG");
    ResponseEntity<String> response = getAuditLogResponse(
        Facility.class, id, author, changedPropertyName, page, returnJson
    );

    profiler.stop().log();
    return response;
  }


  /**
   * Allows updating facilities.
   *
   * @param facilityDto A facility bound to the request body.
   * @param facilityId UUID of facility which we want to update.
   * @return the updated facility.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public FacilityDto saveFacility(
      @RequestBody FacilityDto facilityDto,
      @PathVariable("id") UUID facilityId,
      BindingResult bindingResult) {

    Profiler profiler = new Profiler("UPDATE_FACILITY");
    profiler.setLogger(XLOGGER);

    if (null != facilityDto.getId() && !Objects.equals(facilityDto.getId(), facilityId)) {
      throw new ValidationMessageException(FacilityMessageKeys.ERROR_ID_MISMATCH);
    }

    checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT, profiler);

    profiler.start("VALIDATE_FACILITY_DTO");
    facilityValidator.validate(facilityDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    profiler.start("BUILD_FACILITY_FROM_DTO");
    Facility facilityToSave = facilityBuilder.build(facilityDto);

    profiler.start("SAVE_FACILITY");
    facilityToSave = facilityRepository.saveAndFlush(facilityToSave);

    profiler.start("SYNC_FHIR_RESOURCE");
    fhirClient.synchronizeFacility(facilityToSave);

    profiler.start("REGENERATE_RIGHT_ASSIGNMENTS");
    rightAssignmentService.regenerateRightAssignments();

    XLOGGER.info("Saved facility with id: {}", facilityToSave.getId());
    FacilityDto dto = toDto(facilityToSave, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Get chosen facility.
   *
   * @param facilityId UUID of facility which we want to get
   * @return Facility.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public FacilityDto getFacility(@PathVariable("id") UUID facilityId) {
    Profiler profiler = new Profiler("GET_FACILITY");
    profiler.setLogger(XLOGGER);

    Facility facility = findFacility(facilityId, profiler);
    FacilityDto dto = toDto(facility, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Returns full or non-full supply approved products for the given facility.
   *
   * @param facilityId ID of the facility
   * @param programId  ID of the program
   * @param fullSupply true to retrieve full-supply products, false to retrieve non-full supply
   *                   products
   * @return collection of approved products
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}/approvedProducts")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<ApprovedProductDto> getApprovedProducts(
      @PathVariable("id") UUID facilityId,
      @RequestParam(required = false, value = "programId") UUID programId,
      @RequestParam(required = false, value = "fullSupply") Boolean fullSupply,
      @RequestParam(required = false, value = "orderableId") List<UUID> orderablesId,
      @RequestParam(required = false, value = "active") Boolean active,
      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {

    Profiler profiler = new Profiler("GET_FACILITY_APPROVED_PRODUCTS");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_APPROVED_PRODUCTS");
    Page<FacilityTypeApprovedProduct> products = facilityTypeApprovedProductRepository
        .searchProducts(facilityId, programId, fullSupply, orderablesId, active, pageable);

    Page<ApprovedProductDto> list = toDto(products, pageable, profiler);

    profiler.stop().log();
    return list;
  }

  /**
   * Retrieves all facilities within a boundary.
   *
   * @param boundary GeoJSON polygon specifying a boundary
   * @param pageable object used to encapsulate the pagination related values: page and size.
   * @return List of wanted facilities within the boundary.
   */
  @RequestMapping(value = RESOURCE_PATH + "/byBoundary", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<FacilityDto> findFacilitiesByBoundary(@RequestBody Polygon boundary, 
      Pageable pageable) {
    Profiler profiler = new Profiler("GET_FACILITIES_BY_BOUNDARY");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT, profiler);

    profiler.start("DB_CALL");
    List<Facility> foundFacilities = facilityRepository.findByBoundary(boundary);

    List<FacilityDto> facilityDtos = toDto(foundFacilities, profiler);
    Page<FacilityDto> page = toPage(facilityDtos, pageable, profiler);

    profiler.stop().log();
    return page;
  }

  /**
   * Allows deleting facility.
   *
   * @param facilityId UUID of facility which we want to delete
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFacility(@PathVariable("id") UUID facilityId) {
    Profiler profiler = new Profiler("DELETE_FACILITY");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT, profiler);

    Facility facility = findFacility(facilityId, profiler);

    profiler.start("DELETE_FACILITY");
    facilityRepository.delete(facility);
  }

  /**
   * Retrieves all facilities that are matching given request parameters.
   * If no parameters, all facilities are returned.
   *
   * @param requestParams request parameters (id, code, name, zone, recurse).
   * @return List of wanted Facilities matching query parameters.
   */
  @GetMapping(value = RESOURCE_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<BasicFacilityDto> getFacilities(
      @RequestParam MultiValueMap<String, Object> requestParams, Pageable pageable) {
    Profiler profiler = new Profiler("GET_FACILITIES");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_FACILITIES");
    FacilitySearchParams params = new FacilitySearchParams(requestParams);

    profiler.start("SERVICE_SEARCH");
    Page<Facility> foundFacilities = facilityService.searchFacilities(params, pageable);

    profiler.start("EXPORT_TO_DTO");
    Page<BasicFacilityDto> dto = toBasicDto(foundFacilities, pageable, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Retrieves all Facilities with facilityCode similar to code parameter or facilityName similar to
   * name parameter.
   *
   * @param queryParams request parameters (code, name, zone, recurse) and JSON extraData.
   * @param pageable object used to encapsulate the pagination related values: page, size and sort.
   * @return List of wanted Facilities matching query parameters.
   */
  @RequestMapping(value = RESOURCE_PATH + "/search", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<BasicFacilityDto> searchFacilities(@RequestBody Map<String, Object> queryParams,
                                                 Pageable pageable) {
    XLOGGER.entry(queryParams);
    Profiler profiler = new Profiler("SEARCH_FACILITIES");
    profiler.setLogger(XLOGGER);

    profiler.start("CONVERT_PARAMS");
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    queryParams.forEach(map::add);
    FacilitySearchParams params = new FacilitySearchParams(map);

    profiler.start("SERVICE_SEARCH");
    Page<Facility> foundFacilities = facilityService.searchFacilities(params, pageable);

    Page<BasicFacilityDto> page = toBasicDto(foundFacilities, pageable, profiler);

    XLOGGER.exit(page);
    profiler.stop().log();
    return page;
  }

  private Facility findFacility(UUID id, Profiler profiler) {
    profiler.start("FIND_FACILITY");
    Facility facility = facilityRepository.findOne(id);

    if (facility == null) {
      profiler.stop().log();
      throw new NotFoundException(FacilityMessageKeys.ERROR_NOT_FOUND);
    }

    return facility;
  }

  private FacilityDto toDto(Facility facility, Profiler profiler) {
    profiler.start("EXPORT_FACILITY_TO_DTO");
    return FacilityDto.newInstance(facility);
  }

  private List<FacilityDto> toDto(List<Facility> facilities, Profiler profiler) {
    profiler.start("EXPORT_FACILITIES_TO_DTO");
    return facilities
        .stream()
        .map(FacilityDto::newInstance)
        .collect(Collectors.toList());
  }

  private Page<ApprovedProductDto> toDto(Page<FacilityTypeApprovedProduct> products,
      Pageable pageable, Profiler profiler) {
    profiler.start("EXPORT_PRODUCTS_TO_DTO");

    List<FacilityTypeApprovedProduct> ftaps = products.getContent();

    Set<UUID> orderableId = ftaps
        .stream()
        .map(FacilityTypeApprovedProduct::getOrderableId)
        .collect(Collectors.toSet());

    Map<UUID, Orderable> orderables = orderableRepository
        .findAllLatestByIds(orderableId, new PageRequest(0, orderableId.size()))
        .getContent()
        .stream()
        .collect(Collectors.toMap(Orderable::getId, Function.identity()));

    List<ApprovedProductDto> dtos = new ArrayList<>();
    for (FacilityTypeApprovedProduct ftap : ftaps) {
      ApprovedProductDto productDto = new ApprovedProductDto();
      ftap.export(productDto);

      productDto.setOrderable(orderables.get(ftap.getOrderableId()));

      dtos.add(productDto);
    }

    return toPage(dtos, pageable, products.getTotalElements(), profiler);
  }

  private Page<MinimalFacilityDto> toMinimalDto(Page<Facility> facilities, Profiler profiler,
                                                Pageable pageable) {
    profiler.start("EXPORT_FACILITIES_TO_MINIMAL_DTO");
    List<MinimalFacilityDto> minimalFacilityDtos = facilities
        .getContent()
        .stream()
        .map(MinimalFacilityDto::newInstance)
        .collect(Collectors.toList());

    return toPage(minimalFacilityDtos, pageable, profiler);
  }

  private Page<BasicFacilityDto> toBasicDto(Page<Facility> facilities, Pageable pageable,
      Profiler profiler) {
    profiler.start("EXPORT_FACILITIES_TO_BASIC_DTO");

    List<BasicFacilityDto> facilityDtos = facilities.getContent()
        .stream()
        .map(BasicFacilityDto::newInstance)
        .collect(Collectors.toList());

    return toPage(facilityDtos, pageable, facilities.getTotalElements(), profiler);
  }

}
