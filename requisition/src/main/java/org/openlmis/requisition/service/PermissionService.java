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

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_CANNOT_UPDATE_REQUISITION;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION_FOR_REQUISITION_UPDATE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_NOT_FOUND;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.PermissionStrings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@SuppressWarnings("PMD.TooManyMethods")
@Service
public class PermissionService {

  private static final String REQUISITION_BASE = "REQUISITION_";

  public static final String REQUISITION_CREATE = REQUISITION_BASE + "CREATE";
  public static final String REQUISITION_APPROVE = REQUISITION_BASE + "APPROVE";
  public static final String REQUISITION_AUTHORIZE = REQUISITION_BASE + "AUTHORIZE";
  public static final String REQUISITION_DELETE = REQUISITION_BASE + "DELETE";
  public static final String REQUISITION_VIEW = REQUISITION_BASE + "VIEW";
  static final String REQUISITION_TEMPLATES_MANAGE = REQUISITION_BASE + "TEMPLATES_MANAGE";

  static final String REPORT_TEMPLATES_EDIT = "REPORT_TEMPLATES_EDIT";
  static final String REPORTS_VIEW = "REPORTS_VIEW";

  public static final String ORDERS_EDIT = "ORDERS_EDIT";

  @Autowired
  private RightAssignmentPermissionValidator rightAssignmentPermissionValidator;

  @Autowired
  private RoleAssignmentPermissionValidator roleAssignmentPermissionValidator;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private PermissionStrings permissionStrings;

  /**
   * Checks if current user has permission to initiate a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canInitRequisition(UUID program, UUID facility) {
    return checkRight(REQUISITION_CREATE, facility, program);
  }

  /**
   * Checks if current user has permission to initiate or authorize a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canInitOrAuthorizeRequisition(UUID program, UUID facility) {
    ValidationResult result = checkRight(REQUISITION_CREATE, facility, program);

    if (result.isSuccess()) {
      return result;
    }

    result = checkRight(REQUISITION_AUTHORIZE, facility, program);

    if (result.isSuccess()) {
      return result;
    }

    return ValidationResult.noPermission(
        ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_CREATE, REQUISITION_AUTHORIZE);
  }

  /**
   * Checks if current user has permission to update a requisition. Permissions needed to perform
   * update action depend on the requisition status.
   *
   * @param requisition the requisition.
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canUpdateRequisition(Requisition requisition) {
    if (requisition != null) {
      switch (requisition.getStatus()) {
        case INITIATED:
        case REJECTED:
          return checkPermissionOnUpdate(REQUISITION_CREATE, requisition);
        case SUBMITTED:
          return checkPermissionOnUpdate(REQUISITION_AUTHORIZE, requisition);
        case AUTHORIZED:
        case IN_APPROVAL:
          return checkPermissionOnUpdate(REQUISITION_APPROVE, requisition);
        default:
          return ValidationResult.failedValidation(ERROR_CANNOT_UPDATE_REQUISITION);
      }
    }

    return ValidationResult.success();
  }

  /**
   * Checks if current user has permission to submit a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canSubmitRequisition(Requisition requisition) {
    return checkRight(REQUISITION_CREATE, requisition.getFacilityId(), requisition.getProgramId());
  }

  /**
   * Checks if current user has permission to approve a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canApproveRequisition(Requisition requisition) {
    // we can't be sure that a user will have correct rights based on permission strings
    // that is why we need to verify if the user has a correct right by checking user's
    // roles
    return checkRightOrRole(REQUISITION_APPROVE, requisition);
  }

  /**
   * Checks if current user has permission to authorize a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canAuthorizeRequisition(Requisition requisition) {
    return checkRight(REQUISITION_AUTHORIZE,
        requisition.getFacilityId(), requisition.getProgramId());
  }

  /**
   * Checks if current user has permission to delete a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canDeleteRequisition(Requisition requisition) {
    ValidationResult result = checkRight(REQUISITION_DELETE,
            requisition.getFacilityId(), requisition.getProgramId());

    if (result.hasErrors()) {
      return result;
    }

    if (requisition.getStatus().isSubmittable() || requisition.getStatus().isSkipped()) {
      return checkRight(REQUISITION_CREATE,
              requisition.getFacilityId(), requisition.getProgramId());
    }

    if (requisition.getStatus().equals(RequisitionStatus.SUBMITTED)) {
      return checkRight(REQUISITION_AUTHORIZE,
          requisition.getFacilityId(), requisition.getProgramId());
    }

    return ValidationResult.success();
  }

  /**
   * Checks if current user has permission to view a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canViewRequisition(UUID requisitionId) {
    return canViewRequisition(requisitionRepository.findOne(requisitionId));
  }

  /**
   * Checks if current user has permission to view a requisition.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canViewRequisition(Requisition requisition) {
    // we can't be sure that a user will have correct rights based on permission strings
    // that is why we need to verify if the user has a correct right by checking user's
    // roles
    return checkRightOrRole(REQUISITION_VIEW, requisition);
  }

  /**
   * Checks if current user has permission to convert requisition to order.
   *
   * @param list of ReleasableRequisitionDto containing requisitionId and supplyingDepotId.
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canConvertToOrder(List<ReleasableRequisitionDto> list) {
    Set<UUID> requisitionIds = list
        .stream()
        .map(ReleasableRequisitionDto::getRequisitionId)
        .collect(Collectors.toSet());

    Map<UUID, Requisition> requisitions = requisitionRepository
        .findAll(requisitionIds)
        .stream()
        .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

    for (ReleasableRequisitionDto convertToOrder : list) {
      Requisition requisition = requisitions.get(convertToOrder.getRequisitionId());

      if (requisition == null) {
        return ValidationResult
            .notFound(ERROR_REQUISITION_NOT_FOUND, convertToOrder.getRequisitionId());
      }

      ValidationResult validation = checkRight(ORDERS_EDIT, convertToOrder.getSupplyingDepotId());

      if (validation.hasErrors()) {
        return validation;
      }
    }

    return ValidationResult.success();
  }

  /**
   * Checks if current user has permission to manage a requisition template.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canManageRequisitionTemplate() {
    return checkRight(REQUISITION_TEMPLATES_MANAGE);
  }

  /**
   * Checks if current user has permission to edit a report template.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canEditReportTemplates() {
    return checkRight(REPORT_TEMPLATES_EDIT);
  }

  /**
   * Checks if current user has permission to view a report template.
   *
   * @return ValidationResult containing info about the result of this check
   */
  public ValidationResult canViewReports() {
    return checkRight(REPORTS_VIEW);
  }

