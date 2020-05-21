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

import static org.openlmis.referencedata.domain.RightName.GEOGRAPHIC_ZONES_MANAGE_RIGHT;
import static org.openlmis.referencedata.web.GeographicLevelController.RESOURCE_PATH;

import java.util.UUID;
import org.openlmis.referencedata.domain.GeographicLevel;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.GeographicLevelRepository;
import org.openlmis.referencedata.util.messagekeys.GeographicLevelMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
public class GeographicLevelController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeographicLevelController.class);

  public static final String RESOURCE_PATH = API_PATH + "/geographicLevels";

  @Autowired
  private GeographicLevelRepository geographicLevelRepository;

  /**
   * Allows creating new geographicLevels.
   *
   * @param geographicLevel A geographicLevel bound to the request body.
   * @return the created geographicLevel.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public GeographicLevel createGeographicLevel(
      @RequestBody GeographicLevel geographicLevel) {
    rightService.checkAdminRight(GEOGRAPHIC_ZONES_MANAGE_RIGHT);

    LOGGER.debug("Creating new geographicLevel");
    // Ignore provided id
    geographicLevel.setId(null);
    geographicLevelRepository.save(geographicLevel);
    return geographicLevel;
  }

  /**
   * Get all geographicLevels.
   *
   * @return GeographicLevels.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<GeographicLevel> getAllGeographicLevels() {

    Iterable<GeographicLevel> geographicLevels = geographicLevelRepository.findAll();
    if (geographicLevels == null) {
      throw new NotFoundException(GeographicLevelMessageKeys.ERROR_NOT_FOUND);
    } else {
      return geographicLevels;
    }
  }

  /**
   * Allows updating geographicLevels.
   *
   * @param geographicLevel   A geographicLevel bound to the request body.
   * @param geographicLevelId UUID of geographicLevel which we want to update.
   * @return the updated geographicLevel.
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GeographicLevel updateGeographicLevel(
      @RequestBody GeographicLevel geographicLevel, @PathVariable("id") UUID geographicLevelId) {
    rightService.checkAdminRight(GEOGRAPHIC_ZONES_MANAGE_RIGHT);

    LOGGER.debug("Updating geographicLevel");
    geographicLevelRepository.save(geographicLevel);
    return geographicLevel;
  }

  /**
   * Get chosen geographicLevel.
   *
   * @param geographicLevelId UUID of geographicLevel which we want to get
   * @return the geographicLevel.
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public GeographicLevel getGeographicLevel(
      @PathVariable("id") UUID geographicLevelId) {

    GeographicLevel geographicLevel = geographicLevelRepository.findOne(geographicLevelId);
    if (geographicLevel == null) {
      throw new NotFoundException(GeographicLevelMessageKeys.ERROR_NOT_FOUND);
    } else {
      return geographicLevel;
    }
  }

  /**
   * Allows deleting geographicLevel.
   *
   * @param geographicLevelId UUID of geographicLevel which we want to delete
   */
  @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteGeographicLevel(
      @PathVariable("id") UUID geographicLevelId) {
    rightService.checkAdminRight(GEOGRAPHIC_ZONES_MANAGE_RIGHT);

    GeographicLevel geographicLevel = geographicLevelRepository.findOne(geographicLevelId);
    if (geographicLevel == null) {
      throw new NotFoundException(GeographicLevelMessageKeys.ERROR_NOT_FOUND);
    } else {
      geographicLevelRepository.delete(geographicLevel);
    }
  }

  /**
   * Get the audit information related to geographic level.
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
  public ResponseEntity<String> getGeographicLevelAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    rightService.checkAdminRight(GEOGRAPHIC_ZONES_MANAGE_RIGHT);

    //Return a 404 if the specified instance can't be found
    GeographicLevel instance = geographicLevelRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(GeographicLevelMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(GeographicLevel.class, id, author, changedPropertyName, page,
        returnJson);
  }
}
