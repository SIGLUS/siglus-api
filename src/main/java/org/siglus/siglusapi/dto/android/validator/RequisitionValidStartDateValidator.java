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

import static org.siglus.siglusapi.constant.AndroidConstants.SCHEDULE_CODE;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.ProcessingPeriodRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.config.AndroidTemplateConfigProperties;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidStartDate;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;

@RequiredArgsConstructor
@Slf4j
public class RequisitionValidStartDateValidator implements
    ConstraintValidator<RequisitionValidStartDate, RequisitionCreateRequest> {

  private final AndroidTemplateConfigProperties androidTemplateConfigProperties;
  private final SiglusAuthenticationHelper authHelper;
  private final ProgramReferenceDataService programDataService;
  private final ReportTypeRepository reportTypeRepository;
  private final SiglusRequisitionRepository requisitionRepository;
  private final ProcessingPeriodRepository periodRepository;
  private final SyncUpHashRepository syncUpHashRepository;

  @Override
  public void initialize(RequisitionValidStartDate constraintAnnotation) {
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
    actualContext.addExpressionVariable("failedByReportRestartDate", false);
    actualContext.addExpressionVariable("failedByPeriod", false);
    UUID homeFacilityId = user.getHomeFacilityId();
    LocalDate reportRestartDate = reportTypeRepository
        .findOneByFacilityIdAndProgramCodeAndActiveIsTrue(homeFacilityId, programCode)
        .map(ReportType::getStartDate)
        .orElseThrow(() -> new EntityNotFoundException("Report type not found"));
    if (reportRestartDate.isAfter(value.getActualStartDate())) {
      actualContext.addExpressionVariable("failedByReportRestartDate", true);
      actualContext.addExpressionVariable("reportRestartDate", reportRestartDate);
      return false;
    }
    Requisition lastRequisition = requisitionRepository
        .findLatestRequisitionsByFacilityIdAndAndroidTemplateId(homeFacilityId,
            androidTemplateConfigProperties.getAndroidTemplateIds())
        .stream()
        .filter(req -> programCode.equals(programDataService.findOne(req.getProgramId()).getCode()))
        .findFirst()
        .orElse(null);
    if (lastRequisition == null || lastRequisition.getActualEndDate().isBefore(reportRestartDate)) {
      return true;
    }
    if (!lastRequisition.getActualEndDate().equals(value.getActualStartDate())) {
      actualContext.addExpressionVariable("lastActualEnd", lastRequisition.getActualEndDate());
      return false;
    }
    ProcessingPeriod lastPeriod = periodRepository.findOne(lastRequisition.getProcessingPeriodId());
    ProcessingPeriod period = getPeriod(value);
    if (!lastPeriod.getEndDate().equals(period.getStartDate().minusDays(1))) {
      actualContext.addExpressionVariable("failedByPeriod", true);
      actualContext.addExpressionVariable("periodName", period.getName());
      actualContext.addExpressionVariable("lastPeriodName", lastPeriod.getName());
      return false;
    }
    return true;
  }

  private ProcessingPeriod getPeriod(RequisitionCreateRequest request) {
    YearMonth month = request.getActualStartDate().query(YearMonth::from);
    return periodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, month)
        .orElseThrow(EntityNotFoundException::new);
  }
}
