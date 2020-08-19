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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
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
import org.siglus.siglusapi.util.OperatePermissionService;
import org.springframework.web.servlet.ModelAndView;

@RunWith(MockitoJUnitRunner.class)
public class SiglusReportsControllerTest {

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private PermissionService permissionService;

  @Mock
  private JasperReportsViewService jasperReportsViewService;

  @Mock
  private OperatePermissionService operatePermissionService;

  @InjectMocks
  private SiglusReportsController siglusReportsController;

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
    siglusReportsController.print(mock(HttpServletRequest.class), UUID.randomUUID());
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
    when(operatePermissionService.isEditableRequisition(any()))
        .thenReturn(false);

    // when
    ModelAndView result = siglusReportsController.print(request, UUID.randomUUID());

    // then
    assertEquals(result, view);
  }
}
