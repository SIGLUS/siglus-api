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
import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.dto.android.db.StockCard;
import org.siglus.siglusapi.dto.android.enumeration.AdjustmentReason;
import org.siglus.siglusapi.dto.android.enumeration.Destination;
import org.siglus.siglusapi.dto.android.enumeration.Source;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.repository.StockCardRequestBackupRepository;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.android.MeService;
import org.siglus.siglusapi.service.android.StockCardCreateService;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.validator.android.StockCardCreateRequestValidator;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusMeControllerStockCardMvcTest extends FileBasedTest {

  private MockMvc mockMvc;

  @InjectMocks
  private MeService service;

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
  private SiglusStockEventsService stockEventsService;
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

  private final UUID facilityId = UUID.randomUUID();
  private final UUID facilityTypeId = UUID.randomUUID();
  private final UUID programId1 = UUID.randomUUID();
  private final UUID programId2 = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final ObjectMapper mapper = new ObjectMapper();
  private JavaType stockCardCreateRequestListType;

  @Before
  public void setup() {
    mockHomeFacility();
    mockPrograms();
    mockApprovedProducts();
    mockSourceDestinations();
    mockReasons();
    ReflectionTestUtils.setField(service, "stockCardCreateContextHolder", holder);
    ReflectionTestUtils.setField(service, "stockCardCreateService", stockCardCreateService);
    StocksOnHand stocksOnHand = new StocksOnHand(emptyList());
    when(stockManagementRepository.getAllProductMovements(any(), any(LocalDate.class)))
        .thenReturn(new PeriodOfProductMovements(emptyList(), stocksOnHand));
    when(stockManagementRepository.getStockOnHand(any())).thenReturn(stocksOnHand);
    when(stockManagementRepository.ensureLot(any(), any(), any())).then(a -> {
      String productCode = a.getArgumentAt(0, OrderableDto.class).getProductCode();
      String lotCode = a.getArgumentAt(1, String.class);
      LocalDate expirationDate = a.getArgumentAt(2, LocalDate.class);
      ProductLotCode code = ProductLotCode.of(productCode, lotCode);
      ProductLot productLot = new ProductLot(code, expirationDate);
      productLot.setId(UUID.randomUUID());
      return productLot;
    });
    when(stockManagementRepository.createStockCard(any())).then(a -> {
      StockCard stockCard = a.getArgumentAt(0, StockCard.class);
      stockCard.setId(UUID.randomUUID());
      return stockCard;
    });
    when(stockManagementRepository.createPhysicalInventory(any())).then(a -> {
      PhysicalInventory inventory = a.getArgumentAt(0, PhysicalInventory.class);
      inventory.setId(UUID.randomUUID());
      return inventory;
    });
    SiglusMeController controller = new SiglusMeController(service);
    this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    stockCardCreateRequestListType = mapper.getTypeFactory()
        .constructCollectionType(List.class, StockCardCreateRequest.class);
    when(androidHelper.isAndroid()).thenReturn(false);
  }

  @Test
  public void shouldReturnCreatedWhenSaveStockCardsGivenHappyRequest() throws Exception {
    // given
    RequestBuilder request = post("/api/siglusapi/android/me/facility/stockCards")
        .contentType(MediaType.APPLICATION_JSON)
        .content(readFromFile("happy.json"))
        .characterEncoding("utf-8");

    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(any())).thenReturn(mockHappyCreateRequest());
    when(stockEventsService.createStockEventForNoDraftAllProducts(any()))
        .thenReturn(ImmutableMap.of(UUID.randomUUID(), UUID.randomUUID()));

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isCreated());
    // TODO a little further?
    verify(stockManagementRepository, times(3)).ensureLot(any(), any(), any());
    verify(stockManagementRepository, times(12)).createStockCard(any());
    verify(stockManagementRepository, times(9)).createStockEvent(any());
    verify(stockManagementRepository, times(52)).createStockEventLine(any(), any(), any());
    verify(stockManagementRepository, times(52)).createStockCardLine(any(), any(), any());
    verify(stockManagementRepository, times(2)).createPhysicalInventory(any());
    verify(stockManagementRepository, times(5)).createPhysicalInventoryLine(any(), any(), any());
  }

  @Test
  public void shouldReturnCreatedWhenSaveStockCardsGivenUatsRequest() throws Exception {
    // given
    RequestBuilder request = post("/api/siglusapi/android/me/facility/stockCards")
        .contentType(MediaType.APPLICATION_JSON)
        .content(readFromFile("uats.json"))
        .characterEncoding("utf-8");
    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(any())).thenReturn(mockUatsCreateRequest());
    when(stockEventsService.createStockEventForNoDraftAllProducts(any()))
        .thenReturn(ImmutableMap.of(UUID.randomUUID(), UUID.randomUUID()));

    // when
    ResultActions resultActions = mockMvc.perform(request).andDo(print());

    // then
    resultActions.andExpect(status().isCreated());
    // TODO a little further?
    verify(stockManagementRepository, times(1)).ensureLot(any(), any(), any());
    verify(stockManagementRepository, times(1)).createStockCard(any());
    verify(stockManagementRepository, times(1)).createStockEvent(any());
    verify(stockManagementRepository, times(1)).createStockEventLine(any(), any(), any());
    verify(stockManagementRepository, times(1)).createStockCardLine(any(), any(), any());
    verify(stockManagementRepository, times(0)).createPhysicalInventory(any());
    verify(stockManagementRepository, times(0)).createPhysicalInventoryLine(any(), any(), any());
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
          dto.setId(UUID.randomUUID());
          dto.setProgramId(programId);
          StockCardLineItemReason reason = new StockCardLineItemReason();
          reason.setId(UUID.randomUUID());
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
          dto.setId(UUID.randomUUID());
          Node node = new Node();
          node.setId(UUID.randomUUID());
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
    orderable.setId(UUID.randomUUID());
    orderable.setProductCode(productCode);
    orderable.setExtraData(new HashMap<>());
    ProgramOrderableDto program = new ProgramOrderableDto();
    program.setProgramId(programId);
    orderable.setPrograms(singleton(program));
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
}
