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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.Role;
import org.openlmis.referencedata.dto.RightDto;
import org.openlmis.referencedata.dto.RoleDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.CountResource;
import org.openlmis.referencedata.repository.RightRepository;
import org.openlmis.referencedata.repository.RoleAssignmentRepository;
import org.openlmis.referencedata.repository.RoleRepository;
import org.openlmis.referencedata.service.RightAssignmentService;
import org.openlmis.referencedata.util.messagekeys.RoleMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
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
public class RoleController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RoleController.class);

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private RoleAssignmentRepository roleAssignmentRepository;

  @Autowired
  private RightRepository rightRepository;
  
  @Autowired
  private RightAssignmentService rightAssignmentService;

  /**
   * Get all roles in the system.
   *
   * @return all roles in the system.
   */
  @RequestMapping(value = "/roles", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Set<RoleDto> getAllRoles(@RequestParam MultiValueMap<String, Object> queryParams) {
    Profiler profiler = new Profiler("GET_ALL_ROLES");
    profiler.setLogger(LOGGER);

    profiler.start("CONVERT_TO_PARAMS");
    RoleSearchParams params = new RoleSearchParams(queryParams);

    profiler.start("GET_ALL_ROLES_REPOSITORY");
    Set<Role> roles = Sets.newHashSet(roleRepository.search(params));

    profiler.start("COUNT_ASSIGNED_USERS_TO_ROLES");
    List<CountResource> usersCount = roleAssignmentRepository.countUsersAssignedToRoles();

    profiler.start("TO_DTO");
    Set<RoleDto> dtos = exportToDto(roles, usersCount.stream()
        .collect(toMap(CountResource::getId, CountResource::getCount)));

    profiler.stop().log();
    return dtos;
  }

  /**
   * Get specified role in the system.
   *
   * @param roleId id of the role to get.
   * @return the specified role.
   */
  @RequestMapping(value = "/roles/{roleId}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RoleDto getRole(@PathVariable("roleId") UUID roleId) {

    LOGGER.debug("Getting role");
    Role role = roleRepository.findOne(roleId);
    if (role == null) {
      throw new NotFoundException(RoleMessageKeys.ERROR_NOT_FOUND);
    }

    return exportToDto(role);
  }

  /**
   * Get the audit information related to role.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/roles/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getRoleAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    rightService.checkAdminRight(RightName.USER_ROLES_MANAGE_RIGHT);

    //Return a 404 if the specified instance can't be found
    Role instance = roleRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(RoleMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(Role.class, id, author, changedPropertyName, page, returnJson);
  }

  /**
   * Create a new role using the provided role DTO.
   *
   * @param roleDto a role DTO with which to create the role.
   * @return the new role.
   */
  @RequestMapping(value = "/roles", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public RoleDto createRole(@RequestBody RoleDto roleDto) {
    rightService.checkAdminRight(RightName.USER_ROLES_MANAGE_RIGHT, false);

    populateRights(roleDto);
    Role newRole = Role.newRole(roleDto);
    roleRepository.save(newRole);

    LOGGER.info("Saved new role with id: {}", newRole.getId());

    return exportToDto(newRole);
  }

  /**
   * Update an existing role using the provided role DTO. Note, if the role does not exist, will
   * create one.
   *
   * @param roleId  id of the role to update.
   * @param roleDto provided role DTO.
   * @return the updated role.
   */
  @RequestMapping(value = "/roles/{roleId}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RoleDto updateRole(@PathVariable("roleId") UUID roleId,
                            @RequestBody RoleDto roleDto) {

    Profiler profiler = new Profiler("UPDATE_ROLE");
    profiler.setLogger(LOGGER);

    profiler.start("CHECK_ADMIN");
    rightService.checkAdminRight(RightName.USER_ROLES_MANAGE_RIGHT, false);

    LOGGER.info("Saving role using id: {}", roleId);

    profiler.start("POPULATE_RIGHTS");
    populateRights(roleDto);

    profiler.start("IMPORT_ROLE_FROM_DTO");
    Role roleToSave = Role.newRole(roleDto);
    roleToSave.setId(roleId);

    profiler.start("SAVE_ROLE");
    roleRepository.saveAndFlush(roleToSave);

    profiler.start("REGENERATE_RIGHT_ASSIGNMENTS");
    rightAssignmentService.regenerateRightAssignments();
    
    LOGGER.info("Saved role with id: {}", roleToSave.getId());

    profiler.start("EXPORT_ROLE_TO_DTO");
    RoleDto dto = exportToDto(roleToSave);

    profiler.stop().log();
    return dto;
  }

  /**
   * Delete an existing role.
   *
   * @param roleId id of the role to delete.
   */
  @RequestMapping(value = "/roles/{roleId}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRole(@PathVariable("roleId") UUID roleId) {
    rightService.checkAdminRight(RightName.USER_ROLES_MANAGE_RIGHT, false);

    Role storedRole = roleRepository.findOne(roleId);
    if (storedRole == null) {
      throw new NotFoundException(RoleMessageKeys.ERROR_NOT_FOUND);
    }

    LOGGER.debug("Deleting role");
    roleRepository.delete(roleId);
  }

  private RoleDto exportToDto(Role role) {
    RoleDto roleDto = new RoleDto();
    role.export(roleDto);
    return roleDto;
  }

  private Set<RoleDto> exportToDto(Set<Role> roles, Map<UUID, Long> usersCount) {
    Set<RoleDto> dtos = roles.stream().map(this::exportToDto).collect(toSet());
    dtos.forEach(role -> role.setCount(usersCount.getOrDefault(role.getId(), 0L)));
    return dtos;
  }

  private void populateRights(RoleDto roleDto) {
    Set<Right.Importer> rightDtos = roleDto.getRights();
    for (Right.Importer rightDto : rightDtos) {
      Right storedRight = rightRepository.findFirstByName(rightDto.getName());
      storedRight.export((RightDto) rightDto);
    }
  }
}
