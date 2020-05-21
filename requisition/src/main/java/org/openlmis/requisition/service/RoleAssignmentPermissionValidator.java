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

package org.openlmis.requisition.service;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class RoleAssignmentPermissionValidator extends BasePermissionValidator {

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private RoleReferenceDataService roleReferenceDataService;

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Autowired
  private RequisitionGroupReferenceDataService requisitionGroupReferenceDataService;

  @Override
  boolean checkUserToken(PermissionValidationDetails details, Profiler profiler) {
    profiler.start("GET_CURRENT_USER");
    UserDto user = authenticationHelper.getCurrentUser();

    profiler.start("GET_RIGHT");
    RightDto right = authenticationHelper.getRight(details.getRightName());

    profiler.start("GET_ROLES_FOR_RIGHT");
    List<RoleDto> roles = roleReferenceDataService.search(right.getId());

    Set<UUID> supervisoryNodeIds = getMatchingSupervisoryNodeIds(details, user, profiler);

    profiler.start("CHECK_HAS_ROLE");
    for (int i = 0, length = roles.size(); i < length; ++i) {
      RoleDto role = roles.get(i);

      for (UUID supervisoryNodeId : supervisoryNodeIds) {
        if (user.hasMatchingSupervisorySupervisionRole(role.getId(),
            details.getProgramId(), supervisoryNodeId)) {
          return true;
        }
      }

      if (!details.containsPartnerRequisition()
          && user.hasMatchingHomeFacilitySupervisionRole(role.getId(),
          details.getProgramId(), details.getFacilityId())) {
        return true;
      }
    }

    return false;
  }

  private Set<UUID> getMatchingSupervisoryNodeIds(PermissionValidationDetails details,
      UserDto user, Profiler profiler) {
    Set<UUID> supervisoryNodeIds;

    if (Objects.isNull(details.getSupervisoryNodeId())) {
      profiler.start("GET_SUPERVISORY_NODE_IDS_FROM_USER_ROLE_ASSIGNMENTS");
      Set<UUID> userSupervisoryNodeIds = user
          .getRoleAssignments()
          .stream()
          .map(RoleAssignmentDto::getSupervisoryNodeId)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());

      profiler.start("GET_SUPERVISORY_NODES");
      List<SupervisoryNodeDto> supervisoryNodes = supervisoryNodeReferenceDataService
          .findByIds(userSupervisoryNodeIds)
          .stream()
          .filter(item -> Objects.nonNull(item.getRequisitionGroupId()))
          .collect(Collectors.toList());

      profiler.start("GET_REQUISITION_GROUP");
      Map<UUID, RequisitionGroupDto> requisitionGroups = requisitionGroupReferenceDataService
          .findAll()
          .stream()
          .collect(Collectors.toMap(RequisitionGroupDto::getId, Function.identity()));

      supervisoryNodeIds = Sets.newHashSet();

      for (int i = 0, length = supervisoryNodes.size(); i < length; ++i) {
        SupervisoryNodeDto supervisoryNode = supervisoryNodes.get(i);
        RequisitionGroupDto requisitionGroup = requisitionGroups
            .get(supervisoryNode.getRequisitionGroupId());

        if (requisitionGroup.hasFacility(details.getFacilityId())) {
          supervisoryNodeIds.add(supervisoryNode.getId());
        }
      }

    } else {
      profiler.start("GET_SUPERVISORY_NODE_FROM_PARAM");
      supervisoryNodeIds = Collections.singleton(details.getSupervisoryNodeId());
    }

    return supervisoryNodeIds;
  }

}
