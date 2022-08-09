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

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.Set;
import java.util.UUID;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OperatePermissionService {

  @Autowired
  PermissionService permissionService;

  @Autowired
  org.openlmis.stockmanagement.service.PermissionService stockManagementService;

  @Autowired
  private SupportedProgramsHelper supportedProgramsHelper;

  public boolean isEditable(RequisitionV2Dto dto) {
    Requisition requisition = from(dto);
    return isEditableRequisition(requisition);
  }

  public boolean isEditableRequisition(Requisition requisition) {
    return canSubmit(requisition) || canAuthorize(requisition) || canApprove(requisition);
  }

  public boolean canSubmit(RequisitionV2Dto dto) {
    Requisition requisition = from(dto);
    return canSubmit(requisition);
  }

  private boolean canSubmit(Requisition requisition) {
    if (requisition.getStatus().isSubmittable()) {
      return permissionService.canSubmitRequisition(requisition).isSuccess();
    }
    return false;
  }

  public void checkPermission(UUID facility) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    if (CollectionUtils.isEmpty(supportedPrograms)) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED,
              ALL_PRODUCTS_PROGRAM_ID));
    }
    supportedPrograms.forEach(supportedProgram -> stockManagementService.canAdjustStock(supportedProgram, facility));
  }

  private boolean canAuthorize(Requisition requisition) {
    if (requisition.getStatus().isPreAuthorize() && requisition.getStatus().isPostSubmitted()) {
      return permissionService.canAuthorizeRequisition(requisition).isSuccess();
    }
    return false;
  }

  private boolean canApprove(Requisition requisition) {
    if (requisition.getStatus().duringApproval()) {
      return permissionService.canApproveRequisition(requisition).isSuccess();
    }
    return false;
  }

  private Requisition from(RequisitionV2Dto dto) {
    Requisition requisition = new Requisition();
    requisition.setId(dto.getId());
    requisition.setProgramId(dto.getProgramId());
    requisition.setFacilityId(dto.getFacilityId());
    requisition.setStatus(dto.getStatus());
    requisition.setSupervisoryNodeId(dto.getSupervisoryNode());
    return requisition;
  }
}
