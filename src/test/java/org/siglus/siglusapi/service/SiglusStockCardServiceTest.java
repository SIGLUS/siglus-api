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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.DispensableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.reason.ReasonCategory;
import org.openlmis.stockmanagement.domain.reason.ReasonType;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.service.StockCardService;
import org.openlmis.stockmanagement.util.AuthenticationHelper;
import org.siglus.siglusapi.domain.StockCardExtension;
import org.siglus.siglusapi.dto.LotLocationSohDto;
import org.siglus.siglusapi.dto.ProductMovementDto;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusStockManagementService;
import org.siglus.siglusapi.testutils.CalculatedStockOnHandDataBuilder;
import org.siglus.siglusapi.testutils.StockCardLineItemDataBuilder;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.BeanUtils;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardServiceTest {

  @Mock
  SiglusStockCardRepository stockCardRepository;

  @Mock
  SiglusStockManagementService stockCardStockManagementService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Mock
  private SiglusUnpackService unpackService;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @Mock
  private SiglusDateHelper dateHelper;

  @Mock
  private AndroidHelper androidHelper;

  @Mock
  private StockMovementService stockMovementService;

  @Mock
  private CalculatedStockOnHandByLocationRepository calculatedStockOnHandByLocationRepository;

  @Mock
  private StockCardService stockCardService;

  @InjectMocks
  private SiglusStockCardService siglusStockCardService;

  @Mock
  SiglusOrderableReferenceDataService siglusOrderableReferenceDataService;

  @Mock
  SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  @Mock
  SiglusProgramService siglusProgramService;

  private UUID orderableId;

  private UUID homefacilityId;

  private UUID facilityId;

  private UUID lotId;

  private UUID programId;

  private static final LocalDate CURRENT_DATE = LocalDate.now();

  @Before
  public void prepare() {
    facilityId = UUID.randomUUID();
    UserDto userDto = new UserDto();
    homefacilityId = UUID.randomUUID();
    programId = UUID.randomUUID();
    userDto.setHomeFacilityId(homefacilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    orderableId = UUID.randomUUID();

    when(unpackService.orderablesInKit()).thenReturn(new HashSet<>());
    when(archiveProductService.isArchived(any(UUID.class))).thenReturn(true);
    when(androidHelper.isAndroid()).thenReturn(false);
    CalculatedStockOnHand calculatedStockOnHand = new CalculatedStockOnHandDataBuilder().build();
    calculatedStockOnHand.setStockOnHand(10);
    when(calculatedStockOnHandRepository
        .findFirstByStockCardIdAndOccurredDateLessThanEqualOrderByOccurredDateDesc(any(UUID.class),
            any(LocalDate.class))).thenReturn(Optional.of(calculatedStockOnHand));
    Mockito.when(dateHelper.getCurrentDate()).thenReturn(CURRENT_DATE);
  }

  @Test
  public void shouldReturnMergedPhysicalInventoryItemDtoWhenViewByLotWithLocation() {
    UUID lineId1 = UUID.randomUUID();
    UUID lineId2 = UUID.randomUUID();
    ZonedDateTime physicalInventoryTime = ZonedDateTime.now();
    StockCardLineItem source1 = StockCardLineItem.builder()
            .processedDate(physicalInventoryTime)
            .quantity(200)
            .build();
    StockCardLineItem source2 = StockCardLineItem.builder()
            .processedDate(physicalInventoryTime)
            .quantity(500)
            .build();
    Map<UUID, StockCardLineItem> lineItemsSource = new HashMap<>();
    lineItemsSource.put(lineId1, source1);
    lineItemsSource.put(lineId2, source2);

    StockCardLineItemReasonDto credit = new StockCardLineItemReasonDto();
    credit.setCategory(ReasonCategory.PHYSICAL_INVENTORY.toString());
    credit.setReasonType(ReasonType.CREDIT);

    StockCardLineItemDto physcical1 = StockCardLineItemDto
            .builder()
            .stockOnHand(200)
            .reason(credit)
            .stockAdjustments(Collections.emptyList())
            .lineItem(source1)
            .build();
    physcical1.setId(lineId1);
    StockCardLineItemDto physcical2 = StockCardLineItemDto
            .builder()
            .stockOnHand(500)
            .reason(credit)
            .stockAdjustments(Collections.emptyList())
            .lineItem(source2)
            .build();
    physcical2.setId(lineId2);
    List<StockCardLineItemDto> merged = siglusStockCardService.mergePhysicalInventoryLineItems(
            Arrays.asList(physcical1, physcical2), lineItemsSource);

    assertEquals(1, merged.size());
    StockCardLineItemDto physicalMergedLine = merged.get(0);
    assertEquals(700, physicalMergedLine.getQuantity().intValue());
    assertEquals(700, physicalMergedLine.getStockOnHand().intValue());
  }

  @Test
  public void shouldReturnNUllIfStockNotExistWhenFindStockCardByOrderable() {
    when(stockCardRepository.findByFacilityIdAndOrderableId(homefacilityId, orderableId))
        .thenReturn(new ArrayList<>());
    StockCardDto stockCardDto = siglusStockCardService.findStockCardByOrderable(orderableId);
    assertNull(stockCardDto);
  }

  @Test
  public void shouldReturnNUllIfStockNotExistWhenFindStockCardByStockCard() {
    UUID stockCardId = UUID.randomUUID();
    when(stockCardRepository.findOne(stockCardId))
        .thenReturn(null);
    StockCardDto stockCardDto = siglusStockCardService.findStockCardById(stockCardId);
    assertNull(stockCardDto);
  }

  @Test
  public void shouldGetLocationInfoWhenFindByStockCardId() {
    // given
    StockCard stockCardOne = createStockCardOne();
    when(stockCardRepository.findOne(stockCardOne.getId())).thenReturn(stockCardOne);
    StockCardExtension stockCardExtensionOne = StockCardExtension.builder()
        .stockCardId(stockCardOne.getId())
        .createDate(stockCardOne.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardOne.getId()))
        .thenReturn(stockCardExtensionOne);
    when(stockCardStockManagementService.getStockCard(stockCardOne.getId()))
        .thenReturn(getFromStockCard(stockCardOne));
    when(stockCardService.findStockCardById(stockCardOne.getId(), true))
        .thenReturn(getFromStockCard(stockCardOne));
    LotLocationSohDto locationSohDto =
        LotLocationSohDto.builder().lotId(stockCardOne.getLotId()).locationCode("AA031").stockOnHand(1).build();
    when(calculatedStockOnHandByLocationRepository.getLocationSohByStockCard(stockCardOne.getId())).thenReturn(
        newArrayList(locationSohDto));
    // when
    StockCardDto stockCardDto = siglusStockCardService.findStockCardWithLocationById(stockCardOne.getId());
    assertEquals(false, stockCardDto.getExtraData().get("locationCode").isEmpty());
  }

  @Test
  public void shouldReturnNUllIfStockNotExistWhenFindStockCardWithLocationByStockCard() {
    UUID stockCardId = UUID.randomUUID();
    when(stockCardRepository.findOne(stockCardId))
        .thenReturn(null);
    assertNull(siglusStockCardService.findStockCardWithLocationById(stockCardId));
  }

  @Test
  public void shouldEqualStockCardLineItemWhenFindByStockCardId() {
    StockCard stockCard = createStockCardOne();
    when(stockCardRepository.findOne(stockCard.getId()))
        .thenReturn(stockCard);

    StockCardExtension stockCardExtension = StockCardExtension.builder()
        .createDate(stockCard.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(stockCardExtension);

    when(stockCardStockManagementService.getStockCard(stockCard.getId()))
        .thenReturn(getFromStockCard(stockCard));

    StockCardDto stockCardDto = siglusStockCardService.findStockCardById(stockCard.getId());
    assertEquals(2, stockCardDto.getLineItems().size());
  }

  @Test
  public void shouldEqualStockCardLineItemNumberAddFistInventoryLineItemIfOnlyOneStockCard() {
    StockCard stockCard = createStockCardOne();
    when(stockCardRepository.findByFacilityIdAndOrderableId(homefacilityId, orderableId))
        .thenReturn(Collections.singletonList(stockCard));

    StockCardExtension stockCardExtension = StockCardExtension.builder()
        .stockCardId(stockCard.getId())
        .createDate(stockCard.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(stockCardExtension);

    when(stockCardStockManagementService.getStockCard(stockCard.getId()))
        .thenReturn(getFromStockCard(stockCard));

    StockCardDto stockCardDto = siglusStockCardService.findStockCardByOrderable(orderableId);
    assertEquals(3, stockCardDto.getLineItems().size());
  }

  @Test
  public void shouldEqualStockCardLineItemNumberIfOnlyOneStockCardAndNoMatchExtension() {
    StockCard stockCard = createStockCardOne();
    when(stockCardRepository.findByFacilityIdAndOrderableId(homefacilityId, orderableId))
        .thenReturn(Collections.singletonList(stockCard));

    StockCardExtension stockCardExtension = StockCardExtension.builder()
        .createDate(stockCard.getLineItems().get(0).getOccurredDate())
        .build();

    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(stockCardExtension);

    when(stockCardStockManagementService.getStockCard(stockCard.getId()))
        .thenReturn(getFromStockCard(stockCard));

    StockCardDto stockCardDto = siglusStockCardService.findStockCardByOrderable(orderableId);
    assertEquals(2, stockCardDto.getLineItems().size());
  }

  @Test
  public void sohShouldEqualAllStockCardAggregateWhenFindByOrderableId() {
    StockCard stockCardOne = createStockCardOne();
    StockCard stockCardTwo = createStockCardTwo();
    when(stockCardRepository.findByFacilityIdAndOrderableId(homefacilityId, orderableId))
        .thenReturn(Arrays.asList(stockCardOne, stockCardTwo));

    StockCardExtension stockCardExtensionOne = StockCardExtension.builder()
        .stockCardId(stockCardOne.getId())
        .createDate(stockCardOne.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardOne.getId()))
        .thenReturn(stockCardExtensionOne);

    StockCardExtension stockCardExtensionTwo = StockCardExtension.builder()
        .stockCardId(stockCardTwo.getId())
        .createDate(stockCardTwo.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardTwo.getId()))
        .thenReturn(stockCardExtensionTwo);

    when(stockCardStockManagementService.getStockCard(stockCardOne.getId()))
        .thenReturn(getFromStockCard(stockCardOne));
    when(stockCardStockManagementService.getStockCard(stockCardTwo.getId()))
        .thenReturn(getFromStockCard(stockCardTwo));

    StockCardDto stockCardDto = siglusStockCardService.findStockCardByOrderable(orderableId);
    assertEquals(6, stockCardDto.getLineItems().size());
    assertEquals(Integer.valueOf(82), stockCardDto.getLineItems().get(5).getStockOnHand());
  }

  @Test
  public void shouldDeleteReasonWhenStockContainSource() {
    // given
    StockCard stockCardOne = createStockCardOne();
    Node sourceNode = new Node();
    stockCardOne.getLineItems().get(1).setSource(sourceNode);
    when(stockCardRepository.findOne(stockCardOne.getId())).thenReturn(stockCardOne);
    StockCardExtension stockCardExtensionOne = StockCardExtension.builder()
        .stockCardId(stockCardOne.getId())
        .createDate(stockCardOne.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardOne.getId()))
        .thenReturn(stockCardExtensionOne);
    when(stockCardStockManagementService.getStockCard(stockCardOne.getId()))
        .thenReturn(getFromStockCard(stockCardOne));

    // when
    StockCardDto stockCardDto = siglusStockCardService.findStockCardById(stockCardOne.getId());

    // then
    assertNull(stockCardDto.getLineItems().get(1).getReason());
  }

  @Test
  public void shouldNoDeleteReasonWhenStockLineItemNotContainSource() {
    // given
    StockCard stockCardOne = createStockCardOne();
    when(stockCardRepository.findOne(stockCardOne.getId())).thenReturn(stockCardOne);
    StockCardExtension stockCardExtensionOne = StockCardExtension.builder()
        .stockCardId(stockCardOne.getId())
        .createDate(stockCardOne.getLineItems().get(0).getOccurredDate())
        .build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardOne.getId()))
        .thenReturn(stockCardExtensionOne);
    when(stockCardStockManagementService.getStockCard(stockCardOne.getId()))
        .thenReturn(getFromStockCard(stockCardOne));

    // when
    StockCardDto stockCardDto = siglusStockCardService.findStockCardById(stockCardOne.getId());

    // then
    StockCardLineItemDto lineItemDto = stockCardDto.getLineItems().get(1);
    assertEquals("CREDIT", lineItemDto.getReason().getType());
  }

  @Test
  public void shouldReturnStockMovementWhenFindByFacilityId() {
    StockMovementResDto stockMovementResDto1 = new StockMovementResDto();
    StockMovementResDto stockMovementResDto2 = new StockMovementResDto();
    StockMovementResDto stockMovementResDto3 = new StockMovementResDto();
    HashSet<UUID> orderables = new HashSet<>();
    orderables.add(orderableId);
    when(stockMovementService.getProductMovements(orderables, facilityId, null, null))
        .thenReturn(Arrays.asList(stockMovementResDto1, stockMovementResDto2, stockMovementResDto3));
    List<StockMovementResDto> productMovements =
        siglusStockCardService.getProductMovements(facilityId, orderableId, null, null);
    assertEquals(3, productMovements.size());
  }

  @Test
  public void shouldReturnNUllIfMovementNotExistWhenFindStockMovementbyfacilityId() {
    when(stockMovementService.getProductMovements(new HashSet<>(), facilityId, null, null))
        .thenReturn(null);
    assertNull(siglusStockCardService.getProductMovements(facilityId, null, null, null));
  }

  @Test
  public void shouldReturnStockMovementByProductWhenCallGetMovementByProductByServiceGivenFacilityIdAndOrderableId() {
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programId);
    Set<ProgramOrderableDto> programOrderableDtos = new HashSet<>();
    programOrderableDtos.add(programOrderableDto);
    DispensableDto dispensableDto = new DispensableDto();
    dispensableDto.setToString("111");
    org.openlmis.referencedata.dto.OrderableDto orderableDto = new org.openlmis.referencedata.dto.OrderableDto("00a11",
        dispensableDto, "product1", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    orderableDto.setPrograms(programOrderableDtos);

    StockMovementResDto stockMovementResDto = new StockMovementResDto();
    List<StockMovementResDto> stockMovementResDtoList = Collections.singletonList(stockMovementResDto);
    when(stockMovementService.getMovementsByProduct(facilityId, orderableId)).thenReturn(stockMovementResDtoList);
    org.siglus.siglusapi.dto.FacilityDto facilityDto = org.siglus.siglusapi.dto.FacilityDto.builder().name("facility")
        .build();
    when(siglusFacilityReferenceDataService.findOne(facilityId)).thenReturn(facilityDto);
    when(siglusOrderableReferenceDataService.findOne(orderableId)).thenReturn(orderableDto);
    when(siglusProgramService.getProgram(programId)).thenReturn(new ProgramDto());
    ProductMovementDto movementByProduct = siglusStockCardService.getMovementByProduct(facilityId, orderableId);
    assertEquals(1, movementByProduct.getLineItems().size());
  }

  @Test
  public void shouldGetEmptyListWhenSearchStockCardsGivenFacilityIsNull() {
    List<UUID> result = siglusStockCardService.findStockCardIdByFacilityAndOrderables(null,
        Collections.singletonList(UUID.randomUUID()));

    assertEquals(0, result.size());
  }

  @Test
  public void shouldGetEmptyListWhenSearchStockCardsGivenOrderableIdsIsEmpty() {
    List<UUID> result = siglusStockCardService.findStockCardIdByFacilityAndOrderables(UUID.randomUUID(),
        newArrayList());

    assertEquals(0, result.size());
  }

  @Test
  public void shouldGetStockCardIdsSuccessWhenGivenFacilityAndOrderableIds() {
    UUID facilityId = UUID.randomUUID();
    List<UUID> orderableIds = Collections.singletonList(UUID.randomUUID());
    when(stockCardRepository.findStockCardIdByFacilityAndOrderables(facilityId, orderableIds))
        .thenReturn(newArrayList(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

    List<UUID> result = siglusStockCardService.findStockCardIdByFacilityAndOrderables(facilityId, orderableIds);

    assertEquals(2, result.size());
  }

  private StockCard createStockCardOne() {
    StockCardLineItem lineItem1 = new StockCardLineItemDataBuilder()
        .withOccurredDateNextDay()
        .withProcessedDateNextDay()
        .withCreditReason()
        .withQuantity(10)
        .build();

    StockCardLineItem lineItem2 = new StockCardLineItemDataBuilder()
        .withOccurredDateNextDay()
        .withProcessedDateHourEarlier()
        .withQuantity(2)
        .withCreditReason()
        .build();
    StockCard stockCard = new StockCard();
    stockCard.setFacilityId(facilityId);
    stockCard.setLotId(lotId);
    stockCard.setLineItems(Arrays.asList(lineItem1, lineItem2));
    stockCard.setId(UUID.randomUUID());
    lineItem1.setStockCard(stockCard);
    lineItem2.setStockCard(stockCard);
    return stockCard;
  }

  private StockCard createStockCardTwo() {
    StockCardLineItem lineItem1 = new StockCardLineItemDataBuilder()
        .withProcessedDateNextDay()
        .withQuantity(30)
        .withCreditReason()
        .build();

    StockCardLineItem lineItem2 = new StockCardLineItemDataBuilder()
        .withProcessedDateNextDay()
        .withQuantity(40)
        .withCreditReason()
        .build();
    StockCard stockCard = new StockCard();
    stockCard.setLineItems(Arrays.asList(lineItem1, lineItem2));
    stockCard.setId(UUID.randomUUID());
    lineItem1.setStockCard(stockCard);
    lineItem2.setStockCard(stockCard);
    return stockCard;
  }

  private StockCardDto getFromStockCard(StockCard stockCard) {
    StockCardDto dto = new StockCardDto();
    BeanUtils.copyProperties(stockCard, dto);

    OrderableDto orderable = new OrderableDto();
    orderable.setId(UUID.randomUUID());
    dto.setOrderable(orderable);
    dto.setLineItems(stockCard.getLineItems().stream().map(stockCardLineItem -> {
      StockCardLineItemDto lineItemDto = new StockCardLineItemDto();
      BeanUtils.copyProperties(stockCardLineItem, lineItemDto);
      if (stockCardLineItem.getReason() != null) {
        lineItemDto.setReason(StockCardLineItemReasonDto
            .newInstance(stockCardLineItem.getReason()));
      }
      if (stockCardLineItem.getSource() != null) {
        lineItemDto.setSource(new FacilityDto());
      }
      lineItemDto.setLineItem(stockCardLineItem);
      return lineItemDto;
    }).collect(Collectors.toList()));
    return dto;
  }
}
