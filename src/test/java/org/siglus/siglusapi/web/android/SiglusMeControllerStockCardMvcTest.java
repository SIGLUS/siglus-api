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

package org.siglus.siglusapi.web.android;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.common.domain.referencedata.Orderable.TRADE_ITEM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.siglus.siglusapi.dto.android.db.CalculatedStockOnHand;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLine;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLineAdjustment;
import org.siglus.siglusapi.dto.android.db.RequestedQuantity;
import org.siglus.siglusapi.dto.android.db.StockCardLineItem;
import org.siglus.siglusapi.dto.android.db.StockEventLineItem;
import org.siglus.siglusapi.dto.android.enumeration.AdjustmentReason;
import org.siglus.siglusapi.dto.android.enumeration.Destination;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.enumeration.Source;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.repository.StockCardRequestBackupRepository;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.android.MeService;
import org.siglus.siglusapi.service.android.StockCardCreateService;
import org.siglus.siglusapi.service.android.StockCardSearchService;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMovementMapperImpl;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.validator.android.StockCardCreateRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {MappingJackson2HttpMessageConverter.class, PageableHandlerMethodArgumentResolver.class,
    ObjectMapper.class, ProductMovementMapperImpl.class})
public class SiglusMeControllerStockCardMvcTest extends FileBasedTest {

  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private MappingJackson2HttpMessageConverter jackson2HttpMessageConverter;
  @Autowired
  private ProductMovementMapper productMovementMapper;

  @InjectMocks
  private MeService service;
  @InjectMocks
  private StockCardSearchService stockCardSearchService;

  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;
  @Mock
  private SupportedProgramsHelper programsHelper;
  @Mock
  private ProgramReferenceDataService programDataService;
  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;
  @Mock
  private SiglusValidReasonAssignmentService validReasonAssignmentService;
  @Mock
  private SiglusValidSourceDestinationService siglusValidSourceDestinationService;
  @Mock
  private StockManagementRepository stockManagementRepository;
  @Mock
  private StockCardCreateRequestValidator stockCardCreateRequestValidator;
  @Mock
  private AndroidHelper androidHelper;
  @Mock
  @SuppressWarnings("unused")
  private StockCardRequestBackupRepository stockCardRequestBackupRepository;
  @Mock
  @SuppressWarnings("unused")
  private StockEventProductRequestedRepository requestQuantityRepository;

  @InjectMocks
  private StockCardCreateContextHolder holder;
  @InjectMocks
  private StockCardCreateService stockCardCreateService;

  private final UUID facilityId = randomUUID();
  private final UUID facilityTypeId = randomUUID();
  private final UUID programId1 = randomUUID();
  private final UUID programId2 = randomUUID();
  private final UUID userId = randomUUID();
  private final ObjectMapper mapper = new ObjectMapper();
  private JavaType stockCardCreateRequestListType;

  @Before
  public void setup() {
    mockHomeFacility();
    mockPrograms();
    mockApprovedProducts();
    mockSourceDestinations();
    mockReasons();
    ReflectionTestUtils.setField(stockCardSearchService, "mapper", productMovementMapper);
    ReflectionTestUtils.setField(service, "stockCardSearchService", stockCardSearchService);
    ReflectionTestUtils.setField(service, "stockCardCreateContextHolder", holder);
    ReflectionTestUtils.setField(service, "stockCardCreateService", stockCardCreateService);
    StocksOnHand stocksOnHand = new StocksOnHand(emptyList());
    when(stockManagementRepository.getAllProductMovements(any(), any(LocalDate.class)))
        .thenReturn(new PeriodOfProductMovements(emptyList(), stocksOnHand));
    when(stockManagementRepository.getStockOnHand(any())).thenReturn(stocksOnHand);
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    objectMapper.registerModule(new JavaTimeModule());
    jackson2HttpMessageConverter.setObjectMapper(objectMapper);
    SiglusMeController controller = new SiglusMeController(service);
    this.mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setMessageConverters(jackson2HttpMessageConverter)
        .build();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    stockCardCreateRequestListType = mapper.getTypeFactory()
        .constructCollectionType(List.class, StockCardCreateRequest.class);
    when(androidHelper.isAndroid()).thenReturn(false);
  }

