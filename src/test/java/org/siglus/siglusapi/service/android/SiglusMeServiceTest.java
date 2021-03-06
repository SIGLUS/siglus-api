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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.testutils.CanFulfillForMeEntryDtoDataBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.dto.android.LotStockOnHand;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductChildResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.ReportTypeResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.android.mapper.ProductChildMapperImpl;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMapperImpl;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ProductMapperImpl.class, ProductChildMapperImpl.class})
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusMeServiceTest {

  @InjectMocks
  private SiglusMeService service;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;

  private final List<SupportedProgramDto> programDtos = new ArrayList<>();

  @Mock
  private SiglusOrderableService orderableDataService;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SupportedProgramsHelper programsHelper;

  @Mock
  private ProgramReferenceDataService programDataService;

  @Mock
  private ArchivedProductRepository archivedProductRepo;

  @Mock
  private AppInfoRepository appInfoRepository;

  @Mock
  private FacilityCmmsRepository facilityCmmsRepository;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private SiglusStockCardSummariesService stockCardSummariesService;

  @Mock
  private SiglusStockCardLineItemService stockCardLineItemService;

  @Mock
  private AndroidHelper androidHelper;

  @Mock
  private ReportTypeRepository reportTypeRepository;

  @Mock
  private SiglusRequisitionRepository requisitionRepository;

  @Mock
  private AndroidRequisitionService androidRequisitionService;

  @Captor
  private ArgumentCaptor<HfCmm> hfCmmArgumentCaptor;

  @Autowired
  private ProductMapper mapper;

  private final UUID appInfoId = UUID.randomUUID();

  private final UUID hfCmmId = UUID.randomUUID();

  private final String facilityCode = "01050119";

  private final String productCode = "05A20";

  private final LocalDate periodBegin = LocalDate.of(2021, 4, 21);

  private final LocalDate periodEnd = LocalDate.of(2021, 5, 20);

  private final Instant now = Instant.now();

  private final UUID facilityId = UUID.randomUUID();

  private ZonedDateTime oldTime;
  private Instant syncTime;
  private ZonedDateTime latestTime;

  private final UUID productId1 = UUID.randomUUID();
  private final UUID productId2 = UUID.randomUUID();
  private final UUID productId3 = UUID.randomUUID();

  private final String productCode1 = "product 1";
  private final String productCode2 = "product 2";
  private final String productCode3 = "product 3";

  private final UUID tradeItem1 = UUID.randomUUID();
  private final UUID tradeItem2 = UUID.randomUUID();
  private final UUID tradeItem3 = UUID.randomUUID();

  private final UUID lotId1OrderableId1 = UUID.randomUUID();
  private final UUID lotId2OrderableId1 = UUID.randomUUID();
  private final UUID lotId1OrderableId2 = UUID.randomUUID();

  private final UUID supportProgramId1 = UUID.randomUUID();
  private final UUID supportProgramId2 = UUID.randomUUID();

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(service, "mapper", mapper);
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);

    String oldTimeStr = "2020-12-31T09:18:34.001Z";
    oldTime = Instant.parse(oldTimeStr).atZone(ZoneId.systemDefault());
    String syncTimeStr = "2021-04-12T12:32:26.003Z";
    syncTime = Instant.parse(syncTimeStr);
    String latestTimeStr = "2021-05-31T09:08:35.004Z";
    latestTime = Instant.parse(latestTimeStr).atZone(ZoneId.systemDefault());

    UUID programId1 = UUID.randomUUID();
    ProgramDto program1 = mock(ProgramDto.class);
    when(program1.getId()).thenReturn(programId1);
    when(program1.getCode()).thenReturn("code 1");
    when(programDataService.findOne(programId1)).thenReturn(program1);
    UUID programId2 = UUID.randomUUID();
    ProgramDto program2 = mock(ProgramDto.class);
    when(program2.getId()).thenReturn(programId2);
    when(program2.getCode()).thenReturn("code 2");
    when(programDataService.findOne(programId2)).thenReturn(program2);
    when(programsHelper.findUserSupportedPrograms()).thenReturn(ImmutableSet.of(programId1, programId2));
    when(orderableDataService.searchOrderables(any(), any(), any()))
        .thenReturn(new PageImpl<>(asList(mockOrderable1(), mockOrderable2(), mockOrderable3())));
    when(approvedProductService.getApprovedProducts(facilityId, programId1, emptyList()))
        .thenReturn(asList(mockApprovedProduct1(), mockApprovedProduct2()));
    when(approvedProductService.getApprovedProducts(facilityId, programId2, emptyList()))
        .thenReturn(singletonList(mockApprovedProduct3()));
    when(archivedProductRepo.findArchivedProductsByFacilityId(facilityId)).thenReturn(singleton(productId1.toString()));
    when(androidHelper.isAndroid()).thenReturn(true);
  }


  @Test
  public void shouldCallFacilityReferenceDataServiceWhenGetFacility() {
    // given
    programDtos.addAll(getSupportedPrograms());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setCode("facilityCode");
    facilityDto.setName("facilityName");
    facilityDto.setSupportedPrograms(programDtos);
    when(facilityReferenceDataService.getFacilityById(facilityId)).thenReturn(facilityDto);

    // when
    FacilityResponse response = service.getCurrentFacility();

    // then
    assertEquals(programDtos.get(0).getCode(), response.getSupportedPrograms().get(0).getCode());
  }

  @Test
  public void shouldGetSupportReportTypesWhenGetFacilityInfo() {
    // given
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    facilityDto.setCode("facilityCode");
    facilityDto.setName("facilityName");
    facilityDto.setSupportedPrograms(getSupportedPrograms());
    List<ReportType> reportTypes = new ArrayList<>(asList(mockReportType1(), mockReportType2()));
    Optional<Requisition> rnrOpt = mockProgramRnr();
    List<Requisition> requisitions = Collections.singletonList(rnrOpt.get());
    when(facilityReferenceDataService.getFacilityById(facilityId)).thenReturn(facilityDto);
    when(reportTypeRepository.findByFacilityId(facilityId))
        .thenReturn(reportTypes);
    when(requisitionRepository
        .findLatestRequisitionByFacilityId(facilityId))
        .thenReturn(requisitions);

    // when
    FacilityResponse response = service.getCurrentFacility();

    // then
    List<ReportTypeResponse> actualReportTypes = response.getSupportedReportTypes();
    assertEquals(reportTypes.get(0).getName(), actualReportTypes.get(0).getName());
    assertEquals(reportTypes.get(0).getStartDate(), actualReportTypes.get(0).getSupportStartDate());
    assertEquals(reportTypes.get(0).getProgramCode(), actualReportTypes.get(0).getProgramCode());
    assertEquals(rnrOpt.map(r -> r.getExtraData().get(ACTUAL_END_DATE)).map(String::valueOf)
        .map(LocalDate::parse).get(), actualReportTypes.get(0).getLastReportDate());
    assertEquals(reportTypes.get(1).getName(), actualReportTypes.get(1).getName());
    assertEquals(reportTypes.get(1).getStartDate(), actualReportTypes.get(1).getSupportStartDate());
    assertEquals(reportTypes.get(1).getProgramCode(), actualReportTypes.get(1).getProgramCode());
    assertEquals(null, actualReportTypes.get(1).getLastReportDate());
  }

  @Test
  public void shouldUpdateAppInfoWhenAppInfoIsExist() {
    // given
    AppInfo existedInfo = mockCurrentAppInfo();
    AppInfo toBeUpdatedInfo = mockUpdateAppInfo();
    when(appInfoRepository.findByFacilityCodeAndUniqueId(toBeUpdatedInfo.getFacilityCode(),
        toBeUpdatedInfo.getUniqueId())).thenReturn(existedInfo);
    when(appInfoRepository.save(toBeUpdatedInfo)).thenReturn(existedInfo);

    // when
    service.processAppInfo(toBeUpdatedInfo);

    // then
    assertEquals(toBeUpdatedInfo.getId(), existedInfo.getId());
    assertEquals(toBeUpdatedInfo.getFacilityCode(), existedInfo.getFacilityCode());
    assertEquals(toBeUpdatedInfo.getUniqueId(), existedInfo.getUniqueId());
  }

  @Test
  public void shouldInsertAppInfoWhenAppInfoIsNotExist() {
    // given
    AppInfo toBeUpdatedInfo = mockUpdateAppInfo();
    when(appInfoRepository.findByFacilityCodeAndUniqueId(toBeUpdatedInfo.getFacilityCode(),
        toBeUpdatedInfo.getUniqueId())).thenReturn(null);
    when(appInfoRepository.save(toBeUpdatedInfo)).thenReturn(toBeUpdatedInfo);

    // when
    service.processAppInfo(toBeUpdatedInfo);
    AppInfo returnAppInfo = appInfoRepository.save(toBeUpdatedInfo);

    // then
    assertEquals(toBeUpdatedInfo, returnAppInfo);
  }

  @Test
  public void shouldUpdateHfCmmsWhenHfCmmsIsExist() {
    // given
    List<HfCmmDto> requestCmms = mockRequestHfCmms();
    FacilityDto facilityDto = mock(FacilityDto.class);
    facilityDto.setCode(facilityCode);
    when(facilityReferenceDataService.getFacilityById(any(UUID.class))).thenReturn(facilityDto);
    when(facilityCmmsRepository
        .findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(facilityDto.getCode(),
            requestCmms.get(0).getProductCode(), requestCmms.get(0).getPeriodBegin(),
            requestCmms.get(0).getPeriodEnd()))
        .thenReturn(mockExistFacilityCmms());
    when(facilityCmmsRepository.save(any(HfCmm.class))).thenReturn(mockUpdateSuccessHfCmm());

    // when
    service.processHfCmms(requestCmms);

    // then
    verify(facilityCmmsRepository).findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(any(),
        any(), any(), any());
    verify(facilityCmmsRepository).save(any(HfCmm.class));
    verify(facilityCmmsRepository).save(hfCmmArgumentCaptor.capture());
    HfCmm hfCmms = hfCmmArgumentCaptor.getValue();
    assertEquals(hfCmms.getId(), mockUpdateSuccessHfCmm().getId());
    assertEquals(hfCmms.getCmm(), mockUpdateSuccessHfCmm().getCmm());
  }

  @Test
  public void shouldSaveHfCmmWhenHfCmmIsNotExist() {
    // given
    List<HfCmmDto> requestCmms = mockRequestHfCmms();
    FacilityDto facilityDto = mock(FacilityDto.class);
    facilityDto.setCode(facilityCode);
    when(facilityReferenceDataService.getFacilityById(any(UUID.class))).thenReturn(facilityDto);
    when(facilityCmmsRepository
        .findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(facilityDto.getCode(),
            requestCmms.get(0).getProductCode(), requestCmms.get(0).getPeriodBegin(),
            requestCmms.get(0).getPeriodEnd()))
        .thenReturn(null);
    when(facilityCmmsRepository.save(any(HfCmm.class))).thenReturn(mockInsertSuccessHfCmm());

    // when
    service.processHfCmms(requestCmms);

    // then
    verify(facilityCmmsRepository).findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(any(),
        any(), any(), any());
    verify(facilityCmmsRepository).save(any(HfCmm.class));
    verify(facilityCmmsRepository).save(hfCmmArgumentCaptor.capture());
    HfCmm hfCmms = hfCmmArgumentCaptor.getValue();
    assertNotNull(hfCmms.getId());
    assertEquals(requestCmms.get(0).getCmm(), mockInsertSuccessHfCmm().getCmm());
    assertEquals(requestCmms.get(0).getProductCode(), mockInsertSuccessHfCmm().getProductCode());
    assertEquals(requestCmms.get(0).getPeriodBegin(), mockInsertSuccessHfCmm().getPeriodBegin());
    assertEquals(requestCmms.get(0).getPeriodEnd(), mockInsertSuccessHfCmm().getPeriodEnd());
  }

  @Test
  public void shouldGetSohValueByLot() {
    // given
    ProgramDto programDto = mock(ProgramDto.class);
    when(programDto.getId()).thenReturn(UUID.randomUUID());
    when(programDto.getCode()).thenReturn("code");
    when(approvedProductService.getApprovedProducts(facilityId, programDto.getId(), emptyList()))
        .thenReturn(asList(mockApprovedProduct1(), mockApprovedProduct2()));
    UUID lotId1 = UUID.randomUUID();
    UUID lotId2 = UUID.randomUUID();
    String lotCode1 = "lotCode1";
    String lotCode2 = "lotCode2";
    String extraLotCode3 = "lotCode3";
    when(lotReferenceDataService.findAllLot(any())).thenReturn(Arrays.asList(
        mockLotDto(lotCode1, lotId1, tradeItem1), mockLotDto(lotCode2, lotId2, tradeItem2),
        mockLotDto(extraLotCode3, UUID.randomUUID(), tradeItem2)));
    StockCardSummaryV2Dto summary1 = new StockCardSummaryV2Dto();
    VersionObjectReferenceDto orderableRef1 = new VersionObjectReferenceDto(productId1, "", "", 1L);
    summary1.setOrderable(orderableRef1);
    CanFulfillForMeEntryDto forMeEntryDto1 = new CanFulfillForMeEntryDtoDataBuilder()
        .withStockOnHand(10)
        .withOrderable(orderableRef1)
        .withLot(new VersionObjectReferenceDto(lotId1, "", "", 1L))
        .build();
    forMeEntryDto1.setStockOnHand(10);
    summary1.setCanFulfillForMe(newHashSet(forMeEntryDto1));

    StockCardSummaryV2Dto summary2 = new StockCardSummaryV2Dto();
    VersionObjectReferenceDto orderableRef2 = new VersionObjectReferenceDto(productId2, "", "", 1L);
    summary2.setOrderable(orderableRef2);
    CanFulfillForMeEntryDto forMeEntryDto2 = new CanFulfillForMeEntryDtoDataBuilder()
        .withStockOnHand(7)
        .withOrderable(orderableRef2)
        .withLot(new VersionObjectReferenceDto(lotId2, "", "", 1L))
        .build();
    CanFulfillForMeEntryDto forMeEntryDto3WithNullLot = new CanFulfillForMeEntryDtoDataBuilder()
        .withStockOnHand(15)
        .withOrderable(orderableRef2)
        .withLot(null)
        .build();
    summary2.setCanFulfillForMe(newHashSet(forMeEntryDto2, forMeEntryDto3WithNullLot));
    when(stockCardSummariesService.findAllProgramStockSummaries()).thenReturn(Arrays.asList(summary1, summary2));

    // when
    List<LotStockOnHand> lotStockOnHands = service.getLotStockOnHands();

    // then
    assertEquals(3, lotStockOnHands.size());
    LotStockOnHand stock1 = lotStockOnHands.stream().filter(s -> s.getProductId().equals(productId1))
        .filter(s -> s.getLotId().equals(lotId1))
        .findFirst().orElse(null);
    assertNotNull(stock1);
    assertEquals(10, stock1.getStockOnHand().intValue());

    LotStockOnHand stock2 = lotStockOnHands.stream().filter(s -> s.getProductId().equals(productId2))
        .filter(s -> s.getLotId() != null)
        .filter(s -> s.getLotId().equals(lotId2))
        .findFirst().orElse(null);
    assertNotNull(stock2);
    assertEquals(7, stock2.getStockOnHand().intValue());

    LotStockOnHand stock3 = lotStockOnHands.stream().filter(s -> s.getProductId().equals(productId2))
        .filter(s -> s.getLotId() == null)
        .findFirst().orElse(null);
    assertNotNull(stock3);
    assertEquals(15, stock3.getStockOnHand().intValue());
  }

  @Test
  public void shouldGetProductMovementResponsesByLot() {
    createSohValueByIsNolot(false);
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemMap = new HashMap<>();
    stockMovementItemMap.put(productId1, new ArrayList<>());
    stockMovementItemMap.put(productId2, new ArrayList<>());
    when(
        stockCardLineItemService.getStockMovementByOrderableId(any(), any(), any(), any()))
        .thenReturn(stockMovementItemMap);
    FacilityProductMovementsResponse productMovementsResponse = service.getProductMovements("2021-06-30", "2021-07-01");
    List<ProductMovementResponse> productMovementsResponseList = productMovementsResponse.getProductMovements();
    ProductMovementResponse response = productMovementsResponseList.stream()
        .filter(i -> i.getProductCode().equals(productCode1))
        .findFirst().orElse(new ProductMovementResponse());

    assertEquals(30, response.getStockOnHand().intValue());
  }

  @Test
  public void shouldGetProductMovementResponsesWhenNoLot() {
    createSohValueByIsNolot(true);
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemMap = new HashMap<>();
    stockMovementItemMap.put(productId1, new ArrayList<>());
    stockMovementItemMap.put(productId2, new ArrayList<>());
    when(
        stockCardLineItemService.getStockMovementByOrderableId(any(), any(), any(), any()))
        .thenReturn(stockMovementItemMap);
    FacilityProductMovementsResponse productMovementsResponse = service.getProductMovements("2021-06-30", "2021-07-01");
    List<ProductMovementResponse> productMovementsResponseList = productMovementsResponse.getProductMovements();
    ProductMovementResponse response = productMovementsResponseList.stream()
        .filter(i -> i.getProductCode().equals(productCode1))
        .findFirst().orElse(new ProductMovementResponse());

    assertEquals(emptyList(), response.getStockMovementItems());
    assertEquals(30, response.getStockOnHand().intValue());
  }

  @Test
  public void shouldGetProductMovementResponsesWhenNoMovement() {
    createSohValueByIsNolot(false);
    when(
        stockCardLineItemService.getStockMovementByOrderableId(any(), any(), any(), any())).thenReturn(emptyMap());
    FacilityProductMovementsResponse productMovementsResponse = service.getProductMovements("2021-06-30", "2021-07-01");
    List<ProductMovementResponse> productMovementsResponseList = productMovementsResponse.getProductMovements();
    ProductMovementResponse response = productMovementsResponseList.stream()
        .filter(i -> i.getProductCode().equals(productCode1))
        .findFirst().orElse(new ProductMovementResponse());

    assertEquals(30, response.getStockOnHand().intValue());
  }

  @Test
  public void shouldCallaArchiveProductServiceWhenDoArchive() {
    // given
    List<String> productCodes = Arrays.asList("product1", "product2", "product3");

    // when
    service.archiveAllProducts(productCodes);

    // then
    verify(siglusArchiveProductService).archiveAllProducts(facilityId, productCodes);
  }

  @Test
  public void shouldReturnAllProductsWhenGetFacilityProducts() {
    // when
    ProductSyncResponse syncResponse = service.getFacilityProducts(null);

    // then
    assertNotNull(syncResponse);
    assertJustNow(syncResponse.getLastSyncTime());
    assertNotNull(syncResponse.getProducts());
    assertEquals(3, syncResponse.getProducts().size());
    ProductResponse product1 = syncResponse.getProducts().get(0);
    assertProduct1(product1);

    ProductResponse product2 = syncResponse.getProducts().get(1);
    assertProduct2(product2);

    ProductResponse product3 = syncResponse.getProducts().get(2);
    assertProduct3(product3);
  }

  @Test
  public void shouldReturnAllProductsWhenGetFacilityProductsGivenSyncTime() {
    // when
    ProductSyncResponse syncResponse = service.getFacilityProducts(syncTime);

    // then
    assertNotNull(syncResponse);
    assertJustNow(syncResponse.getLastSyncTime());
    assertNotNull(syncResponse.getProducts());
    assertEquals(2, syncResponse.getProducts().size());
    ProductResponse product1 = syncResponse.getProducts().get(0);
    assertProduct2(product1);

    ProductResponse product2 = syncResponse.getProducts().get(1);
    assertProduct3(product2);
  }

  @Test
  public void shouldCallaSiglusRequisitionServiceWhenCreateRequisition() {
    // given
    RequisitionCreateRequest requisitionRequest = new RequisitionCreateRequest();

    // when
    service.createRequisition(requisitionRequest);

    // then
    verify(androidRequisitionService).create(requisitionRequest);
  }

  @Test
  public void shouldCallaSiglusRequisitionServiceWhenGetRequisition() {
    // when
    service.getRequisitionResponse("2021-05-01");

    // then
    verify(androidRequisitionService).getRequisitionResponseByFacilityIdAndDate(any(), any(), any());
  }

  private SupportedProgramDto getSupportedProgramDto() {
    SupportedProgramDto supportedProgram = new SupportedProgramDto();
    supportedProgram.setId(UUID.randomUUID());
    supportedProgram.setCode("ARV");
    supportedProgram.setName("ARV");
    supportedProgram.setDescription("description");
    supportedProgram.setProgramActive(true);
    supportedProgram.setSupportActive(true);
    supportedProgram.setSupportLocallyFulfilled(true);
    supportedProgram.setSupportStartDate(LocalDate.now());
    return supportedProgram;
  }

  private void createSohValueByIsNolot(boolean isNoLot) {
    ProgramDto programDto = mock(ProgramDto.class);
    when(programDto.getId()).thenReturn(UUID.randomUUID());
    when(programDto.getCode()).thenReturn("code");
    when(approvedProductService.getApprovedProducts(facilityId, programDto.getId(), emptyList()))
        .thenReturn(asList(mockApprovedProduct1(), mockApprovedProduct2()));
    String lotCode1 = "lotCode1";
    String lotCode2 = "lotCode2";
    String lotCode3 = "lotCode3";

    when(lotReferenceDataService.findAllLot(any())).thenReturn(Arrays.asList(
        mockLotDto(lotCode1, lotId1OrderableId1, tradeItem1), mockLotDto(lotCode2, lotId1OrderableId2, tradeItem2),
        mockLotDto(lotCode3, lotId2OrderableId1, tradeItem3)));
    StockCardSummaryV2Dto summary1 = new StockCardSummaryV2Dto();
    summary1.setOrderable(new VersionObjectReferenceDto(productId1, "", "", 1L));
    CanFulfillForMeEntryDto forMeEntryDto1 = new CanFulfillForMeEntryDtoDataBuilder()
        .withStockOnHand(10)
        .build();
    if (!isNoLot) {
      forMeEntryDto1.setLot(new VersionObjectReferenceDto(lotId1OrderableId1, "", "", 1L));
    }
    CanFulfillForMeEntryDto forMeEntryDto3 = new CanFulfillForMeEntryDtoDataBuilder()
        .withStockOnHand(20)
        .withLot(new VersionObjectReferenceDto(lotId2OrderableId1, "", "", 1L))
        .build();
    if (!isNoLot) {
      forMeEntryDto1.setLot(new VersionObjectReferenceDto(lotId2OrderableId1, "", "", 1L));
    }
    summary1.setCanFulfillForMe(newHashSet(forMeEntryDto1, forMeEntryDto3));

    StockCardSummaryV2Dto summary2 = new StockCardSummaryV2Dto();
    summary2.setOrderable(new VersionObjectReferenceDto(productId2, "", "", 1L));
    CanFulfillForMeEntryDto forMeEntryDto2 = new CanFulfillForMeEntryDtoDataBuilder()
        .withStockOnHand(10)
        .withLot(new VersionObjectReferenceDto(lotId1OrderableId2, "", "", 1L))
        .build();
    forMeEntryDto2.setStockOnHand(20);
    summary2.setCanFulfillForMe(newHashSet(forMeEntryDto2));
    when(stockCardSummariesService
        .findAllProgramStockSummaries()).thenReturn(Arrays.asList(summary1, summary2));
  }

  private void assertJustNow(Long time) {
    Duration duration = Duration.between(Instant.ofEpochMilli(time), Instant.now());
    assertTrue(duration.compareTo(Duration.ofSeconds(5)) < 0);
  }

  private void assertProduct1(ProductResponse product) {
    assertEquals(productCode1, product.getProductCode());
    assertEquals("full name of product 1", product.getFullProductName());
    assertEquals("description of product 1", product.getDescription());
    assertTrue(product.getActive());
    assertTrue(product.getArchived());
    assertEquals(1L, (long) product.getNetContent());
    assertEquals(3L, (long) product.getPackRoundingThreshold());
    assertTrue(product.getRoundToZero());
    assertEquals("code 1", product.getProgramCode());
    assertEquals("Default", product.getCategory());
    assertFalse(product.getIsKit());
    assertEquals(0, product.getChildren().size());
    assertFalse(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(oldTime.toInstant(), product.getLastUpdated());
  }

  private void assertProduct2(ProductResponse product) {
    assertEquals(productCode2, product.getProductCode());
    assertEquals("full name of product 2", product.getFullProductName());
    assertEquals("description of product 2", product.getDescription());
    assertTrue(product.getActive());
    assertFalse(product.getArchived());
    assertEquals(2L, (long) product.getNetContent());
    assertEquals(5L, (long) product.getPackRoundingThreshold());
    assertTrue(product.getRoundToZero());
    assertEquals("code 1", product.getProgramCode());
    assertEquals("12", product.getCategory());
    assertTrue(product.getIsKit());
    assertEquals(1, product.getChildren().size());
    ProductChildResponse child = product.getChildren().get(0);
    assertEquals(productCode1, child.getProductCode());
    assertEquals(100L, (long) child.getQuantity());
    assertFalse(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(latestTime.toInstant(), product.getLastUpdated());
  }

  private void assertProduct3(ProductResponse product) {
    assertEquals(productCode3, product.getProductCode());
    assertEquals("full name of product 3", product.getFullProductName());
    assertEquals("description of product 3", product.getDescription());
    assertFalse(product.getActive());
    assertFalse(product.getArchived());
    assertEquals(2L, (long) product.getNetContent());
    assertEquals(5L, (long) product.getPackRoundingThreshold());
    assertFalse(product.getRoundToZero());
    assertEquals("code 2", product.getProgramCode());
    assertEquals("13", product.getCategory());
    assertFalse(product.getIsKit());
    assertEquals(0, product.getChildren().size());
    assertTrue(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(latestTime.toInstant(), product.getLastUpdated());
  }

  private LotDto mockLotDto(String lotCode, UUID lotId, UUID tradeItemId) {
    LotDto lotDto = new LotDto();
    lotDto.setLotCode(lotCode);
    lotDto.setId(lotId);
    lotDto.setTradeItemId(tradeItemId);
    return lotDto;
  }

  private org.siglus.common.dto.referencedata.OrderableDto mockOrderable1() {
    String productCode = productCode1;
    org.siglus.common.dto.referencedata.OrderableDto orderable =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderable.setId(productId1);
    orderable.setArchived(true);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(1L);
    orderable.setPackRoundingThreshold(3L);
    orderable.setRoundToZero(true);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(oldTime);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("Default");
    orderable.setPrograms(singleton(programOrderableDto));
    return orderable;
  }

  private ApprovedProductDto mockApprovedProduct1() {
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    orderable.setIdentifiers(ImmutableMap.of(TRADE_ITEM, tradeItem1.toString()));
    approvedProduct.setOrderable(orderable);
    String productCode = productCode1;
    orderable.setId(productId1);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setNetContent(1L);
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(oldTime);
    return approvedProduct;
  }

  private org.siglus.common.dto.referencedata.OrderableDto mockOrderable2() {
    String productCode = productCode2;
    org.siglus.common.dto.referencedata.OrderableDto orderable =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderable.setId(productId2);
    orderable.setArchived(false);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(true);
    org.siglus.common.dto.referencedata.ObjectReferenceDto childRef =
        new org.siglus.common.dto.referencedata.ObjectReferenceDto();
    childRef.setId(productId1);
    org.siglus.common.dto.referencedata.OrderableChildDto child =
        new org.siglus.common.dto.referencedata.OrderableChildDto(childRef, 100L);
    orderable.setChildren(new HashSet<>());
    orderable.getChildren().add(child);
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(latestTime);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("12");
    orderable.setPrograms(singleton(programOrderableDto));
    return orderable;
  }

  private ApprovedProductDto mockApprovedProduct2() {
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    orderable.setIdentifiers(ImmutableMap.of(TRADE_ITEM, tradeItem2.toString()));
    approvedProduct.setOrderable(orderable);
    String productCode = productCode2;
    orderable.setId(productId2);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setNetContent(2L);
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(latestTime);
    return approvedProduct;
  }

  private org.siglus.common.dto.referencedata.OrderableDto mockOrderable3() {
    String productCode = productCode3;
    org.siglus.common.dto.referencedata.OrderableDto orderable =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderable.setId(productId3);
    orderable.setArchived(false);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(false);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getExtraData().put("active", "false");
    orderable.getExtraData().put("isBasic", "true");
    orderable.getMeta().setLastUpdated(latestTime);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("13");
    orderable.setPrograms(singleton(programOrderableDto));
    return orderable;
  }

  private ApprovedProductDto mockApprovedProduct3() {
    String productCode = productCode3;
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(productId3);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setNetContent(2L);
    orderable.setExtraData(new HashMap<>());
    orderable.getExtraData().put("isBasic", "true");
    orderable.getMeta().setLastUpdated(latestTime);
    return approvedProduct;
  }

  private String genFullName(String productCode) {
    return "full name of " + productCode;
  }

  private String genDescription(String productCode) {
    return "description of " + productCode;
  }

  private AppInfo mockCurrentAppInfo() {
    AppInfo appInfo = AppInfo.builder()
        .deviceInfo("deviceinfo1")
        .facilityName("Centro de Saude de Chiunze")
        .androidSdkVersion(28)
        .versionCode(88)
        .facilityCode("01080904")
        .username("CS_Moine_Role1")
        .uniqueId("ac36c07a09f2fdcd")
        .build();
    appInfo.setId(appInfoId);
    return appInfo;
  }

  private AppInfo mockUpdateAppInfo() {
    AppInfo appInfo = AppInfo.builder()
        .deviceInfo("deviceinfo2")
        .facilityName("Centro de Saude de Chiunze")
        .androidSdkVersion(28)
        .versionCode(88)
        .facilityCode("01080904")
        .username("CS_Moine_Role1")
        .uniqueId("ac36c07a09f2fdcd")
        .build();
    appInfo.setId(appInfoId);
    return appInfo;
  }

  private List<HfCmmDto> mockRequestHfCmms() {
    return Collections.singletonList(HfCmmDto.builder()
        .cmm(11.0)
        .productCode(productCode)
        .periodBegin(periodBegin)
        .periodEnd(periodEnd)
        .build());
  }

  private HfCmm mockExistFacilityCmms() {
    HfCmm existFacilityCmm = HfCmm.builder()
        .facilityCode(facilityCode)
        .cmm(12.0)
        .productCode(productCode)
        .periodBegin(periodBegin)
        .periodEnd(periodEnd)
        .lastUpdated(Instant.now())
        .build();
    existFacilityCmm.setId(hfCmmId);
    return existFacilityCmm;
  }

  private HfCmm mockUpdateSuccessHfCmm() {
    HfCmm hfCmm = HfCmm.builder()
        .facilityCode(facilityCode)
        .cmm(11.0)
        .productCode(productCode)
        .periodBegin(periodBegin)
        .periodEnd(periodEnd)
        .lastUpdated(Instant.now())
        .build();
    hfCmm.setId(hfCmmId);
    return hfCmm;
  }

  private HfCmm mockInsertSuccessHfCmm() {
    HfCmm hfCmm = HfCmm.builder()
        .facilityCode(facilityCode)
        .cmm(11.0)
        .productCode(productCode)
        .periodBegin(periodBegin)
        .periodEnd(periodEnd)
        .lastUpdated(now)
        .build();
    hfCmm.setId(hfCmmId);
    return hfCmm;
  }

  private List<SupportedProgramDto> getSupportedPrograms() {
    SupportedProgramDto supportedProgram1 = SupportedProgramDto.builder()
        .id(supportProgramId1)
        .code("VC")
        .name("Via Clássica")
        .description("description")
        .programActive(true)
        .supportActive(true)
        .supportLocallyFulfilled(true)
        .supportStartDate(LocalDate.now())
        .build();
    SupportedProgramDto supportedProgram2 = SupportedProgramDto.builder()
        .id(supportProgramId2)
        .code("T")
        .name("TARV")
        .description("description")
        .programActive(true)
        .supportActive(true)
        .supportLocallyFulfilled(true)
        .supportStartDate(LocalDate.now())
        .build();
    return asList(supportedProgram1, supportedProgram2);
  }

  private ReportType mockReportType1() {
    return ReportType.builder()
        .name("Requisition")
        .active(true)
        .startDate(LocalDate.parse("2020-08-21"))
        .programCode("VC")
        .facilityId(facilityId)
        .build();
  }

  private ReportType mockReportType2() {
    return ReportType.builder()
        .facilityId(facilityId)
        .name("MMIA")
        .active(false)
        .startDate(LocalDate.parse("2020-08-21"))
        .programCode("T")
        .build();
  }

  private Optional<Requisition> mockProgramRnr() {
    Map<String, Object> extraData = new HashMap<>();
    extraData.put(IS_SAVED, true);
    extraData.put(ACTUAL_END_DATE, "2021-06-26");
    extraData.put(ACTUAL_END_DATE, "2021-05-12");
    extraData.put(SIGNATURE, "");
    Requisition rnr = new Requisition();
    rnr.setExtraData(extraData);
    rnr.setProgramId(supportProgramId1);
    return Optional.ofNullable(rnr);
  }
}
