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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.service.export.IRequisitionReportService;
import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class SiglusRequisitionExportServiceTest {

  @InjectMocks
  private SiglusRequisitionExportService exportService;

  @Mock
  private SiglusRequisitionService siglusRequisitionService;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private List<IRequisitionReportService> reportServices;

  @Mock
  private IRequisitionReportService mtbRequisitionReportServiceService;

  @Before
  public void setUp() throws IOException {
    when(mtbRequisitionReportServiceService.supportedProgramCodes())
        .thenReturn(new HashSet<>(newArrayList(ProgramConstants.MTB_PROGRAM_CODE)));
    InputStream inputStream = SiglusRequisitionExportServiceTest.class
        .getResourceAsStream("src/main/resources/static/requisition/MTB_pt.xlsx");
    when(mtbRequisitionReportServiceService.getTemplateFile())
        .thenReturn(inputStream);
    doNothing().when(mtbRequisitionReportServiceService).generateReport(any(), any());
    when(reportServices.stream()).thenReturn(Stream.of(mtbRequisitionReportServiceService));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenRequisitionStatusDoesNotSupport() throws IOException {
    UUID requisitionId = UUID.randomUUID();
    SiglusRequisitionDto dto = new SiglusRequisitionDto();
    dto.setId(requisitionId);
    dto.setStatus(RequisitionStatus.SUBMITTED);
    when(siglusRequisitionService.searchRequisition(requisitionId)).thenReturn(dto);
    MockHttpServletResponse response = new MockHttpServletResponse();

    exportService.exportExcel(requisitionId, response);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenRequisitionTypeDoesNotSupport() throws IOException {
    UUID requisitionId = UUID.randomUUID();
    SiglusRequisitionDto dto = new SiglusRequisitionDto();
    dto.setId(requisitionId);
    dto.setStatus(RequisitionStatus.APPROVED);
    when(siglusRequisitionService.searchRequisition(requisitionId)).thenReturn(dto);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(ProgramConstants.MALARIA_PROGRAM_CODE);
    when(siglusProgramService.getProgram(any())).thenReturn(programDto);
    MockHttpServletResponse response = new MockHttpServletResponse();

    exportService.exportExcel(requisitionId, response);
  }

  @Test
  public void shouldGenerateExcelSuccess() throws IOException {
    UUID requisitionId = UUID.randomUUID();
    SiglusRequisitionDto dto = new SiglusRequisitionDto();
    dto.setId(requisitionId);
    dto.setStatus(RequisitionStatus.APPROVED);
    dto.setRequisitionNumber("123456");
    when(siglusRequisitionService.searchRequisition(requisitionId)).thenReturn(dto);
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(ProgramConstants.MTB_PROGRAM_CODE);
    when(siglusProgramService.getProgram(any())).thenReturn(programDto);
    MockHttpServletResponse response = new MockHttpServletResponse();

    exportService.exportExcel(requisitionId, response);

    verify(mtbRequisitionReportServiceService).generateReport(any(), any());
  }
}
