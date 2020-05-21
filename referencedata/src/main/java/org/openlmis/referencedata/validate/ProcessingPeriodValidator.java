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

package org.openlmis.referencedata.validate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.util.messagekeys.ProcessingPeriodMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ValidationMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;

public class ProcessingPeriodValidator implements BaseValidator {
  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";
  private static final String PROCESSING_SCHEDULE = "processingSchedule";
  private static final String NAME = "name";

  @Autowired
  private ProcessingPeriodRepository processingPeriodRepository;

  @Override
  public boolean supports(Class<?> clazz) {
    return ProcessingPeriod.class.equals(clazz);
  }

  @Override
  public void validate(Object obj, Errors err) {
    rejectIfEmptyOrWhitespace(err, NAME, ProcessingPeriodMessageKeys.ERROR_NAME_NULL);
    rejectIfEmpty(err, PROCESSING_SCHEDULE, ProcessingPeriodMessageKeys.ERROR_SCHEDULE_NULL);
    rejectIfEmpty(err, START_DATE, ProcessingPeriodMessageKeys.ERROR_START_DATE_NULL);
    rejectIfEmpty(err, END_DATE, ProcessingPeriodMessageKeys.ERROR_END_DATE_NULL);
    if (!err.hasErrors()) {
      ProcessingPeriod period = (ProcessingPeriod) obj;
      UUID periodId = period.getId();
      ProcessingPeriod existingPeriod = (periodId != null)
          ? processingPeriodRepository.findOne(periodId) : null;
      if (existingPeriod != null) {
        rejectIfValueChanged(err, period.getProcessingSchedule(),
            existingPeriod.getProcessingSchedule(), PROCESSING_SCHEDULE);
        rejectIfValueChanged(err, period.getStartDate(),
            existingPeriod.getStartDate(), START_DATE);
        rejectIfValueChanged(err, period.getEndDate(),
            existingPeriod.getEndDate(), END_DATE);
        rejectIfValueChanged(err, period.getDurationInMonths(),
            existingPeriod.getDurationInMonths(), "durationInMonths");
      }
      List<ProcessingPeriod> periodList = processingPeriodRepository
          .findByProcessingSchedule(period.getProcessingSchedule());

      LocalDate startDate = period.getStartDate();
      LocalDate endDate = period.getEndDate();

      if (endDate.isAfter(startDate)) {
        if (!periodList.isEmpty() && existingPeriod == null) {
          LocalDate lastEndDate = periodList.get(periodList.size() - 1).getEndDate();
          if (!startDate.equals(lastEndDate.plusDays(1))) {
            rejectValue(err, START_DATE,
                ProcessingPeriodMessageKeys.ERROR_GAP_BETWEEN_LAST_END_DATE_AND_START_DATE);
          }
        }
      } else {
        rejectValue(err, START_DATE, ProcessingPeriodMessageKeys.ERROR_START_DATE_AFTER_END_DATE);
        rejectValue(err, END_DATE, ProcessingPeriodMessageKeys.ERROR_END_DATE_BEFORE_START_DATE);
      }
    }
  }

  private void rejectIfValueChanged(Errors errors, Object value, Object savedValue, String field) {
    if (value != null && savedValue != null && !savedValue.equals(value)) {
      rejectValue(errors, field, ValidationMessageKeys.ERROR_IS_INVARIANT, field);
    }
  }
}