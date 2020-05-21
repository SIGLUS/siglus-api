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

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.RightType;
import org.openlmis.referencedata.dto.RightDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.RightRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.RightMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

@NoArgsConstructor
@Controller
@Transactional
public class RightController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RightController.class);

  @Autowired
  private RightRepository rightRepository;

  public RightController(RightRepository repository) {
    this.rightRepository = Objects.requireNonNull(repository);
  }

  /**
   * Get all rights in the system.
   *
   * @return all rights in the system.
   */
  @RequestMapping(value = "/rights", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<RightDto> getAllRights() {

    rightService.checkAdminRight(RightName.RIGHTS_VIEW);

    LOGGER.debug("Getting all rights");
    Set<Right> rights = Sets.newHashSet(rightRepository.findAll());
    return rights.stream().map(this::exportToDto).collect(toSet());
  }

  /**
   * Get chosen right.
   *
   * @param rightId id of the right to get.
   * @return the right.
   */
  @RequestMapping(value = "/rights/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RightDto getRight(@PathVariable("id") UUID rightId) {
    
    rightService.checkRootAccess();

    Right right = rightRepository.findOne(rightId);

    if (right == null) {
      throw new NotFoundException(RightMessageKeys.ERROR_NOT_FOUND);
    } else {
      return exportToDto(right);
    }
  }

  /**
   * Get the audit information related to right.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/rights/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getRightAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    rightService.checkRootAccess();

    //Return a 404 if the specified instance can't be found
    Right instance = rightRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(RightMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(Right.class, id, author, changedPropertyName, page, returnJson);
  }

  /**
   * Save a right using the provided right DTO. If the right does not exist, will create one. If it
   * does exist, will update it.
   *
   * @param rightDto provided right DTO.
   * @return the saved right.
   */
  @RequestMapping(value = "/rights", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RightDto saveRight(@RequestBody RightDto rightDto) {

    rightService.checkRootAccess();

    if (rightDto.getAttachments() != null) {
      for (Right.Importer attachmentDto : rightDto.getAttachments()) {
        Right storedAttachment = rightRepository.findFirstByName(attachmentDto.getName());
        if (storedAttachment == null) {
          throw new ValidationMessageException(new Message(
              RightMessageKeys.ERROR_NAME_NON_EXISTENT, attachmentDto.getName()));
        }

        storedAttachment.export((RightDto) attachmentDto);
      }
    }

    Right rightToSave = Right.newRight(rightDto);

    Right storedRight = rightRepository.findFirstByName(rightToSave.getName());
    if (storedRight != null) {
      LOGGER.debug("Right found in the system, assign id");
      rightToSave.setId(storedRight.getId());
    }

    LOGGER.debug("Saving right");
    rightToSave = rightRepository.save(rightToSave);


    LOGGER.debug("Saved right with id: " + rightToSave.getId());

    return exportToDto(rightToSave);
  }

  /**
   * Delete an existing right.
   *
   * @param rightId id of the right to delete.
   */
  @RequestMapping(value = "/rights/{rightId}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRight(@PathVariable("rightId") UUID rightId) {

    rightService.checkRootAccess();

    Right storedRight = rightRepository.findOne(rightId);
    if (storedRight == null) {
      throw new NotFoundException(RightMessageKeys.ERROR_NOT_FOUND);
    }

    LOGGER.debug("Deleting right");
    rightRepository.delete(rightId);
  }

  /**
   * Finds rights matching all of the provided parameters.
   */
  @RequestMapping(value = "/rights/search", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<RightDto> searchRights(
          @RequestParam(value = "name", required = false) String name,
          @RequestParam(value = "type", required = false) String type) {

    Profiler profiler = new Profiler("SEARCH_FOR_RIGHTS");
    profiler.setLogger(LOGGER);

    profiler.start("CHECK_ADMIN_RIGHT");
    rightService.checkAdminRight(RightName.RIGHTS_VIEW);

    profiler.start("FIND_RIGHT_TYPES");
    RightType rightType = null;
    if (type != null) {
      try {
        rightType = RightType.valueOf(type);
      } catch (IllegalArgumentException ex) {
        String rightNames = String.join(", ", Stream.of(RightType.values())
                .map(RightType::name)
                .collect(Collectors.toSet()));

        profiler.stop().log();
        throw new ValidationMessageException(new Message(RightMessageKeys.ERROR_TYPE_INVALID,
                rightNames, ex));
      }
    }
    profiler.start("SEARCH_FOR_RIGHTS_IN_DB");
    LOGGER.debug("Searching for right with {} type and {} name", type, name);
    Set<Right> foundRights = rightRepository.searchRights(name, rightType);

    profiler.start("EXPORT_TO_DTOS");
    Set<RightDto> dtos = exportToDtos(foundRights);

    profiler.stop().log();
    return dtos;
  }

  private RightDto exportToDto(Right right) {
    RightDto rightDto = new RightDto();
    right.export(rightDto);
    return rightDto;
  }

  private Set<RightDto> exportToDtos(Set<Right> rights) {
    Set<RightDto> rightDtos = new HashSet<>();
    for (Right right : rights) {
      RightDto rightDto = new RightDto();
      right.export(rightDto);
      rightDtos.add(rightDto);
    }
    return rightDtos;
  }
}
