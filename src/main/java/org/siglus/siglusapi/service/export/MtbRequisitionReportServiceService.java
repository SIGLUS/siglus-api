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

package org.siglus.siglusapi.service.export;

import static com.google.common.collect.Lists.newArrayList;

import com.alibaba.excel.ExcelWriter;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MtbRequisitionReportServiceService implements IRequisitionReportService {
  private final Set<String> supportedProgramSet = new HashSet<>(newArrayList(ProgramConstants.MTB_PROGRAM_CODE));

  @Override
  public Set<String> supportedProgramCodes() {
    return supportedProgramSet;
  }

  @Override
  public InputStream getTemplateFile() {
    return getClass().getResourceAsStream("/static/requisition/MTB_pt.xlsx");
  }

  @Override
  public void generateReport(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {

  }
}
