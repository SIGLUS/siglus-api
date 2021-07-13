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
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import org.hibernate.validator.internal.util.privilegedactions.NewInstance;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.ProcessingPeriodRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.dto.android.validator.RequisitionValidStartDateValidator;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class SiglusMeControllerCreateRequisitionValidationTest extends FileBasedTest {

  private static final String MAY_NOT_BE_EMPTY = "may not be empty";
  private static final String MAY_NOT_BE_NULL = "may not be null";

  @InjectMocks
  private SiglusMeController controller;

  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private ReportTypeRepository reportTypeRepo;
  @Mock
  private SiglusRequisitionRepository requisitionRepo;
  @Mock
  private ProcessingPeriodRepository periodRepo;
  @Mock
  private ProgramReferenceDataService programDataService;

  private final ObjectMapper mapper = new ObjectMapper();

  private ExecutableValidator forExecutables;

  private Method method;

  // facility with some requisitions
  private final UUID facilityId = UUID.randomUUID();
  // facility with no requisitions
  private final UUID newFacilityId = UUID.randomUUID();
  // facility with some requisitions but before report restart date
  private final UUID restartedFacilityId = UUID.randomUUID();

  @Before
  public void setup() throws NoSuchMethodException {
    Locale.setDefault(Locale.ENGLISH);
    mapper.registerModule(new JavaTimeModule());
    ResourceBundleMessageInterpolator messageInterpolator =
        new ResourceBundleMessageInterpolator(new PlatformResourceBundleLocator("messages"));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    forExecutables = Validation.byDefaultProvider().configure()
        .constraintValidatorFactory(new InnerConstraintValidatorFactory())
        .messageInterpolator(messageInterpolator)
        .buildValidatorFactory().getValidator().forExecutables();
    method = SiglusMeController.class.getDeclaredMethod("createRequisition", RequisitionCreateRequest.class);

    ReportType reportType = mock(ReportType.class);
    when(reportType.getStartDate()).thenReturn(LocalDate.of(2021, 6, 1));
    when(reportTypeRepo.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(any(), eq("13")))
        .thenReturn(Optional.of(reportType));

    ProgramDto otherProgram = mock(ProgramDto.class);
    when(programDataService.findOne(any())).thenReturn(otherProgram);
    UUID program1Id = UUID.randomUUID();
    ProgramDto program1 = mock(ProgramDto.class);
    when(program1.getCode()).thenReturn("13");
    when(programDataService.findOne(program1Id)).thenReturn(program1);

    ProcessingPeriod otherPeriod = mock(ProcessingPeriod.class);
    when(periodRepo.findOne(any(UUID.class))).thenReturn(otherPeriod);
    UUID period1Id = UUID.randomUUID();
    ProcessingPeriod period1 = mock(ProcessingPeriod.class);
    when(period1.getEndDate()).thenReturn(LocalDate.of(2021, 6, 20));
    when(periodRepo.findOne(period1Id)).thenReturn(period1);
    ProcessingPeriod periodForJun = mock(ProcessingPeriod.class);
    when(periodForJun.getStartDate()).thenReturn(LocalDate.of(2021, 6, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 6)))).thenReturn(Optional.of(periodForJun));
    ProcessingPeriod periodForJul = mock(ProcessingPeriod.class);
    when(periodForJul.getStartDate()).thenReturn(LocalDate.of(2021, 7, 21));
    when(periodRepo.findPeriodByCodeAndMonth(any(), eq(YearMonth.of(2021, 7)))).thenReturn(Optional.of(periodForJul));

    Requisition req1 = mock(Requisition.class);
    when(req1.getProgramId()).thenReturn(program1Id);
    when(req1.getProcessingPeriodId()).thenReturn(period1Id);
    when(req1.getActualEndDate()).thenReturn(LocalDate.of(2021, 6, 20));
    // interfering item
    Requisition req2 = mock(Requisition.class);
    when(requisitionRepo.findLatestRequisitionByFacilityId(facilityId)).thenReturn(asList(req1, req2));
    when(requisitionRepo.findLatestRequisitionByFacilityId(newFacilityId)).thenReturn(emptyList());
    Requisition req3 = mock(Requisition.class);
    when(req3.getProgramId()).thenReturn(program1Id);
    when(req3.getActualEndDate()).thenReturn(LocalDate.of(2020, 8, 20));
    when(requisitionRepo.findLatestRequisitionByFacilityId(restartedFacilityId)).thenReturn(singletonList(req3));
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
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.consultationNumber"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.clientSubmittedTime"));
    assertEquals(MAY_NOT_BE_NULL, violations.get("createRequisition.arg0.emergency"));
  }

  @Test
  public void shouldReturnNoViolationWhenValidateCreateRequisitionGivenValidStartDate()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("requisitionValidStartDate.json");

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
    Object param = parseParam("requisitionActualStartDateBeforeReportRestartDate.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals("The start date 2021-05-17 should be after the report restart date 2021-06-01.",
        violations.get("createRequisition.arg0"));

  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenActualStartDateEqualToReportRestartDate()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("requisitionActualStartDateEqualToReportRestartDate.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(0, violations.size());
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenStartDateBeforeLastEnd()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("requisitionActualStartDateBeforeLastEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals("The start date 2021-06-17 is not right after last period's end 2021-06-20.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenStartDateAfterLastEnd()
      throws Exception {
    // given
    mockFacilityId(facilityId);
    Object param = parseParam("requisitionActualStartDateAfterLastEnd.json");

    // when
    Map<String, String> violations = executeValidation(param);

    // then
    assertEquals(1, violations.size());
    assertEquals("The start date 2021-06-25 is not right after last period's end 2021-06-20.",
        violations.get("createRequisition.arg0"));
  }

  @Test
  public void shouldReturnViolationWhenValidateCreateRequisitionGivenNewFacility()
      throws Exception {
    // given
    mockFacilityId(newFacilityId);
    Object param = parseParam("requisitionActualStartDateAfterLastEnd.json");

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
    Object param = parseParam("requisitionActualStartDateAfterLastEnd.json");

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
        .validateParameters(controller, method, params, PerformanceSequence.class)
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
        return (T) new RequisitionValidStartDateValidator(authHelper, reportTypeRepo, requisitionRepo, periodRepo,
            programDataService);
      }
      return NewInstance.action(key, "ConstraintValidator").run();
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
      // nothing to do
    }

  }

}
