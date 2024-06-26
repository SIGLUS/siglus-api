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

import com.alibaba.excel.ExcelWriter;

import java.io.IOException;
import java.util.Set;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;

public interface IRequisitionReportService {

  Set<String> supportedProgramCodes();

  String getTemplateFile() throws IOException;

  void generateReport(SiglusRequisitionDto requisition, ExcelWriter excelWriter);
}
