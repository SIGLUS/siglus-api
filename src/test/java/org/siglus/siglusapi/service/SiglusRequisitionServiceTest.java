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
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.IN_APPROVAL;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_AUTHORIZE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_CREATE;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.OrderableExpirationDateDto;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderStatus;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.ShipmentDto;
import org.openlmis.requisition.dto.ShipmentLineItemDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.ProofOfDeliveryService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ShipmentFulfillmentService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.IdealStockAmountReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockOnHandRetrieverBuilderFactory;
import org.openlmis.requisition.testutils.IdealStockAmountDtoDataBuilder;
import org.openlmis.requisition.testutils.StockCardRangeSummaryDtoDataBuilder;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.openlmis.requisition.web.QueryRequisitionSearchParams;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.requisition.web.RequisitionV2Controller;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SimulateAuthenticationHelper;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.SiglusProgramDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
public class SiglusRequisitionServiceTest {

  private static final long OVERFLOW_QUANTITY = 1000L;

  private static final long NOT_FULLY_SHIPPED_QUANTITY = 0L;

  @Captor
  private ArgumentCaptor<RequisitionSearchParams> requisitionSearchParamsArgumentCaptor;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Mock
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Mock
  private RequisitionController requisitionController;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private RequisitionAuthenticationHelper authenticationHelper;

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private ProgramExtensionService programExtensionService;

  @Mock
  private PeriodService periodService;

  @Mock
  private StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Mock
  private SimulateAuthenticationHelper simulateAuthenticationHelper;

  @Mock
  private ProofOfDeliveryService proofOfDeliveryService;

  @Mock
  @SuppressWarnings("unused")
  private StockOnHandRetrieverBuilderFactory stockOnHandRetrieverBuilderFactory;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private IdealStockAmountReferenceDataService idealStockAmountReferenceDataService;

  @Mock
  private FacilityTypeApprovedProductReferenceDataService
      facilityTypeApprovedProductReferenceDataService;

  @InjectMocks
  private SiglusRequisitionService siglusRequisitionService;

  @Mock
  private Pageable pageable;

  @Mock
  private SiglusUsageReportService siglusUsageReportService;

  @Mock
  private RequisitionV2Controller requisitionV2Controller;

  @Mock
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  private UUID facilityId = UUID.randomUUID();

  private UUID userFacilityId = UUID.randomUUID();

  private UUID userId = UUID.randomUUID();

  private UUID programId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID orderableId2 = UUID.randomUUID();

  private UUID requisitionId = UUID.randomUUID();

  private UUID templateId = UUID.randomUUID();

  private UUID supervisoryNodeId = UUID.randomUUID();

  private UUID processingPeriodId = UUID.randomUUID();

  private UUID productId1 = UUID.randomUUID();
  private Long productVersion1 = 1L;
  private UUID productId2 = UUID.randomUUID();
  private Long productVersion2 = 1L;
  private VersionObjectReferenceDto productVersionObjectReference1
      = createVersionObjectReferenceDto(productId1, productVersion1);
  private VersionObjectReferenceDto productVersionObjectReference2
      = createVersionObjectReferenceDto(productId2, productVersion2);

  private String profilerName = "GET_REQUISITION_TO_APPROVE";

  private Profiler profiler = new Profiler(profilerName);

  private UserDto userDto = new UserDto();

  private ProgramDto programDto = new ProgramDto();

  private FacilityDto facilityDto = new FacilityDto();

  private RequisitionV2Dto requisitionV2Dto = createRequisitionV2Dto();

  private Requisition requisition = createRequisition();

  private SiglusRequisitionDto siglusRequisitionDto;

  //fields for emergency req test start
  private BasicRequisitionDto newBasicReq;

  private UUID preReqId1 = UUID.randomUUID();

  private UUID preReqId2 = UUID.randomUUID();

  private BasicRequisitionDto previousBasicReq1;

  private BasicRequisitionDto previousBasicReq2;

  private RequisitionV2Dto previousV2Req1;

  private int preReqProduct1ApprovedQuantity = randomQuantity();

  private List<OrderDto> preReqOrders = new ArrayList<>();

  private List<ShipmentDto> preReqOrderShipments1;

  private List<ShipmentDto> preReqOrderShipments2;

  @Mock
  private OrderFulfillmentService orderFulfillmentService;

  @Mock
  private ShipmentFulfillmentService shipmentFulfillmentService;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;
  //fields for emergency req test end

