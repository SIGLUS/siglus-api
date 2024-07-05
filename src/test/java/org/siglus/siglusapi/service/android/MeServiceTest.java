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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_START_DATE;
import static org.siglus.common.constant.ExtraDataConstants.IS_SAVED;
import static org.siglus.common.constant.ExtraDataConstants.SIGNATURE;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.validation.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderLineItemDto;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.dto.ObjectReferenceDto;
import org.openlmis.referencedata.dto.OrderableChildDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.PodRequestBackup;
import org.siglus.siglusapi.domain.RequisitionRequestBackup;
import org.siglus.siglusapi.domain.ResyncInfo;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.domain.StockCardRequestBackup;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.GeographicProvinceDistrictDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.InvalidProduct;
import org.siglus.siglusapi.dto.android.RequisitionStatusDto;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.LotBasicRequest;
import org.siglus.siglusapi.dto.android.request.PodLotLineRequest;
import org.siglus.siglusapi.dto.android.request.PodProductLineRequest;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductChildResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.ReportTypeResponse;
import org.siglus.siglusapi.exception.OrderNotFoundException;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.PodRequestBackupRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesRepository;
import org.siglus.siglusapi.repository.RequisitionRequestBackupRepository;
import org.siglus.siglusapi.repository.ResyncInfoRepository;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.StockCardRequestBackupRepository;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;
import org.siglus.siglusapi.service.android.mapper.LotMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodLotLineMapper;
import org.siglus.siglusapi.service.android.mapper.PodLotLineMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodMapper;
import org.siglus.siglusapi.service.android.mapper.PodMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodOrderMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodProductLineMapperImpl;
import org.siglus.siglusapi.service.android.mapper.PodRequisitionMapperImpl;
import org.siglus.siglusapi.service.android.mapper.ProductChildMapperImpl;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMapperImpl;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.testutils.CanFulfillForMeEntryDtoDataBuilder;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.validator.android.StockCardCreateRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ProductMapperImpl.class, ProductChildMapperImpl.class, PodMapperImpl.class,
    ObjectMapper.class, PodOrderMapperImpl.class, PodRequisitionMapperImpl.class, PodProductLineMapperImpl.class,
    PodLotLineMapperImpl.class, LotMapperImpl.class})
@SuppressWarnings("PMD.TooManyMethods")
public class MeServiceTest {

  @InjectMocks
  private MeService service;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;

  private final List<SupportedProgramDto> programDtos = new ArrayList<>();

  @Mock
  private SiglusOrderableService orderableDataService;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SupportedProgramsHelper programsHelper;
  @SuppressWarnings("unused")
  @Mock
  private ProgramAdditionalOrderableRepository additionalProductRepo;

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
  private AndroidHelper androidHelper;

  @Mock
  private SiglusReportTypeRepository reportTypeRepository;

  @Mock
  private SiglusRequisitionRepository requisitionRepository;

  @Mock
  private RequisitionSearchService requisitionSearchService;

  @Mock
  private RequisitionRequestBackupRepository requisitionRequestBackupRepository;

  @Mock
  private StockCardRequestBackupRepository stockCardRequestBackupRepository;

  @Captor
  private ArgumentCaptor<StockCardRequestBackup> stockCardRequestBackupArgumentCaptor;

  @Captor
  private ArgumentCaptor<HfCmm> hfCmmArgumentCaptor;

  @Captor
  private ArgumentCaptor<ResyncInfo> resyncInfoArgumentCaptor;

  @Captor
  private ArgumentCaptor<RequisitionRequestBackup> requestBackupArgumentCaptor;

  @Mock
  private StockCardDeleteService stockCardDeleteService;

  @InjectMocks
  private StockCardSearchService stockCardSearchService;

  @InjectMocks
  private StockCardCreateService stockCardCreateService;

  @Mock
  private StockCardCreateContextHolder stockCardCreateContextHolder;

  @Mock
  private StockCardCreateRequestValidator stockCardCreateRequestValidator;

  @Mock
  private SiglusProofOfDeliveryRepository podRepository;

  @Mock
  private PodRequestBackupRepository podBackupRepository;

  @Mock
  private ResyncInfoRepository resyncInfoRepository;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;
  @Mock
  private SiglusValidReasonAssignmentService validReasonAssignmentService;

  @Mock
  private SiglusOrderService orderService;

  @Mock
  private SiglusOrderableReferenceDataService siglusOrderableDataService;

  @Mock
  private MeCreateRequisitionService meCreateRequisitionService;

  @Mock
  private SiglusGeographicInfoRepository siglusGeographicInfoRepository;

  @Mock
  private ProgramOrderablesRepository programOrderablesRepository;

  @Autowired
  private ProductMapper mapper;

  @Autowired
  private PodMapper podMapper;

  @Autowired
  private PodLotLineMapper podLotLineMapper;

