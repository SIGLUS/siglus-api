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
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.repository.ProcessingPeriodRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidStartDate;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;

@RequiredArgsConstructor
public class RequisitionValidStartDateValidator implements
    ConstraintValidator<RequisitionValidStartDate, RequisitionCreateRequest> {

  private final SiglusAuthenticationHelper authHelper;
  private final ReportTypeRepository reportTypeRepo;
  private final SiglusRequisitionRepository requisitionRepo;
  private final ProcessingPeriodRepository periodRepo;
  private final ProgramReferenceDataService programDataService;

  @Override
  public void initialize(RequisitionValidStartDate constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(RequisitionCreateRequest value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    String programCode = value.getProgramCode();
    LocalDate reportRestartDate = reportTypeRepo.findOneByFacilityIdAndProgramcode(homeFacilityId, programCode)
        .map(ReportType::getStartDate)
        .orElseThrow(EntityNotFoundException::new);
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable("startDate", value.getActualStartDate());
    actualContext.addExpressionVariable("failedByReportRestartDate", false);
    if (!reportRestartDate.isBefore(value.getActualStartDate())) {
      actualContext.addExpressionVariable("failedByReportRestartDate", true);
      actualContext.addExpressionVariable("reportRestartDate", reportRestartDate);
      return false;
    }
    ProcessingPeriod lastPeriod = requisitionRepo.findLatestRequisitionByFacilityId(homeFacilityId).stream()
        .filter(req -> programCode.equals(programDataService.findOne(req.getProgramId()).getCode()))
        .map(req -> periodRepo.findOne(req.getProcessingPeriodId()))
        .findFirst()
        .orElseThrow(EntityNotFoundException::new);
    if (!lastPeriod.getEndDate().equals(value.getActualStartDate().minusDays(1))) {
      actualContext.addExpressionVariable("lastEnd", lastPeriod.getEndDate());
      return false;
    }
    return true;
  }

}
