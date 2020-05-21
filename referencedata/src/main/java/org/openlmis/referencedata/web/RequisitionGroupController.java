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

import static org.openlmis.referencedata.domain.RightName.REQUISITION_GROUPS_MANAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.RequisitionGroupDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.RequisitionGroupRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.service.RequisitionGroupService;
import org.openlmis.referencedata.service.RightAssignmentService;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.RequisitionGroupMessageKeys;
import org.openlmis.referencedata.validate.RequisitionGroupValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class RequisitionGroupController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionGroupController.class);

  public static final String RESOURCE_PATH = "/requisitionGroups";
  private static final String ID_PATH = RESOURCE_PATH + "/{id}";
  private static final String AUDIT_LOG_PATH = ID_PATH + "/auditLog";
  private static final String SEARCH_PATH = RESOURCE_PATH + "/search";

  @Autowired
  @Qualifier("requisitionGroupValidator")
  private RequisitionGroupValidator validator;

  @Autowired
  private RequisitionGroupRepository requisitionGroupRepository;

  @Autowired
  private RequisitionGroupService requisitionGroupService;

  @Autowired
  private RightAssignmentService rightAssignmentService;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  /**
   * Allows creating new requisition group. If the id is specified, it will be ignored.
   *
   * @param requisitionGroupDto a requisition group bound to the request body.
   * @return the created RequisitionGroupDto.
   */
  @PostMapping(RESOURCE_PATH)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public RequisitionGroupDto createRequisitionGroup(
      @RequestBody RequisitionGroupDto requisitionGroupDto, BindingResult bindingResult) {

    Profiler profiler = new Profiler("CREATE_NEW_REQUISITION_GROUP");
    profiler.setLogger(LOGGER);

    checkAdminRight(REQUISITION_GROUPS_MANAGE, profiler);

    profiler.start("VALIDATE_REQUISITION_GROUP_DTO");
    validator.validate(requisitionGroupDto, bindingResult);

    if (bindingResult.getErrorCount() == 0) {
      profiler.start("IMPORT_REQUISITION_GROUP_FROM_DTO");
      requisitionGroupDto.setId(null);
      RequisitionGroup requisitionGroup = RequisitionGroup.newRequisitionGroup(requisitionGroupDto);

      profiler.start("SAVE_REQUISITION_GROUP");
      requisitionGroupRepository.saveAndFlush(requisitionGroup);

      profiler.start("REGENERATE_RIGHT_ASSIGNMENTS");
      rightAssignmentService.regenerateRightAssignments();

      LOGGER.info("Created new requisitionGroup with id: {}", requisitionGroup.getId());
      profiler.start("EXPORT_REQUISITION_GROUP_TO_DTO");
      RequisitionGroupDto dto = exportToDto(requisitionGroup);

      profiler.stop().log();
      return dto;
    } else {
      profiler.stop().log();
      throw new ValidationMessageException(bindingResult.getFieldError().getDefaultMessage());
    }
  }

  /**
   * Get all requisition groups.
   *
   * @return the RequisitionGroupDtos.
   */
  @GetMapping(RESOURCE_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<RequisitionGroupDto> getAllRequisitionGroups() {

    Iterable<RequisitionGroup> requisitionGroups = requisitionGroupRepository.findAll();
    List<RequisitionGroupDto> requisitionGroupDtos = new ArrayList<>();
    for (RequisitionGroup requisitionGroup : requisitionGroups) {
      requisitionGroupDtos.add(exportToDto(requisitionGroup));
    }
    return requisitionGroupDtos;
  }

  /**
   * Get chosen requisition group.
   *
   * @param requisitionGroupId the UUID of requisition group whose we want to get.
   * @return the RequisitionGroupDto.
   */
  @GetMapping(ID_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RequisitionGroupDto getRequisitionGroup(
      @PathVariable("id") UUID requisitionGroupId) {

    RequisitionGroup requisitionGroup = requisitionGroupRepository.findOne(requisitionGroupId);
    if (requisitionGroup == null) {
      throw new NotFoundException(RequisitionGroupMessageKeys.ERROR_NOT_FOUND);
    } else {
      return exportToDto(requisitionGroup);
    }
  }

  /**
   * Allows updating requisition group.
   *
   * @param requisitionGroupDto A requisition group bound to the request body.
   * @param requisitionGroupId UUID of requisition group which we want to update.
   * @return the updated RequisitionGroupDto.
   */
  @PutMapping(ID_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RequisitionGroupDto updateRequisitionGroup(
      @RequestBody RequisitionGroupDto requisitionGroupDto,
      @PathVariable("id") UUID requisitionGroupId,
      BindingResult bindingResult) {

    Profiler profiler = new Profiler("UPDATE_REQUISITION_GROUP");
    profiler.setLogger(LOGGER);

    checkAdminRight(REQUISITION_GROUPS_MANAGE, profiler);

    profiler.start("VALIDATE_REQUISITION_GROUP");
    validator.validate(requisitionGroupDto, bindingResult);

    if (bindingResult.getErrorCount() == 0) {
      profiler.start("FIND_REQUISITION_GROUP");
      RequisitionGroup requisitionGroupToUpdate =
          requisitionGroupRepository.findOne(requisitionGroupId);

      if (null == requisitionGroupToUpdate) {
        profiler.start("CREATE_REQUISITION_GROUP");
        requisitionGroupToUpdate = new RequisitionGroup();
      } else {
        LOGGER.info("Updating requisitionGroup with id: {}", requisitionGroupId);
      }

      profiler.start("IMPORT_REQUISITION_GROUP_FROM_DTO");
      requisitionGroupToUpdate.updateFrom(
              RequisitionGroup.newRequisitionGroup(requisitionGroupDto));

      profiler.start("UPDATE_SUPERVISORY_NODE_FROM_REQUISITION_GROUP_DTO");
      SupervisoryNode newNode = getUpdatedSupervisoryNode(requisitionGroupDto);
      requisitionGroupToUpdate.setSupervisoryNode(newNode);

      profiler.start("SAVE_REQUISITION_GROUP");
      requisitionGroupToUpdate = requisitionGroupRepository.saveAndFlush(requisitionGroupToUpdate);

      profiler.start("REGENERATE_RIGHT_ASSIGNMENTS");
      rightAssignmentService.regenerateRightAssignments();

      LOGGER.info("Saved requisitionGroup with id: {}", requisitionGroupToUpdate.getId());
      profiler.start("EXPORT_REQUISITION_GROUP_TO_DTO");
      RequisitionGroupDto dto = exportToDto(requisitionGroupToUpdate);

      profiler.stop().log();
      return dto;
    } else {
      profiler.stop().log();
      throw new ValidationMessageException(bindingResult.getFieldError().getDefaultMessage());
    }
  }

  /**
   * Allows deleting requisition group.
   *
   * @param requisitionGroupId UUID of requisition group whose we want to delete.
   */
  @DeleteMapping(ID_PATH)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRequisitionGroup(@PathVariable("id") UUID requisitionGroupId) {

    Profiler profiler = new Profiler("DELETE_REQUISITION_GROUP");
    profiler.setLogger(LOGGER);

    checkAdminRight(REQUISITION_GROUPS_MANAGE, profiler);

    profiler.start("FIND_REQUISITION_GROUP");
    RequisitionGroup requisitionGroup = requisitionGroupRepository.findOne(requisitionGroupId);
    if (requisitionGroup == null) {
      profiler.stop().log();
      throw new NotFoundException(RequisitionGroupMessageKeys.ERROR_NOT_FOUND);
    } else {
      profiler.start("DELETE_REQUISITION_GROUP");
      requisitionGroupRepository.delete(requisitionGroup);
      requisitionGroupRepository.flush();

      profiler.start("REGENERATE_RIGHT_ASSIGNMENTS");
      rightAssignmentService.regenerateRightAssignments();

      profiler.stop().log();
    }
  }

  /**
   * Get the audit information related to requisition group.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @GetMapping(AUDIT_LOG_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getRequisitionGroupAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    rightService.checkAdminRight(REQUISITION_GROUPS_MANAGE);

    //Return a 404 if the specified instance can't be found
    RequisitionGroup instance = requisitionGroupRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(RequisitionGroupMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(RequisitionGroup.class, id, author, changedPropertyName, page,
        returnJson);
  }

  /**
   * Retrieves required page of Requisition Groups that are matching given parameters.
   *
   * @param queryParams request parameters (code, name, zone, program).
   * @param pageable object used to encapsulate the pagination related values: page and size.
   * @return Page of wanted Requisition Groups matching query parameters.
   */
  @PostMapping(SEARCH_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<RequisitionGroupDto> search(@RequestBody Map<String, Object> queryParams,
                                                    Pageable pageable) {

    Page<RequisitionGroup> page = requisitionGroupService
        .searchRequisitionGroups(queryParams, pageable);

    return exportToDto(page, pageable);
  }

  private RequisitionGroupDto exportToDto(RequisitionGroup requisitionGroup) {
    RequisitionGroupDto requisitionGroupDto = null;

    if (requisitionGroup != null) {
      requisitionGroupDto = new RequisitionGroupDto();
      requisitionGroup.export(requisitionGroupDto);
    }

    return requisitionGroupDto;
  }

  private Page<RequisitionGroupDto> exportToDto(Page<RequisitionGroup> page, Pageable pageable) {
    List<RequisitionGroupDto> list = page.getContent().stream()
        .map(this::exportToDto).collect(Collectors.toList());
    return Pagination.getPage(list, pageable, page.getTotalElements());
  }

  private SupervisoryNode getUpdatedSupervisoryNode(RequisitionGroupDto dto) {
    return supervisoryNodeRepository.findOne(dto.getSupervisoryNode().getId());
  }
}
