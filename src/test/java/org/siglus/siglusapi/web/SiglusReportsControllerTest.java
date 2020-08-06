package org.siglus.siglusapi.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.exception.ContentNotFoundMessageException;
import org.openlmis.requisition.exception.JasperReportViewException;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.JasperReportsViewService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.web.ReportsController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SiglusReportsControllerTest {

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private PermissionService permissionService;

  @Mock
  private JasperReportsViewService jasperReportsViewService;

  @InjectMocks
  private ReportsController reportsController;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = ContentNotFoundMessageException.class)
  public void shouldNotPrintRequisitionIfTRequisitionDoesNotExist()
      throws JasperReportViewException {
    //given
    when(permissionService.canViewRequisition(any(UUID.class)))
        .thenReturn(ValidationResult.notFound("requisition.not.found"));
    // when
    reportsController.print(mock(HttpServletRequest.class), UUID.randomUUID());
  }

  @Test
  public void shouldPrintRequisition()
      throws JasperReportViewException {
    // given
    HttpServletRequest request = mock(HttpServletRequest.class);
    ModelAndView view = new ModelAndView();

    when(requisitionRepository.findOne(any(UUID.class))).thenReturn(mock(Requisition.class));
    when(jasperReportsViewService.getRequisitionJasperReportView(
        any(Requisition.class), any(HttpServletRequest.class))).thenReturn(view);
    when(permissionService.canViewRequisition(any(UUID.class)))
        .thenReturn(ValidationResult.success());

    // when
    ModelAndView result = reportsController.print(request, UUID.randomUUID());

    // then
    assertEquals(result, view);
  }
}
