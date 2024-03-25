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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.ReportAccessRecord;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.ReportNameEnum;
import org.siglus.siglusapi.repository.SiglusReportAccessRecordRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class SiglusReportAccessRecordServiceTest {

  @InjectMocks
  private SiglusReportAccessRecordService siglusReportAccessRecordService;

  @Mock
  private SiglusReportAccessRecordRepository siglusReportAccessRecordRepository;

  @Mock
  private SiglusAuthenticationHelper siglusAuthenticationHelper;

  @Before
  public void prepare() {
  }

  @Test
  public void shouldSaveRecordWhenUserFirstAccessReportInOneDay() {
    // given
    UUID userId = UUID.randomUUID();
    LocalDate now = LocalDate.now();
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    when(siglusAuthenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(siglusReportAccessRecordRepository.findByUserIdAndReportNameAndAccessDate(
        userId, "USER_ACCESS", now))
        .thenReturn(new ArrayList<>());

    // when
    siglusReportAccessRecordService.accessRecord(ReportNameEnum.USER_ACCESS);

    // then
    verify(siglusReportAccessRecordRepository, times(1)).save(any(ReportAccessRecord.class));
  }

  @Test
  public void shouldNotSaveRecordWhenUserNotFirstAccessReportInOneDay() {
    // given
    UUID userId = UUID.randomUUID();
    LocalDate now = LocalDate.now();
    ReportAccessRecord reportAccessRecord = ReportAccessRecord.builder()
        .userId(userId)
        .reportName(ReportNameEnum.USER_ACCESS)
        .accessDate(now)
        .build();
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    when(siglusAuthenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(siglusReportAccessRecordRepository.findByUserIdAndReportNameAndAccessDate(
        userId, "USER_ACCESS", now))
        .thenReturn(Collections.singletonList(reportAccessRecord));

    // when
    siglusReportAccessRecordService.accessRecord(ReportNameEnum.USER_ACCESS);

    // then
    verify(siglusReportAccessRecordRepository, times(0)).save(any(ReportAccessRecord.class));
  }
}
