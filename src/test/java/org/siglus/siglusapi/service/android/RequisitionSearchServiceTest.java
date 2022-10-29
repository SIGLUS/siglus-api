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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.TOTAL;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.COLUMN_NAME;
import static org.siglus.siglusapi.constant.UsageSectionConstants.ConsultationNumberLineItems.GROUP_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.siglus.siglusapi.constant.UsageSectionConstants.MmtbAgeGroupLineItems;
import org.siglus.siglusapi.constant.UsageSectionConstants.TestConsumptionLineItems;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.Regimen;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.dto.ExtraDataSignatureDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.android.enumeration.TestOutcome;
import org.siglus.siglusapi.dto.android.request.PatientLineItemColumnRequest;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RegimenLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionSignatureRequest;
import org.siglus.siglusapi.dto.android.request.TestConsumptionLineItemRequest;
import org.siglus.siglusapi.dto.android.request.UsageInformationLineItemRequest;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.ConsultationNumberLineItemMapper;
import org.siglus.siglusapi.service.mapper.PatientLineItemMapper;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionSearchServiceTest {

  @InjectMocks
  private RequisitionSearchService service;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;

  @Mock
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private ConsultationNumberLineItemRepository consultationNumberLineItemRepository;

  @Mock
  private RegimenLineItemRepository regimenLineItemRepository;

  @Mock
  private RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;

  @Mock
  private UsageInformationLineItemRepository usageInformationLineItemRepository;

  @Mock
  private RegimenRepository regimenRepository;

  @Mock
  private PatientLineItemRepository patientLineItemRepository;

  @Mock
  private TestConsumptionLineItemRepository testConsumptionLineItemRepository;

  @Mock
  private AgeGroupLineItemRepository ageGroupLineItemRepository;

  @Mock
  private PatientLineItemMapper patientLineItemMapper;

  private final UUID programId = UUID.randomUUID();
  private final UUID programIdMmia = UUID.randomUUID();
  private final UUID programIdMalaria = UUID.randomUUID();
  private final UUID programIdRapidTest = UUID.randomUUID();
  private final UUID programIdMmtb = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID orderableId2 = UUID.randomUUID();
  private final UUID templateId = UUID.fromString("610a52a5-2217-4fb7-9e8e-90bba3051d4d");
  private final UUID mmiaTemplateId = UUID.fromString("873c25d6-e53b-11eb-8494-acde48001122");
  private final UUID malariaTemplateId = UUID.fromString("3f2245ce-ee9f-11eb-ba79-acde48001122");
  private final UUID rapidtestTemplateId = UUID.fromString("2c10856e-eead-11eb-9718-acde48001122");
  private final UUID mmtbTemplateId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID requisitionIdMmia = UUID.randomUUID();
  private final UUID requisitionIdMalaria = UUID.randomUUID();
  private final UUID requisitionIdRapidTest = UUID.randomUUID();
  private final UUID requisitionIdMmtb = UUID.randomUUID();
  private final UUID requisitionLineItemId = UUID.randomUUID();
  private final UUID requisitionLineItemId2 = UUID.randomUUID();
  private final Map<UUID, String> orderableIdToCode = new HashMap<>();
  private final String orderableCode = "orderableCode";
  private final String orderableCode2 = "orderableCode2";
  private final UUID regimenId = UUID.randomUUID();
  private String existentStock = "existentStock";
  private String treatmentsAttended = "treatmentsAttended";
  private String newColumn0 = "newColumn0";
  private final String startDate = "2021-07-13";

  @Before
  public void prepare() {
    ConsultationNumberLineItemMapper consultationNumberLineItemMapper = new ConsultationNumberLineItemMapper();
    ReflectionTestUtils.setField(service, "consultationNumberLineItemMapper", consultationNumberLineItemMapper);
    createGetRequisitionData();
    createGetMmiaRequisitionData();
    createGetMalariaRequisitionData();
    createGetRapidTestRequisitionData();
    createGetMmtbRequisitionData();
  }

  @Test
  public void shouldGetViaRequisitionResponseWhenByFacilityIdAndStartDate() {
    // when
    RequisitionResponse requisitionResponse = service
        .getRequisitionResponseByFacilityIdAndDate(UUID.randomUUID(), startDate, orderableIdToCode);

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

  @Test
  public void shouldGetMmiaRequisitionResponseWhenByFacilityIdAndStartDate() {
    // when
    RequisitionResponse requisitionResponse = service
        .getRequisitionResponseByFacilityIdAndDate(UUID.randomUUID(), startDate, orderableIdToCode);

    // then
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
    Set<String> columnNmaes = patientLineItemsRequests.get(0).getColumns().stream()
        .map(PatientLineItemColumnRequest::getName).collect(Collectors.toSet());
    Set<Integer> columnValues = patientLineItemsRequests.get(0).getColumns().stream()
        .map(PatientLineItemColumnRequest::getValue).collect(Collectors.toSet());
    assertTrue(columnNmaes.contains("dispensed_ds5"));
    assertTrue(columnNmaes.contains("dispensed_dt1"));
    assertTrue(columnValues.contains(20));
    assertTrue(columnValues.contains(27));

    assertEquals("comments", mmiaResponse.getComments());
  }

  @Test
  public void shouldGetMalariaRequisitionResponseWhenByFacilityIdAndStartDate() {
    // when
    RequisitionResponse requisitionResponse = service
        .getRequisitionResponseByFacilityIdAndDate(UUID.randomUUID(), startDate, orderableIdToCode);

    // then
    RequisitionCreateRequest response = requisitionResponse.getRequisitionResponseList().get(2);
    assertEquals("ML", response.getProgramCode());
    assertEquals("2021-07-21T07:59:59Z", String.valueOf(response.getClientSubmittedTime()));
    assertEquals(false, response.getEmergency());
    assertEquals("2021-07-01", String.valueOf(response.getActualStartDate()));
    assertEquals("2021-07-21", String.valueOf(response.getActualEndDate()));
    assertNull(response.getConsultationNumber());
    assertNull(response.getComments());
    assertEquals(0, response.getProducts().size());

    Map<String, String> signatureMap = response.getSignatures().stream()
        .collect(Collectors.toMap(RequisitionSignatureRequest::getType, RequisitionSignatureRequest::getName));
    assertEquals("yyds2", signatureMap.get("submit"));
    assertEquals("yyds3", signatureMap.get("authorize"));
    assertEquals("yyds4", signatureMap.get("approve"));

    UsageInformationLineItemRequest existenInfo = response.getUsageInformationLineItems().stream()
        .filter(item -> existentStock.equals(item.getInformation()))
        .filter(item -> orderableCode.equals(item.getProductCode()))
        .findFirst()
        .orElse(new UsageInformationLineItemRequest());
    assertEquals(Integer.valueOf(100), existenInfo.getHf());
    assertEquals(Integer.valueOf(300), existenInfo.getChw());

    UsageInformationLineItemRequest treatmentInfo = response.getUsageInformationLineItems().stream()
        .filter(item -> treatmentsAttended.equals(item.getInformation()))
        .filter(item -> orderableCode2.equals(item.getProductCode()))
        .findFirst()
        .orElse(new UsageInformationLineItemRequest());
    assertEquals(Integer.valueOf(600), treatmentInfo.getHf());
    assertEquals(Integer.valueOf(800), treatmentInfo.getChw());

  }

  @Test
  public void shouldGetRapidTestRequisitionResponseWhenByFacilityIdAndStartDate() {
    // when
    RequisitionResponse requisitionResponse = service
        .getRequisitionResponseByFacilityIdAndDate(UUID.randomUUID(), startDate, orderableIdToCode);

    // then
    RequisitionCreateRequest trResponse = requisitionResponse.getRequisitionResponseList().get(3);
    List<TestConsumptionLineItemRequest> testConsumptionLineItems = trResponse.getTestConsumptionLineItems();

    assertEquals(3, testConsumptionLineItems.size());

    TestConsumptionLineItemRequest testConsumptionLineItemRequest = testConsumptionLineItems.stream()
        .filter(item -> TestOutcome.POSITIVE.name().equals(item.getTestOutcome()))
        .findFirst()
        .orElse(new TestConsumptionLineItemRequest());
    assertEquals(Integer.valueOf(2), testConsumptionLineItemRequest.getValue());
    TestConsumptionLineItemRequest testConsumptionLineItemRequest1 = testConsumptionLineItems.stream()
        .filter(item -> TestOutcome.CONSUME.name().equals(item.getTestOutcome()))
        .findFirst()
        .orElse(new TestConsumptionLineItemRequest());
    assertEquals(Integer.valueOf(1), testConsumptionLineItemRequest1.getValue());
    TestConsumptionLineItemRequest testConsumptionLineItemRequest2 = testConsumptionLineItems.stream()
        .filter(item -> TestOutcome.UNJUSTIFIED.name().equals(item.getTestOutcome()))
        .findFirst()
        .orElse(new TestConsumptionLineItemRequest());
    assertEquals(Integer.valueOf(1), testConsumptionLineItemRequest2.getValue());
  }

  @Test
  public void shouldGetMmtbRequisitionResponseWhenByFacilityIdAndStartDate() {
    // when
    RequisitionResponse requisitionResponse = service
        .getRequisitionResponseByFacilityIdAndDate(UUID.randomUUID(), startDate, orderableIdToCode);

    // then
    RequisitionCreateRequest mmtbResponse = requisitionResponse.getRequisitionResponseList().get(4);
    assertEquals(6, mmtbResponse.getAgeGroupLineItems().size());
    List<PatientLineItemsRequest> patientLineItems = mmtbResponse.getPatientLineItems();
    assertEquals(1, patientLineItems.size());
    assertEquals(2, patientLineItems.get(0).getColumns().size());
  }

  private void createGetRequisitionData() {
    orderableIdToCode.put(orderableId, orderableCode);
    orderableIdToCode.put(orderableId2, orderableCode2);
    when(requisitionExtensionRepository.searchRequisitionIdByFacilityAndDate(any(), any()))
        .thenReturn(buildRequisitionExtension());

    ConsultationNumberLineItem consultationNumberLineItem = ConsultationNumberLineItem.builder()
        .requisitionId(requisitionId).column(COLUMN_NAME).group(GROUP_NAME).value(10).build();
    ConsultationNumberLineItem mmiaConsultationNumberLineItem = ConsultationNumberLineItem.builder()
        .requisitionId(requisitionId).column("test").group(GROUP_NAME).value(20).build();
    when(consultationNumberLineItemRepository.findByRequisitionIdIn(any()))
        .thenReturn(Arrays.asList(consultationNumberLineItem, mmiaConsultationNumberLineItem));

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

    v2Dto.setExtraData(buildExtraData(signatureDto));

    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(templateId);
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionId);
    v2Dto.setProgram(new ObjectReferenceDto(programId));
    v2Dto.setEmergency(true);
    v2Dto.setStatus(RequisitionStatus.AUTHORIZED);

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

  private void createGetMmiaRequisitionData() {
    Set<UUID> regimenIdSet = new HashSet<>();
    regimenIdSet.add(regimenId);
    when(regimenRepository.findByIdIn(regimenIdSet)).thenReturn(buildRegimens());
    when(regimenLineItemRepository.findByRequisitionIdIn(any())).thenReturn(buildRegimenLineItems());
    when(regimenSummaryLineItemRepository.findByRequisitionIdIn(any()))
        .thenReturn(buildRegimenSummaryLineItems());
    when(patientLineItemRepository.findByRequisitionIdIn(any())).thenReturn(buildAllPatientLineItems());
    when(patientLineItemMapper.from(buildMmiaPatientLineItems())).thenReturn(buildPatientGroupDtos());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionIdMmia)).thenReturn(buildMmiaV2Dto());

    ProgramDto programDto = new ProgramDto();
    programDto.setCode("T");
    programDto.setId(programIdMmia);
    when(siglusProgramService.getProgram(programIdMmia)).thenReturn(programDto);
  }

  private void createGetMalariaRequisitionData() {
    when(usageInformationLineItemRepository.findByRequisitionIdIn(any())).thenReturn(buildUsageInformationLineItems());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionIdMalaria)).thenReturn(buildMalariaV2Dto());

    ProgramDto programDto = new ProgramDto();
    programDto.setCode("ML");
    programDto.setId(programIdMalaria);
    when(siglusProgramService.getProgram(programIdMalaria)).thenReturn(programDto);
  }

  private void createGetRapidTestRequisitionData() {
    when(testConsumptionLineItemRepository.findByRequisitionIdIn(any())).thenReturn(buildTestConsumptionLineItems());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionIdRapidTest))
        .thenReturn(buildRapidTestV2Dto());

    ProgramDto programDto = new ProgramDto();
    programDto.setCode("TR");
    programDto.setId(programIdRapidTest);
    when(siglusProgramService.getProgram(programIdRapidTest)).thenReturn(programDto);
  }

  private void createGetMmtbRequisitionData() {
    when(ageGroupLineItemRepository.findByRequisitionIdIn(any())).thenReturn(buildAgeGroupLineItems());
    when(patientLineItemRepository.findByRequisitionIdIn(any())).thenReturn(buildAllPatientLineItems());
    when(patientLineItemMapper.from(buildMmtbPatientLineItems())).thenReturn(buildPatientGroupDtos());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionIdMmtb)).thenReturn(buildMmtbV2Dto());
    ProgramDto programDto = new ProgramDto();
    programDto.setCode("TB");
    programDto.setId(programIdMmtb);
    when(siglusProgramService.getProgram(programIdMmtb)).thenReturn(programDto);
  }

  private List<UsageInformationLineItem> buildUsageInformationLineItems() {
    UsageInformationLineItem existenHfOrderable = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(existentStock).service("HF").orderableId(orderableId)
        .value(100).build();
    UsageInformationLineItem existenHfOrderable2 = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(existentStock).service("HF").orderableId(orderableId2)
        .value(200).build();
    UsageInformationLineItem existenChwOrderable = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(existentStock).service(newColumn0).orderableId(orderableId)
        .value(300).build();
    UsageInformationLineItem existenChwOrderable2 = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(existentStock).service(newColumn0)
        .orderableId(orderableId2)
        .value(400).build();
    UsageInformationLineItem treatmentHfUsageOrderable = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(treatmentsAttended).service("HF").orderableId(orderableId)
        .value(500).build();
    UsageInformationLineItem treatmentHfUsageOrderable2 = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(treatmentsAttended).service("HF").orderableId(orderableId2)
        .value(600).build();
    UsageInformationLineItem treatmentChwUsageOrderable = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(treatmentsAttended).service(newColumn0)
        .orderableId(orderableId)
        .value(700).build();
    UsageInformationLineItem treatmentChwUsageOrderable2 = UsageInformationLineItem.builder()
        .requisitionId(requisitionIdMalaria).information(treatmentsAttended).service(newColumn0)
        .orderableId(orderableId2)
        .value(800).build();
    return Arrays.asList(existenHfOrderable, existenHfOrderable2, existenChwOrderable, existenChwOrderable2,
        treatmentHfUsageOrderable, treatmentHfUsageOrderable2, treatmentChwUsageOrderable, treatmentChwUsageOrderable2);
  }

  private List<TestConsumptionLineItem> buildTestConsumptionLineItems() {
    TestConsumptionLineItem testConsumptionLineItem1 = TestConsumptionLineItem.builder()
        .requisitionId(requisitionIdRapidTest)
        .service(TestConsumptionLineItems.SERVICE_HF).project(TestConsumptionLineItems.PROJECT_HIVDETERMINE)
        .outcome(TestConsumptionLineItems.PROJECT_POSITIVE).value(2).build();
    TestConsumptionLineItem testConsumptionLineItem2 = TestConsumptionLineItem.builder()
        .requisitionId(requisitionIdRapidTest)
        .service(TestConsumptionLineItems.SERVICE_HF).project(TestConsumptionLineItems.PROJECT_HIVDETERMINE)
        .outcome(TestConsumptionLineItems.PROJECT_CONSUMO).value(1).build();
    TestConsumptionLineItem testConsumptionLineItem3 = TestConsumptionLineItem.builder()
        .requisitionId(requisitionIdRapidTest)
        .service(TestConsumptionLineItems.SERVICE_HF).project(TestConsumptionLineItems.PROJECT_HIVDETERMINE)
        .outcome(TestConsumptionLineItems.PROJECT_UNJUSTIFIED).value(1).build();
    TestConsumptionLineItem testConsumptionLineItem4 = TestConsumptionLineItem.builder()
        .requisitionId(requisitionIdRapidTest)
        .service(TestConsumptionLineItems.SERVICE_HF).project(TestConsumptionLineItems.NEW_COLUMN_1)
        .outcome(TestConsumptionLineItems.PROJECT_POSITIVE).value(null).build();
    TestConsumptionLineItem testConsumptionLineItem5 = TestConsumptionLineItem.builder()
        .requisitionId(requisitionIdRapidTest)
        .service(TOTAL).project(TestConsumptionLineItems.PROJECT_HIVDETERMINE)
        .outcome(TestConsumptionLineItems.PROJECT_POSITIVE).value(2).build();
    return Arrays.asList(testConsumptionLineItem1, testConsumptionLineItem2, testConsumptionLineItem3,
        testConsumptionLineItem4, testConsumptionLineItem5);
  }

  private List<AgeGroupLineItem> buildAgeGroupLineItems() {
    AgeGroupLineItem ageGroupLineItem1 = AgeGroupLineItem.builder()
        .requisitionId(requisitionIdMmtb)
        .service(MmtbAgeGroupLineItems.TABLE_2_COLUMN_1_VALUE)
        .group(MmtbAgeGroupLineItems.TABLE_1_COLUMN_1_VALUE)
        .value(1)
        .build();
    AgeGroupLineItem ageGroupLineItem2 = AgeGroupLineItem.builder()
        .requisitionId(requisitionIdMmtb)
        .service(MmtbAgeGroupLineItems.TABLE_2_COLUMN_1_VALUE)
        .group(MmtbAgeGroupLineItems.TABLE_1_COLUMN_2_VALUE)
        .value(2)
        .build();
    AgeGroupLineItem ageGroupLineItem3 = AgeGroupLineItem.builder()
        .requisitionId(requisitionIdMmtb)
        .service(MmtbAgeGroupLineItems.TABLE_2_COLUMN_2_VALUE)
        .group(MmtbAgeGroupLineItems.TABLE_1_COLUMN_1_VALUE)
        .value(3)
        .build();
    AgeGroupLineItem ageGroupLineItem4 = AgeGroupLineItem.builder()
        .requisitionId(requisitionIdMmtb)
        .service(MmtbAgeGroupLineItems.TABLE_2_COLUMN_2_VALUE)
        .group(MmtbAgeGroupLineItems.TABLE_1_COLUMN_2_VALUE)
        .value(4)
        .build();
    AgeGroupLineItem ageGroupLineItem5 = AgeGroupLineItem.builder()
        .requisitionId(requisitionIdMmtb)
        .service(MmtbAgeGroupLineItems.TABLE_2_COLUMN_3_VALUE)
        .group(MmtbAgeGroupLineItems.TABLE_1_COLUMN_1_VALUE)
        .value(5)
        .build();
    AgeGroupLineItem ageGroupLineItem6 = AgeGroupLineItem.builder()
        .requisitionId(requisitionIdMmtb)
        .service(MmtbAgeGroupLineItems.TABLE_2_COLUMN_3_VALUE)
        .group(MmtbAgeGroupLineItems.TABLE_1_COLUMN_2_VALUE)
        .value(6)
        .build();
    return Arrays.asList(ageGroupLineItem1, ageGroupLineItem2, ageGroupLineItem3, ageGroupLineItem4, ageGroupLineItem5,
        ageGroupLineItem6);
  }

  private RequisitionV2Dto buildMalariaV2Dto() {
    ExtraDataSignatureDto signatureDto = new ExtraDataSignatureDto();
    signatureDto.setSubmit("yyds2");
    signatureDto.setAuthorize("yyds3");
    String[] approve = {"yyds4", "yyds5"};
    signatureDto.setApprove(approve);
    Map<String, Object> extraData = new HashMap<>();
    extraData.put("signaure", signatureDto);
    extraData.put("actualStartDate", "2021-07-01");
    extraData.put("actualEndDate", "2021-07-21");
    extraData.put("clientSubmittedTime", "2021-07-21T07:59:59Z");
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(malariaTemplateId);
    RequisitionV2Dto v2Dto = new RequisitionV2Dto();
    v2Dto.setExtraData(extraData);
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionIdMalaria);
    v2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    v2Dto.setProgram(new ObjectReferenceDto(programIdMalaria));
    v2Dto.setEmergency(false);
    return v2Dto;
  }

  private RequisitionV2Dto buildMmiaV2Dto() {
    ExtraDataSignatureDto signatureDto = new ExtraDataSignatureDto();
    signatureDto.setSubmit("wangj1");
    signatureDto.setAuthorize("wangj2");
    String[] approve = {"wangj3", "wangj4"};
    signatureDto.setApprove(approve);
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(mmiaTemplateId);
    RequisitionV2Dto v2Dto = new RequisitionV2Dto();
    v2Dto.setExtraData(buildExtraData(signatureDto));
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionIdMmia);
    v2Dto.setDraftStatusMessage("comments");
    v2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    v2Dto.setProgram(new ObjectReferenceDto(programIdMmia));
    v2Dto.setEmergency(false);
    return v2Dto;
  }

  private RequisitionV2Dto buildRapidTestV2Dto() {
    ExtraDataSignatureDto signatureDto = new ExtraDataSignatureDto();
    signatureDto.setSubmit("wangj5");
    signatureDto.setAuthorize("wangj6");
    String[] approve = {"wangj7", "wangj8"};
    signatureDto.setApprove(approve);
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(rapidtestTemplateId);
    RequisitionV2Dto v2Dto = new RequisitionV2Dto();
    v2Dto.setExtraData(buildExtraData(signatureDto));
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionIdRapidTest);
    v2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    v2Dto.setProgram(new ObjectReferenceDto(programIdRapidTest));
    v2Dto.setEmergency(false);
    return v2Dto;
  }

  private RequisitionV2Dto buildMmtbV2Dto() {
    ExtraDataSignatureDto signatureDto = new ExtraDataSignatureDto();
    signatureDto.setSubmit("wangj5");
    signatureDto.setAuthorize("wangj6");
    String[] approve = {"wangj7", "wangj8"};
    signatureDto.setApprove(approve);
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(mmtbTemplateId);
    RequisitionV2Dto v2Dto = new RequisitionV2Dto();
    v2Dto.setExtraData(buildExtraData(signatureDto));
    v2Dto.setTemplate(templateDto);
    v2Dto.setId(requisitionIdMmtb);
    v2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    v2Dto.setProgram(new ObjectReferenceDto(programIdMmtb));
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
    RequisitionExtension malariaRequisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionIdMalaria)
        .build();
    RequisitionExtension rapidTestRequisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionIdRapidTest)
        .build();
    RequisitionExtension mmtbRequisitionExtension = RequisitionExtension.builder()
        .requisitionId(requisitionIdMmtb)
        .build();
    return Arrays.asList(viaRequisitionExtension, mmiaRequisitionExtension, malariaRequisitionExtension,
        rapidTestRequisitionExtension, mmtbRequisitionExtension);
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

  private List<PatientLineItem> buildMmiaPatientLineItems() {
    PatientLineItem patientLineItem0 = new PatientLineItem();
    patientLineItem0.setRequisitionId(requisitionIdMmia);
    patientLineItem0.setGroup("newSection2");
    patientLineItem0.setColumn("new");
    patientLineItem0.setValue(20);

    PatientLineItem patientLineItem1 = new PatientLineItem();
    patientLineItem1.setRequisitionId(requisitionIdMmia);
    patientLineItem1.setGroup("newSection3");
    patientLineItem1.setColumn(newColumn0);
    patientLineItem1.setValue(27);

    return Arrays.asList(patientLineItem0, patientLineItem1);
  }

  private List<PatientLineItem> buildMmtbPatientLineItems() {
    PatientLineItem patientLineItem0 = new PatientLineItem();
    patientLineItem0.setRequisitionId(requisitionIdMmtb);
    patientLineItem0.setGroup("newSection2");
    patientLineItem0.setColumn("new");
    patientLineItem0.setValue(20);

    PatientLineItem patientLineItem1 = new PatientLineItem();
    patientLineItem1.setRequisitionId(requisitionIdMmtb);
    patientLineItem1.setGroup("newSection3");
    patientLineItem1.setColumn(newColumn0);
    patientLineItem1.setValue(27);

    return Arrays.asList(patientLineItem0, patientLineItem1);
  }


  private List<PatientLineItem> buildAllPatientLineItems() {
    List<PatientLineItem> all = new ArrayList<>();
    all.addAll(buildMmtbPatientLineItems());
    all.addAll(buildMmiaPatientLineItems());
    return all;
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
    columns1.put(newColumn0, patientColumnDto1);
    PatientGroupDto patientGroupDto1 = new PatientGroupDto();
    patientGroupDto1.setName("newSection3");
    patientGroupDto1.setColumns(columns1);

    return Arrays.asList(patientGroupDto, patientGroupDto1);
  }

  private Map buildExtraData(ExtraDataSignatureDto signatureDto) {
    Map<String, Object> extraData = new HashMap<>();
    extraData.put("signaure", signatureDto);
    extraData.put("actualStartDate", "2021-05-01");
    extraData.put("actualEndDate", "2021-05-11");
    extraData.put("clientSubmittedTime", "2021-06-21T07:59:59Z");
    return extraData;
  }

}