  @Before
  public void prepare() {
    siglusRequisitionDto = new SiglusRequisitionDto();
    BeanUtils.copyProperties(requisitionV2Dto, siglusRequisitionDto);
    when(siglusUsageReportService.searchUsageReport(any())).thenReturn(siglusRequisitionDto);
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(requisitionV2Dto);
    when(requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(templateId)).thenReturn(createTemplateExtension());
    when(requisitionController.getProfiler(
        profilerName, requisitionId)).thenReturn(profiler);
    when(requisitionController.findRequisition(any(), any())).thenReturn(requisition);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(requisitionController.findProgram(programId, profiler)).thenReturn(programDto);
    when(requisitionController.findFacility(facilityId, profiler)).thenReturn(facilityDto);
    when(requisitionService.getApproveProduct(any(), any(), any()))
        .thenReturn(createApproveProductsAggregator());
    when(supervisoryNodeService.findOne(supervisoryNodeId)).thenReturn(createSupervisoryNodeDto());
  }

  @Test
  public void shouldActivateArchivedProducts() {
    Requisition requisition = createRequisition();
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);

    siglusRequisitionService.activateArchivedProducts(requisitionId, facilityId);

    verify(archiveProductService)
        .activateArchivedProducts(Sets.newHashSet(orderableId), facilityId);
  }

  @Test
  public void shouldSearchRequisitionByIdAndKeepApproverApprovedProductIfInApprovePage() {
    when(requisitionService
        .validateCanApproveRequisition(any(), any())).thenReturn(ValidationResult.success());
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference2);
    RequisitionV2Dto filteredRequisitionDto = new RequisitionV2Dto();
    BeanUtils.copyProperties(requisitionV2Dto, filteredRequisitionDto);
    filteredRequisitionDto.setAvailableProducts(products);
    SiglusRequisitionDto siglusRequisitionDto = new SiglusRequisitionDto();
    BeanUtils.copyProperties(filteredRequisitionDto, siglusRequisitionDto);
    when(siglusUsageReportService.searchUsageReport(any(RequisitionV2Dto.class)))
        .thenReturn(siglusRequisitionDto);

    RequisitionV2Dto response = siglusRequisitionService.searchRequisition(requisitionId);

    verify(siglusRequisitionRequisitionService).searchRequisition(requisitionId);
    verify(requisitionTemplateExtensionRepository).findByRequisitionTemplateId(templateId);
    verify(requisitionController).getProfiler(profilerName, requisitionId);
    verify(requisitionController).findRequisition(requisitionId, profiler);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());
    verify(authenticationHelper).getCurrentUser();
    verify(requisitionController).findProgram(programId, profiler);
    verify(requisitionController).findFacility(null, profiler);
    verify(supervisoryNodeService).findOne(null);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());

    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    assertEquals(1, availableProducts.size());
    assertTrue(availableProducts.contains(productVersionObjectReference2));
  }

  @Test
  public void shouldSearchRequisitionIdIfNotInApprovePage() {
    when(requisitionService
        .validateCanApproveRequisition(any(), any()))
        .thenReturn(ValidationResult.noPermission("no approve permission"));

    RequisitionV2Dto response = siglusRequisitionService.searchRequisition(requisitionId);

    verify(siglusRequisitionRequisitionService).searchRequisition(requisitionId);
    verify(requisitionTemplateExtensionRepository).findByRequisitionTemplateId(templateId);
    verify(requisitionController).getProfiler(profilerName, requisitionId);
    verify(requisitionController).findRequisition(requisitionId, profiler);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());

    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    assertEquals(2, availableProducts.size());
    assertTrue(availableProducts.contains(productVersionObjectReference1));
    assertTrue(availableProducts.contains(productVersionObjectReference2));
  }

  @Test
  public void shouldAddRequisitionStatusesDisplayWhenCanAuth() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(QueryRequisitionSearchParams.FACILITY, facilityId.toString());
    queryParams.add(QueryRequisitionSearchParams.PROGRAM, programId.toString());
    when(permissionService.canAuthorizeRequisition(any())).thenReturn(ValidationResult.success());
    final HashSet<RequisitionStatus> requisitionStatusesDisplayWhenCanAuth = newHashSet(
        AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER);

    siglusRequisitionService.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionRequisitionService)
        .searchRequisitions(requisitionSearchParamsArgumentCaptor.capture(), any());
    RequisitionSearchParams requisitionSearchParams = requisitionSearchParamsArgumentCaptor
        .getValue();
    Set<RequisitionStatus> requisitionStatuses = requisitionSearchParams.getRequisitionStatuses();
    assertEquals(requisitionStatuses.size(), requisitionStatusesDisplayWhenCanAuth.size());
    assertTrue(requisitionStatuses.containsAll(requisitionStatusesDisplayWhenCanAuth));
  }

  @Test
  public void shouldAddRequisitionStatusesDisplayWhenOnlyCanCreate() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(QueryRequisitionSearchParams.FACILITY, facilityId.toString());
    queryParams.add(QueryRequisitionSearchParams.PROGRAM, programId.toString());
    when(permissionService.canAuthorizeRequisition(any())).thenReturn(
        ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_AUTHORIZE));
    when(permissionService.canSubmitRequisition(any())).thenReturn(ValidationResult.success());
    final HashSet<RequisitionStatus> requisitionStatusesDisplayWhenCanCreate = newHashSet(
        SUBMITTED, AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER);

    siglusRequisitionService.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionRequisitionService)
        .searchRequisitions(requisitionSearchParamsArgumentCaptor.capture(), any());
    RequisitionSearchParams requisitionSearchParams = requisitionSearchParamsArgumentCaptor
        .getValue();
    Set<RequisitionStatus> requisitionStatuses = requisitionSearchParams.getRequisitionStatuses();
    assertEquals(requisitionStatuses.size(), requisitionStatusesDisplayWhenCanCreate.size());
    assertTrue(requisitionStatuses.containsAll(requisitionStatusesDisplayWhenCanCreate));
  }

  @Test
  public void shouldNotAddRequisitionStatusesDisplayWhenCannotCreateAndAuth() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add(QueryRequisitionSearchParams.FACILITY, facilityId.toString());
    queryParams.add(QueryRequisitionSearchParams.PROGRAM, programId.toString());
    when(permissionService.canAuthorizeRequisition(any())).thenReturn(
        ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_AUTHORIZE));
    when(permissionService.canSubmitRequisition(any())).thenReturn(
        ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, REQUISITION_CREATE));

    siglusRequisitionService.searchRequisitions(queryParams, pageable);

    verify(siglusRequisitionRequisitionService)
        .searchRequisitions(requisitionSearchParamsArgumentCaptor.capture(), any());
    RequisitionSearchParams requisitionSearchParams = requisitionSearchParamsArgumentCaptor
        .getValue();
    Set<RequisitionStatus> requisitionStatuses = requisitionSearchParams.getRequisitionStatuses();
    assertTrue(CollectionUtils.isEmpty(requisitionStatuses));
  }

  @Test
  public void shouldCreateRequisitionLineItem() {
    // given
    Requisition requisition = createMockRequisition();
    when(requisitionController.getProfiler(any())).thenReturn(profiler);
    when(requisitionController.findRequisition(requisitionId, profiler)).thenReturn(requisition);
    when(requisitionController.findProgram(programId, profiler)).thenReturn(createProgram());
    when(requisitionController.findFacility(facilityId, profiler)).thenReturn(createFacility());
    when(permissionService.canInitOrAuthorizeRequisition(programId, facilityId))
        .thenReturn(new ValidationResult());
    when(authenticationHelper.getCurrentUser()).thenReturn(createUserDto());
    when(requisitionController.findFacility(userDto.getHomeFacilityId(), profiler))
        .thenReturn(createUserFacility());
    when(programExtensionService.getProgram(programId)).thenReturn(createSiglusProgramDto());
    when(periodService.getPeriod(processingPeriodId)).thenReturn(createProcessingPeriod());
    when(stockCardRangeSummaryStockManagementService.search(any(), any(), any(), any(), any()))
        .thenReturn(createStockCardRangeSummaryList());
    List<ProcessingPeriodDto> processingPeriodDtos = new ArrayList<>();
    when(periodService.getPeriods(any())).thenReturn(processingPeriodDtos);
    when(simulateAuthenticationHelper.simulateCrossServiceAuth()).thenReturn(null);
    when(proofOfDeliveryService.get(any())).thenReturn(new ProofOfDeliveryDto());
    when(idealStockAmountReferenceDataService.search(facilityId, processingPeriodId))
        .thenReturn(createIdealStockAmountDtoList());
    when(requisitionService.getApproveProduct(any(), any(), any()))
        .thenReturn(createApproveProductsAggregator(orderableId2));
    when(requisitionService.validateCanApproveRequisition(any(), any()))
        .thenReturn(new ValidationResult());
    when(siglusOrderableService.getOrderableExpirationDate(any()))
        .thenReturn(Lists.newArrayList(new OrderableExpirationDateDto(orderableId2,
            LocalDate.of(2022, 1, 1))));
    MetadataDto meta = createMetadataDto();
    OrderableDto orderable = createOrderableDto(meta);
    ApprovedProductDto productDto = createApprovedProductDto(orderable, meta);
    when(facilityTypeApprovedProductReferenceDataService.findByIdentities(any()))
        .thenReturn(Lists.newArrayList(productDto));
    List<UUID> orderableIds = new ArrayList<>();
    orderableIds.add(orderableId2);

    // when
    List<SiglusRequisitionLineItemDto> response =
        siglusRequisitionService.createRequisitionLineItem(requisitionId, orderableIds);

    // then
    assertEquals(1, response.size());
    RequisitionLineItemV2Dto lineItem = response.get(0).getLineItem();
    assertEquals(100, lineItem.getBeginningBalance().intValue());
    assertEquals(50, lineItem.getTotalReceivedQuantity().intValue());
    assertEquals(0, lineItem.getTotalLossesAndAdjustments().intValue());
    assertEquals(50, lineItem.getStockOnHand().intValue());
    assertEquals(0, lineItem.getRequestedQuantity().intValue());
    assertEquals(100, lineItem.getTotalConsumedQuantity().intValue());
  }

  @Test
  public void shouldCallUsageReportInitialWhenInitialRequisition() {
    // given
    UUID suggestedPeriod = UUID.randomUUID();
    String physicalInventoryDateStr = "date_str";
    HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    when(requisitionV2Controller.initiate(programId, facilityId, suggestedPeriod, true,
        physicalInventoryDateStr, httpServletRequest, httpServletResponse))
        .thenReturn(requisitionV2Dto);

    // when
    siglusRequisitionService.initiate(programId, facilityId, suggestedPeriod, true,
        physicalInventoryDateStr, httpServletRequest, httpServletResponse);

    // then
    verify(requisitionV2Controller).initiate(programId, facilityId, suggestedPeriod, true,
        physicalInventoryDateStr, httpServletRequest, httpServletResponse);
    verify(siglusUsageReportService).initiateUsageReport(requisitionV2Dto);
  }

  @Test
  public void shouldCallDeleteUsegeReportWhenDelteRequisition() {
    // given
    when(requisitionRepository.findOne(requisitionId)).thenReturn(createRequisition());
    RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
    extension.setId(UUID.randomUUID());
    when(lineItemExtensionRepository.findLineItems(singletonList(any(UUID.class))))
        .thenReturn(singletonList(extension));

    // when
    siglusRequisitionService.deleteRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).deleteRequisition(requisitionId);
    verify(lineItemExtensionRepository).delete(singletonList(extension));
    verify(siglusUsageReportService).deleteUsageReport(requisitionId);
  }

  @Test
  public void shouldUpdateRequisitionWhenUpdateRequisition() {
    // given
    HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    when(requisitionV2Controller.updateRequisition(requisitionId, siglusRequisitionDto,
        httpServletRequest, httpServletResponse)).thenReturn(requisitionV2Dto);
    SiglusRequisitionDto updatedDto = new SiglusRequisitionDto();
    BeanUtils.copyProperties(requisitionV2Dto, updatedDto);
    when(siglusUsageReportService.saveUsageReport(siglusRequisitionDto, requisitionV2Dto))
        .thenReturn(updatedDto);

    // when
    SiglusRequisitionDto requisitionDto =
        siglusRequisitionService.updateRequisition(requisitionId, siglusRequisitionDto,
        httpServletRequest, httpServletResponse);

    // then
    verify(requisitionV2Controller).updateRequisition(requisitionId, siglusRequisitionDto,
        httpServletRequest, httpServletResponse);
    verify(siglusUsageReportService).saveUsageReport(siglusRequisitionDto, requisitionV2Dto);
    assertEquals(0, requisitionDto.getLineItems().size());
  }

  @Test
  public void shouldUpdateRequisitionLineItemExtensionWhenUpdateRequisition() {
    // given
    RequisitionLineItemV2Dto lineItemV2Dto = new RequisitionLineItemV2Dto();
    OrderableDto productDto = new OrderableDto();
    productDto.setId(UUID.randomUUID());
    lineItemV2Dto.setOrderable(productDto);
    lineItemV2Dto.setId(UUID.randomUUID());
    lineItemV2Dto.setAuthorizedQuantity(10);
    requisitionV2Dto.setRequisitionLineItems(singletonList(lineItemV2Dto));
    siglusRequisitionDto.setRequisitionLineItems(singletonList(lineItemV2Dto));
    HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    when(requisitionV2Controller.updateRequisition(requisitionId, siglusRequisitionDto,
        httpServletRequest, httpServletResponse)).thenReturn(requisitionV2Dto);
    SiglusRequisitionDto updatedDto = new SiglusRequisitionDto();
    BeanUtils.copyProperties(requisitionV2Dto, updatedDto);
    RequisitionLineItemExtension requisitionLineItemExtension = RequisitionLineItemExtension
        .builder()
        .requisitionLineItemId(lineItemV2Dto.getId())
        .authorizedQuantity(10)
        .build();
    when(lineItemExtensionRepository.findLineItems(singletonList(lineItemV2Dto.getId())))
        .thenReturn(singletonList(requisitionLineItemExtension));
    when(lineItemExtensionRepository.save(singletonList(requisitionLineItemExtension)))
        .thenReturn(singletonList(requisitionLineItemExtension));
    when(siglusUsageReportService.saveUsageReport(siglusRequisitionDto, requisitionV2Dto))
        .thenReturn(updatedDto);

    // when
    SiglusRequisitionDto requisitionDto =
        siglusRequisitionService.updateRequisition(requisitionId, siglusRequisitionDto,
            httpServletRequest, httpServletResponse);

    // then
    verify(requisitionV2Controller).updateRequisition(requisitionId, siglusRequisitionDto,
        httpServletRequest, httpServletResponse);
    verify(siglusUsageReportService).saveUsageReport(siglusRequisitionDto, requisitionV2Dto);
    assertEquals(Integer.valueOf(10), requisitionDto.getLineItems().get(0).getAuthorizedQuantity());
  }

  @Test
  public void shouldFilterNoProductWhenGetEmergencyRequisitionGivenFirstEmergencyRequisition() {
    // given
    mockEmergencyRequisition();
    when(requisitionService
        .validateCanApproveRequisition(any(), any())).thenReturn(ValidationResult.success());
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(singletonList(newBasicReq)));

    // when
    SiglusRequisitionDto requisition = siglusRequisitionService.searchRequisition(requisitionId);

    // then
    Set<VersionObjectReferenceDto> availableProducts = requisition.getAvailableProducts();
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    assertEquals(2, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference1, productVersionObjectReference2));
  }

  @Test
  public void shouldFilterNoProductWhenGetEmergencyRequisitionGivenFullyShippedProduct1() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockFullyShippedProduct1();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(2, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference1, productVersionObjectReference2));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenNotFullyFulfilledProduct1() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockNotFullyFulfilledProduct1();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenFulfillSkippedProduct1() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockNotFulfilledProduct1();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenProduct1InProgress() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(IN_APPROVAL);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockProduct1InProgress();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenOverflowReq1AndNotFullyReq2() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    mockPreviousEmergencyRequisition2();
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1, previousBasicReq2)));
    mockOverflowShippedProduct1InPrevReq1();
    mockNotFullyShippedProduct1InPrevReq2();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterNoProductWhenGetEmergencyRequisitionGivenZeroAppQtyReq1AndNoPod() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockZeroAppQtyProduct1InPrevReq1();
    mockEmptyPodForPrevReq1();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(2, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference1, productVersionObjectReference2));
  }

  @Test
  public void shouldFilterNoProductWhenGetEmergencyRequisitionGivenApproveSkippedProduct1() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    previousV2Req1.getLineItems().forEach(lineItem -> lineItem.setSkipped(true));

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService).searchRequisitions(any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(2, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference1, productVersionObjectReference2));
  }

  private int randomQuantity() {
    return nextInt(1, 100);
  }

  private void mockEmergencyRequisition() {
    final RequisitionStatus randomStatus = AUTHORIZED;
    newBasicReq = new BasicRequisitionDto();
    newBasicReq.setEmergency(true);
    newBasicReq.setId(requisitionId);
    newBasicReq.setStatus(randomStatus);
    BasicProcessingPeriodDto basicPeriod = new BasicProcessingPeriodDto();
    basicPeriod.setId(processingPeriodId);
    newBasicReq.setProcessingPeriod(basicPeriod);
    RequisitionV2Dto newV2Req = new RequisitionV2Dto();
    newV2Req.setEmergency(true);
    newV2Req.setStatus(randomStatus);
    newV2Req.setProcessingPeriod(new ObjectReferenceDto(processingPeriodId));
    newV2Req.setId(requisitionId);
    newV2Req.setFacility(new ObjectReferenceDto(facilityId));
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference1);
    products.add(productVersionObjectReference2);
    newV2Req.setAvailableProducts(products);
    newV2Req.setTemplate(createTemplateDto());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(newV2Req);
    when(requisitionService.validateCanApproveRequisition(any(), any()))
        .thenReturn(ValidationResult.failedValidation("skip method"));
    org.openlmis.referencedata.dto.OrderableDto simpleProduct =
        new org.openlmis.referencedata.dto.OrderableDto();
    simpleProduct.setNetContent(1L);
    when(orderableReferenceDataService.findOne(any())).thenReturn(simpleProduct);
  }

  private void mockPreviousEmergencyRequisition1(RequisitionStatus status) {
    previousBasicReq1 = new BasicRequisitionDto();
    previousBasicReq1.setEmergency(true);
    BasicProcessingPeriodDto basicPeriod = new BasicProcessingPeriodDto();
    basicPeriod.setId(processingPeriodId);
    previousBasicReq1.setProcessingPeriod(basicPeriod);
    previousBasicReq1.setId(preReqId1);
    previousBasicReq1.setStatus(status);
    previousV2Req1 = new RequisitionV2Dto();
    previousV2Req1.setEmergency(true);
    previousV2Req1.setProcessingPeriod(new ObjectReferenceDto(processingPeriodId));
    previousV2Req1.setId(preReqId1);
    previousV2Req1.setStatus(status);
    previousV2Req1.setFacility(new ObjectReferenceDto(facilityId));
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference1);
    products.add(productVersionObjectReference2);
    previousV2Req1.setAvailableProducts(products);
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setOrderable(productVersionObjectReference1);
    lineItem.setSkipped(false);
    lineItem.setRequestedQuantity(preReqProduct1ApprovedQuantity);
    lineItem.setAuthorizedQuantity(preReqProduct1ApprovedQuantity);
    lineItem.setApprovedQuantity(preReqProduct1ApprovedQuantity);
    previousV2Req1.setRequisitionLineItems(singletonList(lineItem));
    previousV2Req1.setTemplate(createTemplateDto());
    when(siglusRequisitionRequisitionService.searchRequisition(preReqId1))
        .thenReturn(previousV2Req1);
  }

  private void mockPreviousEmergencyRequisition2() {
    previousBasicReq2 = new BasicRequisitionDto();
    previousBasicReq2.setEmergency(true);
    BasicProcessingPeriodDto basicPeriod = new BasicProcessingPeriodDto();
    basicPeriod.setId(processingPeriodId);
    previousBasicReq2.setProcessingPeriod(basicPeriod);
    previousBasicReq2.setId(preReqId2);
    previousBasicReq2.setStatus(RequisitionStatus.RELEASED);
    RequisitionV2Dto previousV2Req2 = new RequisitionV2Dto();
    previousV2Req2.setEmergency(true);
    previousV2Req2.setProcessingPeriod(new ObjectReferenceDto(processingPeriodId));
    previousV2Req2.setId(preReqId2);
    previousV2Req2.setStatus(RequisitionStatus.RELEASED);
    previousV2Req2.setFacility(new ObjectReferenceDto(facilityId));
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference1);
    products.add(productVersionObjectReference2);
    previousV2Req2.setAvailableProducts(products);
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setSkipped(false);
    lineItem.setOrderable(productVersionObjectReference1);
    lineItem.setRequestedQuantity(preReqProduct1ApprovedQuantity);
    lineItem.setAuthorizedQuantity(preReqProduct1ApprovedQuantity);
    lineItem.setApprovedQuantity(preReqProduct1ApprovedQuantity);
    previousV2Req2.setRequisitionLineItems(singletonList(lineItem));
    previousV2Req2.setTemplate(createTemplateDto());
    when(siglusRequisitionRequisitionService.searchRequisition(preReqId2))
        .thenReturn(previousV2Req2);
  }

  private void mockOrderService() {
    when(orderFulfillmentService.search(any(), any(), any(), any(), any()))
        .thenReturn(preReqOrders);
  }

  private List<ShipmentDto> mockShippedOrder(UUID reqId) {
    mockOrderService();
    OrderDto order = new OrderDto();
    order.setId(UUID.randomUUID());
    order.setStatus(OrderStatus.SHIPPED);
    order.setExternalId(reqId);
    preReqOrders.add(order);
    List<ShipmentDto> shipments = new ArrayList<>();
    when(shipmentFulfillmentService.getShipments(order.getId()))
        .thenReturn(shipments);
    return shipments;
  }

  private void mockShippedOrderAndShipmentForPreviousEmergencyRequisition1() {
    preReqOrderShipments1 = mockShippedOrder(preReqId1);
    preReqOrderShipments1.add(new ShipmentDto());
  }

  private void mockShippedOrderAndShipmentForPreviousEmergencyRequisition2() {
    preReqOrderShipments2 = preReqOrderShipments1 = mockShippedOrder(preReqId2);
    preReqOrderShipments2.add(new ShipmentDto());
  }

  private void mockFullyShippedProduct1() {
    mockShippedOrderAndShipmentForPreviousEmergencyRequisition1();
    ShipmentLineItemDto lineItem = new ShipmentLineItemDto();
    lineItem.setOrderable(new ObjectReferenceDto(productVersionObjectReference1.getId()));
    lineItem.setQuantityShipped((long) preReqProduct1ApprovedQuantity);
    preReqOrderShipments1.forEach(shipment -> shipment.setLineItems(singletonList(lineItem)));
  }

  private void mockNotFullyFulfilledProduct1() {
    mockShippedOrderAndShipmentForPreviousEmergencyRequisition1();
    ShipmentLineItemDto lineItem = new ShipmentLineItemDto();
    lineItem.setOrderable(new ObjectReferenceDto(productVersionObjectReference1.getId()));
    lineItem.setQuantityShipped(NOT_FULLY_SHIPPED_QUANTITY);
    preReqOrderShipments1.forEach(shipment -> shipment.setLineItems(singletonList(lineItem)));
  }

  private void mockNotFulfilledProduct1() {
    mockShippedOrderAndShipmentForPreviousEmergencyRequisition1();
    preReqOrderShipments1.forEach(shipment -> shipment.setLineItems(emptyList()));
  }

  private void mockProduct1InProgress() {
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setOrderable(productVersionObjectReference1);
    lineItem.setRequestedQuantity(preReqProduct1ApprovedQuantity);
    lineItem.setAuthorizedQuantity(preReqProduct1ApprovedQuantity);
    previousV2Req1.setRequisitionLineItems(singletonList(lineItem));
  }

  private void mockOverflowShippedProduct1InPrevReq1() {
    mockShippedOrderAndShipmentForPreviousEmergencyRequisition1();
    ShipmentLineItemDto lineItem = new ShipmentLineItemDto();
    lineItem.setOrderable(new ObjectReferenceDto(productVersionObjectReference1.getId()));
    lineItem.setQuantityShipped(OVERFLOW_QUANTITY);
    preReqOrderShipments1.forEach(shipment -> shipment.setLineItems(singletonList(lineItem)));
  }

  private void mockNotFullyShippedProduct1InPrevReq2() {
    mockShippedOrderAndShipmentForPreviousEmergencyRequisition2();
    ShipmentLineItemDto lineItem = new ShipmentLineItemDto();
    lineItem.setOrderable(new ObjectReferenceDto(productVersionObjectReference1.getId()));
    lineItem.setQuantityShipped(0L);
    preReqOrderShipments2.forEach(shipment -> shipment.setLineItems(singletonList(lineItem)));
  }

  private void mockZeroAppQtyProduct1InPrevReq1() {
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setOrderable(productVersionObjectReference1);
    lineItem.setRequestedQuantity(0);
    lineItem.setAuthorizedQuantity(0);
    lineItem.setApprovedQuantity(0);
    previousV2Req1.setRequisitionLineItems(singletonList(lineItem));
  }

  private void mockEmptyPodForPrevReq1() {
    preReqOrderShipments1 = mockShippedOrder(preReqId1);
    preReqOrderShipments1.add(new ShipmentDto());
    preReqOrderShipments1.forEach(shipment -> shipment.setLineItems(emptyList()));
  }

  private Set<VersionObjectReferenceDto> verifyEmergencyReqResult() {
    ArgumentCaptor<RequisitionV2Dto> captor = ArgumentCaptor.forClass(RequisitionV2Dto.class);
    verify(siglusUsageReportService).searchUsageReport(captor.capture());
    return captor.getValue().getAvailableProducts();
  }

  private Requisition createRequisition() {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .build();
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setEmergency(false);
    requisition.setProgramId(programId);
    requisition.setFacilityId(facilityId);
    requisition.setRequisitionLineItems(Lists.newArrayList(lineItem));
    requisition.setTemplate(createTemplate());
    requisition.setProcessingPeriodId(processingPeriodId);
    Map<String, Object> extraData = new HashMap<>();
    extraData.put("actualStartDate", "2020-01-23");
    extraData.put("actualEndDate", "2020-01-30");
    requisition.setExtraData(extraData);
    return requisition;
  }

  private Requisition createMockRequisition() {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .build();
    Requisition requisition = mock(Requisition.class);
    when(requisition.getRequisitionLineItems()).thenReturn(Lists.newArrayList(lineItem));
    when(requisition.getId()).thenReturn(requisitionId);
    when(requisition.getProgramId()).thenReturn(programId);
    when(requisition.getFacilityId()).thenReturn(facilityId);
    when(requisition.getTemplate()).thenReturn(createTemplate());
    when(requisition.getProcessingPeriodId()).thenReturn(processingPeriodId);
    when(requisition.getActualStartDate()).thenReturn(LocalDate.of(2020, 1, 23));
    when(requisition.getActualEndDate()).thenReturn(LocalDate.of(2020, 1, 30));
    List<Requisition> previousRequisions = new ArrayList<>();
    previousRequisions.add(createRequisition());
    when(requisition.getPreviousRequisitions()).thenReturn(previousRequisions);
    RequisitionLineItem lineItemCreated = new RequisitionLineItemDataBuilder().build();
    when(requisition.constructLineItem(any(), anyInt(), anyInt(), any(), anyInt(), any(),
        any(), any(), any(), any(), any())).thenReturn(lineItemCreated);

    return requisition;
  }

  private VersionObjectReferenceDto createVersionObjectReferenceDto(UUID id, Long version) {
    return new VersionObjectReferenceDto(id, null, "orderables", version);
  }

  private RequisitionV2Dto createRequisitionV2Dto() {
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference1);
    products.add(productVersionObjectReference2);

    RequisitionV2Dto requisitionV2Dto = new RequisitionV2Dto();
    ObjectReferenceDto facility = new ObjectReferenceDto(facilityId);
    requisitionV2Dto.setFacility(facility);
    ObjectReferenceDto program = new ObjectReferenceDto(programId);
    requisitionV2Dto.setProgram(program);
    requisitionV2Dto.setEmergency(false);
    requisitionV2Dto.setAvailableProducts(products);
    requisitionV2Dto.setTemplate(createTemplateDto());
    requisitionV2Dto.setStatus(RequisitionStatus.AUTHORIZED);
    requisitionV2Dto.setId(requisitionId);
    return requisitionV2Dto;
  }

  private BasicRequisitionTemplateDto createTemplateDto() {
    BasicRequisitionTemplateDto templateDto = new BasicRequisitionTemplateDto();
    templateDto.setId(templateId);
    return templateDto;
  }

  private RequisitionTemplateExtension createTemplateExtension() {
    RequisitionTemplateExtension extension = new RequisitionTemplateExtension();
    extension.setRequisitionTemplateId(templateId);
    extension.setEnableConsultationNumber(true);
    extension.setEnableKitUsage(true);
    extension.setEnablePatientLineItem(true);
    extension.setEnableProduct(true);
    extension.setEnableRapidTestConsumption(true);
    extension.setEnableRegimen(true);
    extension.setEnableUsageInformation(true);
    return extension;
  }

  private ApprovedProductDto createApprovedProductDto(OrderableDto orderable, MetadataDto meta) {
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    productDto.setOrderable(orderable);
    return productDto;
  }

  private MetadataDto createMetadataDto() {
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(1L);
    return meta;
  }

  private OrderableDto createOrderableDto(MetadataDto meta) {
    OrderableDto orderable = new OrderableDto();
    orderable.setId(orderableId2);
    orderable.setMeta(meta);
    return orderable;
  }

  private ApproveProductsAggregator createApproveProductsAggregator(UUID orderableId) {
    MetadataDto meta = createMetadataDto();
    OrderableDto orderable = createOrderableDto(meta);
    ApprovedProductDto productDto = createApprovedProductDto(orderable, meta);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto);
    return new ApproveProductsAggregator(list, programId);
  }

  private ApproveProductsAggregator createApproveProductsAggregator() {
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(1L);
    OrderableDto orderable = new OrderableDto();
    orderable.setId(productId2);
    orderable.setMeta(meta);
    ApprovedProductDto productDto = new ApprovedProductDto();
    productDto.setId(UUID.randomUUID());
    productDto.setMeta(meta);
    productDto.setOrderable(orderable);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto);
    return new ApproveProductsAggregator(list, programId);
  }


  private SupervisoryNodeDto createSupervisoryNodeDto() {
    SupervisoryNodeDto nodeDto = new SupervisoryNodeDto();
    nodeDto.setId(supervisoryNodeId);
    return nodeDto;
  }

  private ProgramDto createProgram() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    return programDto;
  }

  private SiglusProgramDto createSiglusProgramDto() {
    SiglusProgramDto siglusProgramDto = new SiglusProgramDto();
    siglusProgramDto.setId(programId);
    siglusProgramDto.setIsVirtual(true);
    return siglusProgramDto;
  }

  private FacilityDto createFacility() {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    return facilityDto;
  }

  private FacilityDto createUserFacility() {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(userFacilityId);
    return facilityDto;
  }

  private UserDto createUserDto() {
    userDto.setId(userId);
    userDto.setHomeFacilityId(userFacilityId);
    return userDto;
  }

  private RequisitionTemplate createTemplate() {
    return baseTemplateBuilder()
        .withNumberOfPeriodsToAverage(3)
        .withPopulateStockOnHandFromStockCards(true)
        .build();

  }

  private RequisitionTemplateDataBuilder baseTemplateBuilder() {

    return new RequisitionTemplateDataBuilder()
        .withRequiredColumns()
        .withAssignment(UUID.randomUUID(), UUID.randomUUID());
  }

  private ProcessingPeriodDto createProcessingPeriod() {
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setId(processingPeriodId);
    return processingPeriodDto;
  }

  private List<StockCardRangeSummaryDto> createStockCardRangeSummaryList() {
    List<StockCardRangeSummaryDto> list = new ArrayList<>();
    list.add(new StockCardRangeSummaryDtoDataBuilder().buildAsDto());
    return list;
  }

  private List<IdealStockAmountDto> createIdealStockAmountDtoList() {
    List<IdealStockAmountDto> list = new ArrayList<>();
    list.add(new IdealStockAmountDtoDataBuilder().buildAsDto());
    return list;
  }
}
