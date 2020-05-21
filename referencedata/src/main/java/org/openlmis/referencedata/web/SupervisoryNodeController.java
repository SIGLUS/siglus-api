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
import static org.openlmis.referencedata.domain.RightName.SUPERVISORY_NODES_MANAGE;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.AvailableFeatures;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.SupervisoryNodeDto;
import org.openlmis.referencedata.dto.UserDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RequisitionGroupRepository;
import org.openlmis.referencedata.repository.RightRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.repository.custom.impl.SupervisoryNodeDtoRedisRepository;
import org.openlmis.referencedata.service.RightAssignmentService;
import org.openlmis.referencedata.service.SupervisoryNodeBuilder;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RightMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.openlmis.referencedata.validate.SupervisoryNodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

@Controller
@Transactional
@SuppressWarnings({"PMD.TooManyMethods"})
public class SupervisoryNodeController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupervisoryNodeController.class);
  public static final String RESOURCE_PATH = "/supervisoryNodes";

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private SupervisoryNodeDtoRedisRepository supervisoryNodeDtoRedisRepository;

  @Autowired
  private ProgramRepository programRepository;
  
  @Autowired
  private RightRepository rightRepository;
  
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RightAssignmentService rightAssignmentService;

  @Autowired
  private SupervisoryNodeValidator validator;

  @Autowired
  private SupervisoryNodeBuilder builder;

  @Autowired
  private RequisitionGroupRepository requisitionGroupRepository;

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Allows creating new supervisoryNode. If the id is specified, it will be ignored.
   *
   * @param supervisoryNodeDto A supervisoryNodeDto bound to the request body.
   * @return the created supervisoryNode.
   */
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public SupervisoryNodeDto createSupervisoryNode(
      @RequestBody SupervisoryNodeDto supervisoryNodeDto,
      BindingResult bindingResult) {

    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    supervisoryNodeDto.setId(null);
    validator.validate(supervisoryNodeDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    SupervisoryNode supervisoryNode = builder.build(supervisoryNodeDto);
    supervisoryNodeRepository.save(supervisoryNode);
    LOGGER.info("Created new supervisoryNode with id: {}", supervisoryNode.getId());

    if (null != supervisoryNodeDto.getRequisitionGroupId()) {
      RequisitionGroup requisitionGroup =
              updateRequisitionGroup(supervisoryNode, supervisoryNodeDto);
      LOGGER.info("SupervisoryNode with id: {} was added to requisitionGroup with id: {}",
              supervisoryNode.getId(), requisitionGroup.getId());
    }

    return exportToDto(supervisoryNode);
  }

  /**
   * Get chosen supervisoryNode.
   *
   * @param supervisoryNodeId UUID of the supervisoryNode whose we want to get.
   * @return the SupervisoryNode.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public SupervisoryNodeDto getSupervisoryNode(
      @PathVariable("id") UUID supervisoryNodeId) {

    Profiler profiler = new Profiler("GET_SUPERVISORY_NODE");
    profiler.setLogger(LOGGER);

    SupervisoryNodeDto supervisoryNodeDto;

    if (AvailableFeatures.REDIS_CACHING.isActive()) {
      supervisoryNodeDto = getSupervisoryNodeDtoFromCache(supervisoryNodeId, profiler);
      profiler.stop().log();
      return supervisoryNodeDto;
    }

    if (!supervisoryNodeRepository.exists(supervisoryNodeId)) {
      profiler.stop().log();
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("GET_SUPERVISORY_NODE_FROM_DATABASE");
    SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);
    profiler.start("EXPORT_TO_DTO");
    supervisoryNodeDto = exportToDto(supervisoryNode);

    profiler.stop().log();
    return supervisoryNodeDto;
  }

  /**
   * Allows updating supervisoryNode.
   *
   * @param supervisoryNodeDto A supervisoryNodeDto bound to the request body.
   * @param supervisoryNodeId UUID of the supervisoryNode which we want to update.
   * @return the updated supervisoryNode.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public SupervisoryNodeDto updateSupervisoryNode(
      @RequestBody SupervisoryNodeDto supervisoryNodeDto,
      @PathVariable("id") UUID supervisoryNodeId,
      BindingResult bindingResult) {

    Profiler profiler = new Profiler("UPDATE_SUPERVISORY_NODE");
    profiler.setLogger(LOGGER);

    profiler.start("CHECK_ADMIN");
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);
    LOGGER.info("Updating supervisoryNode with id: {}", supervisoryNodeId);

    profiler.start("VALIDATE_SUPERVISORY_NODE");
    validator.validate(supervisoryNodeDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    profiler.start("BUILD_DOMAIN_OBJ_FROM_DTO");
    SupervisoryNode supervisoryNodeToUpdate = builder.build(supervisoryNodeDto);

    profiler.start("SAVE_SUPERVISORY_NODE");
    supervisoryNodeRepository.saveAndFlush(supervisoryNodeToUpdate);

    profiler.start("CHECK_IF_REQUISITION_GROUP_IS_ADDED");
    if (null != supervisoryNodeDto.getRequisitionGroupId()) {
      RequisitionGroup requisitionGroup =
              updateRequisitionGroup(supervisoryNodeToUpdate, supervisoryNodeDto);
      LOGGER.info("Updated supervisoryNode with id: {} with requisitionGroup with id: {}",
              supervisoryNodeId, requisitionGroup.getId());
    }

    profiler.start("DELETE_UPDATED_SUPERVISORY_NODE_FROM_CACHE");
    if (AvailableFeatures.REDIS_CACHING.isActive()) {
      deleteSupervisoryNodeDtoFromCache(supervisoryNodeId);
    }

    profiler.start("REGENERATE_RIGHT_ASSIGNMENTS");
    rightAssignmentService.regenerateRightAssignments();

    LOGGER.info("Updated supervisoryNode with id: {}", supervisoryNodeId);
    profiler.start("EXPORT_SUPERVISORY_NODE_TO_DTO");
    SupervisoryNodeDto dto = exportToDto(supervisoryNodeToUpdate);

    profiler.stop().log();
    return dto;
  }

  /**
   * Allows deleting supervisoryNode.
   *
   * @param supervisoryNodeId UUID of supervisoryNode whose we want to delete.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.DELETE)
  public ResponseEntity deleteSupervisoryNode(@PathVariable("id") UUID supervisoryNodeId) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    if (AvailableFeatures.REDIS_CACHING.isActive()) {
      deleteSupervisoryNodeDtoFromCache(supervisoryNodeId);
    }

    SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);
    if (supervisoryNode == null) {
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    } else {
      supervisoryNodeRepository.delete(supervisoryNode);
      return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Find supervising users by right and program.
   *
   * @param rightId UUID of right that user has.
   * @param programId UUID of program.
   * @return the found users.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}/supervisingUsers", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<UserDto> findSupervisingUsers(
      @PathVariable("id") UUID supervisoryNodeId,
      @RequestParam("rightId") UUID rightId,
      @RequestParam("programId") UUID programId) {

    Profiler profiler = new Profiler("GET_SUPERVISING_USERS");
    profiler.setLogger(LOGGER);

    profiler.start("CHECK_ADMIN_RIGHT");
    rightService.checkAdminRight(RightName.USERS_MANAGE_RIGHT);

    profiler.start("CHECK_IF_SUPERVISORY_NODE_EXISTS");
    if (!supervisoryNodeRepository.exists(supervisoryNodeId)) {
      profiler.stop().log();
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("CHECK_IF_RIGHT_EXISTS");
    if (!rightRepository.exists(rightId)) {
      profiler.stop().log();
      throw new ValidationMessageException(RightMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("CHECK_IF_PROGRAM_EXISTS");
    if (!programRepository.exists(programId)) {
      profiler.stop().log();
      throw new ValidationMessageException(ProgramMessageKeys.ERROR_NOT_FOUND);
    }
    
    profiler.start("FIND_USERS_BY_SUPERVISION_RIGHT_IN_DB");
    Set<User> supervisingUsers = userRepository.findUsersBySupervisionRight(rightId,
        supervisoryNodeId, programId);

    profiler.stop().log();
    return supervisingUsers.stream().map(this::exportToDto).collect(toSet());
  }

  /**
   * Find supervising facilities by program.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}/facilities", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<FacilityDto> findSupervisingFacilities(
      @PathVariable("id") UUID supervisoryNodeId,
      @RequestParam(value = "programId", required = false) UUID programId,
      Pageable pageable) {
    rightService.checkAdminRight(RightName.SUPERVISORY_NODES_MANAGE);

    SupervisoryNode supervisoryNode = Optional
        .ofNullable(supervisoryNodeRepository.findOne(supervisoryNodeId))
        .orElseThrow(() -> new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND));

    Program program;

    if (null == programId) {
      program = null;
    } else {
      program = Optional
          .ofNullable(programRepository.findOne(programId))
          .orElseThrow(() -> new NotFoundException(ProgramMessageKeys.ERROR_NOT_FOUND));
    }

    Set<Facility> facilities = supervisoryNode.getAllSupervisedFacilities(program);
    Page<Facility> facilityPage = Pagination.getPage(facilities, pageable);
    List<FacilityDto> facilityDtos = facilityPage
        .getContent()
        .stream()
        .map(FacilityDto::newInstance)
        .collect(Collectors.toList());

    return Pagination.getPage(facilityDtos, pageable, facilities.size());
  }

  /**
   * Retrieves all Supervisory Nodes that are matching given query parameters
   * (code, name, zoneId, programId, facilityId, id - multiple).
   *
   * @param queryParams request parameters
   * @param pageable    object used to encapsulate the pagination related values: page and size.
   * @return List of wanted Supervisory Nodes matching query parameters.
   */
  @GetMapping(RESOURCE_PATH)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<SupervisoryNodeDto> search(@RequestParam MultiValueMap<String, Object> queryParams,
      Pageable pageable) {
    Profiler profiler = new Profiler("SEARCH_SUPERVISORY_NODES");
    profiler.setLogger(LOGGER);

    profiler.start("CONVERT_QUERY_PARAMS");
    SupervisoryNodeSearchParams params = new SupervisoryNodeSearchParams(queryParams);

    profiler.start("GET_SUPERVISORY_NODES_FROM_DB");
    Page<SupervisoryNode> supervisoryNodePage = supervisoryNodeRepository.search(params, pageable);

    profiler.start("TO_DTO");
    Page<SupervisoryNodeDto> dtoPage = exportToDto(supervisoryNodePage, pageable);

    profiler.stop().log();
    return dtoPage;
  }

  /**
   * Get the audit information related to stock supervisory node.
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
  public ResponseEntity<String> getSupervisoryNodeAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    rightService.checkAdminRight(SUPERVISORY_NODES_MANAGE);

    //Return a 404 if the specified instance can't be found
    SupervisoryNode instance = supervisoryNodeRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(SupervisoryNode.class, id, author, changedPropertyName, page,
        returnJson);
  }

  /**
   * Get the supervisory node from cache.
   */
  private SupervisoryNodeDto getSupervisoryNodeDtoFromCache(UUID supervisoryNodeId,
      Profiler profiler) {
    SupervisoryNodeDto supervisoryNodeDto;
    SupervisoryNode supervisoryNode;

    profiler.start("CHECK_IF_SUPERVISORY_NODE_EXISTS_IN_CACHE");
    boolean supervisoryNodeIsInCache = supervisoryNodeDtoRedisRepository
        .exists(supervisoryNodeId);

    if (supervisoryNodeIsInCache) {
      profiler.start("GET_SUPERVISORY_NODE_FROM_CACHE");
      supervisoryNodeDto = supervisoryNodeDtoRedisRepository.findById(supervisoryNodeId);
    } else if (!supervisoryNodeRepository.exists(supervisoryNodeId)) {
      profiler.stop().log();
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    } else {
      profiler.start("GET_SUPERVISORY_NODE_FROM_DATABASE");
      supervisoryNode = supervisoryNodeRepository.findOne(supervisoryNodeId);
      profiler.start("EXPORT_TO_DTO");
      supervisoryNodeDto = exportToDto(supervisoryNode);
      profiler.start("SAVE_SUPERVISORY_NODE_IN_CACHE");
      supervisoryNodeDtoRedisRepository.save(supervisoryNodeDto);
    }
    return supervisoryNodeDto;
  }

  /**
   * Delete the supervisory node from cache.
   */
  private void deleteSupervisoryNodeDtoFromCache(UUID supervisoryNodeId) {
    if (!supervisoryNodeDtoRedisRepository.exists(supervisoryNodeId)) {
      throw new NotFoundException(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);
    } else {
      SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeDtoRedisRepository
          .findById(supervisoryNodeId);
      supervisoryNodeDtoRedisRepository.delete(supervisoryNodeDto);
    }
  }

  private SupervisoryNodeDto exportToDto(SupervisoryNode supervisoryNode) {
    SupervisoryNodeDto supervisoryNodeDto = null;

    if (supervisoryNode != null) {
      supervisoryNodeDto = new SupervisoryNodeDto();
      supervisoryNodeDto.setServiceUrl(serviceUrl);
      supervisoryNode.export(supervisoryNodeDto);
    }

    return supervisoryNodeDto;
  }

  private Page<SupervisoryNodeDto> exportToDto(Page<SupervisoryNode> page,
      Pageable pageable) {
    List<SupervisoryNodeDto> content = page
        .getContent()
        .stream()
        .map(this::exportToDto)
        .collect(Collectors.toList());

    return Pagination.getPage(content, pageable, page.getTotalElements());
  }

  private UserDto exportToDto(User user) {
    UserDto userDto = null;

    if (user != null) {
      userDto = new UserDto();
      user.export(userDto);
    }

    return userDto;
  }

  private RequisitionGroup updateRequisitionGroup(SupervisoryNode existing,
      SupervisoryNodeDto supervisoryNodeDto) {
    RequisitionGroup requisitionGroup =
            requisitionGroupRepository.findOne(supervisoryNodeDto.getRequisitionGroupId());
    requisitionGroup.setSupervisoryNode(supervisoryNodeRepository
            .findOne(existing.getId()));
    requisitionGroupRepository.saveAndFlush(requisitionGroup);
    return requisitionGroup;
  }
}