  private ValidationResult checkPermissionOnUpdate(String rightName, Requisition requisition) {
    ValidationResult result = checkRightOrRole(rightName, requisition);

    if (result.isSuccess()) {
      return result;
    }

    RequisitionStatus status = requisition.getStatus();

    if (status.duringApproval()) {
      status = RequisitionStatus.AUTHORIZED;
    }

    return ValidationResult
        .noPermission(ERROR_NO_FOLLOWING_PERMISSION_FOR_REQUISITION_UPDATE,
            status.toString(), rightName);
  }

  private ValidationResult checkRight(String rightName) {
    return rightAssignmentPermissionValidator
        .hasPermission(new RightAssignmentPermissionValidationDetails(rightName));
  }

  private ValidationResult checkRight(String rightName, UUID facilityId, UUID programId) {
    return rightAssignmentPermissionValidator
        .hasPermission(new RightAssignmentPermissionValidationDetails(
            rightName, facilityId, programId));
  }

  private ValidationResult checkRight(String rightName, UUID warehouseId) {
    return rightAssignmentPermissionValidator
        .hasPermission(new RightAssignmentPermissionValidationDetails(rightName, warehouseId));
  }

  private ValidationResult checkRightOrRole(String rightName, Requisition requisition) {
    // we first check if a user has permission by right assignments because
    // checking if the user has a correct role assignment is slower
    ValidationResult rightCheck = checkRight(rightName,
        requisition.getFacilityId(), requisition.getProgramId());

    if (rightCheck.isSuccess()) {
      return rightCheck;
    }

    return roleAssignmentPermissionValidator
        .hasPermission(new RoleAssignmentPermissionValidationDetails(rightName, requisition));
  }

  /**
   * Get current user's permission strings.
   *
   * @return user's permission strings
   */
  public PermissionStrings.Handler getPermissionStrings(UUID userId) {
    return permissionStrings.forUser(userId);
  }

}
