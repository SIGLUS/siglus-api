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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.ProofOfDeliveryStatus;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.domain.OrderLineItemExtension;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.FcProofOfDeliveryDto;
import org.siglus.siglusapi.dto.FcProofOfDeliveryLineItem;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusUsageTemplateDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.OrderLineItemExtensionRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.ShipmentsExtensionRepository;
import org.siglus.siglusapi.repository.SiglusLotRepository;
import org.siglus.siglusapi.repository.SiglusProgramOrderableRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.siglus.siglusapi.repository.dto.ProgramOrderableDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusFcIntegrationServiceTest {

  @InjectMocks
  private SiglusFcIntegrationService siglusFcIntegrationService;

  @Mock
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private SiglusProcessingPeriodReferenceDataService processingPeriodReferenceDataService;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Mock
  private RequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private SiglusDateHelper dateHelper;

  @Mock
  private SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  @Mock
  private StockCardLineItemReasonRepository stockCardLineItemReasonRepository;

  @Mock
  private OrderExternalRepository orderExternalRepository;

  @Mock
  private ProgramRealProgramRepository programRealProgramRepository;

  @Mock
  private ShipmentsExtensionRepository shipmentsExtensionRepository;

  @Mock
  private PatientLineItemRepository patientLineItemRepository;

  @Mock
  private UsageInformationLineItemRepository usageInformationLineItemRepository;

  @Mock
  private TestConsumptionLineItemRepository testConsumptionLineItemRepository;

  @Mock
  private OrderLineItemExtensionRepository orderLineItemExtensionRepository;

  @Mock
  private SiglusProgramOrderableRepository programOrderableRepository;

  @Mock
  private SiglusLotRepository siglusLotRepository;

  private final UUID dpmFacilityTypeId = UUID.randomUUID();

  private final UUID dpmSupervisoryNodeId = UUID.randomUUID();

  private final UUID fcFacilityTypeId = UUID.randomUUID();

  private final UUID fcSupervisoryNodeId = UUID.randomUUID();

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID processingPeriodId = UUID.randomUUID();

  private final UUID lineItemId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID templateId = UUID.randomUUID();

  private final UUID regimenId = UUID.randomUUID();

  private final UUID orderId = UUID.randomUUID();

  private final UUID externalId = UUID.randomUUID();

  private final UUID lotId = UUID.randomUUID();

  private final UUID rejectionReasonId = UUID.randomUUID();

  private final UUID orderLineItemId = UUID.randomUUID();

  private final UUID realProgramId = UUID.randomUUID();

  private final String reasonName = "Debit";

  private final LocalDate date = LocalDate.of(2020, 2, 21);

  private final String today = "20200831";

  private final String requisitionNumber = "NO010510020000004";

  private final String facilityCode = "03040101";

  private final String facilityType = "DPM";

  private final String facilityName = "DPM ZAMBEZIA";

  private final String facilityDescription = "DPM ZAMBEZIA";

  private final String requestingFacilityCode = "01041205";

  private final String requestingFacilityName = "CS Nhacatundo";

  private final String requestingFacilityDescription = "Centro de Saude de Nhacatundo";

  private final String programName = "Multiple Programs";

  private final String productCode = "21A01";

  private final String productName = "Acetilcisteina; 200mg/mL; Inj";

  private final String productDescription = "Acetilcisteína";

  private final LocalDate periodStartDate = LocalDate.parse("2020-03-01");

  private final LocalDate periodEndDate = LocalDate.parse("2020-03-31");

  private final String realProgramCode = "M";

  private final String realProgramName = "Medicamentos Essenciais";

  private final Integer authorizedQuantity = 100;

  private final Integer quantityAccepted = 1;

  private final Integer quantityRejected = 2;

  private final String notes = "notes";

  private final String receivedBy = "xia1";

  private final String deliveredBy = "xia2";

  private final String lotCode = "Lot-123";

  private final Long orderedQuantity = 120L;

  private final LocalDate receivedDate = LocalDate.of(2020, 9, 1);

  private final Pageable pageable = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
      PaginationConstants.NO_PAGINATION);

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(siglusFcIntegrationService, "dpmFacilityTypeId", dpmFacilityTypeId);
    ReflectionTestUtils.setField(siglusFcIntegrationService, "fcFacilityTypeId", fcFacilityTypeId);
    mockSupervisoryNodeInfo();
    when(dateHelper.getTodayDateStr()).thenReturn(today);
    mockRequisitionInfo(dpmSupervisoryNodeId);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn(requisitionNumber);
    mockFacilityInfo();
    mockPeriodInfo();
    mockLineItemExtensionInfo();
    mockPatientInfo();
    mockUsageInfo();
    mockTestConsumption();
    mockOrderableDtoInfo();
    mockOrderLineItemIdToPartialFulfilledMap();
    mockProductIdToPriceMap();
    mockProgramOrderableExtensionInfo();
    mockTemplateInfo(true);
    mockRegimenInfo();
    mockProofOfDelivery();
    mockOrderExternal();
    mockRequisitionMap();
    mockLotMap();
    mockReasonMap();
    mockRealProgram();
  }

  @Test
  public void shouldSearchRequisitionsWithRegimen() {
    // given
    mockProgramInfo("VC");

    // when
    Page<FcRequisitionDto> fcRequisitionDtos = siglusFcIntegrationService
        .searchRequisitions(date, pageable);
    // then
    assertEquals(1, fcRequisitionDtos.getContent().size());
    FcRequisitionDto fcRequisitionDto = fcRequisitionDtos.getContent().get(0);
    assertEquals(1, fcRequisitionDto.getProducts().size());
    assertEquals(1, fcRequisitionDto.getRegimens().size());
  }

  @Test
  public void shouldSearchRequisitionsWithoutRegimen() {
    // given
    mockTemplateInfo(false);
    mockProgramInfo("VC");

    // when
    Page<FcRequisitionDto> fcRequisitionDtos = siglusFcIntegrationService
        .searchRequisitions(date, pageable);

    // then
    assertEquals(1, fcRequisitionDtos.getContent().size());
    FcRequisitionDto fcRequisitionDto = fcRequisitionDtos.getContent().get(0);
    assertEquals(1, fcRequisitionDto.getProducts().size());
    assertNull(fcRequisitionDto.getRegimens());
  }

  @Test
  public void shouldSearchRequisitionsWithFacilityCodeEqualsRequestingFacilityCode() {
    // given
    mockProgramInfo("VC");
    mockRequisitionInfo(fcSupervisoryNodeId);

    // when
    Page<FcRequisitionDto> fcRequisitionDtos = siglusFcIntegrationService
        .searchRequisitions(date, pageable);

    // then
    assertEquals(1, fcRequisitionDtos.getContent().size());
    FcRequisitionDto fcRequisitionDto = fcRequisitionDtos.getContent().get(0);
    assertEquals(1, fcRequisitionDto.getProducts().size());
    assertEquals(1, fcRequisitionDto.getRegimens().size());
    assertEquals(fcRequisitionDto.getRequestingFacilityCode(), fcRequisitionDto.getFacilityCode());
  }

  @Test
  public void shouldOnlyReturnNeedApprovalRequisitionsWhenCallRequisitionNeedApprovalApi() {
    // given
    mockProgramInfo("VC");
    mockRequisitionInfo(fcSupervisoryNodeId);

    // when
    siglusFcIntegrationService.searchNeedApprovalRequisitions(date, pageable);

    // then
    verify(siglusRequisitionRepository, times(1)).searchNeedApprovalForFc(date, pageable,
        newHashSet(fcSupervisoryNodeId));
  }


  @Test
  public void shouldSearchRequisitionWithPatientInfo() {
    // given
    mockProgramInfo("T");

    // when
    Page<FcRequisitionDto> fcRequisitionDtos = siglusFcIntegrationService.searchRequisitions(date, pageable);

    // then
    assertEquals(1, fcRequisitionDtos.getContent().size());
    FcRequisitionDto fcRequisitionDto = fcRequisitionDtos.getContent().get(0);
    assertEquals(1, fcRequisitionDto.getProducts().size());
    assertEquals(1, fcRequisitionDto.getRegimens().size());
    assertEquals(4, fcRequisitionDtos.getContent().get(0).getPatientLineItems().get(0).get("value"));
  }

  @Test
  public void shouldSearchRequisitionWithUsageInfo() {
    // given
    mockProgramInfo("ML");

    // when
    Page<FcRequisitionDto> fcRequisitionDtos = siglusFcIntegrationService.searchRequisitions(date, pageable);

    // then
    assertEquals(1, fcRequisitionDtos.getContent().size());
    FcRequisitionDto fcRequisitionDto = fcRequisitionDtos.getContent().get(0);
    assertEquals(1, fcRequisitionDto.getProducts().size());
    assertEquals(1, fcRequisitionDto.getRegimens().size());
    assertEquals(11, fcRequisitionDtos.getContent().get(0).getUsageInformationLineItems().get(0).get("value"));
  }

  @Test
  public void shouldSearchRequisitionWithTestConsumption() {
    // given
    mockProgramInfo("TR");

    // when
    Page<FcRequisitionDto> fcRequisitionDtos = siglusFcIntegrationService.searchRequisitions(date, pageable);

    // then
    assertEquals(1, fcRequisitionDtos.getContent().size());
    FcRequisitionDto fcRequisitionDto = fcRequisitionDtos.getContent().get(0);
    assertEquals(1, fcRequisitionDto.getProducts().size());
    assertEquals(1, fcRequisitionDto.getRegimens().size());
    assertEquals(10, fcRequisitionDtos.getContent().get(0).getTestConsumptionLineItems().get(0).get("value"));
  }

  @Test
  public void shouldSearchProofOfDelivery() {
    // given
    when(shipmentsExtensionRepository.findByShipmentIdIn(any())).thenReturn(newArrayList());

    // when
    Page<FcProofOfDeliveryDto> fcProofOfDeliveryDtos = siglusFcIntegrationService
        .searchProofOfDelivery(date, pageable);

    // then
    List<FcProofOfDeliveryDto> list = fcProofOfDeliveryDtos.getContent();
    assertEquals(1, list.size());
    FcProofOfDeliveryDto pod = list.get(0);
    assertEquals(requisitionNumber, pod.getRequisitionNumber());
    assertEquals(deliveredBy, pod.getDeliveredBy());
    assertEquals(receivedDate, pod.getReceivedDate());
    assertEquals(receivedBy, pod.getReceivedBy());
    List<FcProofOfDeliveryLineItem> products = pod.getPodLineItems();
    assertEquals(1, products.size());
    FcProofOfDeliveryLineItem product = products.get(0);
    assertEquals(quantityAccepted, product.getReceivedQuantity());
    assertEquals(quantityRejected, product.getDifference());
    assertEquals(lotCode, product.getLotCode());
    assertEquals(reasonName, product.getAdjustmentReason());
  }

  private void mockSupervisoryNodeInfo() {
    SupervisoryNode dpmSupervisoryNode = new SupervisoryNode();
    dpmSupervisoryNode.setId(dpmSupervisoryNodeId);
    Set<SupervisoryNode> dpmSupervisoryNodes = newHashSet(dpmSupervisoryNode);
    when(supervisoryNodeRepository.findAllByFacilityTypeId(dpmFacilityTypeId))
        .thenReturn(dpmSupervisoryNodes);
    SupervisoryNode fcSupervisoryNode = new SupervisoryNode();
    fcSupervisoryNode.setId(fcSupervisoryNodeId);
    Set<SupervisoryNode> fcSupervisoryNodes = newHashSet(fcSupervisoryNode);
    when(supervisoryNodeRepository.findAllByFacilityTypeId(fcFacilityTypeId))
        .thenReturn(fcSupervisoryNodes);
  }

  private void mockRequisitionInfo(UUID supervisoryNodeId) {
    Requisition requisition = new Requisition();
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    requisition.setId(requisitionId);
    requisition.setFacilityId(facilityId);
    requisition.setProgramId(programId);
    requisition.setProcessingPeriodId(processingPeriodId);
    requisition.setSupervisoryNodeId(supervisoryNodeId);
    Facility facility = new Facility();
    facility.setCode(facilityCode);
    facility.setName(facilityName);
    facility.setDescription(facilityDescription);
    SupervisoryNode supervisoryNode = new SupervisoryNode();
    supervisoryNode.setFacility(facility);
    requisition.setSupervisoryNode(supervisoryNode);
    RequisitionLineItem lineItem = new RequisitionLineItem();
    lineItem.setId(lineItemId);
    VersionEntityReference orderable = new VersionEntityReference();
    orderable.setId(orderableId);
    lineItem.setOrderable(orderable);
    List<RequisitionLineItem> lineItems = newArrayList(lineItem);
    requisition.setRequisitionLineItems(lineItems);
    RequisitionTemplate template = new RequisitionTemplate();
    template.setId(templateId);
    requisition.setTemplate(template);
    List<Requisition> requisitions = newArrayList(requisition);
    when(siglusRequisitionRepository.searchAllForFc(date, pageable))
        .thenReturn(Pagination.getPage(requisitions, pageable));
    when(siglusRequisitionRepository.searchNeedApprovalForFc(date, pageable,
        newHashSet(fcSupervisoryNodeId))).thenReturn(Pagination.getPage(requisitions, pageable));
  }

  private void mockFacilityInfo() {
    FacilityDto requestingFacility = new FacilityDto();
    requestingFacility.setCode(requestingFacilityCode);
    requestingFacility.setName(requestingFacilityName);
    requestingFacility.setDescription(requestingFacilityDescription);
    requestingFacility.setId(facilityId);
    FacilityTypeDto type = new FacilityTypeDto();
    type.setCode(facilityType);
    requestingFacility.setType(type);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(requestingFacility);
    when(facilityReferenceDataService.findAll()).thenReturn(newArrayList(requestingFacility));
  }

  private void mockProgramInfo(String programCode) {
    ProgramDto programDto = new ProgramDto();
    programDto.setCode(programCode);
    programDto.setName(programName);
    when(programReferenceDataService.findOne(programId)).thenReturn(programDto);
  }

  private void mockPeriodInfo() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setStartDate(periodStartDate);
    processingPeriodDto.setEndDate(periodEndDate);
    when(processingPeriodReferenceDataService.findOne(processingPeriodId))
        .thenReturn(processingPeriodDto);
  }

  private void mockLineItemExtensionInfo() {
    RequisitionLineItemExtension lineItemExtension = new RequisitionLineItemExtension();
    lineItemExtension.setRequisitionLineItemId(lineItemId);
    lineItemExtension.setAuthorizedQuantity(authorizedQuantity);
    List<RequisitionLineItemExtension> lineItemExtensions = newArrayList(lineItemExtension);
    when(lineItemExtensionRepository.findLineItems(newHashSet(lineItemId)))
        .thenReturn(lineItemExtensions);
  }

  private void mockPatientInfo() {
    PatientLineItem patientLineItem = new PatientLineItem();
    patientLineItem.setRequisitionId(requisitionId);
    patientLineItem.setId(UUID.randomUUID());
    patientLineItem.setGroup("newSection1");
    patientLineItem.setColumn("new");
    patientLineItem.setValue(4);
    when(patientLineItemRepository.findByRequisitionId(any())).thenReturn(newArrayList(patientLineItem));
  }

  private void mockUsageInfo() {
    UsageInformationLineItem lineItem = new UsageInformationLineItem();
    lineItem.setId(UUID.randomUUID());
    lineItem.setRequisitionId(requisitionId);
    lineItem.setOrderableId(orderableId);
    lineItem.setId(UUID.randomUUID());
    lineItem.setService("total");
    lineItem.setInformation("existentStock");
    lineItem.setValue(11);
    when(usageInformationLineItemRepository.findByRequisitionId(any())).thenReturn(newArrayList(lineItem));
  }

  private void mockTestConsumption() {
    TestConsumptionLineItem lineItem = new TestConsumptionLineItem();
    lineItem.setId(UUID.randomUUID());
    lineItem.setRequisitionId(requisitionId);
    lineItem.setService("total");
    lineItem.setProject("newColumn1");
    lineItem.setOutcome("consumo");
    lineItem.setValue(10);
    when(testConsumptionLineItemRepository.findByRequisitionId(any())).thenReturn(newArrayList(lineItem));
  }

  private void mockOrderableDtoInfo() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setProductCode(productCode);
    orderableDto.setFullProductName(productName);
    orderableDto.setDescription(productDescription);
    orderableDto.setId(orderableId);
    when(orderableReferenceDataService.findOne(orderableId)).thenReturn(orderableDto);
    when(orderableReferenceDataService.findByIds(any())).thenReturn(newArrayList(orderableDto));

    Map<String, String> orderableInfoMap = newHashMap();
    orderableInfoMap.put("code", productCode);
    orderableInfoMap.put("name", productName);
    orderableInfoMap.put("description", productDescription);
    Map<UUID, Map<String, String>> orderableIdToInfoMap = newHashMap();
    orderableIdToInfoMap.put(orderableId, orderableInfoMap);
    when(siglusOrderableService.getAllOrderableInfoForFc()).thenReturn(orderableIdToInfoMap);
  }

  private void mockProgramOrderableExtensionInfo() {
    ProgramOrderablesExtension programOrderablesExtension = new ProgramOrderablesExtension();
    programOrderablesExtension.setRealProgramCode(realProgramCode);
    programOrderablesExtension.setRealProgramName(realProgramName);
    programOrderablesExtension.setOrderableId(orderableId);
    List<ProgramOrderablesExtension> programOrderablesExtensions = newArrayList(
        programOrderablesExtension);
    when(programOrderablesExtensionRepository.findAllByOrderableId(orderableId))
        .thenReturn(programOrderablesExtensions);
    when(programOrderablesExtensionRepository
        .findAllByOrderableIdIn(any())).thenReturn(programOrderablesExtensions);
  }

  private void mockTemplateInfo(Boolean enableRegimen) {
    RequisitionTemplateExtension templateExtension = new RequisitionTemplateExtension();
    templateExtension.setEnableRegimen(enableRegimen);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(templateExtension);
  }

  private void mockRegimenInfo() {
    RegimenDto regimenDto = new RegimenDto();
    regimenDto.setRealProgramId(realProgramId);
    regimenDto.setCode("regimenCode");
    regimenDto.setFullProductName("regimenName");
    Map<UUID, RegimenDto> regimenDtoMap = newHashMap();
    regimenDtoMap.put(regimenId, regimenDto);
    RegimenLineItem regimenLineItem = new RegimenLineItem(requisitionId, regimenId,
        "patients", 20);
    List<RegimenLineDto> regimenLineItems = RegimenLineDto
        .from(newArrayList(regimenLineItem), regimenDtoMap);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    siglusRequisitionDto.setRegimenLineItems(regimenLineItems);
    UsageTemplateColumnDto regimenColumnDto = new UsageTemplateColumnDto();
    regimenColumnDto.setName("patients");
    regimenColumnDto.setLabel("Total patients");
    UsageTemplateSectionDto regimenSectionDto = new UsageTemplateSectionDto();
    regimenSectionDto.setName("regimen");
    regimenSectionDto.setColumns(newArrayList(regimenColumnDto));
    List<UsageTemplateSectionDto> sectionDtos = newArrayList(regimenSectionDto);
    SiglusUsageTemplateDto usageTemplateDto = new SiglusUsageTemplateDto();
    usageTemplateDto.setRegimen(sectionDtos);
    siglusRequisitionDto.setUsageTemplate(usageTemplateDto);
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(new RequisitionV2Dto());
    when(siglusUsageReportService.searchUsageReport(any())).thenReturn(siglusRequisitionDto);
  }

  private void mockProofOfDelivery() {
    ProofOfDelivery proofOfDelivery = new ProofOfDelivery(mockShipment(),
        ProofOfDeliveryStatus.CONFIRMED,
        newArrayList(mockProofOfDeliveryLineItem()),
        receivedBy,
        deliveredBy,
        receivedDate);

    Pageable pageable = new PageRequest(0, 10);
    Page<ProofOfDelivery> page = Pagination.getPage(newArrayList(proofOfDelivery), pageable, 1);
    when(siglusProofOfDeliveryRepository
        .search(any(), any())).thenReturn(page);
  }

  private Shipment mockShipment() {
    return new Shipment(mockOrder(), null, null, null, null);
  }

  private Order mockOrder() {
    Order order = new Order();
    order.setId(orderId);
    order.setExternalId(externalId);
    order.setOrderLineItems(newArrayList(mockOrderLineItem()));
    return order;
  }

  private OrderLineItem mockOrderLineItem() {
    OrderLineItem orderLineItem = new OrderLineItem();
    orderLineItem.setId(orderLineItemId);
    orderLineItem.setOrderable(new org.openlmis.fulfillment.domain.VersionEntityReference(orderableId, 1L));
    orderLineItem.setOrderedQuantity(orderedQuantity);
    return orderLineItem;
  }

  private void mockOrderLineItemIdToPartialFulfilledMap() {
    OrderLineItemExtension orderLineItemExtension = new OrderLineItemExtension();
    orderLineItemExtension.setOrderLineItemId(orderLineItemId);
    orderLineItemExtension.setPartialFulfilledQuantity(100L);
    when(orderLineItemExtensionRepository.findByOrderLineItemIdIn(any())).thenReturn(
        newArrayList(orderLineItemExtension));
  }

  private void mockProductIdToPriceMap() {
    ProgramOrderableDto programOrderableDto = ProgramOrderableDto.builder()
        .orderableId(orderableId)
        .price(BigDecimal.valueOf(100.00))
        .build();
    when(programOrderableRepository.findAllMaxVersionProgramOrderableDtos()).thenReturn(
        newArrayList(programOrderableDto));
  }

  private ProofOfDeliveryLineItem mockProofOfDeliveryLineItem() {
    org.openlmis.fulfillment.domain.VersionEntityReference versionEntityReference
        = new org.openlmis.fulfillment.domain.VersionEntityReference();
    versionEntityReference.setId(orderableId);
    return new ProofOfDeliveryLineItem(versionEntityReference,
        lotId, quantityAccepted, null, quantityRejected, rejectionReasonId, notes);
  }

  private void mockOrderExternal() {
    OrderExternal orderExternal = new OrderExternal();
    orderExternal.setId(externalId);
    orderExternal.setRequisitionId(requisitionId);
    when(orderExternalRepository.findByIdIn(any())).thenReturn(newArrayList(orderExternal));
  }

  private void mockRequisitionMap() {
    Map<UUID, String> map = new HashMap<>();
    map.put(requisitionId, requisitionNumber);
    when(siglusRequisitionExtensionService.getRequisitionNumbers(any())).thenReturn(map);
  }

  private void mockLotMap() {
    Lot lot = new Lot();
    lot.setId(lotId);
    lot.setLotCode(lotCode);
    when(siglusLotRepository.findAllByIdIn(newHashSet(lotId))).thenReturn(newArrayList(lot));
  }

  private void mockReasonMap() {
    StockCardLineItemReason reason = new StockCardLineItemReason();
    reason.setId(rejectionReasonId);
    reason.setName(reasonName);
    when(stockCardLineItemReasonRepository
        .findByReasonTypeIn(any())).thenReturn(newArrayList(reason));
  }

  private void mockRealProgram() {
    when(programRealProgramRepository.findAll()).thenReturn(
        Collections.singletonList(
            mockRealProgram(realProgramId, "PT", "ptv",
                "PTV", true)));
  }

  private ProgramRealProgram mockRealProgram(UUID id, String code, String programCode,
      String name, boolean active) {
    ProgramRealProgram program = new ProgramRealProgram();
    program.setId(id);
    program.setRealProgramCode(code);
    program.setRealProgramName(name);
    program.setProgramCode(programCode);
    program.setActive(active);
    return program;
  }

}
