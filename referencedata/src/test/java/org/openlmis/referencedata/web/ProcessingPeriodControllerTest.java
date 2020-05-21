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

package org.openlmis.referencedata.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.repository.ProcessingScheduleRepository;
import org.openlmis.referencedata.service.RightService;
import org.openlmis.referencedata.testbuilder.ProcessingPeriodDataBuilder;
import org.openlmis.referencedata.testbuilder.ProcessingScheduleDataBuilder;
import org.openlmis.referencedata.validate.ProcessingPeriodValidator;
import org.springframework.validation.BindingResult;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class ProcessingPeriodControllerTest {

  @Mock
  private RightService rightService;

  @Mock
  private ProcessingPeriodValidator processingPeriodValidator;

  @Mock
  private ProcessingPeriodRepository periodRepository;

  @Mock
  private ProcessingScheduleRepository processingScheduleRepository;

  @Mock
  private RequisitionGroupProgramSchedule requisitionGroupProgramSchedule;

  @InjectMocks
  private ProcessingPeriodController controller = new ProcessingPeriodController();

  private ProcessingPeriodDto periodDto = new ProcessingPeriodDto();
  private ProcessingPeriod period = new ProcessingPeriodDataBuilder().build();
  private ProcessingSchedule schedule = new ProcessingScheduleDataBuilder().build();

  @Before
  public void setUp() {
    when(requisitionGroupProgramSchedule.getProcessingSchedule()).thenReturn(schedule);
    when(processingScheduleRepository.exists(schedule.getId())).thenReturn(true);
    period.setProcessingSchedule(schedule);
  }

  protected void mockUserHasRight(String rightName) {
    doNothing().when(rightService).checkAdminRight(eq(rightName), anyBoolean(), any(UUID.class));
    doNothing().when(rightService).checkAdminRight(rightName);

    doReturn(true).when(rightService).hasRight(rightName);
  }

  @Test
  public void shouldUpdateProcessingPeriod() {
    //when
    mockUserHasRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);
    BindingResult result = mock(BindingResult.class);
    when(result.hasErrors()).thenReturn(false);
    when(periodRepository.save(any(ProcessingPeriod.class)))
        .thenAnswer(invocation -> invocation.getArguments()[0]);

    periodDto = new ProcessingPeriodDto();
    period.export(periodDto);
    ProcessingPeriod periodToUpdate = ProcessingPeriod.newPeriod(periodDto);

    //given
    ProcessingPeriodDto updatedPeriodDto = controller
        .updateProcessingPeriod(periodDto, periodDto.getId(), result);

    //then
    verify(periodRepository).save(periodToUpdate);
    assertThat(updatedPeriodDto).isEqualTo(periodDto);
  }
}
