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

package org.siglus.siglusapi.web;

import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.JasperReportViewException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.JasperReportsViewService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.siglus.siglusapi.domain.RequisitionDraft;
import org.siglus.siglusapi.domain.RequisitionLineItemDraft;
import org.siglus.siglusapi.repository.RequisitionDraftRepository;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/api/siglusapi")
public class SiglusReportsController {
  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private JasperReportsViewService jasperReportsViewService;

  @Autowired
  private OperatePermissionService operatePermissionService;

  @Autowired
  private RequisitionAuthenticationHelper authenticationHelper;

  @Autowired
  private RequisitionDraftRepository draftRepository;

  @GetMapping("/requisitions/{id}/print")
  public ModelAndView print(HttpServletRequest request, @PathVariable("id") UUID id)
      throws JasperReportViewException {
    permissionService.canViewRequisition(id).throwExceptionIfHasErrors();

    Requisition requisition = requisitionRepository.findOne(id);
    if (requisition == null) {
      throw new ContentNotFoundMessageException(
          new Message(MessageKeys.ERROR_REQUISITION_NOT_FOUND, id));
    }

    if (operatePermissionService.isEditableRequisition(requisition)) {
      UserDto user = authenticationHelper.getCurrentUser();
      RequisitionDraft draft = draftRepository
          .findRequisitionDraftByRequisitionIdAndFacilityId(requisition.getId(),
              user.getHomeFacilityId());
      if (draft != null) {
        requisition.setRequisitionLineItems(
            draft.getLineItems().stream()
                .map(RequisitionLineItemDraft::getLineItem).collect(Collectors.toList()));
      }
    }

    return jasperReportsViewService.getRequisitionJasperReportView(requisition, request);
  }
}
