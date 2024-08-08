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

import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.INITIATED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.siglus.siglusapi.constant.android.AndroidConstants.SCHEDULE_CODE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_REQUISITION_SUPPLIER_FACILITY_CREATED;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidStartDate;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.exception.InvalidProgramCodeException;
import org.siglus.siglusapi.exception.RequisitionAlreadyCreatedBySupplierFacilityException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("PMD.CyclomaticComplexity")
@RequiredArgsConstructor
@Slf4j
public class RequisitionValidStartDateValidator implements
    ConstraintValidator<RequisitionValidStartDate, RequisitionCreateRequest> {

  private final SiglusAuthenticationHelper authHelper;
  private final ProgramReferenceDataService programDataService;
  private final SiglusProgramService siglusProgramService;
  private final SiglusReportTypeRepository reportTypeRepository;
  private final SiglusRequisitionRepository requisitionRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final ProcessingPeriodRepository periodRepository;
  private final ProcessingPeriodExtensionRepository periodExtensionRepository;
  private final SyncUpHashRepository syncUpHashRepository;

  private static final String IS_SUBMITTED_PERIOD_INVALID = "isSubmittedPeriodInvalid";
  private static final String IS_PREVIOUS_REQUISITION_FAILED = "isPreviousRequisitionFailed";
  private static final String IS_CONFIGURE_PERIOD_INVALID = "isConfigurePeriodInvalid";
  private static final String IS_PERIOD_CANNOT_SUBMIT_YET = "isPeriodCannotSubmittedYet";
  private static final String PROGRAM_CODE_ERROR = "programCodeError";

  @Override
  public void initialize(RequisitionValidStartDate constraintAnnotation) {
    // nothing to do
  }

  @Override
  public boolean isValid(RequisitionCreateRequest value, ConstraintValidatorContext context) {
    // this validator is supposed to running after the default group, so the value will not be null or empty
    UserDto user = authHelper.getCurrentUser();
    HibernateConstraintValidatorContext actualContext = context.unwrap(HibernateConstraintValidatorContext.class);
    actualContext.addExpressionVariable(IS_SUBMITTED_PERIOD_INVALID, false);
    actualContext.addExpressionVariable(IS_PREVIOUS_REQUISITION_FAILED, false);
    actualContext.addExpressionVariable(IS_CONFIGURE_PERIOD_INVALID, false);
    actualContext.addExpressionVariable(IS_PERIOD_CANNOT_SUBMIT_YET, false);
    UUID homeFacilityId = user.getHomeFacilityId();
    boolean alreadySynced = syncUpHashRepository.findOne(value.getSyncUpHash(user)) != null;
    if (alreadySynced || Boolean.TRUE.equals(value.getEmergency())) {
      return true;
    }
    String programCode = value.getProgramCode();
    LocalDate reportRestartDate = reportTypeRepository
        .findOneByFacilityIdAndProgramCodeAndActiveIsTrue(homeFacilityId, programCode)
        .map(SiglusReportType::getStartDate)
        .orElseThrow(() -> new EntityNotFoundException("Report type not found"));
    if (reportRestartDate.isAfter(value.getActualStartDate())) {
      return true;
    }
    ProcessingPeriod period = getPeriod(value);
    if (period == null) {
      actualContext.addExpressionVariable(IS_CONFIGURE_PERIOD_INVALID, true);
      return false;
    }
    Requisition existRequisition = checkRequisitionCreatedBySupplier(homeFacilityId, programCode, period.getId());
    if (!ObjectUtils.isEmpty(existRequisition) && existRequisition.getStatus() == RequisitionStatus.REJECTED) {
      return true;
    }
    Optional<ProgramDto> programByCode = siglusProgramService.getProgramByCode(programCode);
    if (!programByCode.isPresent()) {
      actualContext.addExpressionVariable(PROGRAM_CODE_ERROR, true);
      return false;
    }
    Requisition lastRequisition = requisitionRepository
        .findLatestRequisitionsByFacilityIdAndProgramId(homeFacilityId, programByCode.get().getId())
        .stream()
        .findFirst()
        .orElse(null);

    ProcessingPeriodExtension periodExtension = periodExtensionRepository.findByProcessingPeriodId(period.getId());
    if (LocalDate.now().isBefore(periodExtension.getSubmitStartDate())) {
      actualContext.addExpressionVariable(IS_PERIOD_CANNOT_SUBMIT_YET, true);
      return false;
    }
    if (isFirstRequisition(lastRequisition, reportRestartDate)) {
      return true;
    }
    if (isRequisitionStatusBeforeInApproval(lastRequisition.getStatus())) {
      actualContext.addExpressionVariable(IS_PREVIOUS_REQUISITION_FAILED, true);
      return false;
    }
    LocalDate oneYearAgo = YearMonth.now().minusMonths(13L).atDay(1);
    LocalDate lastEndDate = lastRequisition.getActualEndDate();
    if (lastEndDate.isBefore(oneYearAgo) && value.getActualStartDate().isAfter(lastEndDate)) {
      return true;
    }
    ProcessingPeriod lastPeriod = periodRepository.findOne(lastRequisition.getProcessingPeriodId());
    if (isNotConsecutive(lastEndDate, value.getActualStartDate(), lastPeriod, period)) {
      actualContext.addExpressionVariable(IS_SUBMITTED_PERIOD_INVALID, true);
      return false;
    }
    return true;
  }

  private Requisition checkRequisitionCreatedBySupplier(UUID homeFacilityId, String programCode, UUID periodId) {
    UUID programId = siglusProgramService.getProgramByCode(programCode)
        .map(BaseDto::getId)
        .orElseThrow(() -> InvalidProgramCodeException.requisition(programCode));
    Requisition existRequisition = requisitionRepository.findOneByFacilityIdAndProgramIdAndProcessingPeriodId(
        homeFacilityId, programId, periodId);
    if (existRequisition != null) {
      RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(
          existRequisition.getId());
      if (requisitionExtension != null && requisitionExtension.createdBySupplier()) {
        throw new RequisitionAlreadyCreatedBySupplierFacilityException(
            new Message(ERROR_REQUISITION_SUPPLIER_FACILITY_CREATED));
      }
    }
    return existRequisition;
  }

  private boolean isNotConsecutive(LocalDate lastEndDate, LocalDate submitActualStartDate,
      ProcessingPeriod lastPeriod, ProcessingPeriod submitPeriod) {
    return !isConsecutiveActualDate(lastEndDate, submitActualStartDate)
        || !isConsecutivePeriod(lastPeriod, submitPeriod);
  }

  private boolean isConsecutivePeriod(ProcessingPeriod lastPeriod, ProcessingPeriod submitPeriod) {
    return lastPeriod.getEndDate().equals(submitPeriod.getStartDate().minusDays(1));
  }

  private boolean isConsecutiveActualDate(LocalDate lastEndDate, LocalDate submitActualStartDate) {
    return lastEndDate.equals(submitActualStartDate);
  }

  private boolean isFirstRequisition(Requisition lastRequisition, LocalDate reportRestartDate) {
    return lastRequisition == null || lastRequisition.getActualEndDate().isBefore(reportRestartDate);
  }

  private boolean isRequisitionStatusBeforeInApproval(RequisitionStatus status) {
    return status == INITIATED || status == SUBMITTED || status == AUTHORIZED;
  }

  private ProcessingPeriod getPeriod(RequisitionCreateRequest request) {
    YearMonth month = request.getActualStartDate().query(YearMonth::from);
    return periodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, month).orElse(null);
  }
}
