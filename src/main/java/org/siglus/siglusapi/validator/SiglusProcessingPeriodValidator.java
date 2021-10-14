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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUBMIT_END_DATE_BEFORE_SUBMIT_START_DATE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUBMIT_START_DATE_BEFORE_START_DATE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_SUBMIT_START_DATE_IS_BEFORE_LAST_SUBMIT_END_DATE;

import java.time.LocalDate;
import java.util.List;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.ProcessingScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("SiglusProcessingPeriodValidator")
public class SiglusProcessingPeriodValidator {

  @Autowired
  private ProcessingPeriodRepository processingPeriodRepository;

  @Autowired
  private ProcessingScheduleRepository processingScheduleRepository;

  @Autowired
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  public void validSubmitDuration(ProcessingPeriodDto periodDto) {
    LocalDate startDate = periodDto.getStartDate();
    LocalDate submitStartDate = periodDto.getSubmitStartDate();
    LocalDate submitEndDate = periodDto.getSubmitEndDate();

    List<ProcessingPeriod> periodList = processingPeriodRepository
        .findByProcessingSchedule(
            processingScheduleRepository.findOne(periodDto.getProcessingSchedule().getId())
        );

    if (submitStartDate.isBefore(startDate)) {
      throw new ValidationMessageException(ERROR_SUBMIT_START_DATE_BEFORE_START_DATE);
    }

    if (submitEndDate.isBefore(submitStartDate)) {
      throw new ValidationMessageException(ERROR_SUBMIT_END_DATE_BEFORE_SUBMIT_START_DATE);
    }

    if (!periodList.isEmpty()) {
      LocalDate lastSubmitEndDate = processingPeriodExtensionRepository
          .findByProcessingPeriodId(periodList.get(periodList.size() - 1).getId())
          .getSubmitEndDate();
      if (!submitStartDate.isAfter(lastSubmitEndDate)) {
        throw new ValidationMessageException(
            ERROR_SUBMIT_START_DATE_IS_BEFORE_LAST_SUBMIT_END_DATE
        );
      }
    }
  }

}
