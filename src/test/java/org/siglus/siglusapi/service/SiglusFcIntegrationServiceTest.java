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
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.common.domain.referencedata.SupervisoryNode;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.domain.ProgramOrderablesExtension;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.FcRequisitionDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusUsageTemplateDto;
import org.siglus.siglusapi.dto.UsageTemplateColumnDto;
import org.siglus.siglusapi.dto.UsageTemplateSectionDto;
import org.siglus.siglusapi.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SupervisoryNodeRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
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
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Mock
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private SiglusDateHelper dateHelper;

  private UUID dpmFacilityTypeId = UUID.randomUUID();

  private UUID dpmSupervisoryNodeId = UUID.randomUUID();

  private UUID fcFacilityTypeId = UUID.randomUUID();

  private UUID fcSupervisoryNodeId = UUID.randomUUID();

  private UUID requisitionId = UUID.randomUUID();

  private UUID facilityId = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID processingPeriodId = UUID.randomUUID();

  private UUID lineItemId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID templateId = UUID.randomUUID();

  private UUID regimenId = UUID.randomUUID();

  private String date = "20200221";

  private String today = "20200831";

  private String requisitionNumber = "NO010510020000004";

  private String facilityCode = "03040101";

  private String facilityName = "DPM ZAMBEZIA";

  private String facilityDescription = "DPM ZAMBEZIA";

  private String requestingFacilityCode = "01041205";

  private String requestingFacilityName = "CS Nhacatundo";

  private String requestingFacilityDescription = "Centro de Saude de Nhacatundo";

  private String programCode = "MP";

  private String programName = "Multiple Programs";

  private String productCode = "21A01";

  private String productName = "Acetilcisteina; 200mg/mL; Inj";

  private String productDescription = "Acetilcisteína";

  private LocalDate periodStartDate = LocalDate.parse("2020-03-01");

  private LocalDate periodEndDate = LocalDate.parse("2020-03-31");

  private String realProgramCode = "M";

  private String realProgramName = "Medicamentos Essenciais";

  private Integer authorizedQuantity = 100;

  private Pageable pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
      Pagination.NO_PAGINATION);

  @Before
  public void prepare() {
    ReflectionTestUtils
        .setField(siglusFcIntegrationService, "dpmFacilityTypeId", dpmFacilityTypeId);
    ReflectionTestUtils
        .setField(siglusFcIntegrationService, "fcFacilityTypeId", fcFacilityTypeId);
    mockSupervisoryNodeInfo();
    when(dateHelper.getTodayDateStr()).thenReturn(today);
    mockRequisitionInfo(dpmSupervisoryNodeId);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn(requisitionNumber);
    mockFacilityInfo();
    mockProgramInfo();
    mockPeriodInfo();
    mockLineItemExtensionInfo();
    mockOrderableInfo();
    mockProgramOrderableExtensionInfo();
    mockTemplateInfo(true);
    mockRegimenInfo();
  }

  @Test
  public void shouldSearchRequisitionsWithRegimen() {
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
    when(siglusRequisitionRepository.searchForFc(date, today, newHashSet(dpmSupervisoryNodeId),
        newHashSet(fcSupervisoryNodeId), pageable))
        .thenReturn(Pagination.getPage(requisitions, pageable));
  }

  private void mockFacilityInfo() {
    FacilityDto requestingFacility = new FacilityDto();
    requestingFacility.setCode(requestingFacilityCode);
    requestingFacility.setName(requestingFacilityName);
    requestingFacility.setDescription(requestingFacilityDescription);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(requestingFacility);
  }

  private void mockProgramInfo() {
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

  private void mockOrderableInfo() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setProductCode(productCode);
    orderableDto.setFullProductName(productName);
    orderableDto.setDescription(productDescription);
    when(orderableReferenceDataService.findOne(orderableId)).thenReturn(orderableDto);
  }

  private void mockProgramOrderableExtensionInfo() {
    ProgramOrderablesExtension programOrderablesExtension = new ProgramOrderablesExtension();
    programOrderablesExtension.setRealProgramCode(realProgramCode);
    programOrderablesExtension.setRealProgramName(realProgramName);
    List<ProgramOrderablesExtension> programOrderablesExtensions = newArrayList(
        programOrderablesExtension);
    when(programOrderablesExtensionRepository.findAllByOrderableId(orderableId))
        .thenReturn(programOrderablesExtensions);
  }

  private void mockTemplateInfo(Boolean enableRegimen) {
    RequisitionTemplateExtension templateExtension = new RequisitionTemplateExtension();
    templateExtension.setEnableRegimen(enableRegimen);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(templateExtension);
  }

  private void mockRegimenInfo() {
    RegimenDto regimenDto = new RegimenDto();
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
}
