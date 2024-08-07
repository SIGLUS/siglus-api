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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.PermissionStrings;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.dto.SimpleRequisitionDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.FacilityNativeRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RequisitionNativeSqlRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.dto.FacillityStockCardDateDto;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.testutils.ProcessingPeriodDtoDataBuilder;
import org.siglus.siglusapi.testutils.RequisitionLineItemDataBuilder;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.SiglusProcessingPeriodValidator;
import org.siglus.siglusapi.web.response.RequisitionPeriodExtensionResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField", "checkstyle:LineLength"})
public class SiglusProcessingPeriodServiceTest {

  @Mock
  private SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  @Mock
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Mock
  private SiglusProcessingPeriodExtensionService siglusProcessingPeriodExtensionService;

  @Mock
  private SiglusProcessingPeriodValidator siglusProcessingPeriodValidator;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private PermissionService permissionService;

  @Mock
  private PeriodService periodService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private PermissionStrings permissionStrings;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private SiglusReportTypeRepository reportTypeRepository;

  @Mock
  private SiglusFacilityService siglusFacilityService;

  @Mock
  private FacilityNativeRepository facilityNativeRepository;

  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;

  @Mock
  private RequisitionNativeSqlRepository requisitionNativeSqlRepository;
  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  private final ProcessingPeriodDtoDataBuilder builder = new ProcessingPeriodDtoDataBuilder();
  private final ProcessingPeriodDto fullDto = builder.buildFullDto();
  private final ProcessingPeriodDto periodDto = builder.buildDto();
  private final ProcessingPeriodExtension extension = builder.buildExtenstion();
  private final ProcessingPeriodDto prePeriodDto = builder.buildPerDto();
  private final ProcessingPeriodExtension preExtension = builder.buildPreExtenstion();

  private final UUID processingScheduleId = UUID.randomUUID();
  private final int page = 0;
  private final int size = 10;
  private final Sort sort = new Sort("startDate");

  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID requisitionId2 = UUID.randomUUID();
  private final String extraDataJsonString = "{\"isSaved\":false,\"signaure\":{\"submit\":\"kkk\",\"approve\":[\"lll\",\"dprole2ApproveSig\"],\"authorize\":\"kkk\"},\"actualEndDate\":\"2022-04-18\",\"actualStartDate\":\"2022-03-18\",\"clientSubmittedTime\":\"2022-04-18T13:07:45.982Z\"}";

  private static final String AUTHORIZED = "AUTHORIZED";
  private static final String IN_APPROVAL = "IN_APPROVAL";
  private static final String APPROVED = "APPROVED";
  private static final String RELEASED = "RELEASED";
  private static final String RELEASED_WITHOUT_ORDER = "RELEASED_WITHOUT_ORDER";

  @Test
  public void shouldCreateProcessingPeriodIfPassValidation() {

    when(siglusProcessingPeriodReferenceDataService.saveProcessingPeriod(fullDto))
        .thenReturn(fullDto);
    when(processingPeriodExtensionRepository.save(extension))
        .thenReturn(extension);

    ProcessingPeriodDto response =
        siglusProcessingPeriodService.createProcessingPeriod(fullDto);

    verify(siglusProcessingPeriodReferenceDataService)
        .saveProcessingPeriod(fullDto);
    verify(processingPeriodExtensionRepository).save(extension);
    assertEquals(response, fullDto);
  }

  @Test
  public void shouldGetProcessingPeriods() {
    MultiValueMap<String, Object> map = new LinkedMultiValueMap();
    List<Object> list = new ArrayList<>();
    list.add(processingScheduleId.toString());
    map.put("processingScheduleId", list);

    List<ProcessingPeriodDto> processingPeriodDtos = new ArrayList<>();
    processingPeriodDtos.add(periodDto);

    PageImplRepresentation<ProcessingPeriodDto> pageImpl = new PageImplRepresentation();
    pageImpl.setContent(processingPeriodDtos);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);

    PageRequest pageRequest = new PageRequest(page, size, sort);
    when(siglusProcessingPeriodReferenceDataService.searchProcessingPeriods(
        processingScheduleId, null, null, null, null,
        Collections.emptySet(), pageRequest)).thenReturn(pageImpl);
    when(siglusProcessingPeriodExtensionService.findAll()).thenReturn(extensions);

    Page<ProcessingPeriodDto> response = siglusProcessingPeriodService
        .getAllProcessingPeriods(map, pageRequest);