  private final UUID appInfoId = UUID.randomUUID();

  private final UUID hfCmmId = UUID.randomUUID();

  private final String facilityCode = "01050119";

  private final String productCode = "05A20";

  private final LocalDate periodBegin = LocalDate.of(2021, 4, 21);

  private final LocalDate periodEnd = LocalDate.of(2021, 5, 20);

  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  private ZonedDateTime oldTime;
  private Instant syncTime;
  private ZonedDateTime latestTime;

  private UserDto user;

  private final UUID productId1 = UUID.randomUUID();
  private final UUID productId2 = UUID.randomUUID();
  private final UUID productId3 = UUID.randomUUID();

  private final String productCode1 = "product 1";
  private final String productCode2 = "product 2";
  private final String productCode3 = "product 3";

  private final String nameStockCardSearchService = "stockCardSearchService";
  private final String nameStockCardCreateService = "stockCardCreateService";

  private final UUID tradeItem1 = UUID.randomUUID();
  private final UUID tradeItem2 = UUID.randomUUID();
  private final UUID tradeItem3 = UUID.randomUUID();

  private final UUID lotId1OrderableId1 = UUID.randomUUID();
  private final UUID lotId2OrderableId1 = UUID.randomUUID();
  private final UUID lotId1OrderableId2 = UUID.randomUUID();

  private final UUID supportProgramId1 = UUID.randomUUID();
  private final UUID supportProgramId2 = UUID.randomUUID();

