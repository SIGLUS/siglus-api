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

package org.siglus.siglusapi.util;

import static org.siglus.siglusapi.constant.FieldConstants.SITE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.javers.common.collections.Sets;
import org.openlmis.requisition.dto.DetailedRoleAssignmentDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.siglus.siglusapi.domain.FacilitySuppierLevel;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.repository.FacilitySupplierLevelRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SiglusAuthenticationHelper {

  public static final String MIGRATE_DATA = "MIGRATE_DATA";
  private final SiglusUserReferenceDataService userService;
  private final FacilitySupplierLevelRepository facilitySupplierLevelRepository;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Value("${role.admin.id}")
  private String roleAdminId;
  @Value("${role.report.viewer.id}")
  private String roleReportViewerId;
  @Value("${role.role2.warehouse.manager}")
  private String role2WareHouseManager;
  @Value("${role.role2.warehouse.manager.ddmdpmonly}")
  private String role2WareHouseManagerDdmDpmOnly;
  @Value("${role.role2.warehouse.manager.ddmdpmonly.sn}")
  private String role2WareHouseManagerDdmDpmOnlySn;
  @Value("${role.role3.director}")
  private String role3Director;
  @Value("${role.role3.director.sn}")
  private String role3DirectorSn;

  public Optional<UUID> getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Set<String> trustedClients = Sets.asSet("trusted-client", "fc-client");
    if (principal == null || trustedClients.contains(principal.toString())) {
      return Optional.empty();
    }
    return Optional.of((UUID) principal);
  }

  public UserDto getCurrentUser() {
    Optional<UUID> currentUserId = getCurrentUserId();
    if (Optional.empty().equals(currentUserId)) {
      return null;
    }
    return currentUserId.map(userService::findOne).orElseThrow(() -> authenticateFail(currentUserId.orElse(null)));
  }

  public Collection<DetailedRoleAssignmentDto> getUserRightsAndRoles() {
    Optional<UUID> currentUserId = getCurrentUserId();
    if (Optional.empty().equals(currentUserId)) {
      return new ArrayList<>();
    }
    return userService.getUserRightsAndRoles(getCurrentUserId().orElseThrow(() -> authenticateFail(null)));
  }

  public boolean isTheCurrentUserCanMergeOrDeleteSubDrafts() {
    Collection<DetailedRoleAssignmentDto> userRightsAndRoles = getUserRightsAndRoles();
    return userRightsAndRoles.stream()
        .anyMatch(
            e -> {
              String roleId = e.getRole().getId().toString();
              return Arrays.asList(
                      role2WareHouseManager,
                      role2WareHouseManagerDdmDpmOnly,
                      role2WareHouseManagerDdmDpmOnlySn,
                      role3Director,
                      role3DirectorSn)
                  .contains(roleId);
            });
  }

  public String getUserNameByUserId(UUID userId) {
    if (userId == null) {
      return "";
    }
    return userService.getUserDetailDto(userId).getUsername();
  }

  public String getFacilityGeographicZoneLevel() {

    String typeCode = siglusFacilityReferenceDataService
        .findOne(getCurrentUser().getHomeFacilityId()).getType().getCode();
    Optional<FacilitySuppierLevel> facilityTypeCodeOptional = facilitySupplierLevelRepository.findByFacilityTypeCode(
        typeCode);
    if (facilityTypeCodeOptional.isPresent()) {
      return facilityTypeCodeOptional.get().getLevel();
    }
    return SITE;
  }

  public boolean isTheCurrentUserAdmin() {
    Set<UUID> roleAssignmentIds = getCurrentUser().getRoleAssignments().stream()
        .map(RoleAssignmentDto::getRoleId).collect(Collectors.toSet());
    return roleAssignmentIds.contains(UUID.fromString(roleAdminId));
  }

  public boolean isTheCurrentUserCanViewAllReports() {
    Set<UUID> roleAssignmentIds = getCurrentUser().getRoleAssignments().stream()
        .map(RoleAssignmentDto::getRoleId).collect(Collectors.toSet());
    return roleAssignmentIds.contains(UUID.fromString(roleReportViewerId));
  }

  public Collection<PermissionString> getCurrentUserPermissionStrings() {
    return userService
        .getPermissionStrings(getCurrentUserId().orElseThrow(() -> authenticateFail(null)))
        .stream()
        .map(PermissionString::new)
        .collect(Collectors.toList());
  }

  private AuthenticationException authenticateFail(UUID currentUserId) {
    return new AuthenticationException(new Message(ERROR_USER_NOT_FOUND, currentUserId));
  }

  public boolean isTheDataMigrationUser() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority(
        MIGRATE_DATA));
  }
}
