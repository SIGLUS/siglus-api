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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.siglusapi.constant.AndroidConstants.SCHEDULE_CODE;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.COLUMN_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.GROUP_NAME;

import com.google.common.collect.Sets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
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
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.ProcessingPeriodRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.android.request.AndroidTemplateConfig;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.ConsultationNumberDataProcessor;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;
import org.springframework.beans.BeanUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class AndroidRequisitionServiceTest {

  @InjectMocks
  private AndroidRequisitionService service;

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
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

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
  private ConsultationNumberDataProcessor consultationNumberDataProcessor;

  @Mock
  private RegimenLineItemRepository regimenLineItemRepository;

  @Mock
  private RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;

  @Mock
  private RegimenRepository regimenRepository;

  @Mock
  private PatientLineItemRepository patientLineItemRepository;

  @Mock
  private PatientLineItemMapper patientLineItemMapper;

  @Mock
  private AndroidTemplateConfig androidTemplateConfig;

  @Captor
  private ArgumentCaptor<Requisition> requisitionArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionLineItemExtension> requisitionLineItemExtensionArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionV2Dto> requisitionV2DtoArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionExtension> requisitionExtensionArgumentCaptor;

  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID programIdMmia = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID orderableId2 = UUID.randomUUID();
  private final UUID templateId = UUID.fromString("610a52a5-2217-4fb7-9e8e-90bba3051d4d");
  private final UUID mmiaTemplateId = UUID.fromString("873c25d6-e53b-11eb-8494-acde48001122");
  private final UUID malariaTemplateId = UUID.fromString("3f2245ce-ee9f-11eb-ba79-acde48001122");
  private final UUID supervisoryNodeId = UUID.randomUUID();
  private final UUID processingPeriodId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID requisitionLineItemId = UUID.randomUUID();
  private final UUID requisitionLineItemId2 = UUID.randomUUID();
  private final Map<UUID, String> orderableIdToCode = new HashMap<>();
  private final String orderableCode = "orderableCode";
  private final String orderableCode2 = "orderableCode2";
  private final UUID regimenId = UUID.randomUUID();
  private final UUID requisitionIdMmia = UUID.randomUUID();

  @Before
  public void prepare() {
    Set<UUID> androidTemplateIds = new HashSet<>();
    androidTemplateIds.add(templateId);
    androidTemplateIds.add(mmiaTemplateId);
    androidTemplateIds.add(malariaTemplateId);
    when(androidTemplateConfig.getAndroidTemplateIds()).thenReturn(androidTemplateIds);
    when(androidTemplateConfig.getAndroidViaTemplateId()).thenReturn(templateId);
    when(androidTemplateConfig.getAndroidMmiaTemplateId()).thenReturn(mmiaTemplateId);
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    ApprovedProductDto productDto = createApprovedProductDto(orderableId);
    when(requisitionService.getApproveProduct(facilityId, programId, false))
        .thenReturn(new ApproveProductsAggregator(Collections.singletonList(productDto), programId));
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(templateId);
    when(requisitionTemplateService.findTemplateById(templateId)).thenReturn(template);
    when(siglusProgramService.getProgramIdByCode("VC")).thenReturn(programId);
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    when(siglusOrderableService.getOrderableByCode("02A01")).thenReturn(orderableDto);
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeService.findSupervisoryNode(programId, facilityId)).thenReturn(supervisoryNodeDto);
    when(supervisoryNodeService.findOne(supervisoryNodeId)).thenReturn(supervisoryNodeDto);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(buildRequisitionTemplateExtension());
    when(processingPeriodRepository.findPeriodByCodeAndMonth(SCHEDULE_CODE, YearMonth.of(2021, 6)))
        .thenReturn(buildProcessingPeriod());
    when(requisitionRepository.save(requisitionArgumentCaptor.capture()))
        .thenReturn(buildRequisition(template));
    when(siglusUsageReportService.initiateUsageReport(requisitionV2DtoArgumentCaptor.capture()))
        .thenReturn(buildSiglusRequisitionDto());
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    when(siglusRequisitionExtensionService.buildRequisitionExtension(requisitionId, false, facilityId))
        .thenReturn(requisitionExtension);
    when(requisitionExtensionRepository.save(requisitionExtensionArgumentCaptor.capture()))
        .thenReturn(requisitionExtension);
    createGetRequisitionData();
    createGetMmiaRequisitionData();
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
    service.create(buildRequisitionCreateRequest());
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
    service.create(buildRequisitionCreateRequest());
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
    service.create(buildRequisitionCreateRequest());
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
    service.create(buildRequisitionCreateRequest());
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
    service.create(buildRequisitionCreateRequest());

    // then
    verify(requisitionRepository, times(4)).save(requisitionArgumentCaptor.capture());
    verify(siglusRequisitionExtensionService).buildRequisitionExtension(requisitionId, false, facilityId);
    verify(siglusUsageReportService).initiateUsageReport(any());
    verify(siglusUsageReportService).saveUsageReport(any(), any());
    verify(requisitionLineItemExtensionRepository).save(requisitionLineItemExtensionArgumentCaptor.capture());
  }

  @Test
  public void shouldGetRequisitionResponseWhenByFacilityIdAndStartDate() {
    // when
    RequisitionResponse requisitionResponse = service
        .getRequisitionResponseByFacilityIdAndDate(UUID.randomUUID(), "2021-07-13", orderableIdToCode);

    // then
    RequisitionCreateRequest response = requisitionResponse.getRequisitionResponseList().get(0);
    assertEquals("VC", response.getProgramCode());
    assertEquals("2021-06-21T07:59:59Z", String.valueOf(response.getClientSubmittedTime()));
    assertEquals(true, response.getEmergency());
    assertEquals("2021-05-01", String.valueOf(response.getActualStartDate()));
    assertEquals("2021-05-11", String.valueOf(response.getActualEndDate()));
    assertEquals(Integer.valueOf(10), response.getConsultationNumber());

    List<RequisitionLineItemRequest> products = response.getProducts();
    RequisitionLineItemRequest product = products.get(1);
    assertEquals(orderableCode2, product.getProductCode());
    assertEquals(Integer.valueOf(200), product.getBeginningBalance());
    assertEquals(Integer.valueOf(300), product.getTotalReceivedQuantity());
    assertEquals(Integer.valueOf(400), product.getTotalConsumedQuantity());
    assertEquals(Integer.valueOf(500), product.getStockOnHand());
    assertEquals(Integer.valueOf(100), product.getRequestedQuantity());
    assertEquals(Integer.valueOf(40), product.getAuthorizedQuantity());

    Map<String, String> signatureMap = response.getSignatures().stream()
        .collect(Collectors.toMap(RequisitionSignatureRequest::getType, RequisitionSignatureRequest::getName));
    assertEquals("yyd1", signatureMap.get("submit"));
    assertEquals("yyd2", signatureMap.get("authorize"));
    assertEquals("yyd3", signatureMap.get("approve"));

    RequisitionCreateRequest mmiaResponse = requisitionResponse.getRequisitionResponseList().get(1);
    List<RegimenLineItemRequest> regimenLineItemRequests = mmiaResponse.getRegimenLineItems();
    assertEquals("1alt1", regimenLineItemRequests.get(0).getCode());
    assertEquals("ABC+3TC+DTG", regimenLineItemRequests.get(0).getName());
    assertEquals(Integer.valueOf(2), regimenLineItemRequests.get(0).getPatientsOnTreatment());
    assertEquals(Integer.valueOf(1), regimenLineItemRequests.get(0).getComunitaryPharmacy());

    List<RegimenLineItemRequest> regimenSummaryLineItemRequests = mmiaResponse.getRegimenSummaryLineItems();
    assertEquals("key_regime_3lines_1", regimenSummaryLineItemRequests.get(0).getCode());
    assertEquals(Integer.valueOf(2), regimenSummaryLineItemRequests.get(0).getComunitaryPharmacy());
    assertEquals(Integer.valueOf(1), regimenSummaryLineItemRequests.get(0).getPatientsOnTreatment());

    List<PatientLineItemsRequest> patientLineItemsRequests = mmiaResponse.getPatientLineItems();
    assertEquals("table_dispensed_key", patientLineItemsRequests.get(0).getName());
    assertEquals(2, patientLineItemsRequests.get(0).getColumns().size());
    Set<String> columnNmaes = patientLineItemsRequests.get(0).getColumns().stream().map(t -> t.getName()).collect(
        Collectors.toSet());
    Set<Integer> columnValues = patientLineItemsRequests.get(0).getColumns().stream().map(t -> t.getValue()).collect(
        Collectors.toSet());
    assertEquals(true, columnNmaes.contains("dispensed_ds5"));
    assertEquals(true, columnNmaes.contains("dispensed_dt1"));
    assertEquals(true, columnValues.contains(20));
    assertEquals(true, columnValues.contains(27));

    assertEquals("comments", mmiaResponse.getComments());
  }

  private void createGetRequisitionData() {
    orderableIdToCode.put(orderableId, orderableCode);
    orderableIdToCode.put(orderableId2, orderableCode2);

    when(requisitionExtensionRepository.searchRequisitionIdByFacilityAndDate(any(), any()))
        .thenReturn(buildRequisitionExtension());

    ConsultationNumberGroupDto groupDto = new ConsultationNumberGroupDto();
    Map<String, ConsultationNumberColumnDto> columns = new HashMap<>();
    columns.put(COLUMN_NAME, new ConsultationNumberColumnDto(UUID.randomUUID(), 10));
    columns.put("test", new ConsultationNumberColumnDto(UUID.randomUUID(), 20));
    groupDto.setName(GROUP_NAME);
    groupDto.setColumns(columns);
    doAnswer((Answer) invocation -> {
      SiglusRequisitionDto dto = invocation.getArgumentAt(0, SiglusRequisitionDto.class);
      dto.setConsultationNumberLineItems(Collections.singletonList(groupDto));
      return dto;
    }).when(consultationNumberDataProcessor).get(any());

    RequisitionLineItemV2Dto itemV2Dto = new RequisitionLineItemV2Dto();
    itemV2Dto.setId(requisitionLineItemId);
    itemV2Dto.setBeginningBalance(20);
    itemV2Dto.setTotalReceivedQuantity(30);
    itemV2Dto.setTotalConsumedQuantity(40);
    itemV2Dto.setStockOnHand(50);
    itemV2Dto.setRequestedQuantity(10);
    VersionObjectReferenceDto orderableReference = new VersionObjectReferenceDto();
    orderableReference.setId(orderableId);
    itemV2Dto.setOrderable(orderableReference);

    RequisitionLineItemV2Dto itemV2Dto2 = new RequisitionLineItemV2Dto();
    itemV2Dto2.setId(requisitionLineItemId2);
    itemV2Dto2.setBeginningBalance(200);
    itemV2Dto2.setTotalReceivedQuantity(300);
    itemV2Dto2.setTotalConsumedQuantity(400);
    itemV2Dto2.setStockOnHand(500);
    itemV2Dto2.setRequestedQuantity(100);
    VersionObjectReferenceDto orderableReference2 = new VersionObjectReferenceDto();
    orderableReference2.setId(orderableId2);
    itemV2Dto2.setOrderable(orderableReference2);

    RequisitionV2Dto v2Dto = new RequisitionV2Dto();
    v2Dto.setRequisitionLineItems(Arrays.asList(itemV2Dto, itemV2Dto2));

    ExtraDataSignatureDto signatureDto = new ExtraDataSignatureDto();
    signatureDto.setSubmit("yyd1");
    signatureDto.setAuthorize("yyd2");
    String[] approve = {"yyd3", "yye4"};
    signatureDto.setApprove(approve);
    Map<String, Object> extraData = new HashMap<>();
    extraData.put("signaure", signatureDto);
    extraData.put("actualStartDate", "2021-05-01");
    extraData.put("actualEndDate", "2021-05-11");
    extraData.put("clientSubmittedTime", "2021-06-21T07:59:59Z");

    v2Dto.setExtraData(extraData);

    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(templateId);
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionId);
    v2Dto.setProgram(new ObjectReferenceDto(programId));
    v2Dto.setEmergency(true);
    v2Dto.setStatus(RequisitionStatus.SUBMITTED);

    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId)).thenReturn(v2Dto);

    List<RequisitionLineItemExtension> extensions = Arrays
        .asList(RequisitionLineItemExtension.builder().requisitionLineItemId(requisitionLineItemId)
                .authorizedQuantity(30).build(),
            RequisitionLineItemExtension.builder().requisitionLineItemId(requisitionLineItemId2)
                .authorizedQuantity(40).build());
    when(requisitionLineItemExtensionRepository.findLineItems(any())).thenReturn(extensions);

    ProgramDto programDto = new ProgramDto();
    programDto.setCode("VC");
    programDto.setId(programId);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);
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
        .productCode("02A01")
        .beginningBalance(200)
        .totalReceivedQuantity(20)
        .totalConsumedQuantity(14)
        .stockOnHand(202)
        .requestedQuantity(28)
        .authorizedQuantity(30)
        .build();
    return Collections.singletonList(product);
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
    requisition.setRequisitionLineItems(Collections.singletonList(requisitionLineItem));
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
    requisitionDto.setConsultationNumberLineItems(Collections.singletonList(consultationNumberGroupDto));
    return requisitionDto;
  }

  private void createGetMmiaRequisitionData() {
    Set<UUID> requisitionIdSet = new HashSet<>();
    requisitionIdSet.add(requisitionIdMmia);
    requisitionIdSet.add(requisitionId);
    Set<UUID> regimenIdSet = new HashSet<>();
    regimenIdSet.add(regimenId);
    when(regimenRepository.findByIdIn(regimenIdSet)).thenReturn(buildRegimens());
    when(regimenLineItemRepository.findByRequisitionIdIn(requisitionIdSet)).thenReturn(buildRegimenLineItems());
    when(regimenSummaryLineItemRepository.findByRequisitionIdIn(requisitionIdSet))
        .thenReturn(buildRegimenSummaryLineItems());
    when(patientLineItemRepository.findByRequisitionIdIn(requisitionIdSet)).thenReturn(buildPatientLineItems());
    when(patientLineItemMapper.from(buildPatientLineItems())).thenReturn(buildPatientGroupDtos());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionIdMmia)).thenReturn(buildMmiaV2Dto());

    ProgramDto programDto = new ProgramDto();
    programDto.setCode("T");
    programDto.setId(programIdMmia);
    when(siglusProgramService.getProgram(programIdMmia)).thenReturn(programDto);
  }

  private RequisitionV2Dto buildMmiaV2Dto() {
    ExtraDataSignatureDto signatureDto = new ExtraDataSignatureDto();
    signatureDto.setSubmit("wangj1");
    signatureDto.setAuthorize("wangj2");
    String[] approve = {"wangj3", "wangj4"};
    signatureDto.setApprove(approve);
    Map<String, Object> extraData = new HashMap<>();
    extraData.put("signaure", signatureDto);
    extraData.put("actualStartDate", "2021-05-01");
    extraData.put("actualEndDate", "2021-05-11");
    extraData.put("clientSubmittedTime", "2021-06-21T07:59:59Z");

    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(mmiaTemplateId);
    RequisitionV2Dto v2Dto = new RequisitionV2Dto();
    v2Dto.setExtraData(extraData);
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionIdMmia);
    v2Dto.setDraftStatusMessage("comments");
    v2Dto.setStatus(RequisitionStatus.SUBMITTED);
    v2Dto.setProgram(new ObjectReferenceDto(programIdMmia));
    v2Dto.setEmergency(false);
    return v2Dto;
  }

  private List<RequisitionExtension> buildRequisitionExtension() {
    RequisitionExtension viaRequisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionId)
        .build();
    RequisitionExtension mmiaRequisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionIdMmia)
        .build();
    return Arrays.asList(viaRequisitionExtension, mmiaRequisitionExtension);
  }

  private List<RegimenLineItem> buildRegimenLineItems() {
    RegimenLineItem regimenLineItem1 = new RegimenLineItem();
    regimenLineItem1.setRequisitionId(requisitionIdMmia);
    regimenLineItem1.setRegimenId(regimenId);
    regimenLineItem1.setValue(1);
    regimenLineItem1.setColumn("community");
    RegimenLineItem regimenLineItem2 = new RegimenLineItem();
    regimenLineItem2.setRequisitionId(requisitionIdMmia);
    regimenLineItem2.setRegimenId(regimenId);
    regimenLineItem2.setValue(2);
    regimenLineItem2.setColumn("patients");
    return Arrays.asList(regimenLineItem1, regimenLineItem2);
  }

  private List<Regimen> buildRegimens() {
    Regimen regimen = new Regimen();
    regimen.setId(regimenId);
    regimen.setCode("1alt1");
    regimen.setName("ABC+3TC+DTG");
    return Collections.singletonList(regimen);
  }

  private List<RegimenSummaryLineItem> buildRegimenSummaryLineItems() {
    RegimenSummaryLineItem regimenSummaryLineItem1 = RegimenSummaryLineItem.builder().requisitionId(requisitionIdMmia)
        .column("community").value(2).name("1stLinhas").build();
    RegimenSummaryLineItem regimenSummaryLineItem2 = RegimenSummaryLineItem.builder().requisitionId(requisitionIdMmia)
        .column("patients").value(1).name("1stLinhas").build();
    return Arrays.asList(regimenSummaryLineItem1, regimenSummaryLineItem2);
  }

  private List<PatientLineItem> buildPatientLineItems() {
    PatientLineItem patientLineItem0 = new PatientLineItem();
    patientLineItem0.setRequisitionId(requisitionIdMmia);
    patientLineItem0.setGroup("newSection2");
    patientLineItem0.setColumn("new");
    patientLineItem0.setValue(20);

    PatientLineItem patientLineItem1 = new PatientLineItem();
    patientLineItem1.setRequisitionId(requisitionIdMmia);
    patientLineItem1.setGroup("newSection3");
    patientLineItem1.setColumn("newColumn0");
    patientLineItem1.setValue(27);

    return Arrays.asList(patientLineItem0, patientLineItem1);
  }

  private List<PatientGroupDto> buildPatientGroupDtos() {
    PatientColumnDto patientColumnDto = new PatientColumnDto();
    patientColumnDto.setId(UUID.randomUUID());
    patientColumnDto.setValue(20);
    Map<String, PatientColumnDto> columns = new HashMap<>();
    columns.put("new", patientColumnDto);
    PatientGroupDto patientGroupDto = new PatientGroupDto();
    patientGroupDto.setName("newSection2");
    patientGroupDto.setColumns(columns);

    PatientColumnDto patientColumnDto1 = new PatientColumnDto();
    patientColumnDto1.setId(UUID.randomUUID());
    patientColumnDto1.setValue(27);
    Map<String, PatientColumnDto> columns1 = new HashMap<>();
    columns1.put("newColumn0", patientColumnDto1);
    PatientGroupDto patientGroupDto1 = new PatientGroupDto();
    patientGroupDto1.setName("newSection3");
    patientGroupDto1.setColumns(columns1);

    return Arrays.asList(patientGroupDto, patientGroupDto1);
  }

}