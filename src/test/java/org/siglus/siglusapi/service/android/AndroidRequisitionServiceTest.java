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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.ConsultationNumberGroupDto;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.ConsultationNumberDataProcessor;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.service.SiglusUsageReportService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

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
  private final UUID orderableId = UUID.randomUUID();
  private final UUID orderableId2 = UUID.randomUUID();
  private final UUID templateId = UUID.randomUUID();
  private final UUID supervisoryNodeId = UUID.randomUUID();
  private final UUID processingPeriodId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID requisitionLineItemId = UUID.randomUUID();
  private final UUID requisitionLineItemId2 = UUID.randomUUID();
  private final Map<UUID, String> orderableIdToCode = new HashMap<>();
  private final String orderableCode = "orderableCode";
  private final String orderableCode2 = "orderableCode2";

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(service, "androidViaTemplateId", templateId.toString());
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
  }

  private void createGetRequisitionData() {
    orderableIdToCode.put(orderableId, orderableCode);
    orderableIdToCode.put(orderableId2, orderableCode2);

    when(requisitionExtensionRepository.searchRequisitionIdByFacilityAndDate(any(), any()))
        .thenReturn(Collections.singletonList(RequisitionExtension.builder().requisitionId(requisitionId).build()));

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

}