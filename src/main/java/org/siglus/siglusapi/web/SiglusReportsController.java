package org.siglus.siglusapi.web;

import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.JasperReportViewException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.JasperReportsViewService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.openlmis.requisition.utils.Message;
import org.siglus.siglusapi.domain.RequisitionDraft;
import org.siglus.siglusapi.domain.RequisitionLineItemDraft;
import org.siglus.siglusapi.repository.RequisitionDraftRepository;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.stream.Collectors;

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

  /**
   * Print out requisition as a PDF file.
   *
   * @param id The UUID of the requisition to print
   * @return ResponseEntity with the "#200 OK" HTTP response status and PDF file on success, or
   * ResponseEntity containing the error description status.
   */
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