  @Test
  public void shouldReturnOkWhenGetStockCards() throws Exception {
    // given
    RequestBuilder request = get(
        "/api/siglusapi/android/me/facility/stockCards?startTime=2021-01-01&endTime=2021-01-31")
        .contentType(MediaType.APPLICATION_JSON)
        .characterEncoding("utf-8");

    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(any())).thenReturn(mockHappyCreateRequest());
    when(stockManagementRepository.getAllProductMovements(any(), any(), any())).thenReturn(mockPeriod());

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isOk());
    String target = readFromFile("get.json");
    String response = resultActions.andReturn().getResponse().getContentAsString();
    boolean equals = objectMapper.readValue(target, Map.class).equals(objectMapper.readValue(response, Map.class));
    assertTrue(equals);
  }

  @SuppressWarnings({"rawTypes", "unchecked"})
  @Test
  public void shouldReturnCreatedWhenSaveStockCardsGivenHappyRequest() throws Exception {
    // given
    RequestBuilder request = post("/api/siglusapi/android/me/facility/stockCards")
        .contentType(MediaType.APPLICATION_JSON)
        .content(readFromFile("happy.json"))
        .characterEncoding("utf-8");

    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(any())).thenReturn(mockHappyCreateRequest());

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isCreated());
    // TODO a little further?
    ArgumentCaptor<List> listParamCaptor = ArgumentCaptor.forClass(List.class);
    verify(stockManagementRepository).batchCreateLots(listParamCaptor.capture());
    assertEquals(3, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateEvents(listParamCaptor.capture());
    assertEquals(9, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateStockCards(listParamCaptor.capture());
    assertEquals(12, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateRequestedQuantities(listParamCaptor.capture());
    assertEquals(2, listParamCaptor.getValue().size());
    List<RequestedQuantity> requestedQuantities = listParamCaptor.getValue();
    assertTrue(requestedQuantities.stream().allMatch(l -> l.getRequested() >= 0));
    verify(stockManagementRepository).batchCreateEventLines(listParamCaptor.capture());
    assertEquals(52, listParamCaptor.getValue().size());
    List<StockEventLineItem> eventLineItems = listParamCaptor.getValue();
    assertTrue(eventLineItems.stream().allMatch(l -> l.getLineDetail().getQuantity() >= 0));
    verify(stockManagementRepository).batchCreateLines(listParamCaptor.capture());
    assertEquals(52, listParamCaptor.getValue().size());
    List<StockCardLineItem> lineItems = listParamCaptor.getValue();
    assertTrue(lineItems.stream().allMatch(l -> l.getLineDetail().getQuantity() >= 0));
    verify(stockManagementRepository).batchCreateInventories(listParamCaptor.capture());
    assertEquals(3, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateInventoryLines(listParamCaptor.capture());
    assertEquals(5, listParamCaptor.getValue().size());
    List<PhysicalInventoryLine> inventoryLines = listParamCaptor.getValue();
    assertTrue(inventoryLines.stream().allMatch(l -> l.getAdjustment() >= 0 && l.getInventoryBeforeAdjustment() >= 0));
    verify(stockManagementRepository).batchCreateInventoryLineAdjustments(listParamCaptor.capture());
    assertEquals(15, listParamCaptor.getValue().size());
    List<PhysicalInventoryLineAdjustment> lineAdjustments = listParamCaptor.getValue();
    assertTrue(lineAdjustments.stream().allMatch(l -> l.getAdjustment() >= 0));
    verify(stockManagementRepository).batchSaveStocksOnHand(listParamCaptor.capture());
    assertEquals(25, listParamCaptor.getValue().size());
    List<CalculatedStockOnHand> sohList = listParamCaptor.getValue();
    assertTrue(sohList.stream().allMatch(l -> l.getInventoryDetail().getStockQuantity() >= 0));
  }

  @SuppressWarnings({"rawTypes", "unchecked"})
  @Test
  public void shouldReturnCreatedWhenSaveStockCardsGivenUatsRequest() throws Exception {
    // given
    RequestBuilder request = post("/api/siglusapi/android/me/facility/stockCards")
        .contentType(MediaType.APPLICATION_JSON)
        .content(readFromFile("uats.json"))
        .characterEncoding("utf-8");
    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(any())).thenReturn(mockUatsCreateRequest());

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isCreated());
    // TODO a little further?
    ArgumentCaptor<List> listParamCaptor = ArgumentCaptor.forClass(List.class);
    verify(stockManagementRepository).batchCreateLots(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateEvents(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateStockCards(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateRequestedQuantities(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    List<RequestedQuantity> requestedQuantities = listParamCaptor.getValue();
    assertTrue(requestedQuantities.stream().allMatch(l -> l.getRequested() >= 0));
    verify(stockManagementRepository).batchCreateEventLines(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    List<StockEventLineItem> eventLineItems = listParamCaptor.getValue();
    assertTrue(eventLineItems.stream().allMatch(l -> l.getLineDetail().getQuantity() >= 0));
    verify(stockManagementRepository).batchCreateLines(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    List<StockCardLineItem> lineItems = listParamCaptor.getValue();
    assertTrue(lineItems.stream().allMatch(l -> l.getLineDetail().getQuantity() >= 0));
    verify(stockManagementRepository).batchCreateInventories(listParamCaptor.capture());
    assertEquals(0, listParamCaptor.getValue().size());
    verify(stockManagementRepository).batchCreateInventoryLines(listParamCaptor.capture());
    assertEquals(0, listParamCaptor.getValue().size());
    List<PhysicalInventoryLine> inventoryLines = listParamCaptor.getValue();
    assertTrue(inventoryLines.stream().allMatch(l -> l.getAdjustment() >= 0 && l.getInventoryBeforeAdjustment() >= 0));
    verify(stockManagementRepository).batchCreateInventoryLineAdjustments(listParamCaptor.capture());
    assertEquals(0, listParamCaptor.getValue().size());
    List<PhysicalInventoryLineAdjustment> lineAdjustments = listParamCaptor.getValue();
    assertTrue(lineAdjustments.stream().allMatch(l -> l.getAdjustment() >= 0));
    verify(stockManagementRepository).batchSaveStocksOnHand(listParamCaptor.capture());
    assertEquals(1, listParamCaptor.getValue().size());
    List<CalculatedStockOnHand> sohList = listParamCaptor.getValue();
    assertTrue(sohList.stream().allMatch(l -> l.getInventoryDetail().getStockQuantity() >= 0));
  }

  private void mockHomeFacility() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    user.setId(userId);
    when(authHelper.getCurrentUserId()).thenReturn(Optional.of(userId));
    when(authHelper.getCurrentUser()).thenReturn(user);
    FacilityDto facility = new FacilityDto();
    facility.setId(facilityId);
    FacilityTypeDto facilityType = new FacilityTypeDto();
    facilityType.setId(facilityTypeId);
    facility.setType(facilityType);
    when(facilityReferenceDataService.getFacilityById(eq(facilityId))).thenReturn(facility);
  }

  private void mockPrograms() {
    when(programsHelper.findUserSupportedPrograms()).thenReturn(new HashSet<>(asList(programId1, programId2)));
    ProgramDto program1 = new ProgramDto();
    program1.setId(programId1);
    program1.setCode("program 1");
    when(programDataService.findOne(eq(programId1))).thenReturn(program1);
    ProgramDto program2 = new ProgramDto();
    program2.setId(programId2);
    program2.setCode("program 2");
    when(programDataService.findOne(eq(programId2))).thenReturn(program2);
  }

  private void mockApprovedProducts() {
    when(approvedProductService.getApprovedProducts(eq(facilityId), eq(programId1), eq(emptyList())))
        .thenReturn(mockProgramProducts(programId1, "08A07", "08O05", "08O05Y", "08O05X"));
    when(approvedProductService.getApprovedProducts(eq(facilityId), eq(programId2), eq(emptyList())))
        .thenReturn(mockProgramProducts(programId1, "02A01", "04B05", "07A03", "22A06", "22A08", "22A05", "22A07"));
  }

  private void mockSourceDestinations() {
    List<ValidSourceDestinationDto> sources = Stream.of(Source.values())
        .map(Source::getName)
        .map(this::mockValidSourceDestination)
        .flatMap(Collection::stream)
        .collect(toList());
    when(siglusValidSourceDestinationService.findSourcesForAllProducts(eq(facilityId)))
        .thenReturn(sources);
    List<ValidSourceDestinationDto> destinations = Stream.of(Destination.values())
        .map(Destination::getName)
        .map(this::mockValidSourceDestination)
        .flatMap(Collection::stream)
        .collect(toList());
    when(siglusValidSourceDestinationService.findDestinationsForAllProducts(eq(facilityId)))
        .thenReturn(destinations);
  }

  private void mockReasons() {
    List<ValidReasonAssignmentDto> reasons = Stream.of(AdjustmentReason.values())
        .map(AdjustmentReason::getName)
        .map(this::mockReason)
        .flatMap(Collection::stream)
        .collect(toList());
    when(validReasonAssignmentService.getAllReasons(eq(facilityTypeId))).thenReturn(reasons);
  }

  private List<ValidReasonAssignmentDto> mockReason(String name) {
    return Stream.of(programId1, programId2)
        .map(programId -> {
          ValidReasonAssignmentDto dto = new ValidReasonAssignmentDto();
          dto.setId(randomUUID());
          dto.setProgramId(programId);
          StockCardLineItemReason reason = new StockCardLineItemReason();
          reason.setId(randomUUID());
          reason.setName(name);
          dto.setReason(reason);
          return dto;
        })
        .collect(toList());
  }

  private List<ValidSourceDestinationDto> mockValidSourceDestination(String name) {
    return Stream.of(programId1, programId2)
        .map(programId -> {
          ValidSourceDestinationDto dto = new ValidSourceDestinationDto();
          dto.setId(randomUUID());
          Node node = new Node();
          node.setId(randomUUID());
          dto.setNode(node);
          dto.setProgramId(programId);
          dto.setName(name);
          return dto;
        })
        .collect(toList());
  }

  private List<ApprovedProductDto> mockProgramProducts(UUID programId, String... productCodes) {
    return Arrays.stream(productCodes)
        .map(productCode -> mockApprovedProduct(programId, productCode))
        .collect(toList());
  }

  private ApprovedProductDto mockApprovedProduct(UUID programId, String productCode) {
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(randomUUID());
    orderable.setProductCode(productCode);
    orderable.setExtraData(new HashMap<>());
    ProgramOrderableDto program = new ProgramOrderableDto();
    program.setProgramId(programId);
    orderable.setPrograms(singleton(program));
    orderable.setIdentifiers(ImmutableMap.of(TRADE_ITEM, randomUUID().toString()));
    return approvedProduct;
  }

  private ValidatedStockCards mockHappyCreateRequest() throws Exception {
    String requestJsonStr = readFromFile("happy.json");
    List<StockCardCreateRequest> happyRequests = mapper.readValue(requestJsonStr, stockCardCreateRequestListType);
    return ValidatedStockCards.builder().validStockCardRequests(happyRequests).invalidProducts(new ArrayList<>())
        .build();
  }

  private ValidatedStockCards mockUatsCreateRequest() throws Exception {
    String requestJsonStr = readFromFile("uats.json");
    List<StockCardCreateRequest> uatsRequests = mapper.readValue(requestJsonStr, stockCardCreateRequestListType);
    return ValidatedStockCards.builder().validStockCardRequests(uatsRequests).invalidProducts(new ArrayList<>())
        .build();
  }

  private PeriodOfProductMovements mockPeriod() {
    return new PeriodOfProductMovements(mockProductMovements1(), mockStocksOnHand1());
  }

  private StocksOnHand mockStocksOnHand1() {
    ProductLotStock lot1 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot1"))
        .productName("Test")
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 1), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock lot2 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot2"))
        .productName("Test")
        .stockQuantity(9)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 9, 15), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock lot3 = ProductLotStock.builder()
        .code(ProductLotCode.of("test", "lot3"))
        .productName("Test")
        .stockQuantity(8)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 8, 31), Instant.now()))
        .expirationDate(java.sql.Date.valueOf(LocalDate.of(2023, 12, 31)))
        .build();
    ProductLotStock kitLot = ProductLotStock.builder()
        .code(ProductLotCode.of("26A01", null))
        .productName("Some kit")
        .stockQuantity(10)
        .eventTime(EventTime.fromRequest(LocalDate.of(2021, 10, 31), Instant.now()))
        .build();
    return new StocksOnHand(asList(lot1, lot2, lot3, kitLot));
  }

  private List<ProductMovement> mockProductMovements1() {
    LotMovement movement1Lot1 = LotMovement.builder()
        .stockQuantity(10)
        .movementDetail(new MovementDetail(10, MovementType.ISSUE, "Maternidade"))
        .lot(Lot.fromDatabase("lot1", java.sql.Date.valueOf("2023-12-31")))
        .build();
    Timestamp serverProcessTime = new Timestamp(1635701025000L);
    ProductMovement movement1 = ProductMovement.builder()
        .productCode("test")
        .stockQuantity(10)
        .eventTime(EventTime.fromDatabase(Date.valueOf("2021-10-01"), "2021-09-15T01:23:45Z", serverProcessTime))
        .processedAt(serverProcessTime.toInstant())
        .movementDetail(new MovementDetail(10, MovementType.ISSUE, "Maternidade"))
        .lotMovements(singletonList(movement1Lot1))
        .build();
    LotMovement movement2Lot1 = LotMovement.builder()
        .stockQuantity(10)
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "District(DDM)"))
        .lot(Lot.fromDatabase("lot1", java.sql.Date.valueOf("2023-12-31")))
        .build();
    ProductMovement movement2 = ProductMovement.builder()
        .productCode("test")
        .stockQuantity(10)
        .eventTime(EventTime.fromDatabase(Date.valueOf("2021-10-01"), "2021-10-01T01:23:45Z", serverProcessTime))
        .processedAt(serverProcessTime.toInstant())
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "District(DDM)"))
        .lotMovements(singletonList(movement2Lot1))
        .build();
    ProductMovement movement3 = ProductMovement.builder()
        .productCode("26A01")
        .stockQuantity(10)
        .eventTime(EventTime.fromDatabase(Date.valueOf("2021-10-31"), "2021-11-01T01:23:45Z", serverProcessTime))
        .processedAt(serverProcessTime.toInstant())
        .movementDetail(new MovementDetail(10, MovementType.RECEIVE, "District(DDM)"))
        .build();
    return asList(movement1, movement2, movement3);
  }

}
