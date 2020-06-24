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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemDto;
import org.openlmis.stockmanagement.dto.StockCardLineItemReasonDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.testutils.CalculatedStockOnHandDataBuilder;
import org.openlmis.stockmanagement.testutils.StockCardLineItemDataBuilder;
import org.openlmis.stockmanagement.util.StockmanagementAuthenticationHelper;
import org.siglus.common.domain.StockCardExtension;
import org.siglus.common.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.service.client.SiglusStockManagementService;
import org.springframework.beans.BeanUtils;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardServiceTest {

  @Mock
  SiglusStockCardRepository stockCardRepository;

  @Mock
  SiglusStockManagementService stockCardStockManagementService;

  @Mock
  private StockmanagementAuthenticationHelper authenticationHelper;

  @Mock
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Mock
  private SiglusUnpackService unpackService;

  @Mock
  private SiglusArchiveProductService archiveProductService;

  @Mock
  private CalculatedStockOnHandRepository calculatedStockOnHandRepository;

  @InjectMocks
  private SiglusStockCardService siglusStockCardService;

  private UUID orderableId;

  private UUID homefacilityId;

  @Before
  public void prepare() {

    UserDto userDto = new UserDto();
    homefacilityId = UUID.randomUUID();
    userDto.setHomeFacilityId(homefacilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
    orderableId = UUID.randomUUID();

    when(unpackService.orderablesInKit()).thenReturn(new HashSet<>());
    when(archiveProductService.isArchived(any(UUID.class))).thenReturn(true);
    CalculatedStockOnHand calculatedStockOnHand = new CalculatedStockOnHandDataBuilder().build();
    calculatedStockOnHand.setStockOnHand(10);
    when(calculatedStockOnHandRepository
        .findFirstByStockCardIdAndOccurredDateLessThanEqualOrderByOccurredDateDesc(any(UUID.class),
        any(LocalDate.class))).thenReturn(Optional.ofNullable(calculatedStockOnHand));
  }

  @Test
  public void shouldReturnNUllIfStockNotExistWhenFindStockCardByOrderable() {
    when(stockCardRepository.findByFacilityIdAndOrderableId(homefacilityId, orderableId))
        .thenReturn(new ArrayList<>());
    StockCardDto stockCardDto = siglusStockCardService.findStockCardByOrderable(orderableId);
    assertEquals(null, stockCardDto);
  }

  @Test
  public void shouldReturnNUllIfStockNotExistWhenFindStockCardByStockCard() {
    UUID stockCardId = UUID.randomUUID();
    when(stockCardRepository.findOne(stockCardId))
        .thenReturn(null);
    StockCardDto stockCardDto = siglusStockCardService.findStockCardById(stockCardId);
    assertEquals(null, stockCardDto);
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
        .thenReturn(Arrays.asList(stockCard));

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
        .thenReturn(Arrays.asList(stockCard));

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
    StockCardLineItemDto lineItemDto = stockCardDto.getLineItems().get(1);
    assertEquals(null, lineItemDto.getReason());
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
      return lineItemDto;
    }).collect(Collectors.toList()));
    return dto;
  }

}