    verify(siglusProcessingPeriodReferenceDataService)
        .searchProcessingPeriods(processingScheduleId, null,
            null, null, null, Collections.emptySet(), pageRequest);
    assertEquals(response.getContent().get(0), fullDto);
  }

  @Test
  public void shouldGetProcessingPeriodById() {
    UUID periodId = periodDto.getId();

    when(siglusProcessingPeriodReferenceDataService.findOne(periodId)).thenReturn(periodDto);
    when(processingPeriodExtensionRepository
        .findByProcessingPeriodId(periodId)).thenReturn(extension);

    ProcessingPeriodDto response = siglusProcessingPeriodService.getProcessingPeriodDto(periodId);

    verify(siglusProcessingPeriodReferenceDataService).findOne(periodId);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(periodId);
    assertEquals(response, fullDto);
  }

  @Test
  public void shouldKeepUntouchedIfNoExtensionWhenGetProcessingPeriodById() {
    UUID periodId = periodDto.getId();

    when(siglusProcessingPeriodReferenceDataService.findOne(periodId)).thenReturn(periodDto);
    when(processingPeriodExtensionRepository
        .findByProcessingPeriodId(periodId)).thenReturn(null);

    ProcessingPeriodDto response = siglusProcessingPeriodService.getProcessingPeriodDto(periodId);

    verify(siglusProcessingPeriodReferenceDataService).findOne(periodId);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(periodId);
    assertEquals(response, periodDto);

  }

  @Test
  public void shouldGetProcessingPeriodsForInitiateOfRegularRequisition() {
    setupReportType();
    mockFacility(facilityId, "DDM");
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    Collection<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(periodDto);
    when(periodService.searchByProgramAndFacility(any(), any())).thenReturn(periods);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(extensions);
    when(siglusProcessingPeriodExtensionService.findAll()).thenReturn(extensions);

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, false));
    when(siglusRequisitionRepository.findRequisitionsByFacilityIdAndProgramIdOrderByPeriod(facilityId, programId))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());
    when(requisitionNativeSqlRepository.findSimpleRequisitionDto(any())).thenReturn(buildEmptySimpleRequisitionDtos());

    Collection<RequisitionPeriodExtensionResponse> actualResponseLists =
        siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, false);

    assertEquals(1, actualResponseLists.size());
    RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(fullDto);
    requisitionPeriod.setRequisitionId(requisitionId);
    requisitionPeriod.setRequisitionStatus(RequisitionStatus.INITIATED);
    RequisitionPeriodExtensionResponse expectedResponse = convertToRequisitionPeriodExtensionResponse(
        requisitionPeriod);
    assertTrue(actualResponseLists.contains(expectedResponse));
  }

  @Test
  public void shouldGetProcessingPeriodsForEmergencyRequisitionWhenHaveProcessForPrePeriod() {
    //given
    setupReportType();
    extension.setSubmitStartDate(LocalDate.now().minusDays(3));
    extension.setSubmitEndDate(LocalDate.now().minusDays(7));
    preExtension.setSubmitStartDate(LocalDate.now().minusDays(23));
    preExtension.setSubmitEndDate(LocalDate.now().minusDays(13));
    when(periodService.getCurrentPeriods(any(), any()))
        .thenReturn(Arrays.asList(periodDto));
    when(periodService.searchByProgramAndFacility(any(), any()))
        .thenReturn(Arrays.asList(prePeriodDto, periodDto));
    when(processingPeriodExtensionRepository.findAll())
        .thenReturn(Arrays.asList(preExtension, extension));
    when(siglusProcessingPeriodExtensionService.findAll())
        .thenReturn(Arrays.asList(preExtension, extension));

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, true));
    Requisition preRequisition = createRequisition(UUID.randomUUID(), RequisitionStatus.INITIATED,
        true);
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId,
        true)).thenReturn(Arrays.asList(preRequisition));
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId, true))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    List<Requisition> authorizedRequisitions = new ArrayList<>();
    authorizedRequisitions.add(createRequisition(requisitionId2, RequisitionStatus.AUTHORIZED,
        false));
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId, false))
        .thenReturn(authorizedRequisitions);
    mockProgram(programId, "VC");
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);

    mockFacility(facilityId, "DDM");
    when(periodService.findPreviousPeriod(periodDto.getId())).thenReturn(prePeriodDto);

    //when
    List<RequisitionPeriodExtensionResponse> response =
        siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, true);

    //then
    assertEquals(1, response.size());
    assertEquals("perPeriod", response.get(0).getName());
    assertEquals(LocalDate.of(2020, 7, 24), response.get(0).getSubmitEndDate());
  }

  @Test
  public void shouldGetProcessingPeriodsForEmergencyRequisitionWhenEmptyForPrePeriod() {
    //given
    setupReportType();
    extension.setSubmitStartDate(LocalDate.now().minusDays(3));
    extension.setSubmitEndDate(LocalDate.now().minusDays(7));
    preExtension.setSubmitStartDate(LocalDate.now().minusDays(23));
    preExtension.setSubmitEndDate(LocalDate.now().minusDays(13));
    when(periodService.getCurrentPeriods(any(), any()))
        .thenReturn(Arrays.asList(periodDto));
    when(periodService.searchByProgramAndFacility(any(), any()))
        .thenReturn(Arrays.asList(prePeriodDto, periodDto));
    when(processingPeriodExtensionRepository.findAll())
        .thenReturn(Arrays.asList(preExtension, extension));
    when(siglusProcessingPeriodExtensionService.findAll())
        .thenReturn(Arrays.asList(preExtension, extension));

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED,
        true));
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId,
        true)).thenReturn(Collections.emptyList());
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId,
        true)).thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    List<Requisition> authorizedRequisitions = new ArrayList<>();
    authorizedRequisitions.add(createRequisition(requisitionId2, RequisitionStatus.AUTHORIZED,
        false));
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId, false))
        .thenReturn(authorizedRequisitions);
    mockProgram(programId, "VC");
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    mockFacility(facilityId, "DPM");
    when(periodService.findPreviousPeriod(periodDto.getId())).thenReturn(prePeriodDto);
    when(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(facilityId, programId,
        prePeriodDto.getId(), false, newHashSet(AUTHORIZED, IN_APPROVAL, APPROVED,
            RELEASED, RELEASED_WITHOUT_ORDER)))
        .thenReturn(authorizedRequisitions);
    //when
    List<RequisitionPeriodExtensionResponse> response =
        siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, true);

    //then
    assertEquals(1, response.size());
    assertEquals(LocalDate.of(2020, 7, 16), response.get(0).getSubmitStartDate());
    assertEquals(LocalDate.of(2020, 8, 30), response.get(0).getSubmitEndDate());
  }

  @Test
  public void shouldGetProcessingPeriodsForEmergencyRequisitionWhenOnlyHaveOnePeriods() {
    //given
    setupReportType();
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    extension.setSubmitStartDate(LocalDate.now().minusDays(3));
    extension.setSubmitEndDate(LocalDate.now().minusDays(7));
    preExtension.setSubmitStartDate(LocalDate.now().minusDays(23));
    preExtension.setSubmitEndDate(LocalDate.now().minusDays(13));
    when(periodService.findPreviousPeriod(periodDto.getId())).thenReturn(prePeriodDto);
    List<ProcessingPeriodDto> currentPeriods = new ArrayList<>();
    currentPeriods.add(periodDto);
    when(periodService.getCurrentPeriods(any(), any())).thenReturn(currentPeriods);
    List<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(periodDto);
    periods.add(prePeriodDto);
    when(periodService.searchByProgramAndFacility(any(), any())).thenReturn(periods);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);
    extensions.add(preExtension);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(extensions);
    when(siglusProcessingPeriodExtensionService.findAll()).thenReturn(extensions);

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, true));
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId, true))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    List<Requisition> authorizedRequisitions = new ArrayList<>();
    authorizedRequisitions.add(createRequisition(requisitionId2, RequisitionStatus.AUTHORIZED,
        false));
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId, false))
        .thenReturn(authorizedRequisitions);
    mockProgram(programId, "VC");
    when(requisitionNativeSqlRepository.findSimpleRequisitionDto(any())).thenReturn(
        buildSimpleRequisitionDtosWithExtraDataEmpty());

    RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(fullDto);
    requisitionPeriod.setRequisitionId(requisitionId);
    requisitionPeriod.setRequisitionStatus(RequisitionStatus.INITIATED);
    requisitionPeriod.setCurrentPeriodRegularRequisitionAuthorized(true);
    RequisitionPeriodExtensionResponse expectedResponse = convertToRequisitionPeriodExtensionResponse(
        requisitionPeriod);
    expectedResponse.setSubmitStartDate(LocalDate.of(2020, 7, 11));
    expectedResponse.setSubmitEndDate(LocalDate.of(2020, 7, 17));
    mockFacility(facilityId, "CS");

    //when
    Collection<RequisitionPeriodExtensionResponse> actualResponseList =
        siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, true);

    //then
    assertEquals(1, actualResponseList.size());
    RequisitionPeriodExtensionResponse response = actualResponseList.stream().findFirst().get();
    assertEquals(LocalDate.of(2020, 7, 11), response.getSubmitStartDate());
    assertEquals(LocalDate.of(2020, 7, 17), response.getSubmitEndDate());
  }

  @Test
  public void shouldReturnWithRequisitionResponseWhenAfterInitialed() {
    setupReportType();
    mockFacility(facilityId, "DDM");
    Collection<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(periodDto);
    when(periodService.searchByProgramAndFacility(any(), any())).thenReturn(periods);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(extensions);

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, false));
    when(siglusRequisitionRepository.findRequisitionsByFacilityIdAndProgramIdOrderByPeriod(facilityId, programId))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());
    when(requisitionNativeSqlRepository.findSimpleRequisitionDto(any())).thenReturn(buildSimpleRequisitionDtos());
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);

    List<RequisitionPeriodExtensionResponse> actualResponseLists =
        siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, false);

    assertEquals(1, actualResponseLists.size());
    assertNotNull(actualResponseLists.get(0).getRequisitionExtraData());
  }

  private List<SimpleRequisitionDto> buildEmptySimpleRequisitionDtos() {
    return Lists.newArrayList();
  }

  private List<SimpleRequisitionDto> buildSimpleRequisitionDtosWithExtraDataEmpty() {
    SimpleRequisitionDto simpleRequisitionDto = SimpleRequisitionDto.builder()
        .id(requisitionId)
        .extraData(null)
        .build();
    return Lists.newArrayList(simpleRequisitionDto);
  }

  private List<SimpleRequisitionDto> buildSimpleRequisitionDtos() {
    SimpleRequisitionDto simpleRequisitionDto = SimpleRequisitionDto.builder()
        .id(requisitionId)
        .extraData(extraDataJsonString)
        .build();
    return Lists.newArrayList(simpleRequisitionDto);
  }

  private RequisitionPeriodExtensionResponse convertToRequisitionPeriodExtensionResponse(RequisitionPeriodDto dto) {
    RequisitionPeriodExtensionResponse response = new RequisitionPeriodExtensionResponse();
    BeanUtils.copyProperties(dto, response);
    return response;
  }

  private Requisition createRequisition(UUID requisitionId, RequisitionStatus status,
      boolean isEmergency) {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .build();
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setProgramId(programId);
    requisition.setFacilityId(facilityId);
    requisition.setProcessingPeriodId(periodDto.getId());
    requisition.setRequisitionLineItems(Lists.newArrayList(lineItem));
    requisition.setStatus(status);
    requisition.setEmergency(isEmergency);
    requisition.setCreatedDate(ZonedDateTime.now());
    return requisition;
  }

  private void setupReportType() {
    String programCode = "TARV";
    SiglusReportType reportType = mock(SiglusReportType.class);
    when(reportType.getStartDate()).thenReturn(LocalDate.of(2020, 1, 1));
    mockProgram(programId, programCode);
    when(reportTypeRepository.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(facilityId, programCode))
        .thenReturn(Optional.of(reportType));

    FacillityStockCardDateDto dto = new FacillityStockCardDateDto();
    dto.setFacilityId(facilityId);
    dto.setProgramId(programId);
    dto.setOccurredDate(java.sql.Date.valueOf("2020-01-01"));

    when(facilityNativeRepository.findFirstStockCardGroupByFacilityIdAndProgramId(facilityId, programId))
        .thenReturn(Arrays.asList(dto));
  }

  @Test
  public void shouldReturnNullWhenProgramNotViaAndEmergency() {
    ProgramDto programDto = new ProgramDto();
    programDto.setCode("RT");
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    mockProgram(programId, "RT");
    Collection<RequisitionPeriodExtensionResponse> response =
        siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, facilityId, true);
    assertEquals(new ArrayList<>(), response);
  }

  @Test
  public void shouldReturnWhenGetUpToNowMonthlyPeriods() {
    // when
    siglusProcessingPeriodService.getUpToNowMonthlyPeriods();

    // then
    verify(processingPeriodRepository).getUpToNowMonthlyPeriods(any(LocalDate.class));
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowErrorWhenClientFacilityIdWrong() {
    // given
    UserDto userDto = new UserDto();
    UUID clientFacilityId2 = UUID.randomUUID();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    Facility facility = new Facility();
    facility.setId(clientFacilityId2);
    when(siglusFacilityService.getAllClientFacilities(facilityId, programId))
        .thenReturn(Sets.newHashSet(facility));
    // when
    UUID clientFacilityId = UUID.randomUUID();
    siglusProcessingPeriodService.getRequisitionPeriodExtensionResponses(programId, clientFacilityId, false);
  }

  private ProgramDto mockProgram(UUID programId, String programCode) {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode(programCode);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);
    return programDto;
  }

  private Facility mockFacility(UUID facilityId, String facilityCode) {
    Facility facility = new Facility();
    facility.setId(facilityId);
    FacilityType facilityType = new FacilityType();
    facilityType.setCode(facilityCode);
    facility.setType(facilityType);
    when(siglusFacilityRepository.findOne(facilityId)).thenReturn(facility);
    return facility;
  }
}


