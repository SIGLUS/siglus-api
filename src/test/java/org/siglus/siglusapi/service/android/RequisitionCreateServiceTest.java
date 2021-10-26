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

package org.siglus.siglusapi.service.android;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.siglusapi.constant.AndroidConstants.SCHEDULE_CODE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionTemplateService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.web.PermissionMessageException;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.config.AndroidTemplateConfigProperties;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.siglus.siglusapi.dto.TestConsumptionProjectDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.dto.UsageInformationOrderableDto;
import org.siglus.siglusapi.dto.UsageInformationServiceDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.request.UsageInformationLineItemRequest;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.web.android.FileBasedTest;
import org.springframework.beans.BeanUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionCreateServiceTest extends FileBasedTest {

  @InjectMocks
  private RequisitionCreateService service;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private RequisitionTemplateService requisitionTemplateService;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeService;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;

  @Mock
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private PermissionService permissionService;

  @Mock
  private RegimenRepository regimenRepository;

  @Mock
  private AndroidTemplateConfigProperties androidTemplateConfigProperties;

  @Mock
  private SyncUpHashRepository syncUpHashRepository;

  @Mock
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Mock
  private SiglusProgramAdditionalOrderableService additionalOrderableService;

  @Captor
  private ArgumentCaptor<Requisition> requisitionArgumentCaptor;

  @Captor
  private ArgumentCaptor<SiglusRequisitionDto> siglusRequisitionDtoArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionLineItemExtension> requisitionLineItemExtensionArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionV2Dto> requisitionV2DtoArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionExtension> requisitionExtensionArgumentCaptor;

  @Captor
  private ArgumentCaptor<SyncUpHash> syncUpHashArgumentCaptor;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID malariaProgramId = UUID.randomUUID();
  private final UUID mmiaProgramId = UUID.randomUUID();
  private final UUID rapidTestProgramId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID mlOrderableId = UUID.randomUUID();
  private final UUID mmiaOrderableId = UUID.randomUUID();
  private final UUID rapidTestOrderableId = UUID.randomUUID();
  private final UUID templateId = UUID.fromString("610a52a5-2217-4fb7-9e8e-90bba3051d4d");
  private final UUID mmiaTemplateId = UUID.fromString("873c25d6-e53b-11eb-8494-acde48001122");
  private final UUID malariaTemplateId = UUID.fromString("3f2245ce-ee9f-11eb-ba79-acde48001122");
  private final UUID rapidtestTemplateId = UUID.fromString("2c10856e-eead-11eb-9718-acde48001122");
  private final UUID supervisoryNodeId = UUID.randomUUID();
  private final UUID processingPeriodId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID regimenId = UUID.randomUUID();
  private final UUID regimenId2 = UUID.randomUUID();
  private final UUID regimenSummaryCommunityId = UUID.randomUUID();
  private final UUID regimenSummaryPatientId = UUID.randomUUID();
  private final UUID totalSummaryCommunityId = UUID.randomUUID();
  private final UUID totalSummaryPatientId = UUID.randomUUID();
  private final String community = "community";
  private final String patients = "patients";
  private final String newColumn0 = "newColumn0";
  private final String newColumn1 = "newColumn1";
  private final String newColumn2 = "newColumn2";
  private final String newColumn3 = "newColumn3";
  private final String newColumn4 = "newColumn4";
  private final String newColumn5 = "newColumn5";
  private final String newColumn = "new";
  private final String total = "total";
  private final String hivDetermine = "hivDetermine";
  private final String hf = "HF";
  private final String apes = "APES";
  private final String consumo = "consumo";
  private final String positive = "positive";
  private final String unjustified = "unjustified";
  private final String vcProductCode = "02A01";
  private final String mlProductCode = "08O05";

  @Before
  public void prepare() {
    Locale.setDefault(Locale.ENGLISH);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

    Set<UUID> androidTemplateIds = new HashSet<>();
    androidTemplateIds.add(templateId);
    androidTemplateIds.add(mmiaTemplateId);
    androidTemplateIds.add(malariaTemplateId);
    androidTemplateIds.add(rapidtestTemplateId);
    when(androidTemplateConfigProperties.getAndroidTemplateIds()).thenReturn(androidTemplateIds);
    when(androidTemplateConfigProperties.findAndroidTemplateId(any())).thenReturn(templateId);
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    ApprovedProductDto productDto = createApprovedProductDto(orderableId);
    when(requisitionService.getApproveProduct(facilityId, programId, false))
        .thenReturn(new ApproveProductsAggregator(singletonList(productDto), programId));
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(templateId);
    when(requisitionTemplateService.findTemplateById(templateId)).thenReturn(template);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setProductCode(vcProductCode);
    when(siglusOrderableService.getOrderableByCode(vcProductCode)).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeService.findSupervisoryNode(programId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findSupervisoryNode(malariaProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findSupervisoryNode(mmiaProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findOne(supervisoryNodeId)).thenReturn(supervisoryNodeDto);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(buildRequisitionTemplateExtension());
    when(processingPeriodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, YearMonth.of(2021, 6)))
        .thenReturn(buildProcessingPeriod());
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildRequisition(template));
    when(siglusUsageReportService.initiateUsageReport(requisitionV2DtoArgumentCaptor.capture()))
        .thenReturn(buildSiglusRequisitionDto());
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId))
        .thenReturn(requisitionExtension);
    when(requisitionExtensionRepository.saveAndFlush(requisitionExtensionArgumentCaptor.capture()))
        .thenReturn(requisitionExtension);
    when(syncUpHashRepository.findOne(anyString())).thenReturn(null);
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(programId));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, programId))
        .thenReturn(Collections.singletonList(mockApprovedProduct(vcProductCode)));
    when(additionalOrderableService.searchAdditionalOrderables(any())).thenReturn(emptyList());

    ProgramDto program = Mockito.mock(ProgramDto.class);
    when(program.getId()).thenReturn(programId);
    when(siglusProgramService.getProgramByCode("VC")).thenReturn(Optional.of(program));
    ProgramDto malariaProgram = Mockito.mock(ProgramDto.class);
    when(malariaProgram.getId()).thenReturn(malariaProgramId);
    when(siglusProgramService.getProgramByCode("ML")).thenReturn(Optional.of(malariaProgram));
    ProgramDto mmiaProgram = Mockito.mock(ProgramDto.class);
    when(mmiaProgram.getId()).thenReturn(mmiaProgramId);
    when(siglusProgramService.getProgramByCode("T")).thenReturn(Optional.of(mmiaProgram));
    ProgramDto rapidTestProgram = Mockito.mock(ProgramDto.class);
    when(rapidTestProgram.getId()).thenReturn(rapidTestProgramId);
    when(siglusProgramService.getProgramByCode("TR")).thenReturn(Optional.of(rapidTestProgram));
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowExceptionWhenCreateRequisitionFromAndroidIfMissInitRequisitionPermission() {
    // given
    ValidationResult success = ValidationResult.success();
    ValidationResult fail = ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, "INIT");
    when(permissionService.canInitRequisition(programId, facilityId)).thenReturn(fail);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);

    // when
    service.createRequisition(buildRequisitionCreateRequest());
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowExceptionWhenCreateRequisitionFromAndroidIfMissSubmitRequisitionPermission() {
    // given
    ValidationResult success = ValidationResult.success();
    ValidationResult fail = ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, "SUBMIT");
    when(permissionService.canInitRequisition(programId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(fail);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);

    // when
    service.createRequisition(buildRequisitionCreateRequest());
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowExceptionWhenCreateRequisitionFromAndroidIfMissAuthorizeRequisitionPermission() {
    // given
    ValidationResult success = ValidationResult.success();
    ValidationResult fail = ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, "AUTHORIZE");
    when(permissionService.canInitRequisition(programId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(fail);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);

    // when
    service.createRequisition(buildRequisitionCreateRequest());
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowExceptionWhenCreateRequisitionFromAndroidIfMissInternalApproveRequisitionPermission() {
    // given
    ValidationResult success = ValidationResult.success();
    ValidationResult fail = ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, "APPROVE");
    when(permissionService.canInitRequisition(programId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(fail);

    // when
    service.createRequisition(buildRequisitionCreateRequest());
  }

  @Test
  public void shouldSave4TimesRequisitionWhenCreateRequisitionFromAndroid() {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(programId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);

    // when
    service.createRequisition(buildRequisitionCreateRequest());

    // then
    verify(requisitionRepository, times(1)).save(requisitionArgumentCaptor.capture());
    verify(requisitionRepository, times(3)).saveAndFlush(requisitionArgumentCaptor.capture());
    verify(siglusRequisitionExtensionService).buildRequisitionExtension(requisitionId, false, facilityId);
    verify(siglusUsageReportService).initiateUsageReport(any());
    verify(siglusUsageReportService).saveUsageReport(any(), any());
    verify(requisitionLineItemExtensionRepository).save(requisitionLineItemExtensionArgumentCaptor.capture());
    verify(syncUpHashRepository).save(syncUpHashArgumentCaptor.capture());
  }

  @Test
  public void shouldSkipSaveRequisitionWhenCreateRequisitionIfAlreadySaved() {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(programId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);
    when(syncUpHashRepository.findOne(anyString())).thenReturn(new SyncUpHash("hash-code"));

    // when
    service.createRequisition(buildRequisitionCreateRequest());

    // then
    verify(requisitionRepository, times(0)).saveAndFlush(requisitionArgumentCaptor.capture());
    verify(siglusRequisitionExtensionService, times(0)).buildRequisitionExtension(requisitionId, false, facilityId);
    verify(siglusUsageReportService, times(0)).initiateUsageReport(any());
    verify(siglusUsageReportService, times(0)).saveUsageReport(any(), any());
    verify(requisitionLineItemExtensionRepository, times(0))
        .saveAndFlush(requisitionLineItemExtensionArgumentCaptor.capture());
    verify(syncUpHashRepository, times(0)).saveAndFlush(syncUpHashArgumentCaptor.capture());
  }

  @Test
  public void shouldGetTotalValueWhenCreateRequisitionFromAndroid() {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(malariaProgramId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);

    when(androidTemplateConfigProperties.findAndroidTemplateId("ML")).thenReturn(malariaTemplateId);
    RequisitionTemplate mlTemplate = new RequisitionTemplate();
    mlTemplate.setId(malariaTemplateId);
    ApprovedProductDto productDto = createApprovedProductDto(mlOrderableId);
    when(requisitionService.getApproveProduct(facilityId, malariaProgramId, true))
        .thenReturn(new ApproveProductsAggregator(singletonList(productDto), malariaProgramId));
    when(requisitionTemplateService.findTemplateById(malariaTemplateId)).thenReturn(mlTemplate);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildMlRequisition(mlTemplate));
    when(siglusOrderableService.getOrderableByCode(mlProductCode)).thenReturn(buildOrderableDto());
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(buildOrderableDto()));
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(malariaProgramId));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, malariaProgramId))
        .thenReturn(Collections.singletonList(mockApprovedProduct(mlProductCode)));

    // when
    service.createRequisition(buildMlRequisitionCreateRequest());

    // then
    verify(siglusUsageReportService).saveUsageReport(siglusRequisitionDtoArgumentCaptor.capture(), any());
    SiglusRequisitionDto siglusRequisitionDto = siglusRequisitionDtoArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(3), getTotalValue(siglusRequisitionDto, "existentStock"));
    assertEquals(3, siglusRequisitionDto.getUsageInformationLineItems().size());
  }

  @Test
  public void shouldEqualsValueWhenCreateMmiaRequisitionFromAndroid() throws IOException {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(mmiaProgramId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);
    when(androidTemplateConfigProperties.findAndroidTemplateId("T")).thenReturn(mmiaTemplateId);
    RequisitionTemplate mmiaTemplate = new RequisitionTemplate();
    mmiaTemplate.setId(mmiaTemplateId);
    ApprovedProductDto productDto = createApprovedProductDto(mmiaOrderableId);
    when(requisitionService.getApproveProduct(facilityId, mmiaProgramId, false))
        .thenReturn(new ApproveProductsAggregator(singletonList(productDto), mmiaProgramId));
    when(requisitionTemplateService.findTemplateById(mmiaTemplateId)).thenReturn(mmiaTemplate);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildMmiaRequisition(mmiaTemplate));
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(mmiaOrderableId);
    orderableDto.setProductCode("08S01ZW");
    when(siglusOrderableService.getOrderableByCode("08S01ZW")).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    when(regimenRepository.findAllByProgramIdAndActiveTrue(any())).thenReturn(buildRegimenDto());
    when(siglusUsageReportService.initiateUsageReport(any())).thenReturn(buildMmiaSiglusRequisitionDto());
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(mmiaProgramId));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, mmiaProgramId))
        .thenReturn(Collections.singletonList(mockApprovedProduct("08S01ZW")));

    // when
    service.createRequisition(parseParam("buildMmiaRequisitionCreateRequest.json"));

    // then
    verify(siglusUsageReportService).saveUsageReport(siglusRequisitionDtoArgumentCaptor.capture(), any());
    SiglusRequisitionDto siglusRequisitionDto = siglusRequisitionDtoArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(6),
        getRegimenLineDto(total, siglusRequisitionDto).getColumns().get(community).getValue());

    assertEquals(Integer.valueOf(5),
        getRegimenSummaryLineDto(newColumn1, siglusRequisitionDto).getColumns().get(patients).getValue());
    assertEquals(Integer.valueOf(12),
        getRegimenSummaryLineDto(total, siglusRequisitionDto).getColumns().get(community).getValue());

    assertEquals(Integer.valueOf(21),
        getPatientGroupDto("newSection2", siglusRequisitionDto).getColumns().get(newColumn0).getValue());
    assertEquals(Integer.valueOf(135),
        getPatientGroupDto("newSection2", siglusRequisitionDto).getColumns().get(total).getValue());
    assertEquals(Integer.valueOf(29),
        getPatientGroupDto("newSection5", siglusRequisitionDto).getColumns().get(newColumn1).getValue());
    assertEquals(Integer.valueOf(81),
        getPatientGroupDto("newSection6", siglusRequisitionDto).getColumns().get(newColumn0).getValue());
    assertEquals(Integer.valueOf(3),
        getPatientGroupDto("newSection7", siglusRequisitionDto).getColumns().get(newColumn).getValue());
  }

  @Test
  public void shouldEqualsValueWhenCreateRapidTestRequisitionFromAndroid() throws IOException {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(rapidTestProgramId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);
    when(androidTemplateConfigProperties.findAndroidTemplateId("TR")).thenReturn(rapidtestTemplateId);
    RequisitionTemplate trTemplate = new RequisitionTemplate();
    trTemplate.setId(rapidtestTemplateId);
    ApprovedProductDto productDto = createApprovedProductDto(rapidTestOrderableId);
    when(requisitionService.getApproveProduct(facilityId, rapidTestProgramId, false))
        .thenReturn(new ApproveProductsAggregator(singletonList(productDto), rapidTestProgramId));
    when(requisitionTemplateService.findTemplateById(rapidtestTemplateId)).thenReturn(trTemplate);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildMmiaRequisition(trTemplate));
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(rapidTestOrderableId);
    orderableDto.setProductCode("08A07");
    when(siglusOrderableService.getOrderableByCode("08A07")).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    when(siglusUsageReportService.initiateUsageReport(any())).thenReturn(buildRapidTestSiglusRequisitionDto());
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(rapidTestProgramId));
    when(approvedProductReferenceDataService.getApprovedProducts(facilityId, rapidTestProgramId))
        .thenReturn(Collections.singletonList(mockApprovedProduct("08A07")));

    // when
    service.createRequisition(parseParam("buildRapidTestRequisitionCreateRequest.json"));

    // then
    verify(siglusUsageReportService).saveUsageReport(siglusRequisitionDtoArgumentCaptor.capture(), any());
    SiglusRequisitionDto siglusRequisitionDto = siglusRequisitionDtoArgumentCaptor.getValue();
    assertEquals(Integer.valueOf(5),
        getTestOutcomeDto(newColumn5, hivDetermine, consumo, siglusRequisitionDto).getValue());
    assertEquals(Integer.valueOf(2),
        getTestOutcomeDto(hf, newColumn0, positive, siglusRequisitionDto).getValue());
    assertEquals(Integer.valueOf(7),
        getTestOutcomeDto(apes, newColumn2, unjustified, siglusRequisitionDto).getValue());
    assertEquals(Integer.valueOf(7),
        getTestOutcomeDto(total, newColumn1, unjustified, siglusRequisitionDto).getValue());
  }

  private TestConsumptionOutcomeDto getTestOutcomeDto(String service, String project, String outcome,
      SiglusRequisitionDto siglusRequisitionDto) {
    List<TestConsumptionOutcomeDto> outcomeDto = siglusRequisitionDto.getTestConsumptionLineItems().stream()
        .filter(item -> service.equals(item.getService()))
        .map(p -> p.getProjects().get(project).getOutcomes().get(outcome))
        .collect(Collectors.toList());
    return outcomeDto.get(0);
  }

  private RegimenLineDto getRegimenLineDto(String name, SiglusRequisitionDto siglusRequisitionDto) {
    return siglusRequisitionDto.getRegimenLineItems().stream()
        .filter(lineItem -> lineItem.getName() != null && name.equals(lineItem.getName()))
        .findFirst()
        .orElse(new RegimenLineDto());
  }

  private RegimenSummaryLineDto getRegimenSummaryLineDto(String name, SiglusRequisitionDto siglusRequisitionDto) {
    return siglusRequisitionDto.getRegimenSummaryLineItems().stream()
        .filter(summary -> name.equals(summary.getName()))
        .findFirst()
        .orElse(new RegimenSummaryLineDto());
  }

  private PatientGroupDto getPatientGroupDto(String name, SiglusRequisitionDto siglusRequisitionDto) {
    return siglusRequisitionDto.getPatientLineItems().stream()
        .filter(patient -> name.equals(patient.getName()))
        .findFirst()
        .orElse(new PatientGroupDto());
  }

  private ApprovedProductDto createApprovedProductDto(UUID orderableId) {
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(1L);
    org.openlmis.fulfillment.service.referencedata.OrderableDto orderable =
        new org.openlmis.fulfillment.service.referencedata.OrderableDto();
    orderable.setId(orderableId);
    org.openlmis.fulfillment.web.util.MetadataDto newMeta = new org.openlmis.fulfillment.web.util.MetadataDto();
    BeanUtils.copyProperties(meta, newMeta);
    orderable.setMeta(newMeta);
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    productDto.setOrderable(convertOrderableDto(orderable));
    return productDto;
  }

  private org.openlmis.requisition.dto.OrderableDto convertOrderableDto(
      org.openlmis.fulfillment.service.referencedata.OrderableDto sourceDto) {
    org.openlmis.requisition.dto.OrderableDto orderableDto = new org.openlmis.requisition.dto.OrderableDto();
    BeanUtils.copyProperties(sourceDto, orderableDto);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setFullSupply(true);
    orderableDto.setPrograms(Sets.newHashSet(programOrderableDto));
    return orderableDto;
  }

  private RequisitionCreateRequest buildRequisitionCreateRequest() {
    return RequisitionCreateRequest.builder()
        .programCode("VC")
        .clientSubmittedTime(Instant.parse("2021-07-21T07:59:59Z"))
        .emergency(false)
        .actualStartDate(LocalDate.of(2021, 6, 21))
        .actualEndDate(LocalDate.of(2021, 7, 20))
        .consultationNumber(20)
        .products(buildProducts())
        .signatures(buildSignatures())
        .build();
  }

  private List<RequisitionLineItemRequest> buildProducts() {
    RequisitionLineItemRequest product = RequisitionLineItemRequest.builder()
        .productCode(vcProductCode)
        .beginningBalance(200)
        .totalReceivedQuantity(20)
        .totalConsumedQuantity(14)
        .stockOnHand(202)
        .requestedQuantity(28)
        .authorizedQuantity(30)
        .build();
    return singletonList(product);
  }

  private List<RequisitionSignatureRequest> buildSignatures() {
    RequisitionSignatureRequest signature1 = RequisitionSignatureRequest.builder()
        .type("SUBMITTER")
        .name("zhangsan")
        .build();
    RequisitionSignatureRequest signature2 = RequisitionSignatureRequest.builder()
        .type("APPROVER")
        .name("lisi")
        .build();
    return Arrays.asList(signature1, signature2);
  }

  private RequisitionTemplateExtension buildRequisitionTemplateExtension() {
    RequisitionTemplateExtension templateExtension = new RequisitionTemplateExtension();
    templateExtension.setRequisitionTemplateId(templateId);
    templateExtension.setEnableConsultationNumber(true);
    templateExtension.setEnableKitUsage(true);
    templateExtension.setEnableProduct(true);
    templateExtension.setEnablePatientLineItem(false);
    templateExtension.setEnableRapidTestConsumption(false);
    templateExtension.setEnableRegimen(false);
    templateExtension.setEnableUsageInformation(false);
    templateExtension.setEnableQuicklyFill(false);
    return templateExtension;
  }

  private Requisition buildRequisition(RequisitionTemplate template) {
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setFacilityId(facilityId);
    requisition.setProgramId(programId);
    requisition.setEmergency(false);
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setRequisition(requisition);
    VersionEntityReference orderable = new VersionEntityReference();
    orderable.setId(orderableId);
    requisitionLineItem.setOrderable(orderable);
    requisition.setRequisitionLineItems(singletonList(requisitionLineItem));
    requisition.setTemplate(template);
    return requisition;
  }

  private Optional<ProcessingPeriod> buildProcessingPeriod() {
    ProcessingPeriod processingPeriod = new ProcessingPeriod();
    processingPeriod.setId(processingPeriodId);
    return Optional.of(processingPeriod);
  }

  private SiglusRequisitionDto buildSiglusRequisitionDto() {
    ConsultationNumberGroupDto consultationNumberGroupDto = new ConsultationNumberGroupDto();
    consultationNumberGroupDto.setName("number");
    Map<String, ConsultationNumberColumnDto> consultationNumberColumnDtoMap = new HashMap<>();
    consultationNumberColumnDtoMap.put("consultationNumber", new ConsultationNumberColumnDto(UUID.randomUUID(), 20));
    consultationNumberGroupDto.setColumns(consultationNumberColumnDtoMap);
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    requisitionDto.setConsultationNumberLineItems(singletonList(consultationNumberGroupDto));
    return requisitionDto;
  }

  private SiglusRequisitionDto buildRapidTestSiglusRequisitionDto() {
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    requisitionDto.setTestConsumptionLineItems(buildTestConsumptionServiceDto());
    return requisitionDto;
  }

  private List<TestConsumptionServiceDto> buildTestConsumptionServiceDto() {
    TestConsumptionServiceDto consumptionNewColumn5 = new TestConsumptionServiceDto();
    consumptionNewColumn5.setService(newColumn5);
    consumptionNewColumn5.setProjects(buildConsumptionProjectMap());
    TestConsumptionServiceDto consumptionNewHf = new TestConsumptionServiceDto();
    consumptionNewHf.setService(hf);
    consumptionNewHf.setProjects(buildConsumptionProjectMap());
    TestConsumptionServiceDto consumptionNewApe = new TestConsumptionServiceDto();
    consumptionNewApe.setService(apes);
    consumptionNewApe.setProjects(buildConsumptionProjectMap());
    TestConsumptionServiceDto consumptionNewTotal = new TestConsumptionServiceDto();
    consumptionNewTotal.setService(total);
    consumptionNewTotal.setProjects(buildConsumptionProjectMap());
    return Arrays.asList(consumptionNewColumn5, consumptionNewHf, consumptionNewApe, consumptionNewTotal);
  }

  private Map<String, TestConsumptionProjectDto> buildConsumptionProjectMap() {
    TestConsumptionProjectDto hivDetermineDto = new TestConsumptionProjectDto();
    hivDetermineDto.setProject(hivDetermine);
    hivDetermineDto.setOutcomes(buildOutcomeMap());
    TestConsumptionProjectDto newColumnDto0 = new TestConsumptionProjectDto();
    newColumnDto0.setProject(newColumn0);
    newColumnDto0.setOutcomes(buildOutcomeMap());
    TestConsumptionProjectDto newColumnDto1 = new TestConsumptionProjectDto();
    newColumnDto1.setProject(newColumn1);
    newColumnDto1.setOutcomes(buildOutcomeMap());
    TestConsumptionProjectDto newColumnDto2 = new TestConsumptionProjectDto();
    newColumnDto2.setProject(newColumn2);
    newColumnDto2.setOutcomes(buildOutcomeMap());
    Map<String, TestConsumptionProjectDto> projects = new HashMap<>();
    projects.put(hivDetermine, hivDetermineDto);
    projects.put(newColumn0, newColumnDto0);
    projects.put(newColumn1, newColumnDto1);
    projects.put(newColumn2, newColumnDto2);
    return projects;
  }

  private Map<String, TestConsumptionOutcomeDto> buildOutcomeMap() {
    TestConsumptionOutcomeDto consumoOutcome = TestConsumptionOutcomeDto.builder().outcome(consumo).build();
    TestConsumptionOutcomeDto positiveOutcome = TestConsumptionOutcomeDto.builder().outcome(positive).build();
    TestConsumptionOutcomeDto unjustifiedOutcome = TestConsumptionOutcomeDto.builder().outcome(unjustified).build();
    Map<String, TestConsumptionOutcomeDto> outcomes = new HashMap<>();
    outcomes.put(consumo, consumoOutcome);
    outcomes.put(positive, positiveOutcome);
    outcomes.put(unjustified, unjustifiedOutcome);
    return outcomes;
  }

  private SiglusRequisitionDto buildMmiaSiglusRequisitionDto() {
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    requisitionDto.setRegimenSummaryLineItems(buildRegimenSummaryLineDto());
    requisitionDto.setPatientLineItems(buildPatientGroupDto());
    return requisitionDto;
  }

  private List<Regimen> buildRegimenDto() {
    Regimen regimen = new Regimen();
    regimen.setId(regimenId);
    regimen.setCode("1alt1");
    Regimen regimen2 = new Regimen();
    regimen2.setId(regimenId2);
    regimen2.setCode("A2Cped");
    return Arrays.asList(regimen, regimen2);
  }

  private List<RegimenSummaryLineDto> buildRegimenSummaryLineDto() {
    Map<String, RegimenColumnDto> columnDto0 = new HashMap<>();
    columnDto0.put(community, RegimenColumnDto.builder().id(regimenSummaryCommunityId).build());
    columnDto0.put(patients, RegimenColumnDto.builder().id(regimenSummaryPatientId).build());
    RegimenSummaryLineDto dtoNewColumn0 = new RegimenSummaryLineDto();
    dtoNewColumn0.setName(newColumn0);
    dtoNewColumn0.setColumns(columnDto0);

    Map<String, RegimenColumnDto> columnDto1 = new HashMap<>();
    columnDto1.put(community, RegimenColumnDto.builder().id(regimenSummaryCommunityId).build());
    columnDto1.put(patients, RegimenColumnDto.builder().id(regimenSummaryPatientId).build());
    RegimenSummaryLineDto dtoNewColumn1 = new RegimenSummaryLineDto();
    dtoNewColumn1.setName(newColumn1);
    dtoNewColumn1.setColumns(columnDto1);

    Map<String, RegimenColumnDto> linhas = new HashMap<>();
    linhas.put(community, RegimenColumnDto.builder().id(regimenSummaryCommunityId).build());
    linhas.put(patients, RegimenColumnDto.builder().id(regimenSummaryPatientId).build());
    RegimenSummaryLineDto dtoLinhas = new RegimenSummaryLineDto();
    dtoLinhas.setName("1stLinhas");
    dtoLinhas.setColumns(linhas);

    Map<String, RegimenColumnDto> totalColumns = new HashMap<>();
    totalColumns.put(community, RegimenColumnDto.builder().id(totalSummaryCommunityId).build());
    totalColumns.put(patients, RegimenColumnDto.builder().id(totalSummaryPatientId).build());
    RegimenSummaryLineDto totalSummaryLineDto = new RegimenSummaryLineDto();
    totalSummaryLineDto.setName(total);
    totalSummaryLineDto.setColumns(totalColumns);
    return Arrays.asList(dtoNewColumn0, dtoNewColumn1, dtoLinhas, totalSummaryLineDto);
  }

  private List<PatientGroupDto> buildPatientGroupDto() {
    Map<String, PatientColumnDto> newSection0 = new HashMap<>();
    newSection0.put(newColumn, new PatientColumnDto());
    newSection0.put(newColumn0, new PatientColumnDto());
    newSection0.put(newColumn1, new PatientColumnDto());
    newSection0.put(newColumn2, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection0 = new PatientGroupDto();
    groupDtoNewSection0.setName("newSection0");
    groupDtoNewSection0.setColumns(newSection0);

    Map<String, PatientColumnDto> newSection1 = new HashMap<>();
    newSection1.put(newColumn, new PatientColumnDto());
    newSection1.put(newColumn0, new PatientColumnDto());
    newSection1.put(newColumn1, new PatientColumnDto());
    newSection1.put(total, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection1 = new PatientGroupDto();
    groupDtoNewSection1.setName("newSection1");
    groupDtoNewSection1.setColumns(newSection1);

    Map<String, PatientColumnDto> newSection2 = new HashMap<>();
    newSection2.put(newColumn, new PatientColumnDto());
    newSection2.put(newColumn0, new PatientColumnDto());
    newSection2.put(newColumn1, new PatientColumnDto());
    newSection2.put(newColumn2, new PatientColumnDto());
    newSection2.put(newColumn3, new PatientColumnDto());
    newSection2.put(newColumn4, new PatientColumnDto());
    newSection2.put(total, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection2 = new PatientGroupDto();
    groupDtoNewSection2.setName("newSection2");
    groupDtoNewSection2.setColumns(newSection2);

    Map<String, PatientColumnDto> newSection3 = new HashMap<>();
    newSection3.put(newColumn, new PatientColumnDto());
    newSection3.put(newColumn0, new PatientColumnDto());
    newSection3.put(newColumn1, new PatientColumnDto());
    newSection3.put(total, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection3 = new PatientGroupDto();
    groupDtoNewSection3.setName("newSection3");
    groupDtoNewSection3.setColumns(newSection3);

    Map<String, PatientColumnDto> newSection4 = new HashMap<>();
    newSection4.put(newColumn, new PatientColumnDto());
    newSection4.put(total, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection4 = new PatientGroupDto();
    groupDtoNewSection4.setName("newSection4");
    groupDtoNewSection4.setColumns(newSection4);

    Map<String, PatientColumnDto> newSection5 = new HashMap<>();
    newSection5.put(newColumn, new PatientColumnDto());
    newSection5.put(newColumn0, new PatientColumnDto());
    newSection5.put(newColumn1, new PatientColumnDto());
    newSection5.put(total, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection5 = new PatientGroupDto();
    groupDtoNewSection5.setName("newSection5");
    groupDtoNewSection5.setColumns(newSection5);

    Map<String, PatientColumnDto> newSection6 = new HashMap<>();
    newSection6.put(newColumn, new PatientColumnDto());
    newSection6.put(newColumn0, new PatientColumnDto());
    newSection6.put(newColumn1, new PatientColumnDto());
    newSection6.put(total, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection6 = new PatientGroupDto();
    groupDtoNewSection6.setName("newSection6");
    groupDtoNewSection6.setColumns(newSection6);

    Map<String, PatientColumnDto> newSection7 = new HashMap<>();
    newSection7.put(newColumn, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection7 = new PatientGroupDto();
    groupDtoNewSection7.setName("newSection7");
    groupDtoNewSection7.setColumns(newSection7);

    Map<String, PatientColumnDto> patientType = new HashMap<>();
    patientType.put(newColumn, new PatientColumnDto());
    patientType.put(newColumn0, new PatientColumnDto());
    patientType.put(newColumn1, new PatientColumnDto());
    patientType.put(newColumn2, new PatientColumnDto());
    patientType.put(newColumn3, new PatientColumnDto());
    PatientGroupDto groupDtoPatientType = new PatientGroupDto();
    groupDtoPatientType.setName("patientType");
    groupDtoPatientType.setColumns(patientType);

    return Arrays
        .asList(groupDtoNewSection0, groupDtoNewSection1, groupDtoNewSection2, groupDtoNewSection3, groupDtoNewSection4,
            groupDtoNewSection5, groupDtoNewSection6, groupDtoNewSection7, groupDtoPatientType);
  }

  private RequisitionCreateRequest buildMlRequisitionCreateRequest() {
    return RequisitionCreateRequest.builder()
        .programCode("ML")
        .clientSubmittedTime(Instant.parse("2021-07-21T07:59:59Z"))
        .emergency(false)
        .actualStartDate(LocalDate.of(2021, 6, 21))
        .actualEndDate(LocalDate.of(2021, 7, 20))
        .usageInformationLineItems(buildUsageInfoLineItemRequest())
        .signatures(buildSignatures())
        .build();
  }

  private List<RegimenLineItemRequest> buildRegimenLineItemRequest() {
    RegimenLineItemRequest regimenLineItem1 = RegimenLineItemRequest.builder()
        .code("1alt1")
        .name("ABC+3TC+DTG")
        .patientsOnTreatment(1)
        .comunitaryPharmacy(2)
        .build();
    RegimenLineItemRequest regimenLineItem2 = RegimenLineItemRequest.builder()
        .code("A2Cped")
        .name("AZT+3TC+ABC (2FDC+ABC Baby)")
        .patientsOnTreatment(3)
        .comunitaryPharmacy(4)
        .build();
    return Arrays.asList(regimenLineItem1, regimenLineItem2);
  }

  private List<RegimenLineItemRequest> buildRegimenSummaryLineItemRequest() {
    RegimenLineItemRequest regimenSummary1 = RegimenLineItemRequest.builder()
        .code("key_regime_3lines_1")
        .patientsOnTreatment(5)
        .comunitaryPharmacy(6)
        .build();
    RegimenLineItemRequest regimenSummary2 = RegimenLineItemRequest.builder()
        .code("key_regime_3lines_2")
        .patientsOnTreatment(7)
        .comunitaryPharmacy(8)
        .build();
    RegimenLineItemRequest regimenSummary3 = RegimenLineItemRequest.builder()
        .code("key_regime_3lines_3")
        .patientsOnTreatment(9)
        .comunitaryPharmacy(10)
        .build();
    return Arrays.asList(regimenSummary1, regimenSummary2, regimenSummary3);
  }

  private List<UsageInformationLineItemRequest> buildUsageInfoLineItemRequest() {
    UsageInformationLineItemRequest usageInfo1 = UsageInformationLineItemRequest.builder()
        .productCode(mlProductCode)
        .information("existentStock")
        .hf(1)
        .chw(2)
        .build();
    UsageInformationLineItemRequest usageInfo2 = UsageInformationLineItemRequest.builder()
        .productCode(mlProductCode)
        .information("treatmentsAttended")
        .hf(3)
        .chw(4)
        .build();
    return Arrays.asList(usageInfo1, usageInfo2);
  }

  private Requisition buildMlRequisition(RequisitionTemplate template) {
    VersionEntityReference orderable = new VersionEntityReference(malariaProgramId, 2L);
    ApprovedProductReference avalibleProduct = new ApprovedProductReference(orderable,
        any(VersionEntityReference.class));
    Set<ApprovedProductReference> set = new HashSet<>();
    set.add(avalibleProduct);
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setFacilityId(facilityId);
    requisition.setProgramId(malariaProgramId);
    requisition.setEmergency(false);
    requisition.setRequisitionLineItems(Collections.emptyList());
    requisition.setTemplate(template);
    requisition.setAvailableProducts(set);
    return requisition;
  }

  private Requisition buildMmiaRequisition(RequisitionTemplate template) {
    VersionEntityReference orderable = new VersionEntityReference(mmiaOrderableId, 2L);
    ApprovedProductReference avalibleProduct = new ApprovedProductReference(orderable,
        any(VersionEntityReference.class));
    Set<ApprovedProductReference> set = new HashSet<>();
    set.add(avalibleProduct);
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setFacilityId(facilityId);
    requisition.setProgramId(mmiaProgramId);
    requisition.setEmergency(false);
    requisition.setRequisitionLineItems(Collections.emptyList());
    requisition.setTemplate(template);
    requisition.setAvailableProducts(set);
    return requisition;
  }

  private OrderableDto buildOrderableDto() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(mlOrderableId);
    return orderableDto;
  }

  private Integer getTotalValue(SiglusRequisitionDto requisitionDto, String informationCode) {
    Optional<UsageInformationServiceDto> siglusRequisitionDto = requisitionDto.getUsageInformationLineItems().stream()
        .filter(t -> total.equals(t.getService())).findFirst();
    return siglusRequisitionDto.map(
        m -> m.getInformations().get(informationCode).getOrderables().values().stream().findFirst()
            .map(UsageInformationOrderableDto::getValue)).orElse(Optional.of(-1)).get();
  }

  private RequisitionCreateRequest parseParam(String fileName) throws IOException {
    String json = readFromFile(fileName);
    return objectMapper.readValue(json, RequisitionCreateRequest.class);
  }

  private ApprovedProductDto mockApprovedProduct(String productCode) {
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setId(UUID.randomUUID());
    org.openlmis.requisition.dto.OrderableDto orderableDto = new org.openlmis.requisition.dto.OrderableDto();
    orderableDto.setId(UUID.randomUUID());
    orderableDto.setProductCode(productCode);
    approvedProductDto.setOrderable(orderableDto);
    return approvedProductDto;
  }

}