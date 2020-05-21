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

import static org.openlmis.referencedata.web.FacilityTypeController.RESOURCE_PATH;

import java.util.UUID;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.exception.IntegrityViolationException;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.service.FacilityTypeService;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(RESOURCE_PATH)
@Transactional
public class FacilityTypeController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacilityTypeController.class);

  public static final String RESOURCE_PATH = API_PATH + "/facilityTypes";

  @Autowired
  private FacilityTypeRepository facilityTypeRepository;

  @Autowired
  private FacilityTypeService facilityTypeService;

  /**
   * Allows creating new facilityType. If the id is specified, it will be ignored.
   *
   * @param facilityType A facilityType bound to the request body
   * @return the created facilityType
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public FacilityType createFacilityType(@RequestBody FacilityType facilityType) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    LOGGER.debug("Creating new facility type");
    facilityType.setId(null);
    facilityTypeRepository.save(facilityType);
    LOGGER.debug("Creating new facility type with id: %s", facilityType.getId());
    return facilityType;
  }

  /**
   * Get all facilityTypes.
   *
   * @return FacilityTypes.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<FacilityType> getFacilityTypes(
      @RequestParam MultiValueMap<String, Object> requestParams, Pageable pageable) {
    return facilityTypeService.search(requestParams, pageable);
  }

  /**
   * Allows updating facilityTypes.
   *
   * @param facilityType   A facilityType bound to the request body
   * @param facilityTypeId UUID of facilityType which we want to update
   * @return the updated facilityType
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public FacilityType updateFacilityType(
      @RequestBody FacilityType facilityType, @PathVariable("id") UUID facilityTypeId) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);

    FacilityType facilityTypeToUpdate = facilityTypeRepository.findOne(facilityTypeId);
    try {
      if (facilityTypeToUpdate == null) {
        facilityTypeToUpdate = new FacilityType();
        LOGGER.debug("Creating new facility type");
      } else {
        LOGGER.debug("Updating facility type with id: %s", facilityTypeId);
      }

      facilityTypeToUpdate.updateFrom(facilityType);
      facilityTypeRepository.save(facilityTypeToUpdate);

      LOGGER.debug("Updating facility type with id: %s", facilityTypeToUpdate.getId());
      return facilityTypeToUpdate;
    } catch (DataIntegrityViolationException ex) {
      throw new IntegrityViolationException(FacilityTypeMessageKeys.ERROR_SAVING_WITH_ID, ex);
    }
  }

  /**
   * Get chosen facilityType.
   *
   * @param facilityTypeId UUID of facilityType which we want to get
   * @return the FacilityType.
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public FacilityType getFacilityType(@PathVariable("id") UUID facilityTypeId) {

    FacilityType facilityType = facilityTypeRepository.findOne(facilityTypeId);
    if (facilityType == null) {
      throw new NotFoundException(FacilityTypeMessageKeys.ERROR_NOT_FOUND);
    } else {
      return facilityType;
    }
  }

  /**
   * Allows deleting facilityType.
   *
   * @param facilityTypeId UUID of facilityType which we want to delete
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFacilityType(@PathVariable("id") UUID facilityTypeId) {
    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);
    FacilityType facilityType = facilityTypeRepository.findOne(facilityTypeId);
    if (facilityType == null) {
      throw new NotFoundException(FacilityTypeMessageKeys.ERROR_NOT_FOUND);
    } else {
      try {
        facilityTypeRepository.delete(facilityType);
      } catch (DataIntegrityViolationException ex) {
        throw new IntegrityViolationException(FacilityTypeMessageKeys.ERROR_DELETING_WITH_ID, ex);
      }
    }
  }


  /**
   * Get the audit information related to facility type.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getFacilityTypeAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    rightService.checkAdminRight(RightName.FACILITIES_MANAGE_RIGHT);

    //Return a 404 if the specified instance can't be found
    FacilityType instance = facilityTypeRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(FacilityTypeMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(FacilityType.class, id, author, changedPropertyName, page,
        returnJson);
  }
}
