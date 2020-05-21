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

package org.openlmis.requisition.service;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.INITIATED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.IN_APPROVAL;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.REJECTED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SKIPPED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.service.PermissionService.ORDERS_EDIT;
import static org.openlmis.requisition.utils.Pagination.DEFAULT_PAGE_NUMBER;
import static org.openlmis.requisition.utils.Pagination.NO_PAGINATION;
import static org.openlmis.requisition.utils.Pagination.getPage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.domain.requisition.StockAdjustmentReason;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderLineItemDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReasonCategory;
import org.openlmis.requisition.dto.ReasonType;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.errorhandling.FailureType;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.StatusMessageRepository;
import org.openlmis.requisition.repository.custom.DefaultRequisitionSearchParams;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.IdealStockAmountReferenceDataService;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.service.referencedata.PermissionStrings;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.RightReferenceDataService;
import org.openlmis.requisition.service.referencedata.ScheduleReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyLineReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserFulfillmentFacilitiesReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserRoleAssignmentsReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockCardSummariesStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockOnHandRetrieverBuilderFactory;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.BasicRequisitionDtoDataBuilder;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.FacilityDtoDataBuilder;
import org.openlmis.requisition.testutils.IdealStockAmountDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProcessingPeriodDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.openlmis.requisition.testutils.StatusChangeDataBuilder;
import org.openlmis.requisition.testutils.StockAdjustmentReasonDataBuilder;
import org.openlmis.requisition.testutils.StockCardRangeSummaryDtoDataBuilder;
import org.openlmis.requisition.testutils.SupplyLineDtoDataBuilder;
import org.openlmis.requisition.testutils.UserDtoDataBuilder;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.requisition.web.OrderDtoBuilder;
import org.openlmis.requisition.web.RequisitionForConvertBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class RequisitionServiceTest {
  private static final String COMMODITY_TYPE = "commodityType";
  private static final UUID COMMODITY_TYPE_ID = UUID.randomUUID();

  private static final UUID PERIOD_ID = UUID.randomUUID();
  private RequisitionLineItem lineItem1;
  private RequisitionLineItem lineItem2;
  private Requisition requisition;
  private RequisitionDto requisitionDto;
  private Map<VersionIdentityDto, OrderableDto> orderables;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private Requisition requisitionMock;

  @Mock
  private StatusChange statusChange;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private PeriodService periodService;

  @Mock
  private ScheduleReferenceDataService scheduleReferenceDataService;

  @Mock
  private OrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private UserFulfillmentFacilitiesReferenceDataService fulfillmentFacilitiesReferenceDataService;

  @Mock
  private RightReferenceDataService rightReferenceDataService;

  @Mock
  private OrderFulfillmentService orderFulfillmentService;

  @Mock
  private StatusMessageRepository statusMessageRepository;

  @Mock
  private OrderDtoBuilder orderDtoBuilder;

  @Mock
  private UserRoleAssignmentsReferenceDataService userRoleAssignmentsReferenceDataService;

  @Mock
  private PermissionService permissionService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private RequisitionStatusProcessor requisitionStatusProcessor;

  @Mock
  private RequisitionForConvertBuilder requisitionForConvertBuilder;

  @Mock
  private IdealStockAmountReferenceDataService idealStockAmountReferenceDataService;

  @Mock
  private StockCardSummariesStockManagementService stockCardSummariesStockManagementService;

  @Spy
  private StockOnHandRetrieverBuilderFactory stockOnHandRetrieverBuilderFactory;

  @Mock
  private ProofOfDeliveryService proofOfDeliveryService;

  @Mock
  private StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Mock
  private SupplyLineReferenceDataService supplyLineReferenceDataService;

  @InjectMocks
  private RequisitionService requisitionService;

  @Mock
  private PermissionStrings.Handler permissionStringsHandler;

  @Spy
  private RequisitionTemplate requisitionTemplate = new RequisitionTemplateDataBuilder()
      .withAllColumns().build();

  private RightDto convertToOrderRight = DtoGenerator.of(RightDto.class, 2).get(0);
  private RightDto approveRequisitionRight = DtoGenerator.of(RightDto.class, 2).get(1);
  private RoleDto role = DtoGenerator.of(RoleDto.class);
  private StockCardSummaryDto stockCardSummaryDto = DtoGenerator.of(StockCardSummaryDto.class);
  private ProgramDto program = DtoGenerator.of(ProgramDto.class, true);
  private SupervisoryNodeDto supervisoryNode = DtoGenerator.of(SupervisoryNodeDto.class);
  private UserDto user = new UserDtoDataBuilder()
      .withRoleAssignment(role.getId(), supervisoryNode.getId(), program.getId())
      .buildAsDto();
  private FacilityDto facility = DtoGenerator.of(FacilityDto.class);
  private ProcessingPeriodDto processingPeriod = DtoGenerator.of(ProcessingPeriodDto.class);
  private PageRequest pageRequest = new PageRequest(DEFAULT_PAGE_NUMBER, NO_PAGINATION);
  private List<StockAdjustmentReason> stockAdjustmentReasons;
  private Requisition previousRequisition;
  private SupplyLineDto supplyLine = new SupplyLineDtoDataBuilder()
      .withSupervisoryNode(supervisoryNode)
      .withProgram(program)
      .buildAsDto();

  private static final int SETTING = 5;
  private static final int ADJUSTED_CONSUMPTION = 7;
  private static final UUID PRODUCT_ID = UUID.randomUUID();
  private static final UUID NON_FULL_PRODUCT_ID = UUID.randomUUID();
  private StockCardRangeSummaryDto stockCardRangeSummaryDto;

  @Before
  public void setUp() {
    requisition = generateRequisition();
    requisitionDto = generateRequisitionDto();
    stockAdjustmentReasons = generateReasons();
    mockRepositories();
    ReflectionTestUtils.setField(
        stockOnHandRetrieverBuilderFactory,
        "stockCardSummariesStockManagementService",
        stockCardSummariesStockManagementService
    );

    stockCardRangeSummaryDto = new StockCardRangeSummaryDtoDataBuilder().buildAsDto();
  }

  @Test
  public void shouldDeleteRequisitionIfItIsInitiated() {
    validateRequisitionDeleteWithStatus(INITIATED);
  }

  @Test
  public void shouldDeleteRequisitionWhenStatusIsSubmitted() {
    validateRequisitionDeleteWithStatus(SUBMITTED);
  }

  @Test
  public void shouldDeleteRequisitionWhenStatusIsSkipped() {
    validateRequisitionDeleteWithStatus(SKIPPED);
  }

  @Test
  public void shouldDeleteRequisitionIfRecentRequisitionIsLastInPeriod() {
    requisition.setStatus(INITIATED);
    stubRecentRequisition();

    requisitionService.delete(requisition);
    verify(requisitionRepository).delete(requisition);
  }

  @Test
  public void shouldDeleteRequisitionIfRecentRequisitionIsNotInLastPeriod() {
    requisition.setStatus(INITIATED);

    ProcessingPeriodDto secondPeriod = mock(ProcessingPeriodDto.class);
    when(secondPeriod.getId()).thenReturn(UUID.randomUUID());
    when(periodService.searchByProgramAndFacility(program.getId(), facility.getId()))
        .thenReturn(Arrays.asList(processingPeriod, secondPeriod));
    when(requisitionRepository
        .searchRequisitions(processingPeriod.getId(), facility.getId(), program.getId(), false))
        .thenReturn(singletonList(requisition));
    when(requisitionRepository
        .searchRequisitions(secondPeriod.getId(), facility.getId(), program.getId(), false))
        .thenReturn(emptyList());

    requisitionService.delete(requisition);
    verify(requisitionRepository).delete(requisition);
  }

  @Test
  public void shouldDeleteStatusMessagesWhenDeletingRequisition() {
    requisition.setStatus(INITIATED);
    List<StatusMessage> statusMessages = singletonList(
        StatusMessage.newStatusMessage(requisition, statusChange, null, null, null, "Message 1"));
    when(statusMessageRepository.findByRequisitionId(requisition.getId()))
        .thenReturn(statusMessages);
    stubRecentRequisition();

    requisitionService.delete(requisition);
    verify(requisitionRepository).delete(requisition);
    verify(statusMessageRepository).delete(statusMessages);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotDeleteRequisitionWhenStatusIsAuthorized() throws
      ValidationMessageException {
    requisition.setStatus(AUTHORIZED);
    stubRecentRequisition();

    requisitionService.delete(requisition);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotDeleteRequisitionWhenStatusIsInApproval() throws
      ValidationMessageException {
    requisition.setStatus(IN_APPROVAL);
    stubRecentRequisition();

    requisitionService.delete(requisition);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotDeleteRequisitionWhenStatusIsApproved() throws
      ValidationMessageException {
    requisition.setStatus(APPROVED);
    stubRecentRequisition();

    requisitionService.delete(requisition);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotDeleteRequisitionIfItIsNotNewest() {
    requisition.setStatus(INITIATED);

    prepareRequisitionIsNotNewest();

    requisitionService.delete(requisition);
  }

  @Test
  public void shouldDeleteEmergencyRequisitionEvenIfItIsNewest() {
    requisition.setStatus(INITIATED);
    requisition.setEmergency(true);
    prepareRequisitionIsNotNewest();

    requisitionService.delete(requisition);
    verify(requisitionRepository).delete(requisition);
  }

  @Test
  public void shouldRejectRequisitionIfRequisitionStatusIsAuthorized() {
    requisition.setStatus(AUTHORIZED);
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);
    Requisition returnedRequisition = requisitionService.reject(requisition, orderables);

    assertEquals(returnedRequisition.getStatus(), REJECTED);
  }

  @Test
  public void shouldRejectRequisitionIfRequisitionStatusIsInApproval() {
    requisition.setStatus(IN_APPROVAL);
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);
    Requisition returnedRequisition = requisitionService.reject(requisition, orderables);

    assertEquals(returnedRequisition.getStatus(), REJECTED);
  }

  @Test
  public void shouldNotRejectRequisitionIfRequisitionWasSplit() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(MessageKeys.ERROR_REQUISITION_WAS_SPLIT);

    requisition.setStatus(IN_APPROVAL);

    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);
    when(requisitionRepository.existsByOriginalRequisitionId(requisition.getId()))
        .thenReturn(true);

    requisitionService.reject(requisition, emptyMap());
  }

  @Test
  public void shouldNotRejectRequisitionIfRequisitionHasOriginalRequisition() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(MessageKeys.ERROR_REQUISITION_WAS_SPLIT);

    requisition.setStatus(IN_APPROVAL);
    requisition.setOriginalRequisitionId(UUID.randomUUID());

    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);

    requisitionService.reject(requisition, emptyMap());
  }

  @Test
  public void shouldSaveStatusMessageWhileRejectingRequisition() {
    requisition.setStatus(AUTHORIZED);
    requisition.setDraftStatusMessage("some_message");
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);
    Requisition returnedRequisition = requisitionService.reject(requisition, orderables);

    assertEquals(returnedRequisition.getStatus(), REJECTED);
    verify(statusMessageRepository, times(1)).save(any(StatusMessage.class));
  }

  @Test
  public void shouldSetNullToSupervisoryNodeWhileRejectingRequisition() {
    requisition.setStatus(AUTHORIZED);
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);
    Requisition returnedRequisition = requisitionService.reject(requisition, orderables);

    assertEquals(returnedRequisition.getStatus(), REJECTED);
    assertNull(returnedRequisition.getSupervisoryNodeId());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenRejectingRequisitionWithStatusSubmitted()
      throws ValidationMessageException {
    requisition.setStatus(SUBMITTED);
    requisitionService.reject(requisition, emptyMap());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenRejectingRequisitionWithStatusApproved()
      throws ValidationMessageException {
    requisition.setStatus(APPROVED);
    requisitionService.reject(requisition, emptyMap());
  }

  @Test
  public void shouldGetRequisitionsForApproval() {
    // given
    List<Requisition> requisitions = mockSearchRequisitionsForApproval();
    assertEquals(2, requisitions.size());

    Set<RightDto> rights = new HashSet<>();
    rights.add(approveRequisitionRight);
    role.setRights(rights);

    // when
    Page<Requisition> requisitionsForApproval =
        requisitionService.getRequisitionsForApproval(user, null, pageRequest);

    // then
    assertEquals(2, requisitionsForApproval.getTotalElements());
    assertTrue(requisitionsForApproval.getContent().contains(requisitions.get(0)));
    assertTrue(requisitionsForApproval.getContent().contains(requisitions.get(1)));
  }

  @Test
  public void shouldGetRequisitionsForApprovalWithProgramFilter() {
    List<Requisition> requisitions = mockSearchRequisitionsForApproval();
    assertEquals(2, requisitions.size());

    Set<RightDto> rights = new HashSet<>();
    rights.add(approveRequisitionRight);
    role.setRights(rights);

    Page<Requisition> requisitionsForApproval = requisitionService
                .getRequisitionsForApproval(user, program.getId(), pageRequest);

    assertEquals(2, requisitionsForApproval.getTotalElements());
    assertTrue(requisitionsForApproval.getContent().contains(requisitions.get(0)));
    assertTrue(requisitionsForApproval.getContent().contains(requisitions.get(1)));
  }

  @Test
  public void shouldNotGetRequisitionsForApprovalWithoutApproveRight() {
    Set<RightDto> rights = new HashSet<>();
    role.setRights(rights);

    when(requisitionRepository.searchApprovableRequisitionsByProgramSupervisoryNodePairs(
        any(Set.class), any(Pageable.class)))
        .thenReturn(getPage(emptyList(), pageRequest));

    Page<Requisition> requisitionsForApproval =
        requisitionService.getRequisitionsForApproval(user, null, pageRequest);

    assertEquals(0, requisitionsForApproval.getTotalElements());
  }

  @Test
  public void shouldNotGetRequisitionsForApprovalWithIncorrectSupervisoryNode() {
    Set<RightDto> rights = new HashSet<>();
    rights.add(approveRequisitionRight);
    role.setRights(rights);

    when(requisitionRepository.searchApprovableRequisitionsByProgramSupervisoryNodePairs(
        any(Set.class), any(Pageable.class)))
        .thenReturn(getPage(emptyList(), pageRequest));

    Page<Requisition> requisitionsForApproval =
        requisitionService.getRequisitionsForApproval(user, null, pageRequest);

    assertEquals(0, requisitionsForApproval.getTotalElements());
  }

  @Test
  public void shouldNotGetRequisitionsForApprovalWithIncorrectProgram() {
    Set<RightDto> rights = new HashSet<>();
    rights.add(approveRequisitionRight);
    role.setRights(rights);

    when(requisitionRepository.searchApprovableRequisitionsByProgramSupervisoryNodePairs(
        any(Set.class), any(Pageable.class)))
        .thenReturn(getPage(emptyList(), pageRequest));

    Page<Requisition> requisitionsForApproval =
        requisitionService.getRequisitionsForApproval(user, null, pageRequest);

    assertEquals(0, requisitionsForApproval.getTotalElements());
  }

  @Test
  public void shouldPassValidationIfUserCanApproveRequisition() {
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(userRoleAssignmentsReferenceDataService.hasSupervisionRight(any(RightDto.class),
        any(UUID.class), any(UUID.class), any(UUID.class)))
        .thenReturn(true);

    ValidationResult result = requisitionService.validateCanApproveRequisition(
        requisition, user.getId());

    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldFailValidationIfUserHasNoApproveRightAssigned() {
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.noPermission("no.permission"));

    ValidationResult result = requisitionService.validateCanApproveRequisition(requisition,
        user.getId());

    assertTrue(result.hasErrors());
    assertEquals(FailureType.NO_PERMISSION, result.getError().getType());
  }

  @Test
  public void shouldFailValidationIfRequisitionIsInIncorrectState() {
    when(permissionService.canApproveRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    requisition.setStatus(INITIATED);

    ValidationResult result = requisitionService.validateCanApproveRequisition(requisition,
        user.getId());

    assertTrue(result.hasErrors());
    assertEquals(FailureType.VALIDATION, result.getError().getType());
  }

  @Test
  public void shouldFailValidationIfUserHasNoUpdateRightsAssigned() {
    when(permissionService.canUpdateRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.noPermission("no.permission"));

    ValidationResult result = requisitionService.validateCanSaveRequisition(requisitionDto.getId());

    assertTrue(result.hasErrors());
    assertEquals(FailureType.NO_PERMISSION, result.getError().getType());
  }

  @Test
  public void shouldFailValidationIfRequisitionDoesNotExistOnSave() {
    when(permissionService.canUpdateRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(requisitionRepository.findOne(requisitionDto.getId())).thenReturn(null);

    ValidationResult result = requisitionService.validateCanSaveRequisition(requisitionDto.getId());

    assertTrue(result.hasErrors());
    assertEquals(FailureType.NOT_FOUND, result.getError().getType());
  }

  @Test
  public void shouldFailValidationIfRequisitionIsInIncorrectStatus() {
    when(permissionService.canUpdateRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(requisitionRepository.findOne(requisitionDto.getId())).thenReturn(requisition);
    requisition.setStatus(RELEASED);

    ValidationResult result = requisitionService.validateCanSaveRequisition(requisitionDto.getId());

    assertTrue(result.hasErrors());
    assertEquals(FailureType.VALIDATION, result.getError().getType());
  }

  @Test
  public void shouldPassValidationIfUserCanUpdateRequisition() {
    when(permissionService.canUpdateRequisition(any(Requisition.class)))
        .thenReturn(ValidationResult.success());
    when(requisitionRepository.findOne(requisitionDto.getId())).thenReturn(requisition);
    requisition.setStatus(SUBMITTED);

    ValidationResult result = requisitionService.validateCanSaveRequisition(requisitionDto.getId());

    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldInitiateRequisitionIfItDoesNotAlreadyExist() {
    prepareForTestInitiate(SETTING);
    final ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    when(requisitionTemplate.getNumberOfPeriodsToAverage()).thenReturn(2);

    when(requisitionRepository
        .findOne(requisition.getId()))
        .thenReturn(null);

    ProcessingPeriodDto periodDto = new ProcessingPeriodDto();
    periodDto.setStartDate(LocalDate.of(2016, 11, 1));
    periodDto.setEndDate(LocalDate.of(2016, 11, 30));
    periodDto.setDurationInMonths(1);
    doReturn(periodDto)
        .when(periodService)
        .findPeriod(program.getId(), facility.getId(), processingPeriod.getId(), false);

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    assertEquals(INITIATED, initiatedRequisition.getStatus());
    assertEquals(1, initiatedRequisition.getNumberOfMonthsInPeriod().longValue());
  }

  @Test
  public void shouldInitiatePreviousAdjustedConsumptions() {
    prepareForTestInitiate(SETTING);
    when(periodService.findPreviousPeriods(any(UUID.class), eq(SETTING - 1)))
        .thenReturn(singletonList(new ProcessingPeriodDto()));
    mockPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    RequisitionLineItem requisitionLineItem = initiatedRequisition.getRequisitionLineItems().get(0);
    assertEquals(Integer.valueOf(ADJUSTED_CONSUMPTION),
        requisitionLineItem.getPreviousAdjustedConsumptions().get(0));
  }

  @Test
  public void shouldInitiateReportOnlyRequisition() {
    prepareForTestInitiate(SETTING);
    when(periodService.findPreviousPeriods(any(UUID.class), eq(SETTING - 1)))
        .thenReturn(singletonList(new ProcessingPeriodDto()));
    mockPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    processingPeriod.setExtraData(ImmutableMap.of("reportOnly", "true"));

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    assertTrue(initiatedRequisition.getReportOnly());
  }

  @Test
  public void shouldInitiateRegularRequisitionIfItIsEmergencyForReportOnlyPeriod() {
    prepareForTestInitiate(SETTING);
    when(periodService.findPreviousPeriods(any(UUID.class), eq(SETTING - 1)))
        .thenReturn(singletonList(new ProcessingPeriodDto()));
    mockPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    processingPeriod.setExtraData(ImmutableMap.of("reportOnly", "true"));

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, true,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    assertFalse(initiatedRequisition.getReportOnly());
  }

  @Test
  public void shouldInitiatePreviousAdjustedConsumptionsBasedOnRegularRequisitions() {
    prepareForTestInitiate(SETTING);
    stubPreviousPeriod();
    mockPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    RequisitionLineItem requisitionLineItem = initiatedRequisition.getRequisitionLineItems().get(0);
    assertEquals(Integer.valueOf(ADJUSTED_CONSUMPTION),
        requisitionLineItem.getPreviousAdjustedConsumptions().get(0));
    verify(requisitionRepository).searchRequisitions(
        PERIOD_ID, facility.getId(), program.getId(), false);
  }

  @Test
  public void shouldAssignPreviousRegularRequisition() {
    prepareForTestInitiate(SETTING);
    stubPreviousPeriod();
    mockPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    UUID previousRequisitionId = initiatedRequisition.getPreviousRequisitions().get(0).getId();
    assertEquals(previousRequisition.getId(), previousRequisitionId);
    verify(requisitionRepository).searchRequisitions(
        PERIOD_ID, facility.getId(), program.getId(), false);
  }

  @Test
  public void shouldAssignIdealStockAmount() {
    prepareForTestInitiate(SETTING);
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    when(idealStockAmountReferenceDataService.search(any(UUID.class), any(UUID.class)))
        .thenReturn(Lists.newArrayList(new IdealStockAmountDtoDataBuilder()
            .withCommodityTypeId(COMMODITY_TYPE_ID)
            .buildAsDto()));

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    List<RequisitionLineItem> lineItems = initiatedRequisition.getRequisitionLineItems();
    assertThat(lineItems, hasSize(1));
    assertThat(lineItems.get(0).getIdealStockAmount(), is(1000));
  }

  @Test
  public void shouldSetEmptyPreviousAdjustedConsumptionsWhenNumberOfPeriodsToAverageIsNull() {
    prepareForTestInitiate(null);
    mockPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        this.program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    verify(periodService).findPreviousPeriods(any(UUID.class), eq(1));
    RequisitionLineItem requisitionLineItem = initiatedRequisition.getRequisitionLineItems().get(0);
    assertEquals(0, requisitionLineItem.getPreviousAdjustedConsumptions().size());
  }

  @Test
  public void shouldNotSetPreviousAdjustedConsumptionsIfNoRequisitionForNoPreviousRequisition() {
    prepareForTestInitiate(SETTING);
    when(periodService.findPreviousPeriods(any(UUID.class), eq(SETTING - 1)))
        .thenReturn(singletonList(new ProcessingPeriodDto()));
    mockNoPreviousRequisition();
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        this.program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    RequisitionLineItem requisitionLineItem = initiatedRequisition.getRequisitionLineItems().get(0);
    assertEquals(0, requisitionLineItem.getPreviousAdjustedConsumptions().size());
  }

  @Test
  public void shouldNotSetPreviousAdjustedConsumptionsIfNoRequisitionForNoPreviousPeriod() {
    prepareForTestInitiate(SETTING);
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        this.program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    RequisitionLineItem requisitionLineItem = initiatedRequisition.getRequisitionLineItems().get(0);
    assertEquals(0, requisitionLineItem.getPreviousAdjustedConsumptions().size());
  }

  @Test
  public void shouldSetStockAdjustmenReasonsDuringInitiate() {
    prepareForTestInitiate(SETTING);
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    assertEquals(stockAdjustmentReasons, initiatedRequisition.getStockAdjustmentReasons());
  }

  @Test
  public void shouldPopulateOnlyNonFullProductsDuringInitiateForRegularRequisition() {
    prepareForTestInitiate(SETTING);
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID, NON_FULL_PRODUCT_ID}, new boolean[]{true, false});

    Requisition initiatedRequisition = requisitionService.initiate(
        this.program, facility, processingPeriod, false,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    Set<VersionEntityReference> availableProducts = initiatedRequisition
        .getAvailableProducts()
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .collect(Collectors.toSet());

    assertThat(availableProducts, hasSize(1));
    assertThat(availableProducts, hasItems(new VersionEntityReference(NON_FULL_PRODUCT_ID, 1L)));
  }

  @Test
  public void shouldPopulateFullAndNonFullProductsDuringInitiateForEmergencyRequisition() {
    prepareForTestInitiate(SETTING);
    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID, NON_FULL_PRODUCT_ID}, new boolean[]{true, false});

    Requisition initiatedRequisition = requisitionService.initiate(
        this.program, facility, processingPeriod, true,
        stockAdjustmentReasons, requisitionTemplate, aggregator);

    Set<VersionEntityReference> availableProducts = initiatedRequisition
        .getAvailableProducts()
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .collect(Collectors.toSet());

    assertThat(availableProducts, hasSize(2));
    assertThat(availableProducts, hasItems(
        new VersionEntityReference(PRODUCT_ID, 1L),
        new VersionEntityReference(NON_FULL_PRODUCT_ID, 1L)));
  }

  @Test
  public void shouldSetStockOnHandFromStockIfFlagIsEnabled() {
    prepareForGetStockOnHandTest();
    stockCardSummaryDto.setStockOnHand(10);

    ProcessingPeriodDto previousPeriod = new ProcessingPeriodDtoDataBuilder()
        .withProcessingSchedule(processingPeriod.getProcessingSchedule())
        .withStartDate(processingPeriod.getStartDate().plusMonths(1))
        .withStartDate(processingPeriod.getEndDate().plusMonths(1))
        .buildAsDto();

    List<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(previousPeriod);

    when(periodService
        .findPreviousPeriods(processingPeriod, 4))
        .thenReturn(periods);

    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false, stockAdjustmentReasons,
        requisitionTemplate, aggregator);

    assertEquals(stockCardSummaryDto.getStockOnHand(),
        initiatedRequisition.getRequisitionLineItems().get(0).getStockOnHand());
  }

  @Test
  public void shouldNotSetStockOnHandIfFlagIsDisabled() {
    prepareForTestInitiate(SETTING);
    when(requisitionTemplate.isPopulateStockOnHandFromStockCards()).thenReturn(false);
    whenGetStockCardSummaries().thenThrow(IllegalStateException.class);

    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false, stockAdjustmentReasons,
        requisitionTemplate, aggregator);

    assertNull(initiatedRequisition.getRequisitionLineItems().get(0).getStockOnHand());
    verifyZeroInteractions(stockCardRangeSummaryStockManagementService);
  }

  @Test
  public void shouldNotIncludeLineItemsIfNoStockCardSummariesFound() {
    prepareForTestInitiate(SETTING);
    ProcessingPeriodDto previousPeriod = new ProcessingPeriodDtoDataBuilder()
        .withProcessingSchedule(processingPeriod.getProcessingSchedule())
        .withStartDate(processingPeriod.getStartDate().plusMonths(1))
        .withStartDate(processingPeriod.getEndDate().plusMonths(1))
        .buildAsDto();

    List<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(previousPeriod);

    when(periodService
        .findPreviousPeriods(processingPeriod, 4))
        .thenReturn(periods);
    when(requisitionTemplate.isPopulateStockOnHandFromStockCards()).thenReturn(true);
    whenGetStockCardRangeSummaries().thenReturn(singletonList(stockCardRangeSummaryDto));
    whenGetStockCardSummaries().thenReturn(emptyList());

    ApproveProductsAggregator aggregator = mockApprovedProduct(
        new UUID[]{PRODUCT_ID}, new boolean[]{true});

    Requisition initiatedRequisition = requisitionService.initiate(
        program, facility, processingPeriod, false, stockAdjustmentReasons,
        requisitionTemplate, aggregator);

    assertTrue(initiatedRequisition.getRequisitionLineItems().isEmpty());
  }

  @Test
  public void shouldReleaseRequisitionsAsOrder() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(5,
        APPROVED);
    List<FacilityDto> facilities = requisitions.stream()
        .map(r -> facilityReferenceDataService.findOne(r.getSupplyingDepotId()))
        .collect(toList());

    when(fulfillmentFacilitiesReferenceDataService.getFulfillmentFacilities(user.getId(),
        convertToOrderRight.getId())).thenReturn(facilities);
    when(requisitionForConvertBuilder.getAvailableSupplyingDepots(any(UUID.class)))
        .thenReturn(facilities);

    // when
    List<Requisition> expectedRequisitions = requisitionService
        .convertToOrder(requisitions, user);

    // then
    for (Requisition requisition : expectedRequisitions) {
      assertEquals(RELEASED, requisition.getStatus());
    }
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotReleaseRequisitionsAsOrderIfSupplyingDepotsNotProvided() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(5,
        APPROVED);
    List<FacilityDto> facilities = requisitions.stream()
        .map(r -> facilityReferenceDataService.findOne(r.getSupplyingDepotId()))
        .collect(toList());

    when(fulfillmentFacilitiesReferenceDataService.getFulfillmentFacilities(user.getId(),
        convertToOrderRight.getId())).thenReturn(facilities);

    for (ReleasableRequisitionDto requisition : requisitions) {
      requisition.setSupplyingDepotId(null);
    }

    // when
    requisitionService.convertToOrder(requisitions, user);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotReleaseRequisitionsAsOrderIfUserHasNoFulfillmentRightsForFacility() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(5,
        APPROVED);

    when(fulfillmentFacilitiesReferenceDataService.getFulfillmentFacilities(user.getId(),
        convertToOrderRight.getId())).thenReturn(new ArrayList<>());

    // when
    requisitionService.convertToOrder(requisitions, user);
  }


  @Test(expected = ValidationMessageException.class)
  public void shouldNotReleaseRequisitionsAsOrderIfApprovedQtyDisabled() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(1,
        APPROVED);
    when(requisitionTemplate.isColumnInTemplateAndDisplayed(APPROVED_QUANTITY)).thenReturn(false);

    // when
    requisitionService.convertToOrder(requisitions, user);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotReleaseRequisitionsAsOrderInIncorrectStatus() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(1,
        SUBMITTED);

    // when
    requisitionService.convertToOrder(requisitions, user);
  }

  @Test
  public void shouldCallApproveRequisition() {
    OrderableDto fullSupplyOrderable = new OrderableDtoDataBuilder().buildAsDto();
    SupplyLineDto supplyLineDto = new SupplyLineDtoDataBuilder().buildAsDto();
    when(orderableReferenceDataService.findByIdentities(any())).thenReturn(
        singletonList(fullSupplyOrderable));

    UUID parentId = UUID.randomUUID();

    requisitionService.doApprove(
        parentId, user, ImmutableMap.of(fullSupplyOrderable.getIdentity(), fullSupplyOrderable),
        requisitionMock, singletonList(supplyLineDto)
    );

    verify(requisitionMock, times(1)).approve(eq(parentId),
        eq(ImmutableMap.of(fullSupplyOrderable.getIdentity(), fullSupplyOrderable)),
        eq(singletonList(supplyLineDto)), eq(user.getId()));
  }

  @Test
  public void shouldFindRequisitionIfItExists() {
    // given
    RequisitionSearchParams params = new DefaultRequisitionSearchParams(
        requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), requisition.getSupervisoryNodeId(),
        requisition.getEmergency(), requisition.getCreatedDate().minusDays(2).toLocalDate(),
        requisition.getCreatedDate().plusDays(2).toLocalDate(),
        requisition.getModifiedDate().minusDays(2), requisition.getModifiedDate().plusDays(2),
        EnumSet.of(requisition.getStatus()));

    given(permissionService.getPermissionStrings(user.getId()))
        .willReturn(permissionStringsHandler);

    List<String> permissionStrings = permissionStringsHandler
        .get()
        .stream()
        .map(PermissionStringDto::toString)
        .collect(toList());

    given(requisitionRepository
        .searchRequisitions(params, permissionStrings,
            singleton(new ImmutablePair<>(program.getId(), supervisoryNode.getId())),
            pageRequest))
        .willReturn(getPage(singletonList(requisition), pageRequest));

    // when
    List<Requisition> receivedRequisitions = requisitionService
        .searchRequisitions(params, pageRequest)
        .getContent();

    // then
    assertEquals(1, receivedRequisitions.size());
    assertEquals(
        receivedRequisitions.get(0).getFacilityId(),
        requisition.getFacilityId());
    assertEquals(
        receivedRequisitions.get(0).getProgramId(),
        requisition.getProgramId());
    assertTrue(
        receivedRequisitions.get(0).getCreatedDate().isAfter(
            requisition.getCreatedDate().minusDays(2)));
    assertTrue(
        receivedRequisitions.get(0).getCreatedDate().isBefore(
            requisition.getCreatedDate().plusDays(2)));
    assertTrue(
        receivedRequisitions.get(0).getModifiedDate().isAfter(
            requisition.getCreatedDate().minusDays(2)));
    assertTrue(
        receivedRequisitions.get(0).getModifiedDate().isBefore(
            requisition.getCreatedDate().plusDays(2)));
    assertEquals(
        receivedRequisitions.get(0).getProcessingPeriodId(),
        requisition.getProcessingPeriodId());
    assertEquals(
        receivedRequisitions.get(0).getSupervisoryNodeId(),
        requisition.getSupervisoryNodeId());
    assertEquals(
        receivedRequisitions.get(0).getStatus(),
        requisition.getStatus());
  }

  @Test
  public void searchShouldReturnEmptyListIfPermissionStringsIsEmptyAndUserHasNoRoleAssignments() {
    // given
    final RequisitionSearchParams params = new DefaultRequisitionSearchParams(
        requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), requisition.getSupervisoryNodeId(),
        requisition.getEmergency(), requisition.getCreatedDate().minusDays(2).toLocalDate(),
        requisition.getCreatedDate().plusDays(2).toLocalDate(),
        requisition.getModifiedDate().minusDays(2), requisition.getModifiedDate().plusDays(2),
        EnumSet.of(requisition.getStatus()));

    given(authenticationHelper.getCurrentUser()).willReturn(user);
    given(permissionStringsHandler.get()).willReturn(emptySet());

    user.setRoleAssignments(Sets.newHashSet());

    // when
    List<Requisition> receivedRequisitions = requisitionService
        .searchRequisitions(params, pageRequest)
        .getContent();

    // then
    assertEquals(0, receivedRequisitions.size());
  }

  @Test
  public void shouldFilterRequisitionsForConvertByFacilityIdAndProgramId() {
    // given
    FacilityDto supplyingDepot = new FacilityDtoDataBuilder().buildAsDto();

    Pageable pageable = mockPageable();

    List<BasicRequisitionDto> essentialMedsRequisitions = getBasicRequisitionDtoList();

    setupStubsForTestApprovedRequisition(essentialMedsRequisitions, facility.getId(),
        singleton(Pair.of(program.getId(), supervisoryNode.getId())), supplyingDepot, pageable);

    //when
    requisitionService.searchApprovedRequisitions(facility.getId(), program.getId(), pageable);

    // then
    verifyNoMoreInteractions(programReferenceDataService);
    verifyNoMoreInteractions(facilityReferenceDataService);
  }

  @Test
  public void shouldFilterRequisitionsForConvertFacilityId() {
    // given
    FacilityDto supplyingDepot = new FacilityDtoDataBuilder().buildAsDto();
    Pageable pageable = mockPageable();
    List<BasicRequisitionDto> essentialMedsRequisitions = getBasicRequisitionDtoList();

    setupStubsForTestApprovedRequisition(essentialMedsRequisitions, facility.getId(),
        singleton(
            Pair.of(supplyLine.getProgram().getId(), supplyLine.getSupervisoryNode().getId())),
        supplyingDepot, pageable);

    //when
    requisitionService.searchApprovedRequisitions(facility.getId(), null, pageable);

    // then
    verifyNoMoreInteractions(programReferenceDataService);
    verifyNoMoreInteractions(facilityReferenceDataService);
  }

  @Test
  public void shouldFilterRequisitionsForConvertProgramId() {
    // given
    FacilityDto facilityDto = new FacilityDtoDataBuilder().buildAsDto();
    Pageable pageable = mockPageable();

    List<BasicRequisitionDto> essentialMedsRequisitions = getBasicRequisitionDtoList();

    setupStubsForTestApprovedRequisition(essentialMedsRequisitions, null,
        singleton(Pair.of(program.getId(), supervisoryNode.getId())), facilityDto, pageable);

    //when
    requisitionService.searchApprovedRequisitions(null, program.getId(), pageable);

    // then
    verifyNoMoreInteractions(programReferenceDataService);
    verifyNoMoreInteractions(facilityReferenceDataService);
  }

  @Test
  public void shouldReturnEmptyPageWhenNoFacilityIdsFromPermissionStrings() {
    // given
    Pageable pageable = mockPageable();
    when(authenticationHelper.getCurrentUser())
        .thenReturn(user);
    when(permissionStringsHandler.get())
        .thenReturn(emptySet());

    // when
    Page<RequisitionWithSupplyingDepotsDto> receivedRequisitions = requisitionService
        .searchApprovedRequisitions(facility.getId(), program.getId(), pageable);

    // then
    assertEquals(0, receivedRequisitions.getTotalElements());
  }

  @Test
  public void shouldConvertRequisitionsToOrders() {
    // given
    int requisitionsCount = 5;

    List<ReleasableRequisitionDto> list = setUpReleaseRequisitionsAsOrder(requisitionsCount,
        APPROVED);

    List<FacilityDto> facilities = list.stream()
        .map(r -> facilityReferenceDataService.findOne(r.getSupplyingDepotId()))
        .collect(toList());

    when(fulfillmentFacilitiesReferenceDataService.getFulfillmentFacilities(user.getId(),
        convertToOrderRight.getId())).thenReturn(facilities);
    when(requisitionForConvertBuilder.getAvailableSupplyingDepots(any(UUID.class)))
        .thenReturn(facilities);

    // when
    requisitionService.convertToOrder(list, user);

    // then
    verify(orderFulfillmentService).create(any(List.class));
  }


  @Test(expected = ValidationMessageException.class)
  public void shouldNotSwallowExceptionsFromFulfillmentService() {
    // given
    int requisitionsCount = 5;

    List<ReleasableRequisitionDto> list = setUpReleaseRequisitionsAsOrder(requisitionsCount,
        APPROVED);

    List<FacilityDto> facilities = list.stream()
        .map(r -> facilityReferenceDataService.findOne(r.getSupplyingDepotId()))
        .collect(toList());

    when(fulfillmentFacilitiesReferenceDataService.getFulfillmentFacilities(user.getId(),
        convertToOrderRight.getId())).thenReturn(facilities);

    doThrow(new ValidationMessageException(("test"))).when(
      orderFulfillmentService).create(any(List.class));

    // when
    requisitionService.convertToOrder(list, user);
  }

  @Test
  public void shouldProcessStatusChangeWhenConvertingRequisitionToOrder() throws Exception {
    List<ReleasableRequisitionDto> list = setUpReleaseRequisitionsAsOrder(1, APPROVED);

    List<FacilityDto> facilities = list.stream()
        .map(r -> facilityReferenceDataService.findOne(r.getSupplyingDepotId()))
        .collect(toList());

    when(fulfillmentFacilitiesReferenceDataService.getFulfillmentFacilities(user.getId(),
        convertToOrderRight.getId())).thenReturn(facilities);
    when(requisitionForConvertBuilder.getAvailableSupplyingDepots(any(UUID.class)))
        .thenReturn(facilities);

    requisitionService.convertToOrder(list, user);

    verify(requisitionStatusProcessor).statusChange(any(Requisition.class), any(Locale.class));
  }

  @Test
  public void shouldSaveStatusMessage() {
    // given
    Requisition requisition = generateRequisition();
    StatusChange statusChange = new StatusChange();
    statusChange.setRequisition(requisition);

    StatusChange anotherStatusChange = new StatusChange();
    anotherStatusChange.setId(UUID.randomUUID());
    anotherStatusChange.setCreatedDate(ZonedDateTime.now());

    requisition.setStatusChanges(Arrays.asList(statusChange, anotherStatusChange));
    requisition.setDraftStatusMessage("some_message");

    // when
    requisitionService.saveStatusMessage(requisition, user);

    // then
    ArgumentCaptor<StatusMessage> captor = ArgumentCaptor.forClass(StatusMessage.class);
    verify(statusMessageRepository).save(captor.capture());
    StatusMessage savedMessage = captor.getValue();
    assertEquals(statusChange, savedMessage.getStatusChange());
    assertEquals("", requisition.getDraftStatusMessage());
  }

  @Test
  public void shouldSaveStatusMessageWithAStatusChangeThatWasAlreadyPersisted() {
    // given
    Requisition requisition = generateRequisition();
    StatusChange statusChange = new StatusChangeDataBuilder()
        .withRequisition(requisition)
        .withCreatedDate(ZonedDateTime.now().minusDays(1))
        .build();

    StatusChange anotherStatusChange = new StatusChangeDataBuilder()
        .withRequisition(requisition)
        .withCreatedDate(ZonedDateTime.now())
        .build();

    requisition.setStatusChanges(Arrays.asList(statusChange, anotherStatusChange));
    requisition.setDraftStatusMessage("some_message");

    // when
    requisitionService.saveStatusMessage(requisition, user);

    // then
    ArgumentCaptor<StatusMessage> captor = ArgumentCaptor.forClass(StatusMessage.class);
    verify(statusMessageRepository).save(captor.capture());
    StatusMessage savedMessage = captor.getValue();
    assertEquals(anotherStatusChange, savedMessage.getStatusChange());
    assertEquals("", requisition.getDraftStatusMessage());
  }

  @Test
  public void shouldNotSaveStatusMessageIfDraftIsNullOrEmpty() {
    // given
    Requisition requisition = generateRequisition();
    requisition.setDraftStatusMessage(null);

    // when
    requisitionService.saveStatusMessage(requisition, user);

    requisition.setDraftStatusMessage("");
    requisitionService.saveStatusMessage(requisition, user);

    // then
    verify(statusMessageRepository, never()).save(any(StatusMessage.class));
  }

  @Test
  public void shouldReleaseRequisitionsWithoutOrder() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(5,
        APPROVED);

    // when
    List<Requisition> expectedRequisitions = requisitionService
        .releaseWithoutOrder(requisitions);

    // then
    for (Requisition requisition : expectedRequisitions) {
      assertEquals(RELEASED_WITHOUT_ORDER, requisition.getStatus());
    }
  }

  @Test
  public void shouldReleaseRequisitionsWithoutOrderIfApprovedQtyDisabled() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(1,
        APPROVED);
    when(requisitionTemplate.isColumnInTemplateAndDisplayed(APPROVED_QUANTITY)).thenReturn(false);

    // when
    List<Requisition> result = requisitionService.releaseWithoutOrder(requisitions);
    assertEquals(result.size(), requisitions.size());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldNotReleaseRequisitionsWithoutOrderIfInIncorrectStatus() {
    // given
    List<ReleasableRequisitionDto> requisitions = setUpReleaseRequisitionsAsOrder(1,
        SUBMITTED);

    // when
    requisitionService.releaseWithoutOrder(requisitions);
  }

  private void validateRequisitionDeleteWithStatus(RequisitionStatus status) {
    requisition.setStatus(status);
    when(statusMessageRepository.findByRequisitionId(requisition.getId()))
        .thenReturn(emptyList());
    stubRecentRequisition();

    requisitionService.delete(requisition);
    verify(requisitionRepository).delete(requisition);
  }

  private Pageable mockPageable() {
    return mockPageable(0, 10);
  }

  private Pageable mockPageable(int pageNumber, int pageSize) {
    return new PageRequest(pageNumber, pageSize, new Sort(Sort.Direction.DESC, "programName"));
  }

  private List<ReleasableRequisitionDto> setUpReleaseRequisitionsAsOrder(
      int amount, RequisitionStatus status) {
    if (amount < 1) {
      throw new IllegalArgumentException("Amount must be a positive number");
    }

    List<ReleasableRequisitionDto> result = new ArrayList<>();

    for (int i = 0; i < amount; i++) {
      FacilityDto facility = mock(FacilityDto.class);
      when(facility.getId()).thenReturn(UUID.randomUUID());

      Requisition requisition = new Requisition(UUID.randomUUID(), UUID.randomUUID(),
          UUID.randomUUID(), status, false);
      requisition.setId(UUID.randomUUID());
      requisition.setSupervisoryNodeId(UUID.randomUUID());
      requisition.setSupplyingFacilityId(facility.getId());
      requisition.setRequisitionLineItems(Lists.newArrayList());
      requisition.setTemplate(requisitionTemplate);

      when(requisitionRepository.findOne(requisition.getId())).thenReturn(requisition);
      when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);

      result.add(new ReleasableRequisitionDto(requisition.getId(), facility.getId()));
    }

    return result;
  }

  private Requisition generateRequisition() {
    lineItem1 = new RequisitionLineItemDataBuilder().build();
    lineItem2 = new RequisitionLineItemDataBuilder().build();

    Requisition requisition = new RequisitionDataBuilder()
        .withCreatedDate(ZonedDateTime.now())
        .withModifiedDate(ZonedDateTime.now())
        .withSupplyingFacilityId(facility.getId())
        .withRequisitionLineItems(Arrays.asList(lineItem1, lineItem2))
        .withTemplate(requisitionTemplate)
        .withFacilityId(facility.getId())
        .withProgramId(program.getId())
        .withSupervisoryNodeId(supervisoryNode.getId())
        .withStatus(AUTHORIZED)
        .build();

    orderables = requisition
        .getRequisitionLineItems()
        .stream()
        .map(line -> new OrderableDtoDataBuilder()
            .withId(line.getOrderable().getId())
            .withVersionNumber(line.getOrderable().getVersionNumber())
            .withProgramOrderable(requisition.getProgramId(), true)
            .buildAsDto())
        .collect(Collectors.toMap(OrderableDto::getIdentity, Function.identity()));

    return requisition;
  }

  private RequisitionDto generateRequisitionDto() {
    return new RequisitionDataBuilder()
        .withId(requisition.getId())
        .withSupervisoryNodeId(supervisoryNode.getId())
        .withStatus(AUTHORIZED)
        .buildAsDto();
  }

  private List<StockAdjustmentReason> generateReasons() {
    StockAdjustmentReason reason = new StockAdjustmentReasonDataBuilder()
        .withReasonCategory(ReasonCategory.ADJUSTMENT)
        .withReasonType(ReasonType.DEBIT)
        .withIsFreeTextAllowed(false)
        .build();
    return singletonList(reason);
  }

  private List<BasicRequisitionDto> getBasicRequisitionDtoList() {
    List<BasicRequisitionDto> requisitionDtos = new ArrayList<>();
    String[] programNames = {"one", "two", "three", "four", "five"};

    for (String programName : programNames) {
      BasicRequisitionDto requisitionDto = new BasicRequisitionDtoDataBuilder().buildAsDto();
      ProgramDto programDto = new ProgramDtoDataBuilder()
          .withName(programName)
          .buildAsDto();
      requisitionDto.setProgram(programDto);
      requisitionDtos.add(requisitionDto);
    }
    return requisitionDtos;
  }

  private void setupStubsForTestApprovedRequisition(List<BasicRequisitionDto> requisitionDtos,
      UUID facilityId, Set<Pair<UUID, UUID>> programSupervisoryNode, FacilityDto supplyingDepot,
      Pageable pageable) {
    final Page<Requisition> requisitions = Pagination
        .getPage(emptyList(), pageable, requisitionDtos.size());

    when(requisitionRepository
        .searchApprovedRequisitions(facilityId, programSupervisoryNode, pageable))
        .thenReturn(requisitions);

    List<RequisitionWithSupplyingDepotsDto> requisitionsWithDepots = new ArrayList<>();
    for (BasicRequisitionDto dto : requisitionDtos) {
      requisitionsWithDepots.add(
          new RequisitionWithSupplyingDepotsDto(dto, singletonList(supplyingDepot)));
    }
    when(requisitionForConvertBuilder.buildRequisitions(any(), any(), any()))
        .thenReturn(requisitionsWithDepots);
  }

  private void mockPreviousRequisition() {
    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItemDataBuilder()
        .withAdjustedConsumption(ADJUSTED_CONSUMPTION)
        .withOrderable(PRODUCT_ID, 1L)
        .build();
    previousRequisition = new RequisitionDataBuilder()
        .withRequisitionLineItems(singletonList(previousRequisitionLineItem))
        .build();

    when(requisitionRepository
        .searchRequisitions(any(), eq(facility.getId()), eq(program.getId()), eq(false)))
        .thenReturn(singletonList(previousRequisition));
  }

  private void mockNoPreviousRequisition() {
    when(requisitionRepository
        .searchRequisitions(any(UUID.class), any(UUID.class), any(UUID.class), any(Boolean.class)))
        .thenReturn(emptyList());
  }

  private ApproveProductsAggregator mockApprovedProduct(UUID[] products, boolean[] fullSupply) {
    assertThat(products.length, is(fullSupply.length));

    List<ApprovedProductDto> approvedProducts = new ArrayList<>();

    for (int i = 0, length = products.length; i < length; ++i) {
      String productNamePrefix = "Product ";
      approvedProducts.add(new ApprovedProductDtoDataBuilder()
          .withOrderable(new OrderableDtoDataBuilder()
              .withId(products[i])
              .withVersionNumber(1L)
              .withFullProductName(productNamePrefix + i)
              .withIdentifier(COMMODITY_TYPE, COMMODITY_TYPE_ID.toString())
              .withProgramOrderable(program.getId(), fullSupply[i])
              .buildAsDto())
          .withProgram(program)
          .buildAsDto()
      );
    }

    return new ApproveProductsAggregator(approvedProducts, program.getId());
  }

  private void prepareForTestInitiate(Integer numberOfPeriodsToAverage) {
    when(requisitionTemplate.getNumberOfPeriodsToAverage()).thenReturn(numberOfPeriodsToAverage);

    when(requisitionRepository.findOne(requisition.getId())).thenReturn(null);

    when(facilityReferenceDataService.findOne(facility.getId()))
        .thenReturn(new FacilityDto());
    when(programReferenceDataService.findOne(program.getId()))
        .thenReturn(new ProgramDto());

    when(periodService.findPeriod(
        eq(program.getId()), eq(facility.getId()), eq(processingPeriod.getId()), anyBoolean()))
        .thenReturn(processingPeriod);
  }

  private List<Requisition> mockSearchRequisitionsForApproval() {
    requisition.setStatus(IN_APPROVAL);
    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(requisition);
    Requisition requisition2 = generateRequisition();
    requisition2.setStatus(AUTHORIZED);
    requisitions.add(requisition2);

    when(requisitionRepository.searchApprovableRequisitionsByProgramSupervisoryNodePairs(
        newHashSet(new ImmutablePair<>(program.getId(), supervisoryNode.getId())), pageRequest))
        .thenReturn(getPage(requisitions, pageRequest));
    return requisitions;
  }

  private void mockRepositories() {
    when(requisitionRepository
        .findOne(requisition.getId()))
        .thenReturn(requisition);
    when(requisitionRepository
        .save(requisition))
        .thenReturn(requisition);
    when(requisitionRepository
        .save(requisition))
        .thenReturn(requisition);
    when(programReferenceDataService
        .findOne(any()))
        .thenReturn(program);
    when(authenticationHelper
        .getRight(ORDERS_EDIT))
        .thenReturn(convertToOrderRight);
    when(rightReferenceDataService
        .findRight(PermissionService.REQUISITION_APPROVE))
        .thenReturn(approveRequisitionRight);

    processingPeriod.setDurationInMonths(1);
    when(periodService
        .getPeriod(any()))
        .thenReturn(processingPeriod);

    when(periodService
        .findPeriod(program.getId(), facility.getId(), processingPeriod.getId(), false))
        .thenReturn(processingPeriod);

    when(scheduleReferenceDataService.searchByProgramAndFacility(any(), any()))
        .thenReturn(singletonList(processingPeriod.getProcessingSchedule()));

    when(periodService.searchByProgramAndFacility(any(), any()))
        .thenReturn(singletonList(processingPeriod));

    when(requisitionRepository
        .searchRequisitions(any(UUID.class), any(UUID.class), any(UUID.class), any(Boolean.class)))
        .thenReturn(new ArrayList<>());

    when(idealStockAmountReferenceDataService.search(facility.getId(), processingPeriod.getId()))
        .thenReturn(Lists.newArrayList());

    when(orderDtoBuilder.build(any(Requisition.class), any(UserDto.class)))
        .thenAnswer(invocation -> {
          Requisition requisition = (Requisition) invocation.getArguments()[0];

          if (null == requisition) {
            return null;
          }

          OrderDto order = new OrderDtoDataBuilder()
              .withExternalId(requisition.getId())
              .withEmergency(requisition.getEmergency())
              .withQuotedCost(BigDecimal.ZERO)
              .withOrderLineItems(
                  requisition
                      .getRequisitionLineItems()
                      .stream()
                      .map(line -> OrderLineItemDto.newOrderLineItem(line, null))
                      .collect(toList())
              )
              .withCreatedBy((UserDto) invocation.getArguments()[1])
              .buildAsDto();
          return order;
        });

    when(authenticationHelper.getCurrentUser()).thenReturn(user);
    when(permissionService.getPermissionStrings(user.getId())).thenReturn(permissionStringsHandler);
    when(permissionStringsHandler.get())
        .thenReturn(asSet(
            PermissionStringDto.create(ORDERS_EDIT, facility.getId(), program.getId())));
    when(supplyLineReferenceDataService.search(singleton(facility.getId()), program.getId()))
        .thenReturn(singletonList(supplyLine));
  }

  private void stubRecentRequisition() {
    when(periodService.searchByProgramAndFacility(program.getId(), facility.getId()))
        .thenReturn(singleton(processingPeriod));
    when(requisitionRepository
        .searchRequisitions(processingPeriod.getId(), facility.getId(), program.getId(), false))
        .thenReturn(singletonList(requisition));
  }

  private void stubPreviousPeriod() {
    ProcessingPeriodDto periodDto = new ProcessingPeriodDtoDataBuilder()
        .withId(PERIOD_ID)
        .buildAsDto();
    when(periodService.findPreviousPeriods(any(UUID.class), eq(SETTING - 1)))
        .thenReturn(singletonList(periodDto));
  }

  private void prepareRequisitionIsNotNewest() {
    when(periodService.searchByProgramAndFacility(program.getId(), facility.getId()))
        .thenReturn(singleton(processingPeriod));
    Requisition newestRequisition = mock(Requisition.class);
    when(newestRequisition.getId()).thenReturn(UUID.randomUUID());
    when(requisitionRepository
        .searchRequisitions(processingPeriod.getId(), facility.getId(), program.getId(), false))
        .thenReturn(singletonList(newestRequisition));
  }

  private OngoingStubbing<List<StockCardSummaryDto>> whenGetStockCardSummaries() {
    return when(stockCardSummariesStockManagementService
        .search(any(UUID.class), any(UUID.class), anySetOf(UUID.class),
            any(LocalDate.class)));
  }

  private OngoingStubbing<List<StockCardRangeSummaryDto>> whenGetStockCardRangeSummaries() {
    return when(stockCardRangeSummaryStockManagementService
        .search(any(UUID.class), any(UUID.class), anySetOf(VersionIdentityDto.class),
            any(String.class), any(LocalDate.class), any(LocalDate.class)));
  }

  private void prepareForGetStockOnHandTest() {
    prepareForTestInitiate(SETTING);
    when(requisitionTemplate.isPopulateStockOnHandFromStockCards()).thenReturn(true);
    whenGetStockCardRangeSummaries().thenReturn(singletonList(stockCardRangeSummaryDto));
    when(stockCardSummariesStockManagementService
        .search(program.getId(), facility.getId(), singleton(PRODUCT_ID),
            processingPeriod.getEndDate()))
        .thenReturn(singletonList(stockCardSummaryDto));
    ReflectionTestUtils.setField(stockCardSummaryDto.getOrderable(), "id", PRODUCT_ID);
  }

}
