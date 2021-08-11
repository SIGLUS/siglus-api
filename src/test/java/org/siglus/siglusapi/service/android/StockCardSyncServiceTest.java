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
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.testutils.CanFulfillForMeEntryDtoDataBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.StockCardDeletedBackup;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.request.StockCardListCreateRequest;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.StockCardBackupRepository;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class StockCardSyncServiceTest {

  @InjectMocks
  private StockCardSyncService service;

  @Mock
  private StockCardBackupRepository stockCardBackupRepository;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private ProgramReferenceDataService programDataService;

  @Mock
  private SiglusStockCardLineItemService stockCardLineItemService;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Mock
  private SiglusStockCardSummariesService stockCardSummariesService;

  @Captor
  private ArgumentCaptor<List<StockCardDeletedBackup>> stockCardDeletedBackupsArgumentCaptor;

  @Mock
  private SupportedProgramsHelper programsHelper;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID userId = UUID.randomUUID();

  private final UUID tradeItem1 = UUID.randomUUID();
  private final UUID tradeItem2 = UUID.randomUUID();
  private final UUID tradeItem3 = UUID.randomUUID();

  private ZonedDateTime oldTime;
  private ZonedDateTime latestTime;

  private final UUID productId1 = UUID.randomUUID();
  private final UUID productId2 = UUID.randomUUID();
  private final UUID productId3 = UUID.randomUUID();

  private final String productCode1 = "product 1";
  private final String productCode2 = "product 2";
  private final String productCode3 = "product 3";

  private final UUID lotId1OrderableId1 = UUID.randomUUID();
  private final UUID lotId2OrderableId1 = UUID.randomUUID();
  private final UUID lotId1OrderableId2 = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    userDto.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(userDto);
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
    when(approvedProductService.getApprovedProducts(facilityId, programId1, emptyList()))
        .thenReturn(asList(mockApprovedProduct1(), mockApprovedProduct2()));
    when(approvedProductService.getApprovedProducts(facilityId, programId2, emptyList()))
        .thenReturn(singletonList(mockApprovedProduct3()));
    Map<UUID, List<SiglusStockMovementItemResponse>> stockMovementItemMap = new HashMap<>();
    stockMovementItemMap.put(productId1, new ArrayList<>());
    stockMovementItemMap.put(productId2, new ArrayList<>());
    when(stockCardLineItemService.getStockMovementByOrderableId(any(), any(), any(), any(), any(), any()))
        .thenReturn(stockMovementItemMap);

  }

  @Test
  public void shouldSaveStockCardDeletedBackupWhenCallDeleteMethod() {
    // given
    createSohValueByIsNolot(true);
    List<StockCardDeleteRequest> stockCardDeleteRequests = createStockCardDeleteRequests();

    // when
    service.deleteStockCardByProduct(stockCardDeleteRequests);

    // then
    verify(stockCardBackupRepository).save(stockCardDeletedBackupsArgumentCaptor.capture());
    verify(stockCardLineItemService).deleteStockCardByProduct(any(), any());
    List<StockCardDeletedBackup> stockCardDeletedBackups = stockCardDeletedBackupsArgumentCaptor.getValue();
    assertEquals(3, stockCardDeletedBackups.size());
  }

  private List<StockCardDeleteRequest> createStockCardDeleteRequests() {
    StockCardListCreateRequest stockCardListCreateRequest = new StockCardListCreateRequest();
    StockCardCreateRequest stockCardCreateRequest = new StockCardCreateRequest();
    stockCardCreateRequest.setProductCode(productCode1);
    stockCardCreateRequest.setStockOnHand(100);
    stockCardListCreateRequest.setStockCardCreateRequests(singletonList(stockCardCreateRequest));
    StockCardDeleteRequest request1 = StockCardDeleteRequest.builder()
        .productCode(productCode1)
        .clientMovements(stockCardListCreateRequest)
        .build();
    StockCardDeleteRequest request2 = StockCardDeleteRequest.builder()
        .productCode(productCode2)
        .clientMovements(stockCardListCreateRequest)
        .build();
    StockCardDeleteRequest request3 = StockCardDeleteRequest.builder()
        .productCode(productCode2)
        .clientMovements(stockCardListCreateRequest)
        .build();
    return Arrays.asList(request1, request2, request3);
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

  private LotDto mockLotDto(String lotCode, UUID lotId, UUID tradeItemId) {
    LotDto lotDto = new LotDto();
    lotDto.setLotCode(lotCode);
    lotDto.setId(lotId);
    lotDto.setTradeItemId(tradeItemId);
    return lotDto;
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
}
