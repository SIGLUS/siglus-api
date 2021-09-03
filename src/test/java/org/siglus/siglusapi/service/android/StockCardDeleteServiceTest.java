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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.openlmis.stockmanagement.domain.reason.ReasonCategory.ADJUSTMENT;
import static org.openlmis.stockmanagement.domain.reason.ReasonType.CREDIT;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;

import com.google.common.collect.ImmutableMap;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItem;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventoryLineItemAdjustment;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.referencedata.VersionObjectReferenceDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.StockCardDeletedBackup;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemAdjustmentRepository;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardDeletedBackupRepository;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.testutils.CanFulfillForMeEntryDtoDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class StockCardDeleteServiceTest {

  @InjectMocks
  private StockCardDeleteService service;

  @Mock
  private StockCardDeletedBackupRepository stockCardDeletedBackupRepository;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Mock
  private SiglusStockCardSummariesService stockCardSummariesService;

  @Mock
  private PhysicalInventoryLineItemRepository physicalInventoryLineItemRepository;

  @Mock
  private PhysicalInventoryLineItemAdjustmentRepository physicalInventoryLineItemAdjustmentRepository;

  @Mock
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Mock
  private SiglusStockCardLineItemRepository stockCardLineItemRepository;

  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @Mock
  private StockCardSearchService stockCardSearchService;

  @Mock
  private StockCardCreateService stockCardCreateService;

  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;

  @Captor
  private ArgumentCaptor<List<StockCardDeletedBackup>> stockCardDeletedBackupsArgumentCaptor;

  private ZonedDateTime oldTime;
  private ZonedDateTime latestTime;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID userId = UUID.randomUUID();

  private final UUID tradeItem1 = UUID.randomUUID();
  private final UUID tradeItem2 = UUID.randomUUID();
  private final UUID tradeItem3 = UUID.randomUUID();

  private final UUID productId1 = UUID.randomUUID();
  private final UUID productId2 = UUID.randomUUID();

  private final String productCode1 = "product 1";
  private final String productCode2 = "product 2";

  private final UUID lotId1OrderableId1 = UUID.randomUUID();
  private final UUID lotId2OrderableId1 = UUID.randomUUID();
  private final UUID lotId1OrderableId2 = UUID.randomUUID();

  private final UUID physicalAdjustmentId = UUID.randomUUID();
  private final UUID physicalAdjustmentId2 = UUID.randomUUID();
  private final UUID physicalLineItemId = UUID.randomUUID();
  private final UUID physicalInventoryId = UUID.randomUUID();

  private final UUID stockCardLineItemId = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    userDto.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(userDto);
    OrderableDto orderableDto1 = new OrderableDto();
    orderableDto1.setId(productId1);
    orderableDto1.setProductCode(productCode1);
    OrderableDto orderableDto2 = new OrderableDto();
    orderableDto2.setId(productId2);
    orderableDto2.setProductCode(productCode2);
    when(stockCardCreateService.getAllApprovedProducts()).thenReturn(Arrays.asList(orderableDto1, orderableDto2));
    stockCardCreateService.getAllApprovedProducts();
    FacilityProductMovementsResponse facilityResponse = new FacilityProductMovementsResponse();
    ProductMovementResponse productResponse1 = ProductMovementResponse.builder().productCode(productCode1).build();
    ProductMovementResponse productResponse2 = ProductMovementResponse.builder().productCode(productCode2).build();
    facilityResponse.setProductMovements(Arrays.<ProductMovementResponse>asList(productResponse1, productResponse2));
    when(stockCardSearchService.getProductMovementsByOrderables(any())).thenReturn(facilityResponse);

  }

  @Test
  public void shouldSaveStockCardDeletedBackupWhenCallDeleteMethod() {
    // given
    createSohValueByIsNolot(true);
    List<StockCardDeleteRequest> stockCardDeleteRequests = createStockCardDeleteRequests();
    List<PhysicalInventory> physicalInventories = createPhysicalInventorys();
    List<StockCardLineItem> stockCardLineItems = createStockCardLineItems();
    when(physicalInventoriesRepository.findByFacilityIdAndOrderableIds(any(), any()))
        .thenReturn(physicalInventories);
    when(stockCardLineItemRepository.findByFacilityIdAndOrderableIdIn(any(), any()))
        .thenReturn(stockCardLineItems);

    // when
    service.deleteStockCardByProduct(stockCardDeleteRequests);

    // then
    verify(stockCardDeletedBackupRepository).save(stockCardDeletedBackupsArgumentCaptor.capture());
    verify(stockCardSearchService).getProductMovementsByOrderables(any());
    List<StockCardDeletedBackup> stockCardDeletedBackups = stockCardDeletedBackupsArgumentCaptor.getValue();
    assertEquals(3, stockCardDeletedBackups.size());
    verify(physicalInventoryLineItemAdjustmentRepository, times(2))
        .delete(physicalInventories.get(0).getLineItems().get(0).getStockAdjustments());
    verify(physicalInventoryLineItemRepository).delete(physicalInventories.get(0).getLineItems());
    verify(physicalInventoriesRepository).delete(physicalInventories);
    verify(calculatedStockOnHandRepository).deleteByFacilityIdAndOrderableIds(any(), any());
    verify(stockCardLineItemRepository).delete(stockCardLineItems);
    verify(siglusStockCardRepository).deleteStockCardsByFacilityIdAndOrderableIdIn(any(), any());
  }

  private List<StockCardDeleteRequest> createStockCardDeleteRequests() {
    StockCardCreateRequest stockCardCreateRequest = new StockCardCreateRequest();
    stockCardCreateRequest.setProductCode(productCode1);
    stockCardCreateRequest.setStockOnHand(100);
    List<StockCardCreateRequest> stockCardCreateRequests = singletonList(stockCardCreateRequest);
    StockCardDeleteRequest request1 = StockCardDeleteRequest.builder()
        .productCode(productCode1)
        .clientMovements(stockCardCreateRequests)
        .build();
    StockCardDeleteRequest request2 = StockCardDeleteRequest.builder()
        .productCode(productCode2)
        .clientMovements(stockCardCreateRequests)
        .build();
    StockCardDeleteRequest request3 = StockCardDeleteRequest.builder()
        .productCode(productCode2)
        .clientMovements(stockCardCreateRequests)
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

  private List<PhysicalInventory> createPhysicalInventorys() {
    PhysicalInventoryLineItemAdjustment physicalAdjustment = new PhysicalInventoryLineItemAdjustment();
    physicalAdjustment.setId(physicalAdjustmentId);
    physicalAdjustment.setReason(createStockCardLineItemReason());
    physicalAdjustment.setQuantity(20);

    PhysicalInventoryLineItem physicalInventoryLineItem = new PhysicalInventoryLineItem();
    physicalInventoryLineItem.setId(physicalLineItemId);
    physicalInventoryLineItem.setStockAdjustments(Collections.singletonList(physicalAdjustment));

    PhysicalInventory physicalInventory = new PhysicalInventory();
    physicalInventory.setId(physicalInventoryId);
    physicalInventory.setLineItems(Collections.singletonList(physicalInventoryLineItem));
    return Collections.singletonList(physicalInventory);
  }

  private List<StockCardLineItem> createStockCardLineItems() {
    PhysicalInventoryLineItemAdjustment physicalAdjustment = new PhysicalInventoryLineItemAdjustment();
    physicalAdjustment.setId(physicalAdjustmentId2);
    physicalAdjustment.setReason(createStockCardLineItemReason());
    physicalAdjustment.setQuantity(20);

    StockCardLineItem stockCardLineItem = new StockCardLineItem();
    stockCardLineItem.setId(stockCardLineItemId);
    stockCardLineItem.setStockAdjustments(Collections.singletonList(physicalAdjustment));
    return Collections.singletonList(stockCardLineItem);
  }

  private StockCardLineItemReason createStockCardLineItemReason() {
    return StockCardLineItemReason.builder().reasonCategory(ADJUSTMENT).reasonType(CREDIT).name("heheh").build();
  }


  private String genFullName(String productCode) {
    return "full name of " + productCode;
  }
}
