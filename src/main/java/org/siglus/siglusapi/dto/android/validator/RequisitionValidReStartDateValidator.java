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

package org.siglus.siglusapi.dto.android.validator;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidReStartDate;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RequiredArgsConstructor
@Slf4j
public class RequisitionValidReStartDateValidator implements
    ConstraintValidator<RequisitionValidReStartDate, RequisitionCreateRequest> {

  private final SiglusAuthenticationHelper authHelper;
  private final SiglusReportTypeRepository reportTypeRepository;
  private final SyncUpHashRepository syncUpHashRepository;

  @Override
  public void initialize(RequisitionValidReStartDate constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(RequisitionCreateRequest value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    UserDto user = authHelper.getCurrentUser();
    boolean alreadySynced = syncUpHashRepository.findOne(value.getSyncUpHash(user)) != null;
    if (alreadySynced || Boolean.TRUE.equals(value.getEmergency())) {
      return true;
    }
    String programCode = value.getProgramCode();
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("startDate", value.getActualStartDate());
    UUID homeFacilityId = user.getHomeFacilityId();
    LocalDate reportRestartDate = reportTypeRepository
        .findOneByFacilityIdAndProgramCodeAndActiveIsTrue(homeFacilityId, programCode)
        .map(SiglusReportType::getStartDate)
        .orElseThrow(() -> new EntityNotFoundException("Report type not found"));
    if (reportRestartDate.isAfter(value.getActualStartDate())) {
      actualContext.addExpressionVariable("reportRestartDate", reportRestartDate);
      return false;
    }
    return true;
  }

}
