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

import static org.siglus.siglusapi.constant.FieldConstants.ATTACHMENT_FILENAME;
import static org.siglus.siglusapi.constant.FieldConstants.EXCEL_CONTENT_TYPE;
import static org.siglus.siglusapi.constant.FieldConstants.UTF_8;
import static org.siglus.siglusapi.constant.FieldConstants.XLSX_SUFFIX;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.service.export.IRequisitionReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SiglusRequisitionExportService {

  @Autowired
  private SiglusRequisitionService siglusRequisitionService;
  @Autowired
  private SiglusProgramService siglusProgramService;

  @Autowired
  private List<IRequisitionReportService> reportServices;

  private final Set<RequisitionStatus> supportedStatus = new HashSet<>(Arrays.asList(
      RequisitionStatus.APPROVED,
      RequisitionStatus.RELEASED,
      RequisitionStatus.RELEASED_WITHOUT_ORDER));

  public void exportExcel(UUID requisitionId, HttpServletResponse response) throws IOException {
    SiglusRequisitionDto requisition = siglusRequisitionService.searchRequisition(requisitionId);
    if (!supportedStatus.contains(requisition.getStatus())) {
      throw new IllegalArgumentException("unsupported requisition status");
    }
    ProgramDto program = siglusProgramService.getProgram(requisition.getProgramId());
    IRequisitionReportService reportService = getReportServiceByProgramCode(program.getCode());
    updateResponseHeader(response, requisition);

    try (ExcelWriter excelWriter = EasyExcelFactory
        .write(response.getOutputStream())
        .withTemplate(reportService.getTemplateFile())
        .build()) {
      reportService.generateReport(requisition, excelWriter);
      excelWriter.finish();
    } catch (IOException e) {
      log.error("generate excel error with requisition. " + requisitionId);
      throw e;
    }
  }

  private void updateResponseHeader(HttpServletResponse response, SiglusRequisitionDto requisition)
      throws UnsupportedEncodingException {
    response.setContentType(EXCEL_CONTENT_TYPE);
    response.setCharacterEncoding(UTF_8);
    String fileName = URLEncoder.encode(requisition.getRequisitionNumber(), UTF_8);
    response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + fileName + XLSX_SUFFIX);
  }

  private IRequisitionReportService getReportServiceByProgramCode(String programCode) {
    Optional<IRequisitionReportService> reportService = reportServices.stream()
        .filter(service -> service.supportedProgramCodes().contains(programCode))
        .findFirst();
    if (reportService.isPresent()) {
      return reportService.get();
    }
    throw new IllegalArgumentException("unsupported program: " + programCode);
  }
}
