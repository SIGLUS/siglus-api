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

package org.siglus.siglusapi.validator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.domain.referencedata.ProcessingSchedule;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.ProcessingScheduleRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusProcessingPeriodValidatorTest {

  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;

  @Mock
  private ProcessingScheduleRepository processingScheduleRepository;

  @Mock
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @InjectMocks
  private SiglusProcessingPeriodValidator processingPeriodValidator;

  private List<ProcessingPeriod> periodList;

  private final ProcessingSchedule processingSchedule = new ProcessingSchedule();

  @Before
  public void setup() {
    processingSchedule.setId(UUID.randomUUID());
    LocalDate startDate1 = LocalDate.of(2020, 6, 1);
    LocalDate endDate1 = LocalDate.of(2020, 6, 30);
    LocalDate startDate2 = LocalDate.of(2020, 7, 1);
    LocalDate endDate2 = LocalDate.of(2020, 7, 31);
    LocalDate startDate3 = LocalDate.of(2020, 8, 1);
    LocalDate endDate3 = LocalDate.of(2020, 8, 31);
    periodList = Arrays.asList(
        ProcessingPeriod.newPeriod("period 1", processingSchedule, startDate1, endDate1),
        ProcessingPeriod.newPeriod("period 2", processingSchedule, startDate2, endDate2),
        ProcessingPeriod.newPeriod("period 3", processingSchedule, startDate3, endDate3)
    );
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenSubmitStartDateBeforeStartDate() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setStartDate(LocalDate.of(2020, 6, 1));
    processingPeriodDto.setSubmitStartDate(LocalDate.of(2020, 5, 31));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    processingPeriodDto.setProcessingSchedule(processingScheduleDto);

    when(processingPeriodRepository.findByProcessingSchedule(any())).thenReturn(periodList);
    when(processingScheduleRepository.findOne(any(UUID.class))).thenReturn(processingSchedule);

    processingPeriodValidator.validSubmitDuration(processingPeriodDto);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenSubmitStartDateBeforeSubmitEndDate() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setStartDate(LocalDate.of(2020, 6, 1));
    processingPeriodDto.setSubmitStartDate(LocalDate.of(2020, 6, 25));
    processingPeriodDto.setSubmitEndDate(LocalDate.of(2020, 6, 24));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    processingPeriodDto.setProcessingSchedule(processingScheduleDto);

    when(processingPeriodRepository.findByProcessingSchedule(any())).thenReturn(periodList);
    when(processingScheduleRepository.findOne(any(UUID.class))).thenReturn(processingSchedule);

    processingPeriodValidator.validSubmitDuration(processingPeriodDto);
  }

  @Test
  public void shouldValidWhenPeriodListIsEmpty() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setStartDate(LocalDate.of(2020, 6, 1));
    processingPeriodDto.setSubmitStartDate(LocalDate.of(2020, 6, 25));
    processingPeriodDto.setSubmitEndDate(LocalDate.of(2020, 6, 30));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    processingPeriodDto.setProcessingSchedule(processingScheduleDto);

    when(processingPeriodRepository.findByProcessingSchedule(any()))
        .thenReturn(Collections.emptyList());
    when(processingScheduleRepository.findOne(any(UUID.class))).thenReturn(processingSchedule);

    processingPeriodValidator.validSubmitDuration(processingPeriodDto);
    verify(processingPeriodExtensionRepository, times(0)).findByProcessingPeriodId(any());
  }

  @Test
  public void shouldValidWhenSubmitStartDateAfterLastSubmitEndDate() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setStartDate(LocalDate.of(2020, 6, 1));
    processingPeriodDto.setSubmitStartDate(LocalDate.of(2020, 6, 25));
    processingPeriodDto.setSubmitEndDate(LocalDate.of(2020, 6, 30));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    processingPeriodDto.setProcessingSchedule(processingScheduleDto);
    ProcessingPeriodExtension processingPeriodExtension = new ProcessingPeriodExtension();
    processingPeriodExtension.setSubmitEndDate(LocalDate.of(2020, 6, 24));

    when(processingPeriodRepository.findByProcessingSchedule(any()))
        .thenReturn(periodList);
    when(processingScheduleRepository.findOne(any(UUID.class))).thenReturn(processingSchedule);
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(any(UUID.class)))
        .thenReturn(processingPeriodExtension);

    processingPeriodValidator.validSubmitDuration(processingPeriodDto);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(any());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenSubmitStartDateAfterLastSubmitEndDate() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setStartDate(LocalDate.of(2020, 6, 1));
    processingPeriodDto.setSubmitStartDate(LocalDate.of(2020, 6, 25));
    processingPeriodDto.setSubmitEndDate(LocalDate.of(2020, 6, 30));
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    processingPeriodDto.setProcessingSchedule(processingScheduleDto);
    ProcessingPeriodExtension processingPeriodExtension = new ProcessingPeriodExtension();
    processingPeriodExtension.setSubmitEndDate(LocalDate.of(2020, 6, 25));

    when(processingPeriodRepository.findByProcessingSchedule(any()))
        .thenReturn(periodList);
    when(processingScheduleRepository.findOne(any(UUID.class))).thenReturn(processingSchedule);
    when(processingPeriodExtensionRepository.findByProcessingPeriodId(any(UUID.class)))
        .thenReturn(processingPeriodExtension);

    processingPeriodValidator.validSubmitDuration(processingPeriodDto);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(any());
  }

}