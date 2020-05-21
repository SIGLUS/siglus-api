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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.DirectRoleAssignment;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FulfillmentRoleAssignment;
import org.openlmis.referencedata.domain.Identifiable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.Role;
import org.openlmis.referencedata.domain.RoleAssignment;
import org.openlmis.referencedata.domain.SupervisionRoleAssignment;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.DetailedRoleAssignmentDto;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.NamedResource;
import org.openlmis.referencedata.dto.ProgramDto;
import org.openlmis.referencedata.dto.ResultDto;
import org.openlmis.referencedata.dto.RoleAssignmentDto;
import org.openlmis.referencedata.dto.UserDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.RightAssignmentRepository;
import org.openlmis.referencedata.repository.RightRepository;
import org.openlmis.referencedata.repository.RoleAssignmentRepository;
import org.openlmis.referencedata.repository.RoleRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.repository.UserSearchParams;
import org.openlmis.referencedata.service.UserService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RightMessageKeys;
import org.openlmis.referencedata.util.messagekeys.UserMessageKeys;
import org.openlmis.referencedata.validate.UserValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
@Controller
@Transactional
public class UserController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(UserController.class);
  private static final String USER_ID = "userId";

  private static final String PROFILER_TO_DTO = "TO_DTO";

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private RightRepository rightRepository;

  @Autowired
  private Validator validator;

  @Autowired
  private UserValidator userValidator;
  
  @Autowired
  private RightAssignmentRepository rightAssignmentRepository;
  
  @Autowired
  private RoleAssignmentRepository roleAssignmentRepository;

  /**
   * Constructor for controller unit testing.
   */
  public UserController(UserService userService,
                        UserRepository userRepository,
                        RoleRepository roleRepository,
                        RightRepository rightRepository,
                        ProgramRepository programRepository,
                        SupervisoryNodeRepository supervisoryNodeRepository,
                        FacilityRepository facilityRepository) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.rightRepository = rightRepository;
    this.programRepository = programRepository;
    this.supervisoryNodeRepository = supervisoryNodeRepository;
    this.facilityRepository = facilityRepository;
  }

  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.setValidator(this.validator);
  }

  /**
   * Custom endpoint for creating and updating users and their roles.
   */
  @RequestMapping(value = "/users", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserDto saveUser(@RequestBody @Valid UserDto userDto, BindingResult bindingResult) {
    Profiler profiler = new Profiler("CREATE_USER");
    profiler.setLogger(LOGGER);

    UUID userId = userDto.getId();
    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    profiler.start("VALIDATE_USER");
    userValidator.validate(userDto, bindingResult);
    if (bindingResult.hasErrors()) {
      throw new ValidationMessageException(bindingResult.getFieldError().getDefaultMessage());
    }

    User user;
    profiler.start("USER_EXISTS_IN_DB_CHECK");
    if (userId != null && userRepository.exists(userId)) {
      profiler.start("GET_USER_FROM_DB");
      user = userRepository.findOne(userId);
      profiler.start("UPDATE_USER_FROM_DTO");
      user.updateFrom(userDto);
    } else {
      profiler.start("CREATE_USER_FROM_DTO");
      user = User.newUser(userDto);
    }

    profiler.start("ASSIGN_ROLES_TO_USER");
    Set<RoleAssignmentDto> roleAssignmentDtos = userDto.getRoleAssignments();
    if (roleAssignmentDtos != null) {

      boolean foundNullRoleId = roleAssignmentDtos.stream().anyMatch(
          roleAssignmentDto -> roleAssignmentDto.getRoleId() == null);
      if (foundNullRoleId) {
        throw new ValidationMessageException(UserMessageKeys.ERROR_ROLE_ID_NULL);
      }

      // hibernate is first adding new assignments and then deleting duplicates
      // which violates unique constraint on right_assignment table
      // that is why we clear collections first and then use flush
      user.clearRoleAssignments();
      user = userRepository.saveAndFlush(user);

      assignRolesToUser(roleAssignmentDtos, user);
    }

    profiler.start("SAVE_USER");
    user = userRepository.save(user);

    profiler.start(PROFILER_TO_DTO);
    UserDto responseDto = exportUserToDto(user);

    profiler.start("ADD_ROLE_ASSIGNMENTS_IDS_TO_RESPONSE");
    addRoleAssignmentIdsToUserDto(responseDto);

    profiler.stop().log();

    return responseDto;
  }

  /**
   * Returns all matching users. If no params provided, returns all users.
   *
   * @param requestParams request parameters (id, username, firstName, lastName, email,
   *                      homeFacility, active, verified).
   *                      There can be multiple id params. Other params are ignored if id is
   *                      provided. When id is not provided and if other params have multiple
   *                      values, the first one is used.
   *
   *                      For firstName, lastName, email: finds any values that have entered
   *                      string value in any position of searched field. Not case sensitive.
   *
   *                      Other fields: entered string value must equal to searched value.
   *
   * @param pageable Pageable object that allows client to optionally add "page" (page number).
   *                 "size" (page size) and "sort" (with values "property,asc/desc")
   *                 query parameters to the request.
   *
   * @return the Users.
   */
  @RequestMapping(value = "/users", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<UserDto> getUsers(UserSearchParams requestParams,
                                Pageable pageable) {
    Profiler profiler = new Profiler("GET_USERS");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, profiler);

    LOGGER.debug("Getting all users");
    profiler.start("SEARCH_USERS");
    Page<User> result = userService.searchUsersById(requestParams, pageable);

    profiler.start("EXPORT_TO_DTOS");
    Page<UserDto> userDtos = exportUsersToDtos(result, pageable);

    profiler.stop().log();
    return userDtos;
  }

  /**
   * Get chosen user and role.
   *
   * @param userId UUID of user whose we want to get.
   * @return the User.
   */
  @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserDto getUser(@PathVariable("userId") UUID userId) {
    XLOGGER.entry(userId);
    Profiler profiler = new Profiler("GET_SINGLE_USER");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    profiler.start("FIND_USER");
    User user = userRepository.findOne(userId);
    if (user == null) {
      LOGGER.error("User to get does not exist");
      throw new NotFoundException(UserMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("EXPORT_USER");
    UserDto userDto = exportUserToDto(user);

    profiler.start("ADD_ROLE_ASSIGNMENTS_TO_USER_DTO");
    addRoleAssignmentIdsToUserDto(userDto);

    profiler.stop().log();
    XLOGGER.exit(user);
    return userDto;
  }

  /**
   * Get all rights and roles of the specified user.
   *
   * @param userId UUID of the user to retrieve.
   * @return a set of user role assignments.
   */
  @RequestMapping(value = "/users/{userId}/roleAssignments", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<DetailedRoleAssignmentDto> getUserRightsAndRoles(@PathVariable("userId") UUID userId) {
    Profiler profiler = new Profiler("GET_USER_ROLE_ASSIGNMENTS");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    profiler.start("FIND_USER_ROLE_ASSIGNMENTS");
    User user = userRepository.findOne(userId);
    if (user == null) {
      throw new NotFoundException(UserMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("USER_GET_ROLE_ASSIGNMENTS");
    Set<RoleAssignment> roleAssignments = user.getRoleAssignments();

    profiler.start("EXPORT_TO_DTO");
    Set<DetailedRoleAssignmentDto> result =  exportRoleAssignmentsToDtos(roleAssignments);

    profiler.stop().log();

    return result;
  }

  /**
   * Allows deleting user.
   *
   * @param userId UUID of user whose we want to delete
   */
  @RequestMapping(value = "/users/{userId}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void deleteUser(@PathVariable("userId") UUID userId) {
    Profiler profiler = new Profiler("DELETE_USER");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    profiler.start("FIND_USER");
    User user = userRepository.findOne(userId);
    if (user == null) {
      throw new NotFoundException(UserMessageKeys.ERROR_NOT_FOUND);
    } else {
      profiler.start("DELETE_USER_FROM_DB");
      userRepository.delete(userId);
    }
    profiler.stop().log();
  }

  /**
   * Returns all matching users sorted by username in alphabetically descending order.
   *
   * @param queryParams request parameters (username, firstName, lastName, email, homeFacility,
   *                 active, verified) and JSON extraData.
   *
   *                 For firstName, lastName, email: finds any values that have entered
   *                 string value in any position of searched field. Not case sensitive.
   *
   *                 Other fields: entered string value must equal to searched value.
   *
   * @param pageable Pageable object that allows client to optionally add "page" (page number)
   *                 and "size" (page size) query parameters to the request.
   * @return a list of all Users matching provided parameters.
   */
  @RequestMapping(value = "/users/search", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<UserDto> searchUsers(
      @RequestBody UserSearchParams queryParams, Pageable pageable) {

    Profiler profiler = new Profiler("POST_USER_SEARCH");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, profiler);

    profiler.start("SEARCH_USERS");
    Page<User> result = userService.searchUsers(queryParams, pageable);

    profiler.start("EXPORT_TO_DTOS");
    Page<UserDto> userDtos = exportUsersToDtos(result, pageable);

    profiler.stop().log();
    return userDtos;
  }

  /**
   * Check if user has a right with certain criteria.
   *
   * @param userId      id of user to check for right
   * @param rightId     right to check
   * @param programId   program to check (for supervision rights)
   * @param facilityId  facility to check (for supervision rights)
   * @param warehouseId warehouse to check (for order fulfillment rights)
   * @return if successful, true or false depending on if user has the right
   */
  @RequestMapping(value = "/users/{userId}/hasRight", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResultDto<Boolean> checkIfUserHasRight(@PathVariable(USER_ID) UUID userId,
                                               @RequestParam(value = "rightId") UUID rightId,
                                               @RequestParam(value = "programId",
                                                   required = false) UUID programId,
                                               @RequestParam(value = "facilityId",
                                                   required = false) UUID facilityId,
                                               @RequestParam(value = "warehouseId",
                                                   required = false) UUID warehouseId) {
    Profiler profiler = new Profiler("GET_USER_HAS_RIGHT");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    checkUserExists(userId, profiler);

    boolean hasRight;

    profiler.start("GET_RIGHT");
    Right right = rightRepository.findOne(rightId);

    if (programId != null) {
      profiler.start("CHECK_PROGRAM_EXISTS");
      if (!programRepository.exists(programId)) {
        throw new ValidationMessageException(new Message(
            ProgramMessageKeys.ERROR_NOT_FOUND_WITH_ID, programId));
      }

      if (facilityId != null) {
        profiler.start("CHECK_FACILITY_EXISTS");
        if (!facilityRepository.exists(facilityId)) {
          throw new ValidationMessageException(new Message(
              FacilityMessageKeys.ERROR_NOT_FOUND_WITH_ID, facilityId));
        }

        profiler.start("CHECK_HAS_RIGHT_BY_USER_RIGHT_FACILITY_PROGRAM");
        hasRight = rightAssignmentRepository.existsByUserIdAndAndRightNameAndFacilityIdAndProgramId(
            userId, right.getName(), facilityId, programId);

      } else {
        throw new ValidationMessageException(UserMessageKeys.ERROR_PROGRAM_WITHOUT_FACILITY);
      }

    } else if (warehouseId != null) {

      profiler.start("CHECK_WAREHOUSE_EXISTS");
      if (!facilityRepository.exists(warehouseId)) {
        throw new ValidationMessageException(new Message(
            FacilityMessageKeys.ERROR_NOT_FOUND_WITH_ID, warehouseId));
      }

      profiler.start("CHECK_HAS_RIGHT_BY_USER_RIGHT_WAREHOUSE");
      hasRight = rightAssignmentRepository.existsByUserIdAndAndRightNameAndFacilityId(
          userId, right.getName(), warehouseId);

    } else {
      profiler.start("CHECK_HAS_RIGHT_BY_USER_RIGHT");
      hasRight = rightAssignmentRepository.existsByUserIdAndRightName(userId, right.getName());
    }

    profiler.stop().log();
    LOGGER.info("hasRight(userId={},rightName={},facilityId={},programId={},warehouseId={}) = {}",
        userId, right.getName(), facilityId, programId, warehouseId, hasRight);
    return new ResultDto<>(hasRight);
  }

  /**
   * Get the programs at a user's home facility or programs that the user supervises.
   *
   * @param userId id of user to get programs
   * @return a set of programs
   */
  @RequestMapping(value = "/users/{userId}/programs", method = RequestMethod.GET)
  public ResponseEntity<Set<ProgramDto>> getUserPrograms(@PathVariable(USER_ID) UUID userId) {
    XLOGGER.entry(userId);
    Profiler profiler = new Profiler("GET_USER_PROGRAMS");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);
    checkUserExists(userId, profiler);

    profiler.start("GET_SUPERVISION_PROGRAMS_BY_USER");
    Set<Program> userPrograms = programRepository.findSupervisionProgramsByUser(userId);

    profiler.start("EXPORT_USER_PROGRAMS");
    Set<ProgramDto> userProgramDtos = programsToDto(userPrograms);

    profiler.stop().log();
    XLOGGER.exit(userProgramDtos);
    return ResponseEntity
        .ok()
        .eTag(Integer.toString(userProgramDtos.hashCode()))
        .body(userProgramDtos);
  }
  
  /**
   * Get the programs at a user's home facility that are supported by home facility. Support must be
   * active and supported program must be active.
   *
   * @param userId id of user to get programs
   * @return a set of programs
   */
  @RequestMapping(value = "/users/{userId}/supportedPrograms", method = RequestMethod.GET)
  public ResponseEntity<Set<ProgramDto>> getUserSupportedPrograms(
      @PathVariable(USER_ID) UUID userId) {
    Profiler profiler = new Profiler("GET_USER_SUPPORTED_PROGRAMS");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);
    checkUserExists(userId, profiler);

    profiler.start("GET_HOME_FACILITY_SUPERVISION_PROGRAMS_BY_USER");
    Set<Program> userHomeFacilityPrograms = programRepository
        .findHomeFacilitySupervisionProgramsByUser(userId);

    profiler.start("EXPORT_USER_PROGRAMS");
    Set<ProgramDto> userHomeFacilityProgramDtos = programsToDto(userHomeFacilityPrograms);

    profiler.stop().log();
    return ResponseEntity
        .ok()
        .eTag(Integer.toString(userHomeFacilityProgramDtos.hashCode()))
        .body(userHomeFacilityProgramDtos);
  }

  /**
   * Get all the facilities that the user has fulfillment rights for.
   *
   * @param userId id of user to get fulfillment facilities
   * @return a set of fulfillment facilities
   */
  @RequestMapping(value = "/users/{userId}/fulfillmentFacilities", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<FacilityDto> getUserFulfillmentFacilities(
      @PathVariable(USER_ID) UUID userId,
      @RequestParam(value = "rightId") UUID rightId) {
    Profiler profiler = new Profiler("GET_USER_FULFILLMENT_FACILITIES");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    profiler.start("VALIDATE_USER");
    User user = validateUser(userId);

    profiler.start("VALIDATE_RIGHT");
    Right right = (Right) validateId(rightId, rightRepository).orElseThrow(() ->
        new ValidationMessageException(
            new Message(RightMessageKeys.ERROR_NOT_FOUND_WITH_ID, rightId)));

    profiler.start("GET_FULFILLMENT_FACILITIES");
    Set<Facility> facilities = user.getFulfillmentFacilities(right);

    profiler.start(PROFILER_TO_DTO);
    Set<FacilityDto> facilityDtos = facilitiesToDto(facilities);

    profiler.stop().log();

    return facilityDtos;
  }

  /**
   * Searches for users based having the specified right. The params are required
   * based on the type of the right.
   * @param rightId the ID of the right, always required
   * @param programId the ID of the program, required for supervision rights
   * @param supervisoryNodeId the ID of the supervisory node,
   *                          required for supervision rights
   * @param warehouseId the ID of the warehouse, required for fulfillment rights
   * @return users with the right assigned, matching the criteria
   */
  @RequestMapping(value = "/users/rightSearch", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserDto> rightSearch(@RequestParam UUID rightId,
                                   @RequestParam(required = false) UUID programId,
                                   @RequestParam(required = false) UUID supervisoryNodeId,
                                   @RequestParam(required = false) UUID warehouseId) {
    Profiler profiler = new Profiler("GET_USERS_BY_RIGHT");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, profiler);

    profiler.start("USERS_BY_RIGHT_SEARCH");
    Set<User> users = userService.rightSearch(rightId, programId,
        supervisoryNodeId, warehouseId);

    profiler.stop().log();
    return exportUsersToDtos(users);
  }

  /**
   * Get the audit information related to users.
   *
   * @param author  The author of the changes which should be returned. If null or empty, changes
   *               are returned regardless of author.
   * @param changedPropertyName  The name of the property about which changes should be returned.
   *                             If null or empty, changes associated with any and all properties
   *                             are returned.
   * @param page  A Pageable object that allows client to optionally add "page" (page number) and
   *              "size" (page size) query parameters to the request.
   * @return the list of all matching audit logs as string
   */
  @RequestMapping(value = "/users/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getUsersAuditLog(
      @PathVariable("id") UUID userId,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    Profiler profiler = new Profiler("GET_USER_AUDIT_LOG");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, profiler);
    checkUserExists(userId, profiler);

    profiler.start("GET_AUDIT_LOG");
    ResponseEntity<String> responseEntity = getAuditLogResponse(User.class,
        userId,
        author,
        changedPropertyName,
        page,
        returnJson);

    profiler.stop().log();
    return responseEntity;
  }
  
  /**
   * Get permissions (in string format) of the specified user.
   *
   * @param userId UUID of the user to retrieve.
   * @return a set of user permission strings.
   */
  @RequestMapping(value = "/users/{id}/permissionStrings", method = RequestMethod.GET)
  public ResponseEntity<Set<String>> getUserPermissionStrings(@PathVariable("id") UUID userId) {
    XLOGGER.entry(userId);
    Profiler profiler = new Profiler("GET_USER_PERM_STRINGS");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);
    checkUserExists(userId, profiler);

    profiler.start("GET_PERM_STRINGS_FROM_RIGHT_ASSIGNMENTS");
    Set<String> permissionStrings = rightAssignmentRepository.findByUser(userId);

    profiler.stop().log();
    XLOGGER.exit(permissionStrings);
    return ResponseEntity
        .ok()
        .eTag(Integer.toString(permissionStrings.hashCode()))
        .body(permissionStrings);
  }

  /**
   * Get all the facilities that the user has supervision rights (home facility and supervised 
   * facilities).
   *
   * @param userId id of user to get supervised facilities
   * @return a set of facilities
   */
  @RequestMapping(value = "/users/{userId}/facilities", method = RequestMethod.GET)
  public ResponseEntity<Set<NamedResource>> getUserFacilities(
      @PathVariable(USER_ID) UUID userId) {
    XLOGGER.entry(userId);
    Profiler profiler = new Profiler("GET_USER_FACILITIES");
    profiler.setLogger(LOGGER);

    checkAdminRight(RightName.USERS_MANAGE_RIGHT, true, userId, profiler);

    if (!userRepository.exists(userId)) {
      throw new NotFoundException(UserMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("GET_SUPERVISION_FACILITIES_BY_USER");
    Set<NamedResource> userFacilityDtos = facilityRepository
        .findSupervisionFacilitiesByUser(userId);

    profiler.stop().log();
    XLOGGER.exit(userFacilityDtos);
    return ResponseEntity
        .ok()
        .eTag(Integer.toString(userFacilityDtos.hashCode()))
        .body(userFacilityDtos);
  }

  private User validateUser(UUID userId) {
    User user = userRepository.findOne(userId);
    if (user == null) {
      throw new NotFoundException(new Message(UserMessageKeys.ERROR_NOT_FOUND_WITH_ID, userId));
    }

    return user;
  }

  // finds a given entity by id, wrapping any null in an Optional
  private Optional<Identifiable> validateId(
      UUID id,
      CrudRepository<? extends Identifiable, UUID> repository) {

    Identifiable identifiable = repository.findOne(id);
    return (null != identifiable) ? Optional.of(identifiable) : Optional.empty();
  }

  private void assignRolesToUser(Set<RoleAssignmentDto> roleAssignmentDtos, User user) {
    LOGGER.debug("Assigning roles to user and saving");
    for (RoleAssignmentDto roleAssignmentDto : roleAssignmentDtos) {
      RoleAssignment roleAssignment;

      Role role = roleRepository.findOne(roleAssignmentDto.getRoleId());

      if (role.getRights().isEmpty()) {
        throw new ValidationMessageException(new Message(
            UserMessageKeys.ERROR_ASSIGNED_ROLE_RIGHTS_EMPTY, role.getName()));
      }

      UUID programId = roleAssignmentDto.getProgramId();
      UUID warehouseId = roleAssignmentDto.getWarehouseId();
      if (programId != null) {

        Program program = programRepository.findOne(programId);
        UUID supervisoryNodeId = roleAssignmentDto.getSupervisoryNodeId();
        if (supervisoryNodeId != null) {

          SupervisoryNode supervisoryNode = supervisoryNodeRepository.findOne(
              supervisoryNodeId);
          roleAssignment = new SupervisionRoleAssignment(role, user, program, supervisoryNode);

        } else {
          roleAssignment = new SupervisionRoleAssignment(role, user, program);
        }

      } else if (warehouseId != null) {

        Facility warehouse = facilityRepository.findOne(warehouseId);
        roleAssignment = new FulfillmentRoleAssignment(role, user, warehouse);

      } else {
        roleAssignment = new DirectRoleAssignment(role, user);
      }

      user.assignRoles(roleAssignment);
    }
  }

  private Set<DetailedRoleAssignmentDto> exportRoleAssignmentsToDtos(
      Set<RoleAssignment> roleAssignments) {
    Profiler profiler = new Profiler("EXPORT_USER_ROLE_ASSIGNMENTS_TO_DTO");
    profiler.setLogger(LOGGER);

    Set<DetailedRoleAssignmentDto> assignmentDtos = new HashSet<>();

    profiler.start("FOR_EACH_ROLE_ASSIGNMENT");
    for (RoleAssignment assignment : roleAssignments) {
      DetailedRoleAssignmentDto assignmentDto = new DetailedRoleAssignmentDto();

      if (assignment instanceof SupervisionRoleAssignment) {
        profiler.start("EXPORT_SUPERVISION_ASSIGNMENT");
        ((SupervisionRoleAssignment) assignment).export(assignmentDto);
      } else if (assignment instanceof FulfillmentRoleAssignment) {
        profiler.start("EXPORT_FULFILLMENT_ASSIGNMENT");
        ((FulfillmentRoleAssignment) assignment).export(assignmentDto);
      } else {
        profiler.start("EXPORT_DIRECT_ASSIGNMENT");
        ((DirectRoleAssignment) assignment).export(assignmentDto);
      }

      profiler.start("ADD_DTO_ASSIGNMENT_TO_LIST");
      assignmentDtos.add(assignmentDto);
    }

    profiler.stop().log();

    return assignmentDtos;
  }

  private UserDto exportUserToDto(User user) {
    UserDto userDto = new UserDto();
    user.export(userDto);
    return userDto;
  }

  private List<UserDto> exportUsersToDtos(Collection<User> users) {
    return users.stream().map(this::exportUserToDto).collect(toList());
  }

  private Page<UserDto> exportUsersToDtos(Page<User> users, Pageable pageable) {
    List<UserDto> userDtos = users.getContent().stream()
        .map(this::exportUserToDto)
        .collect(Collectors.toList());
    return Pagination.getPage(userDtos, pageable, users.getTotalElements());
  }
  
  private void addRoleAssignmentIdsToUserDto(UserDto userDto) {
    Set<RoleAssignmentDto> roleAssignmentDtos = 
        Optional
            .ofNullable(roleAssignmentRepository.findByUser(userDto.getId()))
            .orElse(Collections.emptySet());
    
    userDto.setRoleAssignments(roleAssignmentDtos);
  }

  private Set<FacilityDto> facilitiesToDto(Collection<Facility> facilities) {
    Set<FacilityDto> dtos = new HashSet<>();
    for (Facility facility : facilities) {
      FacilityDto dto = new FacilityDto();
      facility.export(dto);
      dtos.add(dto);
    }

    return dtos;
  }

  private Set<ProgramDto> programsToDto(Collection<Program> programs) {
    Set<ProgramDto> dtos = new HashSet<>();

    for (Program program : programs) {
      ProgramDto dto = ProgramDto.newInstance(program);
      dtos.add(dto);
    }

    return dtos;
  }

  private void checkUserExists(UUID userId, Profiler profiler) {
    profiler.start("CHECK_USER_EXISTS");
    if (!userRepository.exists(userId)) {
      throw new NotFoundException(new Message(UserMessageKeys.ERROR_NOT_FOUND_WITH_ID, userId));
    }
  }
}
