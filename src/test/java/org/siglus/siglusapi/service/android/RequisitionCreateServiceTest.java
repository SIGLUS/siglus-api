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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.siglusapi.constant.android.AndroidConstants.SCHEDULE_CODE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.KitUsageLineItems.COLLECTION_KIT_OPENED;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.KitUsageLineItems.COLLECTION_KIT_RECEIVED;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.KitUsageLineItems.SERVICE_CHW;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.KitUsageLineItems.SERVICE_HF;

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
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.RequisitionTemplateRepository;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.RequisitionTemplateService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.web.PermissionMessageException;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.SyncUpHash;
import org.siglus.siglusapi.dto.AgeGroupLineItemDto;
import org.siglus.siglusapi.dto.AgeGroupServiceDto;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.KitUsageLineItemDto;
import org.siglus.siglusapi.dto.KitUsageServiceLineItemDto;
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
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEvent;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedReplayer;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.RegimenOrderableRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SyncUpHashRepository;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramAdditionalOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusRequisitionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.web.android.FileBasedTest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
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
  private SyncUpHashRepository syncUpHashRepository;

  @Mock
  private SupportedProgramsHelper supportedProgramsHelper;

  @Mock
  private SiglusProgramAdditionalOrderableService additionalOrderableService;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  private RequisitionTemplateRepository requisitionTemplateRepository;

  @Mock
  private SiglusNotificationService siglusNotificationService;

  @Mock
  private RegimenOrderableRepository regimenOrderableRepository;

  @Mock
  private SiglusRequisitionService siglusRequisitionService;

  // private SiglusRequisitionDto siglusRequisitionDto = buildSiglusRequisitionDto();

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
  private final UUID viaTemplateId = UUID.fromString("610a52a5-2217-4fb7-9e8e-90bba3051d4d");
  private final UUID viaProgramId = UUID.randomUUID();
  private final UUID viaOrderableId = UUID.randomUUID();
  private final UUID mmiaTemplateId = UUID.fromString("873c25d6-e53b-11eb-8494-acde48001122");
  private final UUID mmiaProgramId = UUID.randomUUID();
  private final UUID mmiaOrderableId = UUID.randomUUID();
  private final UUID rapidtestTemplateId = UUID.fromString("2c10856e-eead-11eb-9718-acde48001122");
  private final UUID rapidTestProgramId = UUID.randomUUID();
  private final UUID rapidTestOrderableId = UUID.randomUUID();
  private final UUID malariaTemplateId = UUID.fromString("3f2245ce-ee9f-11eb-ba79-acde48001122");
  private final UUID malariaProgramId = UUID.randomUUID();
  private final UUID malariaOrderableId = UUID.randomUUID();
  private final UUID mmtbTemplateId = UUID.randomUUID();
  private final UUID mmtbProgramId = UUID.randomUUID();
  private final UUID mmtbOrderableId = UUID.randomUUID();
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
  private final String mmiaProductCode = "08S01ZW";
  private final String rapidTestProductCode = "08A07";
  private final String mmtbProductCode = "08H07";
  private final UUID facilityTypeId = UUID.randomUUID();
  private final LocalDate endDate = LocalDate.of(2021, 7, 20);
  RequisitionExtension requisitionExtension = new RequisitionExtension();

  @Before
  public void prepare() {
    Locale.setDefault(Locale.ENGLISH);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    ApprovedProductDto productDto = createApprovedProductDto(viaOrderableId, vcProductCode);
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, viaProgramId)).thenReturn(
        singletonList(productDto));
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(viaTemplateId);
    when(requisitionTemplateService.findTemplateById(viaTemplateId)).thenReturn(template);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(viaOrderableId);
    orderableDto.setProductCode(vcProductCode);
    when(siglusOrderableService.getOrderableByCode(vcProductCode)).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeService.findSupervisoryNode(viaProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findSupervisoryNode(malariaProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findSupervisoryNode(mmiaProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findSupervisoryNode(rapidTestProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findSupervisoryNode(mmtbProgramId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findOne(supervisoryNodeId)).thenReturn(supervisoryNodeDto);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(viaTemplateId))
        .thenReturn(buildRequisitionTemplateExtension());
    when(processingPeriodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, YearMonth.of(2021, 6)))
        .thenReturn(buildProcessingPeriod());
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildViaRequisition());
    when(siglusUsageReportService.initiateUsageReport(requisitionV2DtoArgumentCaptor.capture()))
        .thenReturn(buildSiglusRequisitionDto());
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId, viaProgramId,
        endDate)).thenReturn(requisitionExtension);
    when(requisitionExtensionRepository.saveAndFlush(requisitionExtensionArgumentCaptor.capture()))
        .thenReturn(requisitionExtension);
    when(syncUpHashRepository.findOne(anyString())).thenReturn(null);
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(Collections.singleton(viaProgramId));
    when(additionalOrderableService.searchAdditionalOrderables(any())).thenReturn(emptyList());

    ProgramDto program = Mockito.mock(ProgramDto.class);
    when(program.getId()).thenReturn(viaProgramId);
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
    ProgramDto mmtbProgram = Mockito.mock(ProgramDto.class);
    when(mmtbProgram.getId()).thenReturn(mmtbProgramId);
    when(siglusProgramService.getProgramByCode("TB")).thenReturn(Optional.of(mmtbProgram));
    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(facilityTypeId);
    FacilityDto facilityDto = FacilityDto.builder().type(facilityTypeDto).build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    RequisitionTemplate requisitionTemplate = new RequisitionTemplate();
    requisitionTemplate.setId(viaTemplateId);
    when(requisitionTemplateRepository.findTemplate(viaProgramId, facilityTypeId)).thenReturn(requisitionTemplate);
    when(requisitionTemplateRepository.findTemplate(malariaProgramId, facilityTypeId)).thenReturn(requisitionTemplate);
    when(requisitionTemplateRepository.findTemplate(mmiaProgramId, facilityTypeId)).thenReturn(requisitionTemplate);
    when(requisitionTemplateRepository.findTemplate(rapidTestProgramId, facilityTypeId))
        .thenReturn(requisitionTemplate);
    when(requisitionTemplateRepository.findTemplate(mmtbProgramId, facilityTypeId)).thenReturn(requisitionTemplate);
    doNothing().when(siglusNotificationService).postApprove(any());
    AndroidRequisitionSyncedReplayer.currentEvent.remove();
  }

  @Test
  public void shouldSetExternalRequisitionNumberWhenReplaying() {
    // given
    AndroidRequisitionSyncedEvent event =
        AndroidRequisitionSyncedEvent.builder()
            .requisitionNumber(90)
            .requisitionNumberPrefix("MTB.xxx.202210.")
            .build();
    AndroidRequisitionSyncedReplayer.currentEvent.set(event);
    RequisitionExtension extension = RequisitionExtension.builder().build();
    // when
    RequisitionCreateService.patchExtensionWhenReplaying(extension);
    // then
    assertThat(extension.getRequisitionNumberPrefix()).isEqualTo(event.getRequisitionNumberPrefix());
    assertThat(extension.getRequisitionNumber()).isEqualTo(event.getRequisitionNumber());
  }

  @Test
  public void shouldNotSetExternalRequisitionNumberWhenNotReplaying() {
    // given
    AndroidRequisitionSyncedReplayer.currentEvent.remove();
    RequisitionExtension extension = RequisitionExtension.builder().build();
    // when
    RequisitionCreateService.patchExtensionWhenReplaying(extension);
    // then
    assertThat(extension.getRequisitionNumberPrefix()).isNull();
    assertThat(extension.getRequisitionNumber()).isNull();
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowExceptionWhenCreateRequisitionFromAndroidIfMissInitRequisitionPermission() {
    // given
    ValidationResult success = ValidationResult.success();
    ValidationResult fail = ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, "INIT");
    when(permissionService.canInitRequisition(viaProgramId, facilityId)).thenReturn(fail);
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
    when(permissionService.canInitRequisition(viaProgramId, facilityId)).thenReturn(success);
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
    when(permissionService.canInitRequisition(viaProgramId, facilityId)).thenReturn(success);
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
    when(permissionService.canInitRequisition(viaProgramId, facilityId)).thenReturn(success);
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
    when(permissionService.canInitRequisition(viaProgramId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);

    // when
    service.createRequisition(buildRequisitionCreateRequest());

    // then
    verify(requisitionRepository, times(1)).save(requisitionArgumentCaptor.capture());
    verify(requisitionRepository, times(3)).saveAndFlush(requisitionArgumentCaptor.capture());
    verify(siglusRequisitionExtensionService).buildRequisitionExtension(requisitionId, false, facilityId, viaProgramId,
        endDate);
    verify(siglusUsageReportService).initiateUsageReport(any());
    verify(siglusUsageReportService).saveUsageReport(any(), any());
    verify(requisitionLineItemExtensionRepository).save(requisitionLineItemExtensionArgumentCaptor.capture());
    verify(syncUpHashRepository).save(syncUpHashArgumentCaptor.capture());
  }

  @Test
  public void shouldSkipSaveRequisitionWhenCreateRequisitionIfAlreadySaved() {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(viaProgramId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);
    when(syncUpHashRepository.findOne(anyString())).thenReturn(new SyncUpHash());

    // when
    service.createRequisition(buildRequisitionCreateRequest());

    // then
    verify(requisitionRepository, times(0)).saveAndFlush(requisitionArgumentCaptor.capture());
    verify(siglusRequisitionExtensionService, times(0)).buildRequisitionExtension(requisitionId, false, facilityId,
        viaProgramId, endDate);
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

    ApprovedProductDto productDto = createApprovedProductDto(malariaOrderableId, mlProductCode);
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, malariaProgramId)).thenReturn(
        singletonList(productDto));
    RequisitionTemplate mlTemplate = new RequisitionTemplate();
    mlTemplate.setId(malariaTemplateId);
    when(requisitionTemplateService.findTemplateById(malariaTemplateId)).thenReturn(mlTemplate);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildMalariaRaequisition());
    when(siglusOrderableService.getOrderableByCode(mlProductCode)).thenReturn(buildOrderableDto());
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(buildOrderableDto()));
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(malariaProgramId));
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId,
        malariaProgramId, endDate)).thenReturn(requisitionExtension);

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
    ApprovedProductDto productDto = createApprovedProductDto(mmiaOrderableId, mmiaProductCode);
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, mmiaProgramId)).thenReturn(
        singletonList(productDto));
    RequisitionTemplate mmiaTemplate = new RequisitionTemplate();
    mmiaTemplate.setId(mmiaTemplateId);
    when(requisitionTemplateService.findTemplateById(mmiaTemplateId)).thenReturn(mmiaTemplate);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildMmiaRequisition());
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(mmiaOrderableId);
    orderableDto.setProductCode(mmiaProductCode);
    when(siglusOrderableService.getOrderableByCode(mmiaProductCode)).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    when(regimenRepository.findAllByProgramIdAndActiveTrue(any())).thenReturn(buildRegimenDto());
    when(siglusUsageReportService.initiateUsageReport(any())).thenReturn(buildMmiaSiglusRequisitionDto());
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(mmiaProgramId));
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId,
        mmiaProgramId, endDate)).thenReturn(requisitionExtension);

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
    ApprovedProductDto productDto = createApprovedProductDto(rapidTestOrderableId, rapidTestProductCode);
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, rapidTestProgramId)).thenReturn(
        singletonList(productDto));
    RequisitionTemplate trTemplate = new RequisitionTemplate();
    trTemplate.setId(rapidtestTemplateId);
    when(requisitionTemplateService.findTemplateById(rapidtestTemplateId)).thenReturn(trTemplate);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture()))
        .thenReturn(buildRapidTestRequisition());
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(rapidTestOrderableId);
    orderableDto.setProductCode(rapidTestProductCode);
    when(siglusOrderableService.getOrderableByCode(rapidTestProductCode)).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    when(siglusUsageReportService.initiateUsageReport(any())).thenReturn(buildRapidTestSiglusRequisitionDto());
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(rapidTestProgramId));
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId,
        rapidTestProgramId, endDate)).thenReturn(requisitionExtension);

    when(regimenOrderableRepository.findByExistedMappingKey()).thenReturn(Sets.newHashSet("1"));

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

  @Test
  public void shouldEqualsValueWhenCreateMmtbFromAndroid() throws IOException {
    // given
    ValidationResult success = ValidationResult.success();
    when(permissionService.canInitRequisition(mmtbProgramId, facilityId)).thenReturn(success);
    when(permissionService.canSubmitRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canAuthorizeRequisition(any(Requisition.class))).thenReturn(success);
    when(permissionService.canApproveRequisition(any(Requisition.class))).thenReturn(success);
    ApprovedProductDto productDto = createApprovedProductDto(mmtbOrderableId, mmtbProductCode);
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, mmtbProgramId)).thenReturn(
        singletonList(productDto));
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(mmtbTemplateId);
    when(requisitionTemplateService.findTemplateById(mmtbTemplateId)).thenReturn(template);
    when(requisitionRepository.saveAndFlush(requisitionArgumentCaptor.capture())).thenReturn(buildMmtbRaequisition());
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(mmtbOrderableId);
    orderableDto.setProductCode(mmtbProductCode);
    when(siglusOrderableService.getOrderableByCode(mmtbProductCode)).thenReturn(orderableDto);
    when(siglusOrderableService.getAllProducts()).thenReturn(singletonList(orderableDto));
    when(siglusUsageReportService.initiateUsageReport(any())).thenReturn(buildMmtbSiglusRequisitionDto());
    when(supportedProgramsHelper.findHomeFacilitySupportedProgramIds())
        .thenReturn(Collections.singleton(mmtbProgramId));
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId,
        mmtbProgramId, endDate)).thenReturn(requisitionExtension);

    SiglusRequisitionDto siglusRequisitionDto2 = buildSiglusRequisitionDto();
    RequisitionLineItemV2Dto lineItemV2Dto = buildRequisitionLineItemV2Dto();
    siglusRequisitionDto2.setRequisitionLineItems(singletonList(lineItemV2Dto));
    siglusRequisitionService.initiateAndSaveRequisitionLineItems(siglusRequisitionDto2);

    // when
    service.createRequisition(parseParam("buildMmtbRequisitionCreateRequest.json"));

    // then
    verify(siglusUsageReportService).saveUsageReport(siglusRequisitionDtoArgumentCaptor.capture(), any());
    SiglusRequisitionDto siglusRequisitionDto = siglusRequisitionDtoArgumentCaptor.getValue();
    assertEquals(6, siglusRequisitionDto.getPatientLineItems().size());
    assertEquals(3, siglusRequisitionDto.getAgeGroupLineItems().size());
  }

  private RequisitionLineItemV2Dto buildRequisitionLineItemV2Dto() {
    RequisitionLineItemV2Dto lineItemV2Dto = new RequisitionLineItemV2Dto();
    UUID requisitionLineItemId = UUID.randomUUID();
    OrderableDto productDto = new OrderableDto();
    productDto.setId(UUID.randomUUID());
    lineItemV2Dto.setId(requisitionLineItemId);
    // lineItemV2Dto.setOrderable(productDto);
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setId(UUID.randomUUID());
    lineItemV2Dto.setApprovedProduct(approvedProductDto);
    lineItemV2Dto.setId(requisitionLineItemId);
    lineItemV2Dto.setAuthorizedQuantity(10);
    lineItemV2Dto.setPreviousAdjustedConsumptions(newArrayList());
    return lineItemV2Dto;
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

  private ApprovedProductDto createApprovedProductDto(UUID orderableId, String productCode) {
    org.openlmis.requisition.dto.OrderableDto orderableDto = new org.openlmis.requisition.dto.OrderableDto();
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(1L);
    orderableDto.setMeta(meta);
    orderableDto.setId(orderableId);
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setFullSupply(true);
    programOrderableDto.setProgramId(viaProgramId);
    orderableDto.setPrograms(Sets.newHashSet(programOrderableDto));
    orderableDto.setProductCode(productCode);
    productDto.setOrderable(orderableDto);
    return productDto;
  }

  private RequisitionCreateRequest buildRequisitionCreateRequest() {
    return RequisitionCreateRequest.builder()
        .programCode("VC")
        .clientSubmittedTime(Instant.parse("2021-07-21T07:59:59Z"))
        .emergency(false)
        .actualStartDate(LocalDate.of(2021, 6, 21))
        .actualEndDate(endDate)
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
    templateExtension.setRequisitionTemplateId(viaTemplateId);
    templateExtension.setEnableConsultationNumber(true);
    templateExtension.setEnableKitUsage(true);
    templateExtension.setEnableProduct(true);
    templateExtension.setEnablePatientLineItem(false);
    templateExtension.setEnableRapidTestConsumption(false);
    templateExtension.setEnableRegimen(false);
    templateExtension.setEnableUsageInformation(false);
    templateExtension.setEnableQuicklyFill(false);
    templateExtension.setEnableAgeGroup(false);
    return templateExtension;
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

    Map<String, KitUsageServiceLineItemDto> services = new HashMap<>();
    services.put(SERVICE_CHW, new KitUsageServiceLineItemDto());
    services.put(SERVICE_HF, new KitUsageServiceLineItemDto());
    KitUsageLineItemDto kitUsageLineItem1 = new KitUsageLineItemDto();
    kitUsageLineItem1.setCollection(COLLECTION_KIT_RECEIVED);
    kitUsageLineItem1.setServices(services);
    KitUsageLineItemDto kitUsageLineItem2 = new KitUsageLineItemDto();
    kitUsageLineItem2.setCollection(COLLECTION_KIT_OPENED);
    kitUsageLineItem2.setServices(services);
    requisitionDto.setKitUsageLineItems(Arrays.asList(kitUsageLineItem1, kitUsageLineItem2));
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

  private SiglusRequisitionDto buildMmtbSiglusRequisitionDto() {
    SiglusRequisitionDto requisitionDto = new SiglusRequisitionDto();
    requisitionDto.setPatientLineItems(buildMmtbPatientGroupDto());
    requisitionDto.setAgeGroupLineItems(buildMmtbAgeGroupDto());
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

  private List<PatientGroupDto> buildMmtbPatientGroupDto() {
    Map<String, PatientColumnDto> table1Columns = new HashMap<>();
    table1Columns.put("new", new PatientColumnDto());
    table1Columns.put("newColumn6", new PatientColumnDto());
    table1Columns.put("newColumn0", new PatientColumnDto());
    table1Columns.put("newColumn1", new PatientColumnDto());
    table1Columns.put("newColumn2", new PatientColumnDto());
    table1Columns.put("newColumn3", new PatientColumnDto());
    table1Columns.put("newColumn4", new PatientColumnDto());
    PatientGroupDto table1 = new PatientGroupDto();
    table1.setName("newSection3");
    table1.setColumns(table1Columns);

    Map<String, PatientColumnDto> table2Columns = new HashMap<>();
    table2Columns.put("new", new PatientColumnDto());
    table2Columns.put("newColumn6", new PatientColumnDto());
    table2Columns.put("newColumn0", new PatientColumnDto());
    table2Columns.put("newColumn1", new PatientColumnDto());
    table2Columns.put("newColumn2", new PatientColumnDto());
    table2Columns.put("newColumn3", new PatientColumnDto());
    table2Columns.put("newColumn4", new PatientColumnDto());
    PatientGroupDto table2 = new PatientGroupDto();
    table2.setName("newSection4");
    table2.setColumns(table2Columns);

    Map<String, PatientColumnDto> table3Columns = new HashMap<>();
    table3Columns.put("new", new PatientColumnDto());
    table3Columns.put("newColumn0", new PatientColumnDto());
    table3Columns.put("newColumn1", new PatientColumnDto());
    table3Columns.put("newColumn2", new PatientColumnDto());
    table3Columns.put("newColumn3", new PatientColumnDto());
    table3Columns.put("newColumn4", new PatientColumnDto());
    table3Columns.put("newColumn5", new PatientColumnDto());
    table3Columns.put("newColumn6", new PatientColumnDto());
    PatientGroupDto table3 = new PatientGroupDto();
    table3.setName("newSection2");
    table3.setColumns(table3Columns);

    Map<String, PatientColumnDto> table4Columns = new HashMap<>();
    table4Columns.put("new", new PatientColumnDto());
    table4Columns.put("newColumn0", new PatientColumnDto());
    table4Columns.put("newColumn1", new PatientColumnDto());
    table4Columns.put("newColumn2", new PatientColumnDto());
    table4Columns.put("newColumn3", new PatientColumnDto());
    table4Columns.put("newColumn4", new PatientColumnDto());
    table4Columns.put("total", new PatientColumnDto());
    PatientGroupDto table4 = new PatientGroupDto();
    table4.setName("patientType");
    table4.setColumns(table4Columns);

    Map<String, PatientColumnDto> table5Columns = new HashMap<>();
    table5Columns.put("new", new PatientColumnDto());
    table5Columns.put("newColumn0", new PatientColumnDto());
    table5Columns.put("newColumn1", new PatientColumnDto());
    table5Columns.put("total", new PatientColumnDto());
    PatientGroupDto table5 = new PatientGroupDto();
    table5.setName("newSection0");
    table5.setColumns(table5Columns);

    Map<String, PatientColumnDto> table6Columns = new HashMap<>();
    table6Columns.put("new", new PatientColumnDto());
    table6Columns.put("newColumn0", new PatientColumnDto());
    table6Columns.put("total", new PatientColumnDto());
    PatientGroupDto table6 = new PatientGroupDto();
    table6.setName("newSection1");
    table6.setColumns(table6Columns);

    return Arrays.asList(table1, table2, table3, table4, table5, table6);
  }

  private List<AgeGroupServiceDto> buildMmtbAgeGroupDto() {
    Map<String, AgeGroupLineItemDto> table1Column = new HashMap<>();
    table1Column.put("treatment", new AgeGroupLineItemDto());
    table1Column.put("prophylaxis", new AgeGroupLineItemDto());
    AgeGroupServiceDto table1 = new AgeGroupServiceDto();
    table1.setService("adultos");
    table1.setColumns(table1Column);
    AgeGroupServiceDto table2 = new AgeGroupServiceDto();
    table2.setService("criança < 25Kg");
    table2.setColumns(table1Column);
    AgeGroupServiceDto table3 = new AgeGroupServiceDto();
    table3.setService("criança > 25Kg");
    table3.setColumns(table1Column);

    return Arrays.asList(table1, table2, table3);
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

    Map<String, PatientColumnDto> newSection8 = new HashMap<>();
    newSection8.put(newColumn, new PatientColumnDto());
    newSection8.put(newColumn0, new PatientColumnDto());
    PatientGroupDto groupDtoNewSection8 = new PatientGroupDto();
    groupDtoNewSection8.setName("newSection8");
    groupDtoNewSection8.setColumns(newSection8);
    return Arrays
        .asList(groupDtoNewSection0, groupDtoNewSection1, groupDtoNewSection2, groupDtoNewSection3, groupDtoNewSection4,
            groupDtoNewSection5, groupDtoNewSection6, groupDtoNewSection7, groupDtoPatientType, groupDtoNewSection8);
  }

  private RequisitionCreateRequest buildMlRequisitionCreateRequest() {
    return RequisitionCreateRequest.builder()
        .programCode("ML")
        .clientSubmittedTime(Instant.parse("2021-07-21T07:59:59Z"))
        .emergency(false)
        .actualStartDate(LocalDate.of(2021, 6, 21))
        .actualEndDate(endDate)
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

  private Requisition buildRequisition(UUID templateId, UUID orderableId, UUID programId) {
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(templateId);
    VersionEntityReference orderable = new VersionEntityReference(orderableId, 2L);
    ApprovedProductReference avalibleProduct = new ApprovedProductReference(orderable, new VersionEntityReference());
    Set<ApprovedProductReference> set = new HashSet<>();
    set.add(avalibleProduct);
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setFacilityId(facilityId);
    requisition.setProgramId(programId);
    requisition.setEmergency(false);
    requisition.setRequisitionLineItems(Collections.emptyList());
    requisition.setTemplate(template);
    requisition.setAvailableProducts(set);
    if (templateId != malariaTemplateId) {
      RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
      requisitionLineItem.setRequisition(requisition);
      requisitionLineItem.setOrderable(orderable);
      requisition.setRequisitionLineItems(singletonList(requisitionLineItem));
    }
    Map<String, Object> extraData = new HashMap<>();
    extraData.put("actualStartDate", "2021-06-21");
    extraData.put("actualEndDate", "2021-07-20");
    requisition.setExtraData(extraData);
    return requisition;
  }

  private Requisition buildViaRequisition() {
    return buildRequisition(viaTemplateId, viaOrderableId, viaProgramId);
  }

  private Requisition buildRapidTestRequisition() {
    return buildRequisition(rapidtestTemplateId, rapidTestOrderableId, rapidTestProgramId);
  }

  private Requisition buildMmiaRequisition() {
    return buildRequisition(mmiaTemplateId, mmiaOrderableId, mmiaProgramId);
  }

  private Requisition buildMalariaRaequisition() {
    return buildRequisition(malariaTemplateId, malariaOrderableId, malariaProgramId);
  }

  private Requisition buildMmtbRaequisition() {
    return buildRequisition(mmtbTemplateId, mmtbOrderableId, mmtbProgramId);
  }

  private OrderableDto buildOrderableDto() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(malariaOrderableId);
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

}
