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

package org.siglus.siglusapi.web.android;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableValidator;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.siglusapi.config.AndroidTemplateConfigProperties;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.dto.android.validator.RequisitionValidReStartDateValidator;
import org.siglus.siglusapi.dto.android.validator.RequisitionValidStartDateValidator;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RequisitionRequestBackupRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.android.RequisitionCreateService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RequisitionValidStartDateValidator.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class SiglusMeControllerCreateRequisitionValidationTest extends FileBasedTest {

  private static final String MAY_NOT_BE_EMPTY = "may not be empty";
  private static final String MAY_NOT_BE_NULL = "may not be null";

  @InjectMocks
  private RequisitionCreateService requisitionCreateService;

  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private SiglusReportTypeRepository reportTypeRepo;
  @Mock
  private SiglusRequisitionRepository requisitionRepo;
  @Mock
  private ProcessingPeriodRepository periodRepo;
  @Mock
  private ProgramReferenceDataService programDataService;
  @Mock
  private AndroidTemplateConfigProperties androidTemplateConfigProperties;
  @Mock
  private SyncUpHashRepository syncUpHashRepository;
  @Mock
  private RequisitionRequestBackupRepository requisitionRequestBackupRepository;

  private final ObjectMapper mapper = new ObjectMapper();

  private ExecutableValidator forExecutables;

  private Method method;

  @Mock
  // last requisition
  private Requisition req1;
  // facility with some requisitions
  private final UUID facilityId = UUID.randomUUID();
  // facility with no requisitions
  private final UUID newFacilityId = UUID.randomUUID();
  // facility with some requisitions but before report restart date
  private final UUID restartedFacilityId = UUID.randomUUID();

  private final UUID program1Id = UUID.randomUUID();

  @Before
  public void setup() throws NoSuchMethodException {
    Locale.setDefault(Locale.ENGLISH);
    mapper.registerModule(new JavaTimeModule());
    ResourceBundleMessageInterpolator messageInterpolator =
        new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("messages/messages"));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    forExecutables = Validation.byDefaultProvider().configure()
        .constraintValidatorFactory(new InnerConstraintValidatorFactory())
        .messageInterpolator(messageInterpolator)
        .buildValidatorFactory().getValidator().forExecutables();
    method = RequisitionCreateService.class.getDeclaredMethod("createRequisition", RequisitionCreateRequest.class);

    SiglusReportType reportType = mock(SiglusReportType.class);
    when(reportType.getStartDate()).thenReturn(LocalDate.of(2021, 3, 1));
    when(reportTypeRepo.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(any(), eq("VC")))
        .thenReturn(Optional.of(reportType));

    ProgramDto otherProgram = mock(ProgramDto.class);
    when(programDataService.findOne(any())).thenReturn(otherProgram);
    ProgramDto program1 = mock(ProgramDto.class);
    when(program1.getCode()).thenReturn("VC");
    when(programDataService.findOne(program1Id)).thenReturn(program1);

    ProcessingPeriod otherPeriod = mock(ProcessingPeriod.class);
    when(periodRepo.findOne(any(UUID.class))).thenReturn(otherPeriod);
    UUID period1Id = UUID.randomUUID();
    ProcessingPeriod period1 = mock(ProcessingPeriod.class);
    when(period1.getName()).thenReturn("May 21-2021");
    when(period1.getStartDate()).thenReturn(LocalDate.of(2021, 5, 21));
    when(period1.getEndDate()).thenReturn(LocalDate.of(2021, 6, 20));
    when(periodRepo.findOne(period1Id)).thenReturn(period1);
    ProcessingPeriod periodForApr = mock(ProcessingPeriod.class);
    when(periodForApr.getName()).thenReturn("Apr 21-2021");
    when(periodForApr.getStartDate()).thenReturn(LocalDate.of(2021, 4, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 4)))).thenReturn(Optional.of(periodForApr));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 5)))).thenReturn(Optional.of(period1));
    ProcessingPeriod periodForJun = mock(ProcessingPeriod.class);
    when(periodForJun.getName()).thenReturn("Jun 21-2021");
    when(periodForJun.getStartDate()).thenReturn(LocalDate.of(2021, 6, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 6)))).thenReturn(Optional.of(periodForJun));
    ProcessingPeriod periodForJul08 = mock(ProcessingPeriod.class);
    when(periodForJul08.getName()).thenReturn("Aug 07-2008");
    when(periodForJul08.getStartDate()).thenReturn(LocalDate.of(2008, 7, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2008, 7)))).thenReturn(Optional.of(periodForJul08));
    ProcessingPeriod periodForMarch = mock(ProcessingPeriod.class);
    when(periodForMarch.getName()).thenReturn("Mar 03-2021");
    when(periodForMarch.getStartDate()).thenReturn(LocalDate.of(2021, 3, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 3)))).thenReturn(Optional.of(periodForMarch));

    req1 = mock(Requisition.class);
    when(req1.getProgramId()).thenReturn(program1Id);
    when(req1.getProcessingPeriodId()).thenReturn(period1Id);
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 6, 20));
    Set<UUID> androidTemplateIds = new HashSet<>();
    androidTemplateIds.add(UUID.fromString("610a52a5-2217-4fb7-9e8e-90bba3051d4d"));
    androidTemplateIds.add(UUID.fromString("873c25d6-e53b-11eb-8494-acde48001122"));
    androidTemplateIds.add(UUID.fromString("3f2245ce-ee9f-11eb-ba79-acde48001122"));
    androidTemplateIds.add(UUID.fromString("2c10856e-eead-11eb-9718-acde48001122"));
    when(androidTemplateConfigProperties.getAndroidTemplateIds()).thenReturn(androidTemplateIds);
    // interfering item
    Requisition req2 = mock(Requisition.class);
    when(requisitionRepo
        .findLatestRequisitionsByFacilityIdAndAndroidTemplateId(facilityId,
            androidTemplateConfigProperties.getAndroidTemplateIds()))
        .thenReturn(asList(req1, req2));
    when(requisitionRepo
        .findLatestRequisitionsByFacilityIdAndAndroidTemplateId(newFacilityId,
            androidTemplateConfigProperties.getAndroidTemplateIds()))
        .thenReturn(emptyList());
    Requisition req3 = mock(Requisition.class);
    when(req3.getProgramId()).thenReturn(program1Id);
    when(req3.getActualEndDate()).thenReturn(LocalDate.of(2020, 8, 20));
    when(requisitionRepo.findLatestRequisitionsByFacilityIdAndAndroidTemplateId(restartedFacilityId,
        androidTemplateConfigProperties.getAndroidTemplateIds())).thenReturn(singletonList(req3));
    when(syncUpHashRepository.findOne(anyString())).thenReturn(null);
    when(requisitionRequestBackupRepository.findOneByHash(anyString())).thenReturn(null);
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenEmpty() throws Exception {
    // given
    String json = "{}";
    Object param = mapper.readValue(json, RequisitionCreateRequest.class);

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(6, violations.size());
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createRequisition.arg0.programCode"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.actualEndDate"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.actualStartDate"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.clientSubmittedTime"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.emergency"));
    assertEquals(MAY_NOT_BE_EMPTY, violations.get("createRequisition.arg0.signatures"));
  }

  @Test
  public void shouldReturnNoViolationWhenValidateCreateRequisitionGivenValidStartDate()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("validStartDate.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenActualStartDateBeforeReportRestartDate()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("actualStartDateBeforeReportRestartDate.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "The start date 2021-02-17 should be after the report restart date 2021-03-01.",
        violations.get("createRequisition.arg0"));

  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenActualStartDateEqualToReportRestartDate()
      throws Exception {
    // given
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 2, 28));
    ProcessingPeriod period1 = mock(ProcessingPeriod.class);
    when(period1.getEndDate()).thenReturn(LocalDate.of(2021, 4, 20));
    when(periodRepo.findOne(req1.getProcessingPeriodId())).thenReturn(period1);
    mockFacilityId(facilityId);
    Object param = parseParam("actualStartDateEqualToReportRestartDate.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenActualStartDateBeforeLastActualEnd()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("actualStartDateBeforeLastActualEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. There is a gap between current period and the submission date of previous period. "
            + "Please contact the administrator.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenActualStartDateAfterLastActualEnd()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    LocalDate localDate = LocalDate.of(2021, 5, 20).plusMonths(13L);
    YearMonth yearMonth = YearMonth.of(localDate.getYear(), localDate.getMonth());
    mockStatic(YearMonth.class);
    when(YearMonth.now()).thenReturn(yearMonth);

    Object param = parseParam("actualStartDateAfterLastActualEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. There is a gap between current period and the submission date of previous period. "
            + "Please contact the administrator.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenLastEndTooFar1()
      throws Exception {
    // given
    SiglusReportType reportType = mock(SiglusReportType.class);
    when(reportType.getStartDate()).thenReturn(LocalDate.of(1999, 3, 1));
    when(reportTypeRepo.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(any(), eq("VC")))
        .thenReturn(Optional.of(reportType));
    mockFacilityId(facilityId);
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2013, 6, 20));
    Object param = parseParam("actualStartDateAfterLastActualEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenLastEndTooFar2()
      throws Exception {
    // given
    SiglusReportType reportType = mock(SiglusReportType.class);
    when(reportType.getStartDate()).thenReturn(LocalDate.of(1999, 3, 1));
    when(reportTypeRepo.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(any(), eq("VC")))
        .thenReturn(Optional.of(reportType));
    mockFacilityId(facilityId);
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2013, 6, 20));
    Object param = parseParam("actualStartDateBeforeLastEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. There is a gap between current period and the submission date of previous period. "
            + "Please contact the administrator.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenNewFacility()
      throws Exception {
    // given
    mockFacilityId(newFacilityId);
    Object param = parseParam("actualStartDateAfterLastActualEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenRestartedFacility()
      throws Exception {
    // given
    mockFacilityId(restartedFacilityId);
    Object param = parseParam("actualStartDateAfterLastActualEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenPeriodApr()
      throws Exception {
    // given
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 4, 20));
    mockFacilityId(facilityId);
    Object param = parseParam("periodApr.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. There is a gap between current period and the submission date of previous period. "
            + "Please contact the administrator.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenPeriodMay()
      throws Exception {
    // given
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 5, 20));
    mockFacilityId(facilityId);
    Object param = parseParam("periodMay.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. There is a gap between current period and the submission date of previous period. "
            + "Please contact the administrator.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenPeriodJul() throws Exception {
    // given
    ProcessingPeriod periodForJul = mock(ProcessingPeriod.class);
    when(periodForJul.getName()).thenReturn("Jul 21-2021");
    when(periodForJul.getStartDate()).thenReturn(LocalDate.of(2021, 7, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 7)))).thenReturn(Optional.of(periodForJul));
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 7, 20));
    mockFacilityId(facilityId);
    Object param = parseParam("periodJul.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. There is a gap between current period and the submission date of previous period. "
            + "Please contact the administrator.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenPeriodNull() throws Exception {
    // given
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 7)))).thenReturn(Optional.ofNullable(null));
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 7, 20));
    mockFacilityId(facilityId);
    Object param = parseParam("periodJul.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals(
        "Sync failed. The period you are submitting does not match the period in schedule.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenNecessaryParameterEmptyByProgramCode() throws Exception {
    // given
    Object paramVia = parseParam("viaNecessaryParameterEmpty.json");
    // when
    Map<String, String> violationsVia = executeValidation(paramVia);
    // then
    assertEquals(1, violationsVia.size());
    assertEquals(" products may not be empty.", violationsVia.get("createRequisition.arg0"));

    // given
    Object paramMmia = parseParam("mmiaNecessaryParameterEmpty.json");
    // when
    Map<String, String> violationsMmia = executeValidation(paramMmia);
    // then
    assertEquals(1, violationsMmia.size());
    assertEquals(" products regimenLineItems patientLineItems may not be empty.",
        violationsMmia.get("createRequisition.arg0"));

    // given
    Object paramMalaria = parseParam("malariaNecessaryParameterEmpty.json");
    // when
    Map<String, String> violationsMalaria = executeValidation(paramMalaria);
    // then
    assertEquals(1, violationsMalaria.size());
    assertEquals(" usageInfomationLineItems may not be empty.", violationsMalaria.get("createRequisition.arg0"));

    // given
    Object paramRapidtest = parseParam("rapidtestNecessaryParameterEmpty.json");
    // when
    Map<String, String> violationsRapidtest = executeValidation(paramRapidtest);
    // then
    assertEquals(1, violationsRapidtest.size());
    assertEquals(" testConsumptionLineItems may not be empty.", violationsRapidtest.get("createRequisition.arg0"));
  }

  @Test
  public void shouldThrowEntityNotFoundWhenValidateCreateRequisitionGivenNoReportType()
      throws Exception {
    // given
    when(reportTypeRepo.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(any(), eq("VC"))).thenReturn(Optional.empty());
    mockFacilityId(facilityId);
    Object param = parseParam("validStartDate.json");

    // when
    try {
      executeValidation(param);
    } catch (ValidationException e) {
      assertEquals(EntityNotFoundException.class, e.getCause().getClass());
    }
  }

  @Test
  public void shouldThrowErrorWhenActualEndDateBeforeActualStartDate() throws Exception {
    // given
    Object param = parseParam("actualEndDateBeforeActualStartDate.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals("The end date 2021-06-24 should be after start date 2021-06-25.",
        violations.get("createRequisition.arg0"));

  }

  @Test
  public void shouldNotThrowErrorWhenSameRequisitionAlreadySaved() throws Exception {
    // given
    mockFacilityId(restartedFacilityId);
    Object param = parseParam("actualStartDateAfterLastActualEnd.json");
    when(syncUpHashRepository.findOne(anyString())).thenReturn(new SyncUpHash());

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldNotThrowErrorWhenSaveEmergencyRequisition() throws Exception {
    // given
    mockFacilityId(restartedFacilityId);
    Object param = parseParam("emergencyRequisition.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  private Object parseParam(String fileName) throws IOException {
    String json = readFromFile(fileName);
    return mapper.readValue(json, RequisitionCreateRequest.class);
  }

  private Map<String, String> executeValidation(Object... params) {
    return forExecutables
        .validateParameters(requisitionCreateService, method, params, PerformanceSequence.class)
        .stream()
        .collect(toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage));
  }

  private void mockFacilityId(UUID facilityId) {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);
  }

  private class InnerConstraintValidatorFactory implements ConstraintValidatorFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
      if (key == RequisitionValidStartDateValidator.class) {
        return (T) new RequisitionValidStartDateValidator(androidTemplateConfigProperties, authHelper,
            programDataService, reportTypeRepo, requisitionRepo, periodRepo, syncUpHashRepository);
      } else if (key == RequisitionValidReStartDateValidator.class) {
        return (T) new RequisitionValidReStartDateValidator(authHelper, reportTypeRepo, syncUpHashRepository);
      }
      return NewInstance.action(key, "ConstraintValidator").run();
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
      // nothing to do
    }

  }

}
