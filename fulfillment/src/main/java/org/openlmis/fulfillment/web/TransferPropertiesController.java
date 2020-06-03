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

package org.openlmis.fulfillment.web;

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.openlmis.fulfillment.service.ExporterBuilder;
import org.openlmis.fulfillment.service.IncorrectTransferPropertiesException;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.service.TransferPropertiesService;
import org.openlmis.fulfillment.util.Message;
import org.openlmis.fulfillment.web.util.TransferPropertiesDto;
import org.openlmis.fulfillment.web.util.TransferPropertiesFactory;
import org.openlmis.fulfillment.web.validator.FtpTransferPropertiesValidator;
import org.openlmis.fulfillment.web.validator.LocalTransferPropertiesValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class TransferPropertiesController extends BaseController {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransferPropertiesController.class);

  @Autowired
  private TransferPropertiesRepository transferPropertiesRepository;

  @Autowired
  private TransferPropertiesService transferPropertiesService;

  @Autowired
  private ExporterBuilder exporter;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private FtpTransferPropertiesValidator ftpTransferPropertiesValidator;

  @Autowired
  private LocalTransferPropertiesValidator localTransferPropertiesValidator;

  /**
   * Allows creating new transfer properties.
   * If the id is specified, it will be ignored.
   *
   * @param properties A transfer properties bound to the request body
   * @return ResponseEntity containing the created transfer properties
   */
  @RequestMapping(value = "/transferProperties", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity create(@RequestBody TransferPropertiesDto properties) {

    LOGGER.debug("Checking right to create transfer properties");
    permissionService.canManageSystemSettings();

    LOGGER.debug("Creating new Transfer Properties");

    TransferProperties entity = TransferPropertiesFactory.newInstance(properties);

    List<Message.LocalizedMessage> errors = validate(entity);
    if (isNotTrue(errors.isEmpty())) {
      return ResponseEntity.badRequest().body(errors);
    }

    properties.setId(null);
    TransferProperties saved = transferPropertiesService.save(entity);

    LOGGER.debug("Created new Transfer Properties with id: {}", saved.getId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(TransferPropertiesFactory.newInstance(saved, exporter));
  }

  /**
   * Allows updating transfer properties.
   *
   * @param properties   A transfer properties bound to the request body
   * @param id UUID of transfer properties which we want to update
   * @return ResponseEntity containing the updated transfer properties
   */
  @RequestMapping(value = "/transferProperties/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity update(@RequestBody TransferPropertiesDto properties,
                               @PathVariable("id") UUID id) {
    LOGGER.debug("Checking right to update transfer properties ");
    permissionService.canManageSystemSettings();
    TransferProperties toUpdate = transferPropertiesRepository.findOne(id);

    if (null == toUpdate) {
      return ResponseEntity.notFound().build();
    } else if (null == properties.getFacility()
        || !Objects.equals(toUpdate.getFacilityId(), properties.getFacility().getId())) {
      throw new IncorrectTransferPropertiesException();
    } else {
      LOGGER.debug("Updating Transfer Properties with id: {}", id);
    }

    TransferProperties entity = TransferPropertiesFactory.newInstance(properties);

    if (!Objects.equals(entity.getClass(), toUpdate.getClass())) {
      LOGGER.debug("Removed old Transfer Properties with id: {}", id);
      transferPropertiesRepository.delete(toUpdate);
      transferPropertiesRepository.flush();
    }

    List<Message.LocalizedMessage> errors = validate(entity);
    if (isNotTrue(errors.isEmpty())) {
      return ResponseEntity.badRequest().body(errors);
    }

    toUpdate = transferPropertiesRepository.save(entity);

    LOGGER.debug("Updated Transfer Properties with id: {}", toUpdate.getId());

    return ResponseEntity.ok(TransferPropertiesFactory.newInstance(toUpdate, exporter));
  }

  /**
   * Get chosen transfer properties.
   *
   * @param id UUID of ftransfer properties whose we want to get
   * @return {@link TransferPropertiesDto}.
   */
  @RequestMapping(value = "/transferProperties/{id}", method = RequestMethod.GET)
  public ResponseEntity retrieve(@PathVariable("id") UUID id) {

    LOGGER.debug("Checking right to view transfer properties");
    permissionService.canManageSystemSettings();

    TransferProperties properties = transferPropertiesRepository.findOne(id);
    return properties == null
        ? ResponseEntity.notFound().build()
        : ResponseEntity.ok(TransferPropertiesFactory.newInstance(properties, exporter));
  }

  /**
   * Allows deleting transfer properties.
   *
   * @param id UUID of transfer properties which we want to delete
   * @return ResponseEntity containing the HTTP Status
   */
  @RequestMapping(value = "/transferProperties/{id}", method = RequestMethod.DELETE)
  public ResponseEntity delete(@PathVariable("id") UUID id) {

    LOGGER.debug("Checking right to delete transfer properties");
    permissionService.canManageSystemSettings();

    TransferProperties toDelete = transferPropertiesRepository.findOne(id);

    if (toDelete == null) {
      return ResponseEntity.notFound().build();
    } else {
      transferPropertiesRepository.delete(toDelete);
      return ResponseEntity.noContent().build();
    }
  }

  /**
   * Get transfer properties by facility ID.
   *
   * @param facility UUID of facility
   * @return transfer properties related with the given facility
   */
  @RequestMapping(value = "/transferProperties/search", method = RequestMethod.GET)
  public ResponseEntity search(@RequestParam("facility") UUID facility,
      @RequestParam(value = "transferType", required = false, defaultValue = "ORDER")
          TransferType transferType) {

    LOGGER.debug("Checking right to view transfer properties");
    permissionService.canManageSystemSettings();

    TransferProperties properties = transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(facility, transferType);

    if (properties == null) {
      return ResponseEntity.notFound().build();
    } else {
      return ResponseEntity.ok(TransferPropertiesFactory.newInstance(properties, exporter));
    }
  }

  private List<Message.LocalizedMessage> validate(TransferProperties entity) {
    List<Message.LocalizedMessage> errors;
    if (FtpTransferProperties.class.equals(entity.getClass())) {
      errors = ftpTransferPropertiesValidator.validate((FtpTransferProperties) entity);
    } else {
      errors = localTransferPropertiesValidator.validate((LocalTransferProperties) entity);
    }
    return errors;
  }
}
