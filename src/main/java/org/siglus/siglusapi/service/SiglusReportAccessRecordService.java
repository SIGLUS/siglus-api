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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.siglusapi.domain.ReportAccessRecord;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.ReportNameEnum;
import org.siglus.siglusapi.repository.SiglusReportAccessRecordRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@AllArgsConstructor
@NoArgsConstructor
public class SiglusReportAccessRecordService {

  @Autowired
  private SiglusReportAccessRecordRepository siglusReportAccessRecordRepository;
  @Autowired
  private SiglusAuthenticationHelper siglusAuthenticationHelper;


  public void accessRecord(ReportNameEnum reportName) {
    UserDto userDto = siglusAuthenticationHelper.getCurrentUser();
    if (ObjectUtils.isEmpty(userDto)) {
      return;
    }
    UUID userId = userDto.getId();
    LocalDate accessDate = LocalDate.now();
    List<ReportAccessRecord> accessRecords = siglusReportAccessRecordRepository.findByUserIdAndReportNameAndAccessDate(
        userId, reportName.getName(), accessDate);
    if (CollectionUtils.isEmpty(accessRecords)) {
      ReportAccessRecord reportAccessRecord = ReportAccessRecord.builder()
          .userId(userId)
          .reportName(reportName)
          .accessDate(accessDate)
          .build();
      siglusReportAccessRecordRepository.save(reportAccessRecord);
    }
  }
}
