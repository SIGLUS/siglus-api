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

package org.openlmis.requisition.web;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Maps.newHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.RequisitionTemplate.ORDER_RELATED_COLUMNS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_REQUISITION_PERIODS_FOR_INITIATE_MISSING_PARAMETERS;
import static org.openlmis.requisition.i18n.MessageKeys.IDEMPOTENCY_KEY_ALREADY_USED;
import static org.openlmis.requisition.i18n.MessageKeys.IDEMPOTENCY_KEY_WRONG_FORMAT;
import static org.openlmis.requisition.web.BaseController.API_URL;
import static org.openlmis.requisition.web.BaseRequisitionController.IDEMPOTENCY_KEY_HEADER;
import static org.openlmis.requisition.web.BaseRequisitionController.RESOURCE_URL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.RequisitionValidationService;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.FacilityTypeDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.dto.SupportedProgramDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.dto.stockmanagement.StockEventDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.exception.BindingResultException;
import org.openlmis.requisition.exception.IdempotencyKeyException;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.exception.VersionMismatchException;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.requisition.repository.custom.ProcessedRequestsRedisRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionStatusNotifier;
import org.openlmis.requisition.service.RequisitionStatusProcessor;
import org.openlmis.requisition.service.RequisitionTemplateService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyLineReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockEventStockManagementService;
import org.openlmis.requisition.service.stockmanagement.ValidReasonStockmanagementService;
import org.openlmis.requisition.settings.service.ConfigurationSettingService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.FacilityDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.openlmis.requisition.testutils.RequisitionPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.SupervisoryNodeDtoDataBuilder;
import org.openlmis.requisition.testutils.SupplyLineDtoDataBuilder;
import org.openlmis.requisition.testutils.SupportedProgramDtoDataBuilder;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.requisition.utils.DateHelper;
import org.openlmis.requisition.utils.DatePhysicalStockCountCompletedEnabledPredicate;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.StockEventBuilder;
import org.openlmis.requisition.validate.ReasonsValidator;
import org.openlmis.requisition.validate.RequisitionVersionValidator;
import org.slf4j.profiler.Profiler;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class RequisitionControllerTest {
  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private PeriodService periodService;

  @Mock
  private Requisition initiatedRequsition;

  @Mock
  private Requisition submittedRequsition;

  @Mock
  private Requisition authorizedRequsition;

  @Mock
  private Requisition approvedRequsition;

  @Mock
  private RequisitionTemplate template;

  @Mock
  private BasicRequisitionTemplateDto templateDto;

  @Mock
  private RequisitionTemplateRepository templateRepository;

  @Mock
  private PermissionService permissionService;

  @Mock
  private BasicRequisitionDtoBuilder basicRequisitionDtoBuilder;

  @Mock
  private RequisitionDtoBuilder requisitionDtoBuilder;

  @Mock
  private FacilitySupportsProgramHelper facilitySupportsProgramHelper;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Mock
  private ConfigurationSettingService configurationSettingService;

  @Mock
  private RequisitionVersionValidator requisitionVersionValidator;

  @Mock
  private RequisitionStatusProcessor requisitionStatusProcessor;

  @Mock
  private RequisitionStatusNotifier requisitionStatusNotifier;

  @Mock
  private DatePhysicalStockCountCompletedEnabledPredicate predicate;

  @Mock
  private StockEventBuilder stockEventBuilderBuilder;

  @Mock
  private StockEventStockManagementService stockEventService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private SupplyLineReferenceDataService supplyLineReferenceDataService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private FacilityTypeApprovedProductReferenceDataService
      facilityTypeApprovedProductReferenceDataService;

  @Mock
  private StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Mock
  private ProcessedRequestsRedisRepository processedRequestsRedisRepository;

  @Mock
  private DateHelper dateHelper;

  @Mock
  private HttpServletResponse response;

  @Mock
  private HttpServletRequest request;
  
  @Mock
  private ValidReasonStockmanagementService validReasonStockmanagementService;
  
  @Mock
  private ReasonsValidator reasonsValidator;
  
  @Mock
  private RequisitionTemplateService requisitionTemplateService;

  @Mock
  private RequisitionSplitter requisitionSplitter;

  @Mock
  private RequisitionSplitResult requisitionSplitResult;

  @Mock
  private ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @InjectMocks
  private RequisitionController requisitionController;

  private UUID programUuid = UUID.randomUUID();
  private UUID facilityUuid = UUID.randomUUID();
  private UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private UUID uuid3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private UUID uuid4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
  private UUID uuid5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
  private ProcessingPeriodDto processingPeriod = mock(ProcessingPeriodDto.class);
  private FacilityDto facility = mock(FacilityDto.class);
  private BasicRequisitionDto basicRequisitionDto = new BasicRequisitionDto();
  private ValidationResult fieldErrors = ValidationResult
      .fieldErrors(newHashMap("someField", new Message("some-key", "someParam")));
  private String bindingResultMessage = "{someField=some-key: someParam}";
  private SupervisoryNodeDto supervisoryNode;
  private UserDto currentUser;
  private StockCardRangeSummaryDto stockCardRangeSummaryDto;
  private UUID key = UUID.randomUUID();
  private String baseUrl = "https://openlmis/";
  private String wrongUuidFormat = "wrong-key";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    List<RequisitionPeriodDto> processingPeriods = generateProcessingPeriods();
    when(initiatedRequsition.getStatus()).thenReturn(RequisitionStatus.INITIATED);
    when(submittedRequsition.getStatus()).thenReturn(RequisitionStatus.SUBMITTED);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.AUTHORIZED);
    when(approvedRequsition.getStatus()).thenReturn(RequisitionStatus.APPROVED);

    when(submittedRequsition.getId()).thenReturn(uuid3);
    when(authorizedRequsition.getId()).thenReturn(uuid4);
    when(authorizedRequsition.isApprovable()).thenReturn(true);
    when(authorizedRequsition.getTemplate()).thenReturn(template);
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(false);

    when(periodService.getPeriods(programUuid, facilityUuid, false))
        .thenReturn(processingPeriods);
    when(periodService.getPeriods(programUuid, facilityUuid, true))
        .thenReturn(Collections.singletonList(processingPeriods.get(0)));
    when(periodService.getPeriod(processingPeriod.getId()))
        .thenReturn(processingPeriod);

    when(predicate.exec(any(UUID.class))).thenReturn(true);
    when(predicate.exec(any(ProgramDto.class))).thenReturn(true);

    mockRequisitionRepository();

    when(periodReferenceDataService.findOne(any(UUID.class)))
        .thenReturn(processingPeriod);

    stubValidations(initiatedRequsition, submittedRequsition, authorizedRequsition,
        approvedRequsition);
    when(dateHelper.getCurrentDateWithSystemZone()).thenReturn(LocalDate.now());
    when(predicate.exec(programUuid)).thenReturn(true);
    mockFindSupervisoryNodeByProgramAndFacility();

    currentUser = DtoGenerator.of(UserDto.class);
    when(authenticationHelper.getCurrentUser()).thenReturn(currentUser);

    when(processedRequestsRedisRepository.exists(any()))
        .thenReturn(false);

    when(request.getHeader(IDEMPOTENCY_KEY_HEADER))
        .thenReturn(null);

    when(basicRequisitionDtoBuilder.build(any(Requisition.class)))
        .thenReturn(basicRequisitionDto);

    basicRequisitionDto.setId(uuid1);

    when(requisitionVersionValidator.validateRequisitionTimestamps(
        any(ZonedDateTime.class), any(Requisition.class)))
        .thenReturn(ValidationResult.success());

    ReflectionTestUtils.setField(requisitionController, BaseRequisitionController.class,
        "baseUrl", baseUrl, String.class);

    when(requisitionSplitter.split(any(), any())).thenReturn(requisitionSplitResult);
    when(requisitionSplitResult.wasSplit()).thenReturn(false);

    when(approvedProductReferenceDataService.getApprovedProducts(any(), any()))
        .thenReturn(new ApproveProductsAggregator(emptyList(), UUID.randomUUID()));
  }

  private void stubValidations(Requisition... requisitions) {
    for (Requisition requisition : requisitions) {
      when(requisition.getProgramId()).thenReturn(programUuid);
      when(requisition.validateCanChangeStatus(any(LocalDate.class), anyBoolean(),
          anyMap(), anyMap()))
          .thenReturn(ValidationResult.success());
    }
  }

  @Test
  public void shouldReturnCurrentPeriodForEmergency() {
    when(permissionService.canInitOrAuthorizeRequisition(programUuid, facilityUuid))
        .thenReturn(ValidationResult.success());

    Collection<RequisitionPeriodDto> periods =
        requisitionController.getProcessingPeriodIds(programUuid, facilityUuid, true);

    verify(periodService).getPeriods(programUuid, facilityUuid, true);
    verifyZeroInteractions(periodService, requisitionRepository);

    assertNotNull(periods);
    assertEquals(1, periods.size());

    List<UUID> periodUuids = periods
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());

    assertTrue(periodUuids.contains(uuid1));
  }

  @Test
  public void shouldThrowExceptionIfFacilityIdIsNotProvided() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_REQUISITION_PERIODS_FOR_INITIATE_MISSING_PARAMETERS);

    requisitionController.getProcessingPeriodIds(UUID.randomUUID(), null, false);
  }

  @Test
  public void shouldThrowExceptionIfProgramIdIsNotProvided() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(ERROR_REQUISITION_PERIODS_FOR_INITIATE_MISSING_PARAMETERS);

    requisitionController.getProcessingPeriodIds(null, UUID.randomUUID(), false);
  }

  @Test
  public void shouldSubmitValidInitiatedRequisition() {
    mockDependenciesForSubmit();
    requisitionController.submitRequisition(uuid1, request, response);

    verify(initiatedRequsition).submit(eq(Collections.emptyMap()), any(UUID.class), eq(false));
    // we do not update in this endpoint
    verify(initiatedRequsition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
    verify(initiatedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldDirectlyAuthorizeInitiatedRequisitionIfAuthorizeStepSkipped() {
    mockDependenciesForSubmit();
    ProgramDto programDto = new ProgramDtoDataBuilder().buildWithSkippedAuthorizationStep();
    doReturn(programDto).when(programReferenceDataService).findOne(any(UUID.class));

    requisitionController.submitRequisition(uuid1, request, response);

    verify(initiatedRequsition).submit(eq(Collections.emptyMap()), any(UUID.class), eq(true));
    // we do not update in this endpoint
    verify(initiatedRequsition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
  }

  @Test
  public void shouldSubmitEmergencyRequisitionForAnyPeriod() {
    mockDependenciesForSubmit();
    when(initiatedRequsition.getEmergency()).thenReturn(true);
    when(processingPeriod.getEndDate()).thenReturn(LocalDate.now());

    when(dateHelper.isDateAfterNow(processingPeriod.getEndDate())).thenReturn(true);

    requisitionController.submitRequisition(uuid1, request, response);

    verify(initiatedRequsition).submit(eq(Collections.emptyMap()), any(UUID.class), eq(false));
    // we do not update in this endpoint
    verify(initiatedRequsition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
    verify(initiatedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldRejectSubmitRegularRequisitionForWrongPeriod() {
    mockDependenciesForSubmit();
    when(initiatedRequsition.getEmergency()).thenReturn(false);
    when(processingPeriod.getEndDate()).thenReturn(LocalDate.now());

    when(dateHelper.isDateAfterNow(processingPeriod.getEndDate())).thenReturn(true);

    requisitionController.submitRequisition(uuid1, request, response);

    verify(initiatedRequsition).submit(eq(Collections.emptyMap()), any(UUID.class), eq(false));
    // we do not update in this endpoint
    verify(initiatedRequsition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
  }

  @Test
  public void shouldSubmitRequisitionWithIdempotencyKey() {
    mockDependenciesForSubmit();
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    requisitionController.submitRequisition(uuid1, request, response);

    verify(initiatedRequsition).submit(eq(Collections.emptyMap()), any(UUID.class), eq(false));
    // we do not update in this endpoint
    verify(initiatedRequsition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
    verify(initiatedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());

    verify(response, times(1)).addHeader(
        HttpHeaders.LOCATION, baseUrl + API_URL + RESOURCE_URL + '/' + uuid1.toString());
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, null);
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, uuid1);
  }

  @Test
  public void shouldNotSubmitRequisitionIfIdempotencyKeyHasWrongFormat() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_WRONG_FORMAT);

    mockDependenciesForSubmit();
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(wrongUuidFormat);

    requisitionController.submitRequisition(uuid1, request, response);
  }

  @Test
  public void shouldNotSubmitRequisitionWithUsedIdempotencyKey() {
    exception.expect(IdempotencyKeyException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_ALREADY_USED);

    mockDependenciesForSubmit();
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    when(processedRequestsRedisRepository.exists(key)).thenReturn(true);

    requisitionController.submitRequisition(uuid1, request, response);
  }

  @Test
  public void shouldNotSubmitInvalidRequisition() {
    when(permissionService.canSubmitRequisition(initiatedRequsition))
        .thenReturn(ValidationResult.success());
    doReturn(fieldErrors)
        .when(initiatedRequsition)
        .validateCanChangeStatus(any(LocalDate.class), anyBoolean(), anyMap(), anyMap());
    when(initiatedRequsition.getId()).thenReturn(uuid1);

    assertThatThrownBy(() -> requisitionController.submitRequisition(uuid1, request, response))
        .isInstanceOf(BindingResultException.class)
        .hasMessage(bindingResultMessage);

    verifyNoSubmitOrUpdate(initiatedRequsition);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldReturnBadRequestWhenRequisitionIdDiffersFromTheOneInUrl() throws Exception {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);
    when(requisitionDto.getId()).thenReturn(uuid1);

    requisitionController.updateRequisition(requisitionDto, uuid2, request, response);
  }

  @Test(expected = VersionMismatchException.class)
  public void shouldReturnBadRequestWhenThereIsNewerVersion() throws Exception {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);
    when(requisitionDto.getId()).thenReturn(uuid1);
    when(requisitionDto.getFacilityId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProgramId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProcessingPeriodId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getSupervisoryNode()).thenReturn(UUID.randomUUID());

    ZonedDateTime currentDate = ZonedDateTime.now();
    when(requisitionDto.getModifiedDate()).thenReturn(currentDate.minusMonths(10));
    when(initiatedRequsition.getId()).thenReturn(uuid1);
    when(initiatedRequsition.getModifiedDate()).thenReturn(currentDate);

    when(requisitionVersionValidator.validateRequisitionTimestamps(
        any(ZonedDateTime.class), any(Requisition.class)))
        .thenCallRealMethod();

    requisitionController.updateRequisition(requisitionDto, uuid1, request, response);
  }

  @Test
  public void shouldUpdateRequisition() {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);

    when(requisitionDto.getId()).thenReturn(uuid1);
    when(requisitionDto.getFacilityId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProgramId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProcessingPeriodId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getSupervisoryNode()).thenReturn(UUID.randomUUID());

    when(initiatedRequsition.getTemplate()).thenReturn(template);

    when(initiatedRequsition.getSupervisoryNodeId()).thenReturn(null);
    when(initiatedRequsition.getId()).thenReturn(uuid1);
    when(initiatedRequsition.validateCanBeUpdated(any(RequisitionValidationService.class)))
        .thenReturn(ValidationResult.success());
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(false);

    when(requisitionService.validateCanSaveRequisition(initiatedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionVersionValidator.validateEtagVersionIfPresent(
        any(HttpServletRequest.class), any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(programReferenceDataService.findOne(any(UUID.class))).thenReturn(
        new ProgramDtoDataBuilder().buildAsDto());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(
        DtoGenerator.of(FacilityDto.class));

    requisitionController.updateRequisition(requisitionDto, uuid1, request, response);

    assertEquals(template, initiatedRequsition.getTemplate());
    verify(initiatedRequsition).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), eq(true));
    verify(requisitionRepository).save(initiatedRequsition);
    verify(requisitionVersionValidator).validateEtagVersionIfPresent(any(HttpServletRequest.class),
        eq(initiatedRequsition));
    verifySupervisoryNodeWasNotUpdated(initiatedRequsition);
  }

  @Test
  public void shouldUpdateStockBasedRequisition() {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);

    when(requisitionDto.getId()).thenReturn(uuid1);
    when(requisitionDto.getFacilityId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProgramId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProcessingPeriodId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getSupervisoryNode()).thenReturn(UUID.randomUUID());

    when(initiatedRequsition.getTemplate()).thenReturn(template);
    when(initiatedRequsition.getSupervisoryNodeId()).thenReturn(null);
    when(initiatedRequsition.getId()).thenReturn(uuid1);
    when(initiatedRequsition.validateCanBeUpdated(any(RequisitionValidationService.class)))
        .thenReturn(ValidationResult.success());
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(true);

    when(requisitionService.validateCanSaveRequisition(initiatedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionVersionValidator.validateEtagVersionIfPresent(
        any(HttpServletRequest.class), any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(programReferenceDataService.findOne(any(UUID.class))).thenReturn(
        new ProgramDtoDataBuilder().buildAsDto());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(
        DtoGenerator.of(FacilityDto.class));
    when(periodReferenceDataService.findOne(any(UUID.class))).thenReturn(
        DtoGenerator.of(ProcessingPeriodDto.class));
    when(stockCardRangeSummaryStockManagementService.search(any(UUID.class), any(UUID.class),
        anySetOf(VersionIdentityDto.class), any(String.class),
        any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.singletonList(stockCardRangeSummaryDto));

    requisitionController.updateRequisition(requisitionDto, uuid1, request, response);

    assertEquals(template, initiatedRequsition.getTemplate());
    verify(initiatedRequsition).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), eq(true));
    verify(requisitionRepository).save(initiatedRequsition);
    verify(requisitionVersionValidator).validateEtagVersionIfPresent(any(HttpServletRequest.class),
        eq(initiatedRequsition));
    verifySupervisoryNodeWasNotUpdated(initiatedRequsition);
  }

  @Test
  public void shouldNotUpdateWithInvalidRequisition() {
    RequisitionDto requisitionDto = mock(RequisitionDto.class);
    when(requisitionDto.getTemplate()).thenReturn(templateDto);
    when(requisitionDto.getFacilityId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProgramId()).thenReturn(UUID.randomUUID());
    when(requisitionDto.getProcessingPeriodId()).thenReturn(UUID.randomUUID());
    when(requisitionService.validateCanSaveRequisition(initiatedRequsition))
        .thenReturn(ValidationResult.success());

    doReturn(ValidationResult.conflict("test")).when(requisitionVersionValidator)
        .validateEtagVersionIfPresent(any(HttpServletRequest.class), any(Requisition.class));

    assertThatThrownBy(() -> requisitionController
        .updateRequisition(requisitionDto, uuid1, request, response))
        .isInstanceOf(VersionMismatchException.class);

    verify(requisitionService).validateCanSaveRequisition(initiatedRequsition);
    verifyNoSubmitOrUpdate(initiatedRequsition);
  }

  @Test
  public void shouldThrowExceptionWhenFacilityOrProgramIdNotFound() throws Exception {
    exception.expect(ValidationMessageException.class);
    requisitionController.initiate(programUuid, null, null, false, request, response);
    exception.expect(ValidationMessageException.class);
    requisitionController.initiate(null, facilityUuid, null, false, request, response);
  }

  @Test
  public void initiateShouldFindReportOnlyTemplateIfPeriodIsReportOnly() {
    //given
    when(permissionService.canInitRequisition(programUuid, facilityUuid))
        .thenReturn(ValidationResult.success());
    UUID facilityTypeId = UUID.randomUUID();
    FacilityTypeDto facilityType = mock(FacilityTypeDto.class);
    when(facilityReferenceDataService.findOne(facilityUuid)).thenReturn(facility);
    when(facility.getType()).thenReturn(facilityType);
    when(facilityType.getId()).thenReturn(facilityTypeId);
    ProgramDto program = new ProgramDto();
    program.setId(programUuid);
    when(programReferenceDataService.findOne(programUuid)).thenReturn(program);
    doNothing().when(facilitySupportsProgramHelper).checkIfFacilitySupportsProgram(
        any(FacilityDto.class), eq(programUuid));
    when(validReasonStockmanagementService.search(programUuid, facilityTypeId))
        .thenReturn(Collections.emptyList());
    Requisition requisition = mock(Requisition.class);
    when(requisitionService.initiate(any(ProgramDto.class), any(FacilityDto.class),
        any(ProcessingPeriodDto.class), eq(false), anyListOf(StockAdjustmentReason.class),
        any(RequisitionTemplate.class), any(ApproveProductsAggregator.class)))
        .thenReturn(requisition);
    when(requisition.getTemplate()).thenReturn(mock(RequisitionTemplate.class));
    doNothing().when(reasonsValidator).validate(anyListOf(StockAdjustmentReason.class),
        any(RequisitionTemplate.class));
    RequisitionDto requisitionDto = new RequisitionDto();
    requisitionDto.setId(UUID.randomUUID());
    when(requisitionDtoBuilder.build(any(Requisition.class),
        anyMapOf(VersionIdentityDto.class, OrderableDto.class),
        anyMapOf(VersionIdentityDto.class, ApprovedProductDto.class),
        any(FacilityDto.class), any(ProgramDto.class),
        any(ProcessingPeriodDto.class)))
        .thenReturn(requisitionDto);

    when(periodService.findPeriod(programUuid, facilityUuid, null, false))
        .thenReturn(processingPeriod);
    when(processingPeriod.isReportOnly()).thenReturn(true);

    //when
    requisitionController.initiate(programUuid, facilityUuid, null, false, request, response);

    //then
    verify(requisitionTemplateService).findTemplate(programUuid, facilityTypeId, true);
  }

  @Test
  public void initiateEmergencyRequisitionShouldFindRegularTemplateIfPeriodIsReportOnly() {
    //given
    when(permissionService.canInitRequisition(programUuid, facilityUuid))
        .thenReturn(ValidationResult.success());
    UUID facilityTypeId = UUID.randomUUID();
    FacilityTypeDto facilityType = mock(FacilityTypeDto.class);
    when(facilityReferenceDataService.findOne(facilityUuid)).thenReturn(facility);
    when(facility.getType()).thenReturn(facilityType);
    when(facilityType.getId()).thenReturn(facilityTypeId);
    ProgramDto program = new ProgramDto();
    program.setId(programUuid);
    when(programReferenceDataService.findOne(programUuid)).thenReturn(program);
    doNothing().when(facilitySupportsProgramHelper).checkIfFacilitySupportsProgram(
        any(FacilityDto.class), eq(programUuid));
    when(validReasonStockmanagementService.search(programUuid, facilityTypeId))
        .thenReturn(Collections.emptyList());
    Requisition requisition = mock(Requisition.class);
    when(requisitionService.initiate(any(ProgramDto.class), any(FacilityDto.class),
        any(ProcessingPeriodDto.class), eq(true), anyListOf(StockAdjustmentReason.class),
        any(RequisitionTemplate.class), any(ApproveProductsAggregator.class)))
        .thenReturn(requisition);
    when(requisition.getTemplate()).thenReturn(mock(RequisitionTemplate.class));
    doNothing().when(reasonsValidator).validate(anyListOf(StockAdjustmentReason.class),
        any(RequisitionTemplate.class));
    RequisitionDto requisitionDto = new RequisitionDto();
    requisitionDto.setId(UUID.randomUUID());
    when(requisitionDtoBuilder.build(any(Requisition.class),
        anyMapOf(VersionIdentityDto.class, OrderableDto.class),
        anyMapOf(VersionIdentityDto.class, ApprovedProductDto.class),
        any(FacilityDto.class), any(ProgramDto.class),
        any(ProcessingPeriodDto.class)))
        .thenReturn(requisitionDto);

    when(periodService.findPeriod(programUuid, facilityUuid, null, true))
        .thenReturn(processingPeriod);
    when(processingPeriod.isReportOnly()).thenReturn(true);

    //when
    requisitionController.initiate(programUuid, facilityUuid, null, true, request, response);

    //then
    verify(requisitionTemplateService).findTemplate(programUuid, facilityTypeId, false);
  }

  @Test
  public void shouldApproveRequisitionWithIdempotencyKey() {
    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();
    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(response, times(1)).addHeader(
        HttpHeaders.LOCATION, baseUrl + API_URL + RESOURCE_URL + '/' + uuid1.toString());
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, null);
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, uuid1);
  }

  @Test
  public void shouldNotApproveRequisitionIfIdempotencyKeyHasWrongFormat() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_WRONG_FORMAT);

    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();
    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(wrongUuidFormat);
    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);
  }

  @Test
  public void shouldNotApproveRequisitionWithUsedIdempotencyKey() {
    exception.expect(IdempotencyKeyException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_ALREADY_USED);

    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();
    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    when(processedRequestsRedisRepository.exists(key)).thenReturn(true);
    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);
  }

  @Test
  public void shouldApproveAuthorizedRequisitionWithParentNode() {
    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();

    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);

    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verify(requisitionService, times(1)).doApprove(eq(parentNodeId), any(),
        any(), eq(authorizedRequsition), eq(emptyList()));

    verifyZeroInteractions(stockEventBuilderBuilder, stockEventService);
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldApproveSplittableRequisition() {
    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();

    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);

    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);

    SupervisoryNodeDto partnerNode = DtoGenerator.of(SupervisoryNodeDto.class);
    when(supervisoryNodeReferenceDataService.findByIds(Sets.newHashSet(partnerNode.getId())))
        .thenReturn(Lists.newArrayList(partnerNode));

    Requisition partnerRequisition = mock(Requisition.class);
    when(partnerRequisition.getSupervisoryNodeId()).thenReturn(partnerNode.getId());
    when(partnerRequisition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);

    RequisitionSplitResult splitResult = new RequisitionSplitResult(
        authorizedRequsition, Lists.newArrayList(partnerRequisition));

    when(requisitionSplitter.split(authorizedRequsition, parentNodeId)).thenReturn(splitResult);

    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService)
        .validateCanApproveRequisition(any(Requisition.class), any(UUID.class));

    verify(requisitionService).doApprove(eq(parentNodeId), any(),
        any(), eq(authorizedRequsition), eq(emptyList()));
    verify(requisitionService).doApprove(eq(partnerNode.getParentNodeId()), any(),
        any(), eq(partnerRequisition), eq(emptyList()));

    verifyZeroInteractions(stockEventBuilderBuilder, stockEventService);
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldApproveAuthorizedRequisitionWithParentNodeAndWithoutSupplyLineForProgram() {
    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();

    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);

    when(supplyLineReferenceDataService.search(authorizedRequsition.getProgramId(),
        authorizedRequsition.getSupervisoryNodeId())).thenReturn(null);
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.IN_APPROVAL);

    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verify(requisitionService, times(1)).doApprove(eq(parentNodeId), any(),
        any(), eq(authorizedRequsition), eq(null));

    verifyZeroInteractions(stockEventBuilderBuilder, stockEventService);
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldApproveAuthorizedRequisitionWithParentNodeAndSupplyLineForProgram() {
    SupervisoryNodeDto supervisoryNode = mockSupervisoryNodeForApprove();

    UUID parentNodeId = UUID.randomUUID();
    ObjectReferenceDto parentNode = mock(ObjectReferenceDto.class);
    when(parentNode.getId()).thenReturn(parentNodeId);
    supervisoryNode.setParentNode(parentNode);

    final SupplyLineDto supplyLineDto = prepareForApproveWithSupplyLine();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verify(requisitionService, times(1)).doApprove(eq(parentNodeId), any(),
        any(), eq(authorizedRequsition), eq(singletonList(supplyLineDto)));
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldApproveAuthorizedRequisitionWithoutParentNode() {
    final SupplyLineDto supplyLineDto = prepareForApproveWithSupplyLine();
    when(authorizedRequsition.getEmergency()).thenReturn(false);
    StockEventDto stockEventDto = DtoGenerator.of(StockEventDto.class);
    when(stockEventBuilderBuilder.fromRequisition(any(Requisition.class), any(), anyMap()))
        .thenReturn(stockEventDto);

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verify(stockEventBuilderBuilder).fromRequisition(authorizedRequsition,
        currentUser.getId(), Maps.newHashMap());
    verify(stockEventService).submit(stockEventDto);
    verify(requisitionService, times(1)).doApprove(eq(null), any(),
        any(), eq(authorizedRequsition), eq(singletonList(supplyLineDto)));
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldNotSendStockEventOnFinalApprovalForEmergencyRequisition() {
    final SupplyLineDto supplyLineDto = prepareForApproveWithSupplyLine();
    when(authorizedRequsition.getEmergency()).thenReturn(true);

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verifyZeroInteractions(stockEventBuilderBuilder, stockEventService);
    verify(requisitionService, times(1)).doApprove(eq(null), any(),
        any(), eq(authorizedRequsition), eq(singletonList(supplyLineDto)));
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldNotSendStockEventWhenRequisitionIsConfiguredToPullFromStockCards() {
    final SupplyLineDto supplyLineDto = prepareForApproveWithSupplyLine();
    when(template.isPopulateStockOnHandFromStockCards()).thenReturn(true);

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verifyZeroInteractions(stockEventBuilderBuilder, stockEventService);
    verify(requisitionService, times(1)).doApprove(eq(null), any(),
        any(), eq(authorizedRequsition), eq(singletonList(supplyLineDto)));
    verify(authorizedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldNotApproveInvalidRequisition() {
    setUpApprover();
    doReturn(fieldErrors)
        .when(authorizedRequsition)
        .validateCanChangeStatus(any(LocalDate.class), anyBoolean(), anyMap(), anyMap());

    assertThatThrownBy(() ->
        requisitionController.approveRequisition(authorizedRequsition.getId(), request, response))
        .isInstanceOf(BindingResultException.class)
        .hasMessage(bindingResultMessage);

    verifyNoApproveOrUpdate(authorizedRequsition);
  }

  @Test
  public void shouldAutomaticallyConvertIfLocalFulfillsIsSet() {
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.APPROVED);

    SupplyLineDto supplyLineDto = prepareSupplyLine(authorizedRequsition, true, true);
    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    ReleasableRequisitionDto entry = new ReleasableRequisitionDto(uuid4,
        supplyLineDto.getSupplyingFacility().getId());
    ImmutableList<ReleasableRequisitionDto> list = ImmutableList.of(entry);
    verify(requisitionService).convertToOrder(eq(list), any(UserDto.class));
  }

  @Test
  public void shouldNotAutomaticallyConvertIfSupplyingFacilityDoesNotSupportProgram() {
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.APPROVED);

    prepareSupplyLine(authorizedRequsition, true, false);
    setUpApprover();

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, never()).convertToOrder(anyList(), any(UserDto.class));
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldNotApproveIfHasNoPermission() {
    mockSupervisoryNodeForApprove();

    PermissionMessageException exception = mock(PermissionMessageException.class);
    doThrow(exception).when(requisitionService).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);
  }

  @Test
  public void shouldRejectRequisitionWhenUserCanApproveRequisition() {
    when(permissionService.canApproveRequisition(authorizedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionService.reject(authorizedRequsition, Collections.emptyMap()))
        .thenReturn(initiatedRequsition);

    requisitionController.rejectRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).reject(authorizedRequsition, Collections.emptyMap());
  }

  @Test
  public void shouldNotRejectRequisitionWhenUserCanNotApproveRequisition() {
    doReturn(ValidationResult.noPermission("notAuthorized"))
        .when(permissionService).canApproveRequisition(authorizedRequsition);

    assertThatThrownBy(() ->
        requisitionController.rejectRequisition(authorizedRequsition.getId(), request, response))
        .isInstanceOf(PermissionMessageException.class);

    verify(requisitionService, times(0)).reject(authorizedRequsition, Collections.emptyMap());
  }

  @Test
  public void shouldRejectRequisitionWithIdempotencyKey() {
    when(permissionService.canApproveRequisition(authorizedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionService.reject(authorizedRequsition, Collections.emptyMap()))
        .thenReturn(initiatedRequsition);
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());

    requisitionController.rejectRequisition(authorizedRequsition.getId(), request, response);

    verify(response, times(1)).addHeader(
        HttpHeaders.LOCATION, baseUrl + API_URL + RESOURCE_URL + '/' + uuid1.toString());
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, null);
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, uuid1);
  }

  @Test
  public void shouldNotRejectRequisitionIfIdempotencyKeyHasWrongFormat() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_WRONG_FORMAT);

    when(permissionService.canApproveRequisition(authorizedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionService.reject(authorizedRequsition, Collections.emptyMap()))
        .thenReturn(initiatedRequsition);
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(wrongUuidFormat);

    requisitionController.rejectRequisition(authorizedRequsition.getId(), request, response);
  }

  @Test
  public void shouldNotRejectRequisitionWithUsedIdempotencyKey() {
    exception.expect(IdempotencyKeyException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_ALREADY_USED);

    when(permissionService.canApproveRequisition(authorizedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionService.reject(authorizedRequsition, Collections.emptyMap()))
        .thenReturn(initiatedRequsition);
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    when(processedRequestsRedisRepository.exists(key)).thenReturn(true);

    requisitionController.rejectRequisition(authorizedRequsition.getId(), request, response);
  }

  @Test
  public void shouldCallRequisitionStatusNotifierWhenReject() {
    when(permissionService.canApproveRequisition(authorizedRequsition))
        .thenReturn(ValidationResult.success());
    when(requisitionService.reject(authorizedRequsition, Collections.emptyMap()))
        .thenReturn(initiatedRequsition);

    requisitionController.rejectRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionStatusNotifier)
        .notifyStatusChanged(initiatedRequsition, LocaleContextHolder.getLocale());
  }

  @Test
  public void shouldProcessStatusChangeWhenApprovingRequisition() throws Exception {
    when(requisitionService.validateCanApproveRequisition(any(Requisition.class),
        any(UUID.class)))
        .thenReturn(ValidationResult.success());

    requisitionController.approveRequisition(authorizedRequsition.getId(), request, response);

    verify(requisitionService, times(1)).validateCanApproveRequisition(
        any(Requisition.class),
        any(UUID.class));

    verify(requisitionStatusProcessor)
        .statusChange(authorizedRequsition, LocaleContextHolder.getLocale());
  }

  @Test
  public void shouldAuthorizeRequisitionWithIdempotencyKey() {
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    setUpAuthorizer();

    requisitionController.authorizeRequisition(submittedRequsition.getId(), request, response);

    verify(response, times(1)).addHeader(
        HttpHeaders.LOCATION, baseUrl + API_URL + RESOURCE_URL + '/' + uuid1.toString());
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, null);
    verify(processedRequestsRedisRepository, times(1)).addOrUpdate(key, uuid1);
  }

  @Test
  public void shouldNotAuthorizeRequisitionIfIdempotencyKeyHasWrongFormat() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_WRONG_FORMAT);

    setUpAuthorizer();
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(wrongUuidFormat);

    requisitionController.authorizeRequisition(submittedRequsition.getId(), request, response);
  }

  @Test
  public void shouldNotAuthorizeRequisitionWithUsedIdempotencyKey() {
    exception.expect(IdempotencyKeyException.class);
    exception.expectMessage(IDEMPOTENCY_KEY_ALREADY_USED);

    setUpAuthorizer();
    when(request.getHeader(IDEMPOTENCY_KEY_HEADER)).thenReturn(key.toString());
    when(processedRequestsRedisRepository.exists(key)).thenReturn(true);

    requisitionController.authorizeRequisition(submittedRequsition.getId(), request, response);
  }

  @Test
  public void shouldProcessStatusChangeWhenAuthorizingRequisition() {
    setUpAuthorizer();

    requisitionController.authorizeRequisition(submittedRequsition.getId(), request, response);

    verify(requisitionStatusProcessor)
        .statusChange(submittedRequsition, LocaleContextHolder.getLocale());
  }

  @Test
  public void shouldCallValidationsWhenAuthorizingRequisition() {
    setUpAuthorizer();

    requisitionController.authorizeRequisition(submittedRequsition.getId(), request, response);

    verify(submittedRequsition)
        .validateCanChangeStatus(dateHelper.getCurrentDateWithSystemZone(),
            true, Maps.newHashMap(), Maps.newHashMap());
  }

  @Test
  public void shouldNotAuthorizeInvalidRequisition() {
    setUpAuthorizer();
    doReturn(fieldErrors)
        .when(submittedRequsition)
        .validateCanChangeStatus(any(LocalDate.class), anyBoolean(), anyMap(), anyMap());

    assertThatThrownBy(() ->
        requisitionController.authorizeRequisition(submittedRequsition.getId(), request, response))
        .isInstanceOf(BindingResultException.class)
        .hasMessage(bindingResultMessage);

    verifyNoAuthorizeOrUpdate(submittedRequsition);
  }

  @Test
  public void shouldAssignInitialSupervisoryNodeToRequisition() {
    Requisition requisition = new Requisition();
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    requisitionController.callStatusChangeProcessor(mock(Profiler.class), requisition);

    assertEquals(supervisoryNode.getId(), requisition.getSupervisoryNodeId());
  }

  @Test
  public void shouldNotOverwriteSupervisoryNodeWhenOneIsAssigned() {
    Requisition requisition = new Requisition();
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    UUID assignedSupervisoryNode = UUID.randomUUID();
    requisition.setSupervisoryNodeId(assignedSupervisoryNode);

    requisitionController.callStatusChangeProcessor(mock(Profiler.class), requisition);

    verify(supervisoryNodeReferenceDataService, never())
        .findSupervisoryNode(any(UUID.class), any(UUID.class));
    assertEquals(assignedSupervisoryNode, requisition.getSupervisoryNodeId());
  }

  @Test
  public void shouldNotAssignSupervisoryNodeWhenRequisitionIsNotReadyForApproval() {
    Requisition requisition = new Requisition();
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    requisitionController.callStatusChangeProcessor(mock(Profiler.class), requisition);

    verify(supervisoryNodeReferenceDataService, never())
        .findSupervisoryNode(any(UUID.class), any(UUID.class));
    assertNull(requisition.getSupervisoryNodeId());
  }

  @Test
  public void findRequisitionShouldHaveHiddenColumnsInTemplateForReportOnlyRequisition() {
    //given
    RequisitionTemplate repositoryTemplate = new RequisitionTemplateDataBuilder()
        .withAllColumns()
        .withAdditionalQuantityRequiredColumnDisplayed()
        .build();
    Requisition repositoryRequisition = new Requisition(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), RequisitionStatus.INITIATED, false);
    repositoryRequisition.setReportOnly(true);
    repositoryRequisition.setTemplate(repositoryTemplate);
    when(requisitionRepository.findOne(any(UUID.class))).thenReturn(repositoryRequisition);

    //when
    Requisition foundRequisition = requisitionController.findRequisition(UUID.randomUUID(),
        mock(Profiler.class));

    //then
    RequisitionTemplate foundTemplate = foundRequisition.getTemplate();
    for (String columnName : ORDER_RELATED_COLUMNS) {
      try {
        assertNotEquals(
            repositoryTemplate.findColumn(columnName).getIsDisplayed(),
            foundTemplate.findColumn(columnName).getIsDisplayed());
      } catch (ValidationMessageException vme) {
        assertTrue(vme.getMessage().contains("requisition.error.columnNotInTemplate"));
      }
    }
  }

  private void mockDependenciesForSubmit() {
    when(permissionService.canSubmitRequisition(initiatedRequsition))
        .thenReturn(ValidationResult.success());

    when(initiatedRequsition.getTemplate()).thenReturn(template);
    when(requisitionRepository.findOne(uuid1)).thenReturn(initiatedRequsition);
    when(programReferenceDataService.findOne(any(UUID.class))).thenReturn(
        new ProgramDtoDataBuilder().buildWithNotSkippedAuthorizationStep());
  }

  private void mockFindSupervisoryNodeByProgramAndFacility() {
    supervisoryNode = new SupervisoryNodeDtoDataBuilder().buildAsDto();

    when(supervisoryNodeReferenceDataService.findSupervisoryNode(any(), any()))
        .thenReturn(supervisoryNode);
  }

  private SupervisoryNodeDto mockSupervisoryNodeForApprove() {
    UUID supervisoryNodeId = UUID.randomUUID();
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDtoDataBuilder().buildAsDto();
    when(supervisoryNodeReferenceDataService.findOne(supervisoryNodeId))
        .thenReturn(supervisoryNodeDto);
    when(authorizedRequsition.getSupervisoryNodeId()).thenReturn(supervisoryNodeId);
    return supervisoryNodeDto;
  }

  private List<RequisitionPeriodDto> generateProcessingPeriods() {
    RequisitionPeriodDto period = new RequisitionPeriodDtoDataBuilder()
        .withId(uuid1)
        .buildAsDto();
    RequisitionPeriodDto period2 = new RequisitionPeriodDtoDataBuilder()
        .withId(uuid2)
        .buildAsDto();
    RequisitionPeriodDto period3 = new RequisitionPeriodDtoDataBuilder()
        .withId(uuid3)
        .buildAsDto();
    RequisitionPeriodDto period4 = new RequisitionPeriodDtoDataBuilder()
        .withId(uuid4)
        .buildAsDto();
    RequisitionPeriodDto period5 = new RequisitionPeriodDtoDataBuilder()
        .withId(uuid5)
        .buildAsDto();

    List<RequisitionPeriodDto> periods = new ArrayList<>();
    periods.add(period);
    periods.add(period2);
    periods.add(period3);
    periods.add(period4);
    periods.add(period5);

    return periods;
  }

  private void mockRequisitionRepository() {
    when(requisitionRepository.searchRequisitions(uuid1, facilityUuid, programUuid, false))
        .thenReturn(new ArrayList<>());
    when(requisitionRepository.searchRequisitions(uuid2, facilityUuid, programUuid, false))
        .thenReturn(Collections.singletonList(initiatedRequsition));
    when(requisitionRepository.searchRequisitions(uuid3, facilityUuid, programUuid, false))
        .thenReturn(Collections.singletonList(submittedRequsition));
    when(requisitionRepository.searchRequisitions(uuid4, facilityUuid, programUuid, false))
        .thenReturn(Collections.singletonList(authorizedRequsition));
    when(requisitionRepository.searchRequisitions(uuid5, facilityUuid, programUuid, false))
        .thenReturn(Collections.singletonList(approvedRequsition));
    when(requisitionRepository.save(initiatedRequsition))
        .thenReturn(initiatedRequsition);
    when(requisitionRepository.findOne(uuid1))
        .thenReturn(initiatedRequsition);
    when(requisitionRepository.findOne(authorizedRequsition.getId()))
        .thenReturn(authorizedRequsition);
    when(requisitionRepository.findOne(submittedRequsition.getId()))
        .thenReturn(submittedRequsition);
  }

  private void verifyNoSubmitOrUpdate(Requisition requisition) {
    verifyNoMoreInteractions(requisitionService);
    verify(requisition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
    verify(requisition, never()).validateCanBeUpdated(any(RequisitionValidationService.class));
    verify(requisition, never()).submit(eq(emptyMap()), any(UUID.class), anyBoolean());
  }

  private void verifyNoApproveOrUpdate(Requisition requisition) {
    verify(requisition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
    verify(requisition, never()).validateCanBeUpdated(any(RequisitionValidationService.class));
    verify(requisition, never())
        .approve(any(UUID.class), anyMap(), anyCollection(), any(UUID.class));
  }

  private void verifyNoAuthorizeOrUpdate(Requisition requisition) {
    verify(requisition, never()).updateFrom(
        any(Requisition.class), anyMap(), anyMap(), anyBoolean());
    verify(requisition, never()).validateCanBeUpdated(any(RequisitionValidationService.class));
    verify(requisition, never())
        .authorize(anyMap(), any(UUID.class));
  }

  private void verifySupervisoryNodeWasNotUpdated(Requisition requisition) {
    verify(requisition, never()).setSupervisoryNodeId(any());
    assertNull(requisition.getSupervisoryNodeId());
  }

  private void setUpApprover() {
    when(requisitionService.validateCanApproveRequisition(any(Requisition.class),
        any(UUID.class)))
        .thenReturn(ValidationResult.success());
  }

  private void setUpAuthorizer() {
    when(permissionService.canAuthorizeRequisition(submittedRequsition))
        .thenReturn(ValidationResult.success());
  }

  private SupplyLineDto prepareSupplyLine(Requisition requisition, boolean locallyFulfills,
      boolean withSupportedProgram) {
    SupplyLineDto supplyLine = new SupplyLineDtoDataBuilder().buildAsDto();

    FacilityDto facility = new FacilityDtoDataBuilder()
        .withId(supplyLine.getSupplyingFacility().getId())
        .buildAsDto();

    SupportedProgramDto supportedProgram = new SupportedProgramDtoDataBuilder()
        .withSupportLocallyFulfilled(locallyFulfills)
        .buildAsDto();

    when(supplyLineReferenceDataService.search(
        requisition.getProgramId(), requisition.getSupervisoryNodeId()))
        .thenReturn(Collections.singletonList(supplyLine));
    when(facilityReferenceDataService.findOne(supplyLine.getSupplyingFacility().getId()))
        .thenReturn(facility);
    when(facilitySupportsProgramHelper.getSupportedProgram(facility, requisition.getProgramId()))
        .thenReturn(withSupportedProgram ? supportedProgram : null);

    return supplyLine;
  }

  private SupplyLineDto prepareForApproveWithSupplyLine() {
    when(authorizedRequsition.getStatus()).thenReturn(RequisitionStatus.APPROVED);
    final SupplyLineDto supplyLineDto = prepareSupplyLine(authorizedRequsition, false, true);
    setUpApprover();
    return supplyLineDto;
  }

}