  private final UUID order1Id = UUID.randomUUID();
  private final UUID order1FacilityId = UUID.randomUUID();
  private final UUID product1Id = UUID.randomUUID();
  private final UUID product1Lot1Id = UUID.randomUUID();
  private final UUID product2Lot1Id = UUID.randomUUID();
  private final UUID reasonId = UUID.randomUUID();
  private final String orderCode = "ORDER-AS20JF";

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(service, "mapper", mapper);
    ReflectionTestUtils.setField(service, "podMapper", podMapper);
    ReflectionTestUtils.setField(service, "podLotLineMapper", podLotLineMapper);
    ReflectionTestUtils.setField(service, nameStockCardSearchService, stockCardSearchService);
    ReflectionTestUtils.setField(service, nameStockCardCreateService, stockCardCreateService);
    user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(user.getId()).thenReturn(userId);
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
    when(programsHelper.findHomeFacilitySupportedProgramIds()).thenReturn(ImmutableSet.of(programId1, programId2));
    when(programsHelper.findHomeFacilitySupportedPrograms())
        .thenReturn(getSupportedPrograms());
    when(orderableDataService.searchOrderables(any(), any(), any()))
        .thenReturn(new PageImpl<>(asList(mockOrderable1(), mockOrderable2(), mockOrderable3())));
    when(programOrderablesRepository.findByProgramIdIn(any()))
        .thenReturn(asList(mockProgramOrderable1(), mockProgramOrderable2(), mockProgramOrderable3()));
    when(programOrderablesExtensionRepository.findAllByOrderableIdIn(any()))
        .thenReturn(asList(mockProgramOrderableExtension1(), mockProgramOrderableExtension2(),
            mockProgramOrderableExtension3()));
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, programId1))
        .thenReturn(asList(mockApprovedProduct1(), mockApprovedProduct2()));
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, programId2))
        .thenReturn(singletonList(mockApprovedProduct3()));
    when(archivedProductRepo.findArchivedProductsByFacilityId(facilityId)).thenReturn(singleton(productId1.toString()));
    when(androidHelper.isAndroid()).thenReturn(true);
    when(requisitionRequestBackupRepository.findOneByHash(anyString())).thenReturn(null);
  }


  @Test
  public void shouldCallFacilityReferenceDataServiceWhenGetFacility() {
    // given
    programDtos.addAll(getSupportedPrograms());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setCode("facilityCode");
    facilityDto.setName("facilityName");
    facilityDto.setSupportedPrograms(programDtos);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    GeographicProvinceDistrictDto geographicDto = GeographicProvinceDistrictDto.builder()
        .provinceCode("provinceCode")
        .provinceName("provinceName")
        .districtCode("districtCode")
        .districtName("districtName")
        .build();
    when(siglusGeographicInfoRepository.getGeographicProvinceDistrictInfo(facilityDto.getCode()))
        .thenReturn(geographicDto);

    // when
    FacilityResponse response = service.getCurrentFacility();

    // then
    assertEquals(programDtos.get(0).getCode(), response.getSupportedPrograms().get(0).getCode());
    assertEquals(geographicDto.getProvinceCode(), response.getProvinceCode());
  }

  @Test
  public void shouldGetSupportReportTypesWhenGetFacilityInfo() {
    // given
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    facilityDto.setCode("facilityCode");
    facilityDto.setName("facilityName");
    facilityDto.setSupportedPrograms(getSupportedPrograms());
    List<SiglusReportType> reportTypes = new ArrayList<>(asList(mockReportType1(), mockReportType2()));
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(reportTypeRepository.findByFacilityId(facilityId))
        .thenReturn(reportTypes);
    List<Requisition> requisitions = mockProgramRnr().map(Collections::singletonList).orElse(emptyList());
    when(requisitionRepository.findLatestRequisitionsByFacilityId(facilityId)).thenReturn(requisitions);
    GeographicProvinceDistrictDto geographicDto = GeographicProvinceDistrictDto.builder()
        .provinceCode("provinceCode")
        .provinceName("provinceName")
        .districtCode("districtCode")
        .districtName("districtName")
        .build();
    when(siglusGeographicInfoRepository.getGeographicProvinceDistrictInfo(facilityDto.getCode()))
        .thenReturn(geographicDto);

    // when
    FacilityResponse response = service.getCurrentFacility();

    // then
    List<ReportTypeResponse> actualReportTypes = response.getSupportedReportTypes();
    assertEquals(reportTypes.get(0).getName(), actualReportTypes.get(0).getName());
    assertEquals(reportTypes.get(0).getStartDate(), actualReportTypes.get(0).getSupportStartDate());
    assertEquals(reportTypes.get(0).getProgramCode(), actualReportTypes.get(0).getProgramCode());
    assertEquals(mockProgramRnr().map(r -> r.getExtraData().get(ACTUAL_END_DATE)).map(String::valueOf)
        .map(LocalDate::parse).orElse(null), actualReportTypes.get(0).getLastReportDate());
    assertEquals(reportTypes.get(1).getName(), actualReportTypes.get(1).getName());
    assertEquals(reportTypes.get(1).getStartDate(), actualReportTypes.get(1).getSupportStartDate());
    assertEquals(reportTypes.get(1).getProgramCode(), actualReportTypes.get(1).getProgramCode());
    assertNull(actualReportTypes.get(1).getLastReportDate());
    assertEquals(geographicDto.getProvinceCode(), response.getProvinceCode());
  }

  @Test
  public void shouldUpdateAppInfoWhenAppInfoIsExist() {
    // given
    AppInfo existedInfo = mockCurrentAppInfo();
    AppInfo toBeUpdatedInfo = mockUpdateAppInfo();
    when(appInfoRepository.findByFacilityCode(toBeUpdatedInfo.getFacilityCode())).thenReturn(existedInfo);
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
    when(appInfoRepository.findByFacilityCode(toBeUpdatedInfo.getFacilityCode())).thenReturn(null);
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
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facilityDto);
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
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facilityDto);
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
  public void shouldCallProcessResyncInfo() {
    // given
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    userDto.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(userDto);
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setId(facilityId);
    facilityDto.setName("facilityName");
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    AndroidHeader header = mock(AndroidHeader.class);

    // when
    service.processResyncInfo(header);

    // then
    verify(resyncInfoRepository, times(1)).save(any(ResyncInfo.class));
    verify(resyncInfoRepository).save(resyncInfoArgumentCaptor.capture());
    ResyncInfo resyncInfo = resyncInfoArgumentCaptor.getValue();
    assertEquals(resyncInfo.getFacilityId(), facilityId);
    assertEquals(resyncInfo.getUserId(), userId);
  }

  @Test
  public void shouldCallDeleteStockCardByProduct() {
    // given
    List<StockCardDeleteRequest> stockCardDeleteRequests = Collections.singletonList(new StockCardDeleteRequest());

    // when
    service.deleteStockCardByProduct(stockCardDeleteRequests);

    // then
    verify(stockCardDeleteService).deleteStockCardByProduct(stockCardDeleteRequests);
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
    assertEquals(latestTime.toInstant().toEpochMilli(), (long) syncResponse.getLastSyncTime());
    assertNotNull(syncResponse.getProducts());
    assertEquals(3, syncResponse.getProducts().size());
  }

  @Test
  public void shouldReturnAllProductsWhenGetFacilityProductsGivenSyncTime() {
    // when
    ProductSyncResponse syncResponse = service.getFacilityProducts(syncTime);

    // then
    assertNotNull(syncResponse);
    assertEquals(latestTime.toInstant().toEpochMilli(), (long) syncResponse.getLastSyncTime());
    assertNotNull(syncResponse.getProducts());
    assertEquals(2, syncResponse.getProducts().size());
  }

  @Test
  public void shouldCallaSiglusRequisitionServiceWhenCreateRequisition() {
    // given
    RequisitionCreateRequest requisitionRequest = buildRequisitionCreateRequest();

    // when
    service.createRequisition(requisitionRequest);

    // then
    verify(meCreateRequisitionService).createRequisition(requisitionRequest);
  }

  @Test
  public void shouldCallaSiglusRequisitionServiceWhenGetRequisition() {
    // when
    service.getRequisitionResponse("2021-05-01");

    // then
    verify(requisitionSearchService).getRequisitionResponseByFacilityIdAndDate(any(), any(), any());
  }

  @Test
  public void shouldBackupRequisitionRequestWhenErrorHappenedIfBackupNotExisted() {
    // given
    RequisitionCreateRequest requisitionRequest = buildRequisitionCreateRequest();
    when(requisitionRequestBackupRepository.findOneByHash(anyString())).thenReturn(null);
    doThrow(new NullPointerException()).when(meCreateRequisitionService).createRequisition(requisitionRequest);

    try {
      // when
      service.createRequisition(requisitionRequest);
    } catch (Exception e) {
      // then
      verify(meCreateRequisitionService).createRequisition(requisitionRequest);
      verify(requisitionRequestBackupRepository).save(requestBackupArgumentCaptor.capture());
    }
  }

  @Test
  public void shouldSkipBackupRequisitionRequestWhenErrorHappenedIfBackupExisted() {
    // given
    RequisitionCreateRequest requisitionRequest = buildRequisitionCreateRequest();
    RequisitionRequestBackup backup = new RequisitionRequestBackup();
    when(requisitionRequestBackupRepository.findOneByHash(anyString())).thenReturn(backup);
    doThrow(new ConstraintViolationException(Collections.emptySet()))
        .when(meCreateRequisitionService).createRequisition(requisitionRequest);

    try {
      // when
      service.createRequisition(requisitionRequest);
    } catch (Exception e) {
      // then
      verify(meCreateRequisitionService).createRequisition(requisitionRequest);
      verify(requisitionRequestBackupRepository, times(0)).save(backup);
    }
  }

  @Test
  public void shouldThrowExceptionWhenCreateRequisitionRequestIsEmpty() {
    // given
    RequisitionCreateRequest requisitionRequest = new RequisitionCreateRequest();
    doThrow(new ConstraintViolationException(Collections.emptySet()))
        .when(meCreateRequisitionService).createRequisition(requisitionRequest);
    // when
    try {
      // when
      service.createRequisition(requisitionRequest);
    } catch (Exception e) {
      verify(requisitionRequestBackupRepository, times(0)).save((RequisitionRequestBackup) any());
    }
  }

  @Test
  public void shouldBackupStockCardRequestWhenErrorHappenedIfBackupNotExisted() {
    // given
    List<StockCardCreateRequest> stockCardCreateRequests = buildStockCardCreateRequests();
    when(stockCardRequestBackupRepository.findOneByHash(anyString())).thenReturn(null);
    StockCardCreateService stockCardCreateServiceMock = mock(StockCardCreateService.class);
    ReflectionTestUtils.setField(service, nameStockCardCreateService, stockCardCreateServiceMock);
    doThrow(new NullPointerException()).when(stockCardCreateServiceMock)
        .createStockCards(eq(stockCardCreateRequests), any());
    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(stockCardCreateRequests))
        .thenReturn(mock(ValidatedStockCards.class));
    try {
      // when
      service.createStockCards(stockCardCreateRequests);
    } catch (Exception e) {
      // then
      verify(stockCardCreateServiceMock).createStockCards(eq(stockCardCreateRequests), any());
      verify(stockCardRequestBackupRepository).save(stockCardRequestBackupArgumentCaptor.capture());
    }
  }

  @Test
  public void shouldSkipBackupStockCardRequestWhenErrorHappenedIfBackupExisted() {
    // given
    List<StockCardCreateRequest> stockCardCreateRequests = buildStockCardCreateRequests();
    StockCardRequestBackup backup = new StockCardRequestBackup();
    when(stockCardRequestBackupRepository.findOneByHash(anyString())).thenReturn(backup);
    StockCardCreateService stockCardCreateServiceMock = mock(StockCardCreateService.class);
    ReflectionTestUtils.setField(service, nameStockCardCreateService, stockCardCreateServiceMock);
    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(stockCardCreateRequests))
        .thenReturn(mock(ValidatedStockCards.class));
    doThrow(new ConstraintViolationException(Collections.emptySet()))
        .when(stockCardCreateServiceMock).createStockCards(eq(stockCardCreateRequests), any());
    try {
      // when
      service.createStockCards(stockCardCreateRequests);
    } catch (Exception e) {
      // then
      verify(stockCardCreateServiceMock).createStockCards(eq(stockCardCreateRequests), any());
      verify(stockCardRequestBackupRepository, times(0)).save(backup);
    }
  }

  @Test
  public void shouldThrowExceptionWhenCreateStockCardRequestIsEmpty() {
    // given
    List<StockCardCreateRequest> stockCardCreateRequests = singletonList(new StockCardCreateRequest());
    StockCardCreateService stockCardCreateServiceMock = mock(StockCardCreateService.class);
    ReflectionTestUtils.setField(service, nameStockCardCreateService, stockCardCreateServiceMock);
    doThrow(new ConstraintViolationException(Collections.emptySet()))
        .when(stockCardCreateServiceMock).createStockCards(eq(stockCardCreateRequests), any());
    // when
    try {
      // when
      service.createStockCards(stockCardCreateRequests);
    } catch (Exception e) {
      verify(stockCardRequestBackupRepository, times(0)).save((StockCardRequestBackup) any());
    }
  }

  @Test
  public void shouldCallCreateStockCardContextHolder() {
    // given
    List<StockCardCreateRequest> stockCardCreateRequests = buildStockCardCreateRequests();
    ValidatedStockCards validatedStockCards = mock(ValidatedStockCards.class);
    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(stockCardCreateRequests))
        .thenReturn(validatedStockCards);

    // when
    service.createStockCards(stockCardCreateRequests);

    // then
    verify(stockCardCreateContextHolder, times(1)).initContext(any(), any());
  }

  @Test
  public void shouldCallBackupStockCardRequest() {
    // given
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    userDto.setId(userId);
    List<StockCardCreateRequest> stockCardCreateRequests = buildStockCardCreateRequests();
    InvalidProduct invalidProduct = InvalidProduct.builder().productCode("code").build();
    ValidatedStockCards validatedStockCards = ValidatedStockCards.builder()
        .validStockCardRequests(Collections.emptyList())
        .invalidProducts(Collections.singletonList(invalidProduct)).build();
    when(stockCardCreateRequestValidator.validateStockCardCreateRequest(stockCardCreateRequests))
        .thenReturn(validatedStockCards);
    when(authHelper.getCurrentUser()).thenReturn(userDto);
    when(stockCardRequestBackupRepository.findOneByHash(any())).thenReturn(null);

    // when
    service.createStockCards(stockCardCreateRequests);

    //then
    verify(stockCardRequestBackupRepository, times(1)).save(any(StockCardRequestBackup.class));
  }

  @Test(expected = OrderNotFoundException.class)
  public void shouldBackUpWhenOrderNumIsNotExist() {
    //given
    PodRequest podRequest = mockPodRequest();
    when(podRepository.findInitiatedPodByOrderCode(podRequest.getOrderCode())).thenReturn(null);
    String syncHash = "hashcode";
    when(podBackupRepository.findOneByHash(syncHash)).thenReturn(null);

    // when
    service.confirmPod(podRequest, true);

    // then
    verify(podBackupRepository, times(1)).save(any(PodRequestBackup.class));

  }

  @Test
  public void shouldGetRegularRequisitionStatus() {
    mockAuth();
    SupportedProgramDto supportedProgramDto = SupportedProgramDto.builder().programActive(true).supportActive(true)
        .code("programCode").build();
    SupportedProgramDto supportedProgramDto1 = SupportedProgramDto.builder().programActive(false).supportActive(true)
        .code("programCode_1").build();
    when(programsHelper.findHomeFacilitySupportedPrograms())
        .thenReturn(newArrayList(supportedProgramDto, supportedProgramDto1));
    RequisitionStatusDto dto = new RequisitionStatusDto();
    dto.setProgramCode("programCode");
    List<RequisitionStatusDto> requisitionStatusDtos = Collections.singletonList(dto);

    service.getRegularRequisitionStatus(requisitionStatusDtos);

    verify(requisitionSearchService).getRegularRequisitionsStatus(any(), any(), any());
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionWhenGetRegularRequisitionStatusGivenUnSupportProgramCode() {
    mockAuth();
    SupportedProgramDto supportedProgramDto = SupportedProgramDto.builder().programActive(true).supportActive(true)
        .code("programCode").build();
    SupportedProgramDto supportedProgramDto1 = SupportedProgramDto.builder().programActive(false).supportActive(true)
        .code("programCode_1").build();
    when(programsHelper.findHomeFacilitySupportedPrograms())
        .thenReturn(newArrayList(supportedProgramDto, supportedProgramDto1));
    RequisitionStatusDto dto = new RequisitionStatusDto();
    dto.setProgramCode("programCode_2");
    List<RequisitionStatusDto> requisitionStatusDtos = Collections.singletonList(dto);

    service.getRegularRequisitionStatus(requisitionStatusDtos);
  }

  private List<StockCardCreateRequest> buildStockCardCreateRequests() {
    StockCardLotEventRequest eventRequest = new StockCardLotEventRequest();
    eventRequest.setStockOnHand(100);
    eventRequest.setDocumentationNo("document");
    eventRequest.setLotCode("20210811-yyd");
    eventRequest.setExpirationDate(LocalDate.of(2021, 10, 20));
    eventRequest.setQuantity(20);
    StockCardCreateRequest stockCardRequest = new StockCardCreateRequest();
    stockCardRequest.setStockOnHand(100);
    stockCardRequest.setQuantity(20);
    stockCardRequest.setProductCode("02A05");
    stockCardRequest.setOccurredDate(LocalDate.of(2021, 8, 11));
    stockCardRequest.setType("ADJUSTMENT");
    stockCardRequest.setRecordedAt(Instant.now());
    stockCardRequest.setLotEvents(singletonList(eventRequest));
    return singletonList(stockCardRequest);
  }

  private RequisitionCreateRequest buildRequisitionCreateRequest() {
    return RequisitionCreateRequest.builder()
        .actualStartDate(LocalDate.of(2021, 5, 20))
        .actualEndDate(LocalDate.of(2021, 6, 20))
        .emergency(false)
        .programCode("VC")
        .clientSubmittedTime(Instant.now())
        .build();
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
    when(requisitionService.getApprovedProductsWithoutAdditional(facilityId, programDto.getId()))
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
    assertTrue(product.getShowInReport());
    assertEquals("comp", product.getUnit());
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
    assertFalse(product.getShowInReport());
    assertEquals("each", product.getUnit());
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
    assertTrue(product.getShowInReport());
    assertEquals("pack", product.getUnit());
  }

  private LotDto mockLotDto(String lotCode, UUID lotId, UUID tradeItemId) {
    LotDto lotDto = new LotDto();
    lotDto.setLotCode(lotCode);
    lotDto.setId(lotId);
    lotDto.setTradeItemId(tradeItemId);
    return lotDto;
  }

  private ProgramOrderable mockProgramOrderable1() {
    Program program = new Program(supportProgramId1);
    program.setCode(new Code("program code 1"));
    Orderable orderable = new Orderable(new Code(productCode1),
        null, 1, 3, true, productId1, 1L);
    orderable.setLastUpdated(oldTime);
    return new ProgramOrderable(program, orderable, 1, true, null, true, 0, null);
  }

  private ProgramOrderable mockProgramOrderable2() {
    Program program = new Program(supportProgramId1);
    program.setCode(new Code("program code 1"));
    Orderable orderable = new Orderable(new Code(productCode2),
        null, 1, 3, true, productId2, 1L);
    orderable.setLastUpdated(latestTime);
    return new ProgramOrderable(program, orderable, 1, true, null, true, 0, null);
  }

  private ProgramOrderable mockProgramOrderable3() {
    Program program = new Program(supportProgramId2);
    program.setCode(new Code("program code 2"));
    Orderable orderable = new Orderable(new Code(productCode3),
        null, 1, 3, true, productId3, 1L);
    orderable.setLastUpdated(latestTime);
    return new ProgramOrderable(program, orderable, 1, true, null, true, 0, null);
  }

  private ProgramOrderablesExtension mockProgramOrderableExtension1() {
    return ProgramOrderablesExtension.builder()
        .orderableId(productId1)
        .showInReport(true)
        .unit("comp")
        .build();
  }

  private ProgramOrderablesExtension mockProgramOrderableExtension2() {
    return ProgramOrderablesExtension.builder()
        .orderableId(productId2)
        .showInReport(false)
        .unit("each")
        .build();
  }

  private ProgramOrderablesExtension mockProgramOrderableExtension3() {
    return ProgramOrderablesExtension.builder()
        .orderableId(productId3)
        .showInReport(true)
        .unit("pack")
        .build();
  }

  private org.openlmis.referencedata.dto.OrderableDto mockOrderable1() {
    String productCode = productCode1;
    org.openlmis.referencedata.dto.OrderableDto orderable =
        new org.openlmis.referencedata.dto.OrderableDto();
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

  private org.openlmis.referencedata.dto.OrderableDto mockOrderable2() {
    String productCode = productCode2;
    org.openlmis.referencedata.dto.OrderableDto orderable =
        new org.openlmis.referencedata.dto.OrderableDto();
    orderable.setId(productId2);
    orderable.setArchived(false);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(true);
    ObjectReferenceDto childRef = new ObjectReferenceDto();
    childRef.setId(productId1);
    OrderableChildDto child = new OrderableChildDto(childRef, 100L);
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

  private org.openlmis.referencedata.dto.OrderableDto mockOrderable3() {
    String productCode = productCode3;
    org.openlmis.referencedata.dto.OrderableDto orderable =
        new org.openlmis.referencedata.dto.OrderableDto();
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
        .androidSdkVersion("28")
        .versionCode("88")
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
        .androidSdkVersion("28")
        .versionCode("88")
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

  private SiglusReportType mockReportType1() {
    return SiglusReportType.builder()
        .name("Requisition")
        .active(true)
        .startDate(LocalDate.parse("2020-08-21"))
        .programCode("VC")
        .facilityId(facilityId)
        .build();
  }

  private SiglusReportType mockReportType2() {
    return SiglusReportType.builder()
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
    extraData.put(ACTUAL_START_DATE, "2021-05-12");
    extraData.put(SIGNATURE, "");
    Requisition rnr = new Requisition();
    rnr.setExtraData(extraData);
    rnr.setProgramId(supportProgramId1);
    return Optional.of(rnr);
  }

  private PodRequest mockPodRequest() {
    LotBasicRequest lot = new LotBasicRequest();
    String lotCode = "lotCode";
    lot.setCode(lotCode);
    lot.setExpirationDate(LocalDate.now());
    PodLotLineRequest podLotLineRequest = new PodLotLineRequest();
    podLotLineRequest.setLot(lot);
    String notes = "notes";
    podLotLineRequest.setNotes(notes);
    String rejectReason = "rejectReason";
    podLotLineRequest.setRejectedReason(rejectReason);
    List<PodLotLineRequest> podLots = Arrays.asList(podLotLineRequest);
    PodProductLineRequest podProductLineRequest = new PodProductLineRequest();
    String orderProductCode = "orderProductCode";
    podProductLineRequest.setCode(orderProductCode);
    podProductLineRequest.setOrderedQuantity(10);
    podProductLineRequest.setPartialFulfilledQuantity(0);
    podProductLineRequest.setLots(podLots);
    List<PodProductLineRequest> products = Arrays.asList(podProductLineRequest);
    PodRequest podRequest = new PodRequest();
    podRequest.setOrderCode(orderCode);
    podRequest.setProducts(products);
    podRequest.setOriginNumber("ORDER-AS10JF");
    return podRequest;
  }

  private void mockExistedPod() {
    VersionEntityReference orderable1 = mock(VersionEntityReference.class);
    when(orderable1.getId()).thenReturn(product1Id);

    ProofOfDelivery pod1 = mockPod1(orderable1);

    when(podRepository.findAllByFacilitySince(any(), any(), anyVararg())).thenReturn(singletonList(pod1));
  }

  private ProofOfDelivery mockPod1(VersionEntityReference orderable1) {
    ProofOfDelivery pod1 = mock(ProofOfDelivery.class);
    when(pod1.getReceivedDate()).thenReturn(LocalDate.of(2020, 10, 1));
    when(pod1.getDeliveredBy()).thenReturn("qla");
    when(pod1.getReceivedBy()).thenReturn("zjj");
    Shipment shipment1 = mock(Shipment.class);
    when(pod1.getShipment()).thenReturn(shipment1);
    when(shipment1.getShippedDate()).thenReturn(LocalDate.of(2020, 9, 2).atStartOfDay(ZoneId.systemDefault()));
    Order order1 = mock(Order.class);
    when(shipment1.getOrder()).thenReturn(order1);
    ShipmentLineItem shipment1Line1 = mock(ShipmentLineItem.class);
    when(shipment1Line1.getLotId()).thenReturn(product1Lot1Id);
    when(shipment1Line1.getQuantityShipped()).thenReturn(20L);
    when(shipment1Line1.getOrderable()).thenReturn(orderable1);
    when(shipment1.getLineItems()).thenReturn(singletonList(shipment1Line1));
    when(order1.getId()).thenReturn(order1Id);
    when(order1.getFacilityId()).thenReturn(facilityId);
    ProofOfDeliveryLineItem pod1Line1 = mock(ProofOfDeliveryLineItem.class);
    when(pod1Line1.getLotId()).thenReturn(product1Lot1Id);
    when(pod1Line1.getQuantityAccepted()).thenReturn(10);
    when(pod1Line1.getQuantityRejected()).thenReturn(10);
    when(pod1Line1.getRejectionReasonId()).thenReturn(reasonId);
    when(pod1Line1.getNotes()).thenReturn("123");
    when(pod1Line1.getOrderable()).thenReturn(orderable1);
    when(pod1.getLineItems()).thenReturn(singletonList(pod1Line1));
    return pod1;
  }

  private void mockAuth() {
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    org.siglus.siglusapi.dto.FacilityDto homeFacility =
        new org.siglus.siglusapi.dto.FacilityDto();
    homeFacility.setId(facilityId);
    FacilityTypeDto facilityType = new FacilityTypeDto();
    facilityType.setId(UUID.randomUUID());
    homeFacility.setType(facilityType);
    when(facilityReferenceDataService.findOne(facilityId)).thenReturn(homeFacility);
  }

  private void mockOrders() {
    org.openlmis.fulfillment.service.referencedata.FacilityDto supplyingFacility = mock(
        org.openlmis.fulfillment.service.referencedata.FacilityDto.class);
    when(supplyingFacility.getName()).thenReturn("Centro de Saude de ntopa");
    org.openlmis.fulfillment.service.referencedata.OrderableDto product1 = mock(
        org.openlmis.fulfillment.service.referencedata.OrderableDto.class);
    when(product1.getId()).thenReturn(product1Id);
    when(product1.getProductCode()).thenReturn("22A01");
    mockOrder1(supplyingFacility, product1);
  }

  private void mockOrder1(org.openlmis.fulfillment.service.referencedata.FacilityDto supplyingFacility,
      org.openlmis.fulfillment.service.referencedata.OrderableDto product1) {
    OrderDto order1 = mock(OrderDto.class);
    when(order1.getId()).thenReturn(order1Id);
    when(order1.getOrderCode()).thenReturn(orderCode);
    when(order1.getCreatedDate()).thenReturn(LocalDate.of(2020, 9, 2).atStartOfDay(ZoneId.systemDefault()));
    when(order1.getLastUpdatedDate())
        .thenReturn(LocalDate.of(2020, 9, 2).atTime(10, 15).atZone(ZoneId.systemDefault()));
    when(order1.getStatus()).thenReturn(OrderStatus.RECEIVED);
    when(order1.getRequisitionNumber()).thenReturn("RNR-NO01050119-0");
    when(order1.getEmergency()).thenReturn(false);
    when(order1.getSupplyingFacility()).thenReturn(supplyingFacility);
    ProcessingPeriodDto order1Period = new ProcessingPeriodDto();
    order1Period.setStartDate(LocalDate.of(2020, 7, 21));
    order1Period.setEndDate(LocalDate.of(2020, 8, 20));
    when(order1.getActualStartDate()).thenReturn(LocalDate.of(2020, 7, 22));
    when(order1.getActualEndDate()).thenReturn(LocalDate.of(2020, 8, 25));
    when(order1.getProcessingPeriod()).thenReturn(order1Period);
    org.openlmis.fulfillment.service.referencedata.ProgramDto order1Program = mock(
        org.openlmis.fulfillment.service.referencedata.ProgramDto.class);
    when(order1Program.getCode()).thenReturn("VC");
    when(order1.getProgram()).thenReturn(order1Program);
    OrderLineItemDto order1Line1 = mock(OrderLineItemDto.class);
    when(order1.orderLineItems()).thenReturn(singletonList(order1Line1));
    when(order1Line1.getOrderable()).thenReturn(product1);
    when(order1Line1.getOrderedQuantity()).thenReturn(20L);
    when(order1Line1.getPartialFulfilledQuantity()).thenReturn(0L);
    when(orderService.searchOrderByIdWithoutProducts(order1Id)).thenReturn(new SiglusOrderDto(order1, emptySet()));
    Requisition requisition = new Requisition();
    requisition.setCreatedDate(ZonedDateTime.now());
    requisition.setModifiedDate(ZonedDateTime.now());
    when(orderService.getRequisitionByOrder(order1)).thenReturn(requisition);
    when(facilityReferenceDataService.findOne(order1FacilityId))
        .thenReturn(new org.siglus.siglusapi.dto.FacilityDto());
  }

  private void mockLots() {
    LotDto product1Lot1 = mock(LotDto.class);
    when(product1Lot1.getId()).thenReturn(product1Lot1Id);
    when(product1Lot1.getLotCode()).thenReturn("SME-LOTE-22A01-062023");
    when(product1Lot1.getExpirationDate()).thenReturn(LocalDate.of(2023, 6, 30));
    LotDto product2Lot1 = mock(LotDto.class);
    when(product2Lot1.getId()).thenReturn(product2Lot1Id);
    when(product2Lot1.getLotCode()).thenReturn("SME-LOTE-22B01-062023");
    when(product2Lot1.getExpirationDate()).thenReturn(LocalDate.of(2023, 6, 30));
    when(lotReferenceDataService.findByIds(any())).thenReturn(asList(product1Lot1, product2Lot1));
  }

  private void mockReasons() {
    ValidReasonAssignmentDto reason1 = mock(ValidReasonAssignmentDto.class);
    when(reason1.getId()).thenReturn(reasonId);
    StockCardLineItemReason reason = mock(StockCardLineItemReason.class);
    when(reason.getName()).thenReturn("reject");
    when(reason1.getReason()).thenReturn(reason);
    when(validReasonAssignmentService.getValidReasonsForAllProducts(any(), any(), any()))
        .thenReturn(Collections.singleton(reason1));
  }

  private void mockProducts() {
    org.openlmis.referencedata.dto.OrderableDto product1 =
        mock(org.openlmis.referencedata.dto.OrderableDto.class);
    when(product1.getId()).thenReturn(product1Id);
    when(product1.getProductCode()).thenReturn("22A01");
    when(siglusOrderableDataService.findByIds(any())).thenReturn(Collections.singletonList(product1));
  }
}
