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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderLineItem;
import org.openlmis.fulfillment.service.OrderService;
import org.openlmis.requisition.domain.AvailableRequisitionColumnOption;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.SourceType;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicProcessingPeriodDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.MinimalFacilityDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.ProofOfDeliveryService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ShipmentFulfillmentService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.IdealStockAmountReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.RightReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisingUsersReferenceDataService;
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
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.Message;
import org.siglus.common.util.SimulateAuthenticationHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.domain.KitUsageLineItemDraft;
import org.siglus.siglusapi.domain.RequisitionDraft;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemDraft;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItemDraft;
import org.siglus.siglusapi.domain.UsageInformationLineItemDraft;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.i18n.MessageService;
import org.siglus.siglusapi.repository.RequisitionDraftRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusNotificationNotificationService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.fc.FcIntegrationCmmCpService;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
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

  private static final String REQUISITION_NUMBER = "requisitionNumber";
  private static final String CODE = "code";
  private static final String MESSAGE = "message";

  public static final String SUGGESTED_QUANTITY_COLUMN = "suggestedQuantity";

  @Rule
  public ExpectedException exception = ExpectedException.none();

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
  private SiglusProgramService siglusProgramService;

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
  private RequisitionExtensionRepository requisitionExtensionRepository;

  @Mock
  private RequisitionV2Controller requisitionV2Controller;

  @Mock
  private RequisitionDraftRepository draftRepository;

  @Mock
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private OperatePermissionService operatePermissionService;

  @Mock
  private SiglusNotificationService notificationService;

  @Mock
  private RightReferenceDataService rightReferenceDataService;

  @Mock
  private RoleReferenceDataService roleReferenceDataService;

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Mock
  private RequisitionGroupReferenceDataService requisitionGroupReferenceDataService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;

  @Captor
  private ArgumentCaptor<RequisitionExtension> requisitionExtensionCaptor;

  @Mock
  private SupervisingUsersReferenceDataService supervisingUsersReferenceDataService;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private RequisitionSimamEmailService requisitionSimamEmailService;

  @Mock
  private MessageService messageService;

  @Mock
  private SiglusNotificationNotificationService siglusNotificationNotificationService;

  @Mock
  private MessageSource messageSource;

  @Mock
  private RegimenDataProcessor regimenDataProcessor;

  @Mock
  private SiglusApprovedProductReferenceDataService siglusApprovedReferenceDataService;

  @Mock
  private FcIntegrationCmmCpService fcIntegrationCmmCpService;

  @Mock
  private OrderService orderService;

  @Mock
  private OrderableKitRepository orderableKitRepository;

  @Mock
  private SiglusFilterAddProductForEmergencyService filterProductService;

  @Captor
  private ArgumentCaptor<Requisition> requisitionArgumentCaptor;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID facilityId2 = UUID.randomUUID();

  private final UUID userFacilityId = UUID.randomUUID();

  private final UUID userId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID orderableId2 = UUID.randomUUID();

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID templateId = UUID.randomUUID();

  private final UUID supervisoryNodeId = UUID.randomUUID();

  private final UUID supervisoryNodeId2 = UUID.randomUUID();

  private final UUID childSupervisoryNodeId = UUID.randomUUID();

  private final UUID requisitionGroupId = UUID.randomUUID();

  private final UUID requisitionGroupId2 = UUID.randomUUID();

  private final UUID childRequisitionGroupId = UUID.randomUUID();

  private final UUID processingPeriodId = UUID.randomUUID();

  private final UUID regimenId = UUID.randomUUID();

  private final String rowName = "1stLinhas";

  private final UUID productId1 = UUID.randomUUID();
  private final Long productVersion1 = 1L;
  private final UUID productId2 = UUID.randomUUID();
  private final Long productVersion2 = 1L;
  private final VersionObjectReferenceDto productVersionObjectReference1
      = createVersionObjectReferenceDto(productId1, productVersion1);
  private final VersionObjectReferenceDto productVersionObjectReference2
      = createVersionObjectReferenceDto(productId2, productVersion2);

  private final String profilerName = "GET_REQUISITION_TO_APPROVE";

  private final Profiler profiler = new Profiler(profilerName);

  private final UserDto userDto = new UserDto();

  private final ProgramDto programDto = new ProgramDto();

  private final FacilityDto facilityDto = new FacilityDto();

  private final RequisitionV2Dto requisitionV2Dto = createRequisitionV2Dto();

  private final Requisition requisition = createRequisition();

  private SiglusRequisitionDto siglusRequisitionDto;

  @Captor
  private ArgumentCaptor<SiglusRequisitionDto> siglusRequisitionDtoCaptor;

  //fields for emergency req test start
  private BasicRequisitionDto newBasicReq;

  private final UUID preReqId1 = UUID.randomUUID();

  private final UUID preReqId2 = UUID.randomUUID();

  private BasicRequisitionDto previousBasicReq1;

  private BasicRequisitionDto previousBasicReq2;

  private RequisitionV2Dto previousV2Req1;

  private final long preReqProduct1ApprovedQuantity = randomQuantity();

  private final List<OrderDto> preReqOrders = new ArrayList<>();

  private final UUID rightId = UUID.randomUUID();

  private final UUID roleId = UUID.randomUUID();

  private final UUID orderId = UUID.randomUUID();

  @Mock
  private OrderFulfillmentService orderFulfillmentService;

  @Mock
  private ShipmentFulfillmentService shipmentFulfillmentService;
  //fields for emergency req test end

  @Mock
  private OrderExternalRepository orderExternalRepository;

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
    when(requisitionService.getApproveProduct(any(), any(), anyBoolean()))
        .thenReturn(createApproveProductsAggregator());
    when(supervisoryNodeService.findOne(supervisoryNodeId)).thenReturn(createSupervisoryNodeDto());
    when(draftRepository.findByRequisitionId(any(UUID.class))).thenReturn(null);
    when(operatePermissionService.canSubmit(any())).thenReturn(true);
    when(operatePermissionService.isEditable(any())).thenReturn(false);
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto(facilityId));
    when(orderableKitRepository.findAllKitProduct()).thenReturn(Collections.emptyList());
    when(orderExternalRepository.findByRequisitionId(any())).thenReturn(newArrayList());
    when(siglusRequisitionRequisitionService.getPreviousEmergencyRequisition(any(), any(), any()))
        .thenReturn(Arrays.asList(new RequisitionV2Dto()));
    mockSearchOrder();
  }

  @Test
  public void shouldActivateArchivedProducts() {
    Requisition requisition = createRequisition();
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);

    siglusRequisitionService.activateArchivedProducts(requisitionId, facilityId);

    verify(archiveProductService).activateProducts(facilityId, Sets.newHashSet(orderableId));
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
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionId))
        .thenReturn(REQUISITION_NUMBER);

    SiglusRequisitionDto response = siglusRequisitionService.searchRequisition(requisitionId);

    verify(siglusRequisitionRequisitionService).searchRequisition(requisitionId);
    verify(requisitionTemplateExtensionRepository).findByRequisitionTemplateId(templateId);
    verify(requisitionController).getProfiler(profilerName, requisitionId);
    verify(requisitionController).findRequisition(requisitionId, profiler);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());
    verify(supervisoryNodeService).findOne(null);
    verify(requisitionService).validateCanApproveRequisition(requisition, userDto.getId());

    Set<VersionObjectReferenceDto> availableProducts = response.getAvailableProducts();
    assertEquals(1, availableProducts.size());
    assertTrue(availableProducts.contains(productVersionObjectReference2));
    assertEquals(REQUISITION_NUMBER, response.getRequisitionNumber());
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
    when(siglusProgramService.getProgram(programId)).thenReturn(createProgramDto());
    when(periodService.getPeriod(processingPeriodId)).thenReturn(createProcessingPeriod());
    when(stockCardRangeSummaryStockManagementService.search(any(), any(), any(), any(), any(),
        any())).thenReturn(createStockCardRangeSummaryList());
    List<ProcessingPeriodDto> processingPeriodDtos = new ArrayList<>();
    when(periodService.getPeriods(any())).thenReturn(processingPeriodDtos);
    when(simulateAuthenticationHelper.simulateCrossServiceAuth()).thenReturn(null);
    when(proofOfDeliveryService.get(any())).thenReturn(new ProofOfDeliveryDto());
    when(idealStockAmountReferenceDataService.search(facilityId, processingPeriodId))
        .thenReturn(createIdealStockAmountDtoList());
    when(siglusApprovedReferenceDataService.getApprovedProducts(any(), any(),
        anyCollection(), anyBoolean()))
        .thenReturn(getApprovedProductList());
    when(requisitionService.validateCanApproveRequisition(any(), any()))
        .thenReturn(new ValidationResult());
    when(siglusOrderableService.getOrderableExpirationDate(any(), any()))
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
  public void shouldCallUsageReportInitialAndSetRequisitionNumberWhenInitialRequisition() {
    // given
    UUID suggestedPeriod = UUID.randomUUID();
    String physicalInventoryDateStr = "date_str";
    HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    HttpServletResponse httpServletResponse = new MockHttpServletResponse();
    when(requisitionV2Controller.initiate(programId, facilityId, suggestedPeriod, true,
        physicalInventoryDateStr, httpServletRequest, httpServletResponse))
        .thenReturn(requisitionV2Dto);
    when(siglusUsageReportService.initiateUsageReport(requisitionV2Dto))
        .thenReturn(SiglusRequisitionDto.from(requisitionV2Dto));
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    when(siglusRequisitionExtensionService.createRequisitionExtension(requisitionId, false,
        facilityId)).thenReturn(requisitionExtension);
    when(siglusRequisitionExtensionService.formatRequisitionNumber(requisitionExtension))
        .thenReturn(REQUISITION_NUMBER);

    // when
    SiglusRequisitionDto siglusRequisitionDto = siglusRequisitionService.initiate(programId,
        facilityId, suggestedPeriod, true, physicalInventoryDateStr, httpServletRequest,
        httpServletResponse);

    // then
    verify(requisitionV2Controller).initiate(programId, facilityId, suggestedPeriod, true,
        physicalInventoryDateStr, httpServletRequest, httpServletResponse);
    verify(siglusUsageReportService).initiateUsageReport(requisitionV2Dto);
    assertEquals(REQUISITION_NUMBER, siglusRequisitionDto.getRequisitionNumber());
  }

  @Test
  public void shouldCallDeleteUsageReportWhenDeleteRequisition() {
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
    verify(siglusRequisitionExtensionService).deleteRequisitionExtension(requisitionId);
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
    when(operatePermissionService.isEditable(any())).thenReturn(true);
    when(siglusUsageReportService.searchUsageReport(any(RequisitionV2Dto.class)))
        .thenAnswer(i -> convert((RequisitionV2Dto) i.getArguments()[0]));
    when(siglusRequisitionRequisitionService.getPreviousEmergencyRequisition(any(), any(), any()))
        .thenReturn(Arrays.asList(new RequisitionV2Dto()));

    // when
    SiglusRequisitionDto requisition = siglusRequisitionService.searchRequisition(requisitionId);

    // then
    Set<VersionObjectReferenceDto> availableProducts = requisition.getAvailableProducts();
    verify(siglusRequisitionRequisitionService)
        .getPreviousEmergencyRequisition(any(), any(), any());
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterNoProductWhenGetEmergencyRequisitionGivenFullyShippedProduct1() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    List<BasicRequisitionDto> emergencyRequisitions = asList(newBasicReq, previousBasicReq1);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(emergencyRequisitions));
    when(filterProductService.getNotFullyShippedProducts(any())).thenReturn(Collections.emptySet());

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService)
        .getPreviousEmergencyRequisition(any(), any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(2, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
    assertThat(availableProducts,
        hasItems(productVersionObjectReference1));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenNotFullyFulfilledProduct1() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    when(filterProductService.getInProgressProducts(any())).thenReturn(Collections.emptySet());
    when(filterProductService.getNotFullyShippedProducts(any()))
        .thenReturn(Sets.newHashSet(productId1));

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService)
        .getPreviousEmergencyRequisition(any(), any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenProduct1ReqInProgress() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(IN_APPROVAL);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockProduct1ReqInProgress();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService)
        .getPreviousEmergencyRequisition(any(), any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFilterProduct1WhenGetEmergencyRequisitionGivenProduct1OrderInProgress() {
    // given
    mockEmergencyRequisition();
    mockPreviousEmergencyRequisition1(RELEASED);
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(asList(newBasicReq, previousBasicReq1)));
    mockProduct1OrderInProgress();

    // when
    siglusRequisitionService.searchRequisition(requisitionId);

    // then
    verify(siglusRequisitionRequisitionService)
        .getPreviousEmergencyRequisition(any(), any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(1, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference2));
  }

  @Test
  public void shouldFillDraftWhenRequisitionCanEditAndHaveDraft() {
    // given
    mockEmergencyRequisition();
    when(requisitionService
        .validateCanApproveRequisition(any(), any())).thenReturn(ValidationResult.success());
    when(siglusRequisitionRequisitionService.searchRequisitions(any(), any()))
        .thenReturn(new PageImpl<>(singletonList(newBasicReq)));
    when(operatePermissionService.isEditable(any())).thenReturn(true);
    RequisitionDraft draft = getRequisitionDraft(requisitionId);
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(UUID.randomUUID());
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    when(draftRepository.findRequisitionDraftByRequisitionIdAndFacilityId(requisitionId,
        userDto.getHomeFacilityId())).thenReturn(draft);
    when(regimenDataProcessor.getRegimenDtoMap()).thenReturn(mockRegimenMap());

    // when
    SiglusRequisitionDto requisitionDto = siglusRequisitionService.searchRequisition(requisitionId);

    // then
    assertEquals(draft.getKitUsageLineItems().get(0).getValue(),
        requisitionDto.getKitUsageLineItems().get(0).getServices().get("HF").getValue());
    assertEquals("draft status", requisitionDto.getDraftStatusMessage());
    UsageInformationLineItemDraft usageLineItemDraft = draft.getUsageInformationLineItemDrafts()
        .get(0);
    assertEquals(usageLineItemDraft.getValue(),
        requisitionDto.getUsageInformationLineItems().get(0).getInformations().get("information")
            .getOrderables().get(usageLineItemDraft.getOrderableId()).getValue());
    TestConsumptionLineItemDraft testConsumptionLineItemDraft =
        draft.getTestConsumptionLineItemDrafts().get(0);
    assertEquals(testConsumptionLineItemDraft.getValue(),
        requisitionDto.getTestConsumptionLineItems().get(0).getProjects().get("HIV Determine")
            .getOutcomes().get("Positive").getValue());
  }

  @Test
  public void shouldSubmitRequisition() {
    // given
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    MinimalFacilityDto facilityDto = new MinimalFacilityDto();
    facilityDto.setId(UUID.randomUUID());
    mockBasicRequisitionDto.setFacility(facilityDto);
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(requisitionController.submitRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);
    requisition.setRequisitionLineItems(emptyList());
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(requisitionV2Controller
        .updateRequisition(requisitionId, siglusRequisitionDto, request, response))
        .thenReturn(requisitionV2Dto);

    // when
    BasicRequisitionDto requisitionDto = siglusRequisitionService
        .submitRequisition(requisitionId, request, response);

    // then
    verify(requisitionController).submitRequisition(requisitionId, request, response);
    verify(siglusUsageReportService).saveUsageReportWithValidation(any(), any());
    verify(archiveProductService).activateProducts(any(), any());
    verify(notificationService).postSubmit(requisitionDto);
  }

  @Test
  public void shouldAuthorizeRequisition() {
    // given
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setId(requisitionId);
    MinimalFacilityDto facilityDto = new MinimalFacilityDto();
    facilityDto.setId(facilityId);
    mockBasicRequisitionDto.setFacility(facilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode(CODE);
    mockBasicRequisitionDto.setProgram(programDto);
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(requisitionController.authorizeRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setId(UUID.randomUUID());
    siglusRequisitionDto.setRequisitionLineItems(Arrays.asList(lineItem));
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(siglusRequisitionDto);
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    RequisitionLineItemExtension requisitionLineItemExtension = RequisitionLineItemExtension
        .builder()
        .requisitionLineItemId(lineItem.getId())
        .authorizedQuantity(10)
        .build();
    when(lineItemExtensionRepository.findLineItems(singletonList(lineItem.getId())))
        .thenReturn(singletonList(requisitionLineItemExtension));
    when(requisitionV2Controller
        .updateRequisition(requisitionId, siglusRequisitionDto, request, response))
        .thenReturn(requisitionV2Dto);
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeReferenceDataService.findSupervisoryNode(programId, facilityId))
        .thenReturn(supervisoryNodeDto);
    RightDto right = new RightDto();
    right.setId(rightId);
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(right);
    when(programReferenceDataService.findOne(programId)).thenReturn(programDto);
    when(messageService.localize(any(Message.class))).thenAnswer(invocation -> {
      Message message = invocation.getArgumentAt(0, Message.class);
      return message.localMessage(messageSource, Locale.ENGLISH);
    });
    when(messageSource.getMessage(any(), any(), any()))
        .thenReturn(MESSAGE);
    when(siglusUsageReportService.saveUsageReportWithValidation(any(), any()))
        .thenReturn(siglusRequisitionDto);

    // when
    BasicRequisitionDto requisitionDto = siglusRequisitionService
        .authorizeRequisition(requisitionId, request, response);

    // then
    verify(requisitionController).authorizeRequisition(requisitionId, request, response);
    verify(siglusUsageReportService).saveUsageReportWithValidation(any(), any());
    verify(archiveProductService).activateProducts(any(), any());
    verify(notificationService).postAuthorize(requisitionDto);
    verify(requisitionSimamEmailService).prepareEmailAttachmentsForSimam(any(), any());
  }

  @Test
  public void shouldRevertRequisitionWhenReject() {
    // given
    UUID requisitionId = UUID.randomUUID();
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    BasicRequisitionDto dto = new BasicRequisitionDto();
    when(requisitionController.rejectRequisition(requisitionId, request, response))
        .thenReturn(dto);
    RequisitionLineItem lineItem = new RequisitionLineItem();
    lineItem.setId(UUID.randomUUID());
    lineItem.setBeginningBalance(10);
    lineItem.setApprovedQuantity(20);
    lineItem.setRemarks("123");
    lineItem.setSkipped(true);
    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Arrays.asList(lineItem));
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);

    // when
    siglusRequisitionService.rejectRequisition(requisitionId, request, response);

    // then
    verify(requisitionRepository).save(requisitionArgumentCaptor.capture());
    RequisitionLineItem lineItemCaptor = requisitionArgumentCaptor.getValue()
        .getRequisitionLineItems().get(0);
    assertEquals(Integer.valueOf(10), lineItemCaptor.getBeginningBalance());
    assertEquals(null, lineItemCaptor.getRemarks());
    assertEquals(false, lineItemCaptor.getSkipped());
    assertEquals(null, lineItemCaptor.getApprovedQuantity());
    verify(notificationService).postReject(dto);
  }

  @Test
  public void shouldApproveRequisition() {
    // given
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setId(requisitionId);
    MinimalFacilityDto facilityDto = new MinimalFacilityDto();
    facilityDto.setId(facilityId);
    mockBasicRequisitionDto.setFacility(facilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode(CODE);
    mockBasicRequisitionDto.setProgram(programDto);
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(requisitionController.approveRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(siglusRequisitionDto);
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto(facilityId));
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setApprovedQuantity(10);
    lineItem.setId(UUID.randomUUID());
    siglusRequisitionDto.setRequisitionLineItems(Arrays.asList(lineItem));
    siglusRequisitionDto.setStatus(AUTHORIZED);
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(requisitionV2Controller
        .updateRequisition(any(UUID.class), any(SiglusRequisitionDto.class),
            any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenReturn(requisitionV2Dto);
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeReferenceDataService.findSupervisoryNode(programId, facilityId))
        .thenReturn(supervisoryNodeDto);
    RightDto right = new RightDto();
    right.setId(rightId);
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(right);
    when(programReferenceDataService.findOne(programId)).thenReturn(programDto);
    when(messageService.localize(any(Message.class))).thenAnswer(invocation -> {
      Message message = invocation.getArgumentAt(0, Message.class);
      return message.localMessage(messageSource, Locale.ENGLISH);
    });
    when(messageSource.getMessage(any(), any(), any()))
        .thenReturn(MESSAGE);
    when(siglusUsageReportService.saveUsageReportWithValidation(any(), any()))
        .thenReturn(siglusRequisitionDto);

    // when
    siglusRequisitionService.approveRequisition(requisitionId, request, response);

    // then
    verify(requisitionV2Controller).updateRequisition(any(UUID.class),
        siglusRequisitionDtoCaptor.capture(), any(HttpServletRequest.class),
        any(HttpServletResponse.class));
    SiglusRequisitionDto dto = siglusRequisitionDtoCaptor.getValue();
    assertEquals(Integer.valueOf(10), dto.getRequisitionLineItems().get(0).getApprovedQuantity());
    verify(requisitionController).approveRequisition(requisitionId, request, response);
    verify(siglusUsageReportService).saveUsageReportWithValidation(any(), any());
    verify(archiveProductService).activateProducts(any(), any());
    verify(requisitionSimamEmailService).prepareEmailAttachmentsForSimam(any(), any());
  }

  @Test
  public void shouldDeleteDraftIfDraftExistWhenApproval() {
    // given
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setId(requisitionId);
    MinimalFacilityDto facilityDto = new MinimalFacilityDto();
    facilityDto.setId(facilityId);
    mockBasicRequisitionDto.setFacility(facilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode(CODE);
    mockBasicRequisitionDto.setProgram(programDto);
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(requisitionController.approveRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);
    requisition.setRequisitionLineItems(emptyList());
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(requisitionV2Controller
        .updateRequisition(any(UUID.class), any(RequisitionV2Dto.class),
            any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenReturn(requisitionV2Dto);
    when(draftRepository.findByRequisitionId(requisitionId))
        .thenReturn(getRequisitionDraft(requisitionId));
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto(facilityId));
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeReferenceDataService.findSupervisoryNode(programId, facilityId))
        .thenReturn(supervisoryNodeDto);
    RightDto right = new RightDto();
    right.setId(rightId);
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(right);
    when(programReferenceDataService.findOne(programId)).thenReturn(programDto);
    when(messageService.localize(any(Message.class))).thenAnswer(invocation -> {
      Message message = invocation.getArgumentAt(0, Message.class);
      return message.localMessage(messageSource, Locale.ENGLISH);
    });
    when(messageSource.getMessage(any(), any(), any()))
        .thenReturn(MESSAGE);
    when(siglusUsageReportService.saveUsageReportWithValidation(any(), any()))
        .thenReturn(siglusRequisitionDto);
    when(regimenDataProcessor.getRegimenDtoMap()).thenReturn(mockRegimenMap());

    // when
    siglusRequisitionService.approveRequisition(requisitionId, request, response);

    // then
    verify(draftRepository).delete(any(UUID.class));
    verify(requisitionController).approveRequisition(requisitionId, request, response);
    verify(archiveProductService).activateProducts(any(), any());
    verify(requisitionSimamEmailService).prepareEmailAttachmentsForSimam(any(), any());
  }

  @Test
  public void shouldSaveDraftWhenRequisitionUpdate() {
    // given
    OrderableDto productDto = new OrderableDto();
    productDto.setId(UUID.randomUUID());
    RequisitionLineItemV2Dto lineItemV2Dto = new RequisitionLineItemV2Dto();
    lineItemV2Dto.setOrderable(productDto);
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setId(UUID.randomUUID());
    lineItemV2Dto.setApprovedProduct(approvedProductDto);
    lineItemV2Dto.setId(UUID.randomUUID());
    lineItemV2Dto.setAuthorizedQuantity(10);
    siglusRequisitionDto.setRequisitionLineItems(singletonList(lineItemV2Dto));
    when(operatePermissionService.canSubmit(siglusRequisitionDto)).thenReturn(false);
    RequisitionTemplate requisitionTemplate = new RequisitionTemplate();
    requisitionTemplate.setId(templateId);
    RequisitionTemplateExtension templateExtension = createTemplateExtension();
    requisitionTemplate.setTemplateExtension(templateExtension);
    requisition.setTemplate(requisitionTemplate);
    when(requisitionRepository.findOne(siglusRequisitionDto.getId()))
        .thenReturn(requisition);
    when(requisitionTemplateExtensionRepository.findByRequisitionTemplateId(
        siglusRequisitionDto.getId())).thenReturn(templateExtension);
    RequisitionDraft draft = getRequisitionDraft(siglusRequisitionDto.getId());
    when(draftRepository.save(any(RequisitionDraft.class))).thenReturn(draft);
    when(regimenDataProcessor.getRegimenDtoMap()).thenReturn(mockRegimenMap());

    // when
    SiglusRequisitionDto requisitionDto = siglusRequisitionService.updateRequisition(
        siglusRequisitionDto.getId(), siglusRequisitionDto, new MockHttpServletRequest(),
        new MockHttpServletResponse());

    // then
    assertEquals(Integer.valueOf(20),
        requisitionDto.getLineItems().get(0).getApprovedQuantity());
    UsageInformationLineItemDraft usageLineItemDraft = draft.getUsageInformationLineItemDrafts()
        .get(0);
    assertEquals(usageLineItemDraft.getValue(),
        requisitionDto.getUsageInformationLineItems().get(0).getInformations().get("information")
            .getOrderables().get(usageLineItemDraft.getOrderableId()).getValue());
    TestConsumptionLineItemDraft testConsumptionLineItemDraft =
        draft.getTestConsumptionLineItemDrafts().get(0);
    assertEquals(testConsumptionLineItemDraft.getValue(),
        requisitionDto.getTestConsumptionLineItems().get(0).getProjects().get("HIV Determine")
            .getOutcomes().get("Positive").getValue());
  }

  @Test
  public void shouldReceiveExceptionWhenRequisitionIdIsError() {
    exception.expect(ValidationMessageException.class);

    siglusRequisitionService.updateRequisition(UUID.randomUUID(), siglusRequisitionDto,
        new MockHttpServletRequest(), new MockHttpServletResponse());
  }

  @Test
  public void shouldGetFacilitiesForInternalApproval() {
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(mockRightDto());
    when(roleReferenceDataService.search(rightId)).thenReturn(mockRoleDto());
    when(authenticationHelper.getCurrentUser()).thenReturn(mockInternalUserDto());
    when(supervisoryNodeReferenceDataService.findAllSupervisoryNodes())
        .thenReturn(mockAllSupervisoryNodeForInternal());
    when(requisitionGroupReferenceDataService.findAll()).thenReturn(mockAllRequisitionGroup());
    when(facilityReferenceDataService.findAll()).thenReturn(mockAllFacilityDto());

    List<FacilityDto> response = siglusRequisitionService.searchFacilitiesForApproval();

    assertEquals(1, response.size());
    assertEquals(response.stream().findFirst().get().getId(), userFacilityId);
  }

  @Test
  public void shouldGetFacilitiesForExternalApproval() {
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(mockRightDto());
    when(roleReferenceDataService.search(rightId)).thenReturn(mockRoleDto());
    when(authenticationHelper.getCurrentUser()).thenReturn(mockExternalUserDto());
    when(supervisoryNodeReferenceDataService.findAllSupervisoryNodes())
        .thenReturn(mockAllSupervisoryNodeForExternal());
    when(requisitionGroupReferenceDataService.findAll()).thenReturn(mockAllRequisitionGroup());
    when(facilityReferenceDataService.findAll()).thenReturn(mockAllFacilityDto());

    List<FacilityDto> response = siglusRequisitionService.searchFacilitiesForApproval();

    Set<UUID> uuids = response.stream().map(FacilityDto::getId).collect(Collectors.toSet());
    assertEquals(2, response.size());
    assertTrue(uuids.contains(facilityId));
    assertTrue(uuids.contains(userFacilityId));
  }

  @Test
  public void shouldGetFacilitiesForInternalAndExternalApproval() {
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(mockRightDto());
    when(roleReferenceDataService.search(rightId)).thenReturn(mockRoleDto());
    when(authenticationHelper.getCurrentUser()).thenReturn(mockInternalAndExternalUserDto());
    when(supervisoryNodeReferenceDataService.findAllSupervisoryNodes())
        .thenReturn(mockAllSupervisoryNodeForInternalAndExternal());
    when(requisitionGroupReferenceDataService.findAll()).thenReturn(mockAllRequisitionGroup());
    when(facilityReferenceDataService.findAll()).thenReturn(mockAllFacilityDto());

    List<FacilityDto> response = siglusRequisitionService.searchFacilitiesForApproval();

    assertEquals(2, response.size());
    assertEquals(response.get(0).getId(), userFacilityId);
    assertEquals(response.get(1).getId(), facilityId);
  }

  private SiglusRequisitionDto convert(RequisitionV2Dto requisitionV2Dto) {
    return SiglusRequisitionDto.from(requisitionV2Dto);
  }

  private RequisitionDraft getRequisitionDraft(UUID requisitionId) {
    RequisitionDraft draft = new RequisitionDraft();
    draft.setId(UUID.randomUUID());
    draft.setRequisitionId(requisitionId);
    draft.setDraftStatusMessage("draft status");
    RequisitionLineItemDraft lineItemDraft1 = new RequisitionLineItemDraft();
    lineItemDraft1.setRequisitionLineItemId(UUID.randomUUID());
    lineItemDraft1.setOrderable(
        new VersionEntityReference(UUID.randomUUID(), (long) 1));
    lineItemDraft1.setFacilityTypeApprovedProduct(new VersionEntityReference(UUID.randomUUID(),
        (long) 1));
    lineItemDraft1.setStockAdjustments(emptyList());
    lineItemDraft1.setApprovedQuantity(20);
    draft.setLineItems(Arrays.asList(lineItemDraft1));
    KitUsageLineItemDraft usageLineItemDraft = getKitUsageLineItemDraft();
    draft.setKitUsageLineItems(Arrays.asList(usageLineItemDraft));
    UsageInformationLineItemDraft usageLineItem = getUsageInformationLineItemDraft();
    draft.setUsageInformationLineItemDrafts(Arrays.asList(usageLineItem));
    TestConsumptionLineItemDraft testConsumptionLineItemDraft = getTestConsumptionLineItemDraft();
    draft.setTestConsumptionLineItemDrafts(Arrays.asList(testConsumptionLineItemDraft));
    return draft;
  }

  private KitUsageLineItemDraft getKitUsageLineItemDraft() {
    KitUsageLineItemDraft usageLineItemDraft = new KitUsageLineItemDraft();
    usageLineItemDraft.setCollection("collection");
    usageLineItemDraft.setService("HF");
    usageLineItemDraft.setValue(10);
    usageLineItemDraft.setKitUsageLineItemId(UUID.randomUUID());
    return usageLineItemDraft;
  }

  private UsageInformationLineItemDraft getUsageInformationLineItemDraft() {
    UsageInformationLineItemDraft usageLineItem =
        new UsageInformationLineItemDraft();
    usageLineItem.setUsageLineItemId(UUID.randomUUID());
    usageLineItem.setService("CHW");
    usageLineItem.setInformation("information");
    usageLineItem.setService("testService");
    usageLineItem.setValue(20);
    usageLineItem.setOrderableId(UUID.randomUUID());
    return usageLineItem;
  }

  private TestConsumptionLineItemDraft getTestConsumptionLineItemDraft() {
    TestConsumptionLineItemDraft draft = new TestConsumptionLineItemDraft();
    draft.setTestConsumptionLineItemId(UUID.randomUUID());
    draft.setService("Total");
    draft.setProject("HIV Determine");
    draft.setOutcome("Positive");
    draft.setValue(3);
    return draft;
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
    verify(siglusRequisitionRequisitionService)
        .getPreviousEmergencyRequisition(any(), any(), any());
    Set<VersionObjectReferenceDto> availableProducts = verifyEmergencyReqResult();
    assertEquals(2, availableProducts.size());
    assertThat(availableProducts,
        hasItems(productVersionObjectReference1, productVersionObjectReference2));
  }

  @Test
  public void shouldSaveIsApprovedByInternalIsTrueWhenUserIdEqualRequsitionFacilityId() {
    // given
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setId(requisitionId);
    MinimalFacilityDto facilityDto = new MinimalFacilityDto();
    facilityDto.setId(facilityId);
    mockBasicRequisitionDto.setFacility(facilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode(CODE);
    mockBasicRequisitionDto.setProgram(programDto);
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(requisitionController.approveRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(siglusRequisitionDto);
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto(facilityId));
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setApprovedQuantity(10);
    lineItem.setId(UUID.randomUUID());
    siglusRequisitionDto.setRequisitionLineItems(Arrays.asList(lineItem));
    siglusRequisitionDto.setStatus(AUTHORIZED);
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(requisitionV2Controller
        .updateRequisition(any(UUID.class), any(SiglusRequisitionDto.class),
            any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenReturn(requisitionV2Dto);
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeReferenceDataService.findSupervisoryNode(programId, facilityId))
        .thenReturn(supervisoryNodeDto);
    RightDto right = new RightDto();
    right.setId(rightId);
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(right);
    when(programReferenceDataService.findOne(programId)).thenReturn(programDto);
    when(messageService.localize(any(Message.class))).thenAnswer(invocation -> {
      Message message = invocation.getArgumentAt(0, Message.class);
      return message.localMessage(messageSource, Locale.ENGLISH);
    });
    when(messageSource.getMessage(any(), any(), any()))
        .thenReturn(MESSAGE);
    when(siglusUsageReportService.saveUsageReportWithValidation(any(), any()))
        .thenReturn(siglusRequisitionDto);

    // when
    siglusRequisitionService
        .approveRequisition(requisitionId, request, response);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionCaptor.capture());
    RequisitionExtension captorValue = requisitionExtensionCaptor.getValue();
    assertEquals(Boolean.TRUE, captorValue.getIsApprovedByInternal());
  }


  @Test
  public void shouldSaveIsApprovedByInternalIsFalseWhenUserIdNotEqualRequsitionFacilityId() {
    // given
    BasicRequisitionDto mockBasicRequisitionDto = new BasicRequisitionDto();
    mockBasicRequisitionDto.setId(requisitionId);
    MinimalFacilityDto facilityDto = new MinimalFacilityDto();
    facilityDto.setId(facilityId);
    mockBasicRequisitionDto.setFacility(facilityDto);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    programDto.setCode(CODE);
    mockBasicRequisitionDto.setProgram(programDto);
    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    when(requisitionController.approveRequisition(requisitionId, request, response))
        .thenReturn(mockBasicRequisitionDto);
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(siglusRequisitionDto);
    when(authenticationHelper.getCurrentUser()).thenReturn(mockUserDto(UUID.randomUUID()));
    RequisitionLineItemV2Dto lineItem = new RequisitionLineItemV2Dto();
    lineItem.setApprovedQuantity(10);
    lineItem.setId(UUID.randomUUID());
    siglusRequisitionDto.setRequisitionLineItems(Arrays.asList(lineItem));
    siglusRequisitionDto.setStatus(AUTHORIZED);
    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(requisitionV2Controller
        .updateRequisition(any(UUID.class), any(SiglusRequisitionDto.class),
            any(HttpServletRequest.class), any(HttpServletResponse.class)))
        .thenReturn(requisitionV2Dto);
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(supervisoryNodeId);
    when(supervisoryNodeReferenceDataService.findSupervisoryNode(programId, facilityId))
        .thenReturn(supervisoryNodeDto);
    RightDto right = new RightDto();
    right.setId(rightId);
    when(rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(right);
    when(programReferenceDataService.findOne(programId)).thenReturn(programDto);
    when(messageService.localize(any(Message.class))).thenAnswer(invocation -> {
      Message message = invocation.getArgumentAt(0, Message.class);
      return message.localMessage(messageSource, Locale.ENGLISH);
    });
    when(messageSource.getMessage(any(), any(), any()))
        .thenReturn(MESSAGE);
    when(siglusUsageReportService.saveUsageReportWithValidation(any(), any()))
        .thenReturn(siglusRequisitionDto);

    // when
    siglusRequisitionService
        .approveRequisition(requisitionId, request, response);

    // then
    verify(requisitionExtensionRepository).save(requisitionExtensionCaptor.capture());
    RequisitionExtension captorValue = requisitionExtensionCaptor.getValue();
    assertEquals(Boolean.FALSE, captorValue.getIsApprovedByInternal());
  }


  private UserDto mockUserDto(UUID facilityId) {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    return userDto;
  }

  private RightDto mockRightDto() {
    RightDto rightDto = new RightDto();
    rightDto.setId(rightId);
    rightDto.setName(PermissionService.REQUISITION_APPROVE);
    return rightDto;
  }

  private List<RoleDto> mockRoleDto() {
    RoleDto roleDto = new RoleDto();
    roleDto.setId(roleId);
    roleDto.setName("Requisition Approver");
    roleDto.setRights(Sets.newHashSet(mockRightDto()));
    return Lists.newArrayList(roleDto);
  }

  private UserDto mockInternalUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(userFacilityId);
    RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
    roleAssignmentDto.setRoleId(roleId);
    roleAssignmentDto.setSupervisoryNodeId(supervisoryNodeId);
    userDto.setRoleAssignments(Sets.newHashSet(roleAssignmentDto));
    return userDto;
  }

  private UserDto mockExternalUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(facilityId2);
    RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
    roleAssignmentDto.setRoleId(roleId);
    roleAssignmentDto.setSupervisoryNodeId(supervisoryNodeId);
    userDto.setRoleAssignments(
        Sets.newHashSet(roleAssignmentDto));
    return userDto;
  }

  private UserDto mockInternalAndExternalUserDto() {
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    userDto.setHomeFacilityId(userFacilityId);
    RoleAssignmentDto roleAssignmentDto2 = new RoleAssignmentDto();
    roleAssignmentDto2.setRoleId(roleId);
    roleAssignmentDto2.setSupervisoryNodeId(supervisoryNodeId2);
    RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
    roleAssignmentDto.setRoleId(roleId);
    roleAssignmentDto.setSupervisoryNodeId(supervisoryNodeId);
    userDto.setRoleAssignments(Sets.newHashSet(roleAssignmentDto2, roleAssignmentDto));
    return userDto;
  }

  private List<SupervisoryNodeDto> mockAllSupervisoryNodeForInternal() {
    return Lists.newArrayList(
        mockSupervisoryNodeWithChildForInternal(supervisoryNodeId, supervisoryNodeId2),
        mockSupervisoryNodeDto(supervisoryNodeId2, requisitionGroupId2));
  }

  private List<SupervisoryNodeDto> mockAllSupervisoryNodeForExternal() {
    return Lists.newArrayList(
        mockSupervisoryNodeWithChild(supervisoryNodeId, supervisoryNodeId2),
        mockSupervisoryNodeDto(supervisoryNodeId2, requisitionGroupId2));
  }

  private List<SupervisoryNodeDto> mockAllSupervisoryNodeForInternalAndExternal() {
    return Lists.newArrayList(
        mockSupervisoryNodeWithChildForInternal(supervisoryNodeId, childSupervisoryNodeId),
        mockSupervisoryNodeDto(supervisoryNodeId2, requisitionGroupId2));
  }

  private List<RequisitionGroupDto> mockAllRequisitionGroup() {
    return Lists.newArrayList(
        mockRequisitionGroupDto(requisitionGroupId, userFacilityId),
        mockRequisitionGroupDto(requisitionGroupId2, facilityId));
  }

  private List<FacilityDto> mockAllFacilityDto() {
    return Lists.newArrayList(mockFacilityDto(facilityId),
        mockFacilityDto(userFacilityId));
  }

  private SupervisoryNodeDto mockSupervisoryNodeDto(UUID id, UUID groupId) {
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(id);
    supervisoryNodeDto.setRequisitionGroup(new ObjectReferenceDto(groupId));
    supervisoryNodeDto.setChildNodes(Collections.emptySet());
    return supervisoryNodeDto;
  }

  private SupervisoryNodeDto mockSupervisoryNodeWithChild(UUID id, UUID childId) {
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(id);
    supervisoryNodeDto.setRequisitionGroup(new ObjectReferenceDto(requisitionGroupId));
    SupervisoryNodeDto child = new SupervisoryNodeDto();
    child.setId(childId);
    child.setRequisitionGroup(new ObjectReferenceDto(requisitionGroupId2));
    child.setChildNodes(Collections.emptySet());
    supervisoryNodeDto.setChildNodes(
        Sets.newHashSet(new ObjectReferenceDto(childId)));
    return supervisoryNodeDto;
  }

  private SupervisoryNodeDto mockSupervisoryNodeWithChildForInternal(UUID id, UUID childId) {
    SupervisoryNodeDto supervisoryNodeDto = new SupervisoryNodeDto();
    supervisoryNodeDto.setId(id);
    supervisoryNodeDto.setRequisitionGroup(new ObjectReferenceDto(requisitionGroupId));
    supervisoryNodeDto.setParentNode(new ObjectReferenceDto());
    SupervisoryNodeDto child = new SupervisoryNodeDto();
    child.setId(childId);
    child.setRequisitionGroup(new ObjectReferenceDto(childRequisitionGroupId));
    child.setChildNodes(Collections.emptySet());
    supervisoryNodeDto.setChildNodes(
        Sets.newHashSet(new ObjectReferenceDto(childId)));
    return supervisoryNodeDto;
  }

  private RequisitionGroupDto mockRequisitionGroupDto(UUID id, UUID facilityId) {
    RequisitionGroupDto requisitionGroupDto = new RequisitionGroupDto();
    requisitionGroupDto.setId(id);
    requisitionGroupDto.setMemberFacilities(
        Sets.newHashSet(mockFacilityDto(facilityId)));
    return requisitionGroupDto;
  }

  private FacilityDto mockFacilityDto(UUID id) {
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(id);
    facilityDto.setName(RandomStringUtils.random(6));
    return facilityDto;
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
    newV2Req.setReportOnly(false);
    Set<VersionObjectReferenceDto> products = new HashSet<>();
    products.add(productVersionObjectReference1);
    products.add(productVersionObjectReference2);
    newV2Req.setAvailableProducts(products);
    newV2Req.setTemplate(createTemplateDto());
    when(siglusRequisitionRequisitionService.searchRequisition(requisitionId))
        .thenReturn(newV2Req);
    when(requisitionService.validateCanApproveRequisition(any(), any()))
        .thenReturn(ValidationResult.failedValidation("skip method"));
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
    lineItem.setPacksToShip(preReqProduct1ApprovedQuantity);
    lineItem.setApprovedQuantity((int) preReqProduct1ApprovedQuantity);
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
    lineItem.setPacksToShip(preReqProduct1ApprovedQuantity);
    lineItem.setApprovedQuantity((int) preReqProduct1ApprovedQuantity);
    previousV2Req2.setRequisitionLineItems(singletonList(lineItem));
    previousV2Req2.setTemplate(createTemplateDto());
    when(siglusRequisitionRequisitionService.searchRequisition(preReqId2))
        .thenReturn(previousV2Req2);
  }

  private void mockOrderService() {
    when(orderFulfillmentService.search(any(), any(), any(), any(), any()))
        .thenReturn(preReqOrders);
  }

  private void mockSearchOrder() {
    Order order2 = new Order();
    order2.setStatus(org.openlmis.fulfillment.domain.OrderStatus.SHIPPED);
    order2.setExternalId(preReqId2);
    Order order = new Order();
    order.setId(orderId);
    order.setStatus(org.openlmis.fulfillment.domain.OrderStatus.PARTIALLY_FULFILLED);
    OrderLineItem lineItem = new OrderLineItem();
    org.openlmis.fulfillment.domain.VersionEntityReference versionEntityReference =
        new org.openlmis.fulfillment.domain.VersionEntityReference();
    versionEntityReference.setId(productId1);
    lineItem.setOrderable(versionEntityReference);
    order.setOrderLineItems(newArrayList(lineItem));
    order.setExternalId(preReqId1);
    List<Order> orders = newArrayList(order, order2);
    Page<Order> orderPage = Pagination.getPage(orders, null);
    when(orderService.searchOrders(any(), any())).thenReturn(orderPage);
  }


  private void mockProduct1ReqInProgress() {
    when(filterProductService.getInProgressProducts(any()))
        .thenReturn(Sets.newHashSet(productVersionObjectReference1.getId()));
  }

  private void mockProduct1OrderInProgress() {
    when(filterProductService.getNotFullyShippedProducts(any()))
        .thenReturn(Sets.newHashSet(productVersionObjectReference1.getId()));
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
    requisitionV2Dto.setReportOnly(false);
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
    extension.setEnableQuicklyFill(true);
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

  private List<ApprovedProductDto> getApprovedProductList() {
    MetadataDto meta = createMetadataDto();
    OrderableDto orderable = createOrderableDto(meta);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setFullSupply(true);
    orderable.setPrograms(Sets.newHashSet(programOrderableDto));
    ApprovedProductDto productDto = createApprovedProductDto(orderable, meta);
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    productDto.setProgram(programDto);
    List<ApprovedProductDto> list = new ArrayList<>();
    list.add(productDto);
    return list;
  }

  private ApproveProductsAggregator createApproveProductsAggregator() {
    MetadataDto meta = new MetadataDto();
    meta.setVersionNumber(1L);
    OrderableDto orderable = new OrderableDto();
    orderable.setId(productId2);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setFullSupply(true);
    orderable.setPrograms(Sets.newHashSet(programOrderableDto));
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

  private ProgramDto createProgramDto() {
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    return programDto;
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
    AvailableRequisitionColumnOption option = new AvailableRequisitionColumnOption();
    option.setOptionName("cmm");
    return baseTemplateBuilder()
        .withNumberOfPeriodsToAverage(3)
        .withPopulateStockOnHandFromStockCards(true)
        .withColumn(SUGGESTED_QUANTITY_COLUMN, "SQ", SourceType.CALCULATED, option,
            newHashSet(SourceType.CALCULATED))
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
    processingPeriodDto.setExtraData(ImmutableMap.of("reportOnly", "true"));
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

  private Map<UUID, RegimenDto> mockRegimenMap() {
    Map<UUID, RegimenDto> map = new HashMap<>();
    map.put(regimenId, mockRegimen());
    return map;
  }

  private RegimenDto mockRegimen() {
    RegimenDto regimen = new RegimenDto();
    regimen.setId(regimenId);
    regimen.setCode("ABC+3TC+RAL+DRV+RTV");
    regimen.setIsCustom(false);
    return regimen;
  }
}
