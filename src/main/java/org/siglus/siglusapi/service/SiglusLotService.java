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

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_TRADE_ITEM_IS_EMPTY;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.repository.StockCardLineItemReasonRepository;
import org.organicdesign.fp.tuple.Tuple2;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotSearchParams;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.RemovedLotDto;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.enumeration.AdjustmentReason;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.SiglusLotRepository;
import org.siglus.siglusapi.repository.dto.LotStockDto;
import org.siglus.siglusapi.repository.dto.StockCardStockDto;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
@Slf4j
public class SiglusLotService {

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;
  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  private SiglusDateHelper dateHelper;
  @Autowired
  private SiglusLotReferenceDataService lotReferenceDataService;
  @Autowired
  private LotConflictService lotConflictService;
  @Autowired
  private LotRepository lotRepository;
  @Autowired
  private SiglusLotRepository siglusLotRepository;
  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;
  @Autowired
  private SiglusStockEventsService siglusStockEventsService;
  @Autowired
  private StockCardLineItemReasonRepository stockCardLineItemReasonRepository;

  @Autowired
  private FacilityConfigHelper facilityConfigHelper;

  /**
   * reason for create a new transaction: Running this method in the super transaction will cause
   * 'stockmanagement.error.event.lot.not.exist' execption. detail steps： 1. method createStockEventForOneProgram do
   * something 2. method createAndFillLotId insert a new lot, with new uuid(This method) 3. method
   * createStockEventForOneProgram call siglusCreateStockEvent and in stockEventProcessor.process build context. When
   * building context, it start a http request /api/lots/ to getLotsByIds(which beyond the super transaction scope, so
   * the http must see the change in step 2)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createAndFillLotId(StockEventDto eventDto) {
    final List<StockEventLineItemDto> lineItems = eventDto.getLineItems();
    Set<UUID> orderableIds = lineItems.stream().map(StockEventLineItemDto::getOrderableId)
        .collect(Collectors.toSet());
    Map<UUID, OrderableDto> orderableDtos = orderableReferenceDataService.findByIds(orderableIds)
        .stream()
        .collect(Collectors.toMap(OrderableDto::getId, orderableDto -> orderableDto));
    for (StockEventLineItemDto eventLineItem : lineItems) {
      UUID orderableId = eventLineItem.getOrderableId();
      OrderableDto orderable = orderableDtos.get(orderableId);
      if (orderable.getIsKit()) {
        validateLotMustBeNull(eventLineItem);
        continue;
      }
      UUID facilityId = getFacilityId(eventDto);
      fillLotIdIfNull(facilityId, orderable, eventLineItem);
    }
  }

  public List<LotDto> getLotList(List<UUID> lotIds) {
    List<UUID> nonNullLotIds = lotIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    Iterable<org.openlmis.referencedata.domain.Lot> lots = lotRepository.findAll(nonNullLotIds);
    return StreamSupport.stream(lots.spliterator(), false)
        .map(lot -> {
          LotDto lotDto = LotDto.builder()
              .lotCode(lot.getLotCode())
              .expirationDate(lot.getExpirationDate())
              .tradeItemId(lot.getTradeItem().getId())
              .manufactureDate(lot.getManufactureDate())
              .active(lot.isActive())
              .build();
          lotDto.setId(lot.getId());
          return lotDto;
        })
        .collect(Collectors.toList());
  }

  public LotDto createNewLotOrReturnExisted(UUID facilityId, String tradeItemId, String lotCode,
      LocalDate expirationDate) {
    if (null == tradeItemId) {
      throw new ValidationMessageException(new Message(ERROR_TRADE_ITEM_IS_EMPTY));
    }
    if (lotCode != null) {
      lotCode = lotCode.toUpperCase();
    }
    LotDto existedLot = findExistedLot(lotCode, tradeItemId);
    if (existedLot == null) {
      LotDto lotDto = new LotDto();
      lotDto.setId(Lot.of(lotCode, expirationDate).getUUid(UUID.fromString(tradeItemId)));
      lotDto.setTradeItemId(UUID.fromString(tradeItemId));
      lotDto.setManufactureDate(dateHelper.getCurrentDate());
      lotDto.setExpirationDate(expirationDate);
      lotDto.setActive(true);
      lotDto.setLotCode(lotCode);
      return lotReferenceDataService.saveLot(lotDto);
    }
    lotConflictService.handleLotConflict(facilityId, lotCode, existedLot.getId(), expirationDate,
        existedLot.getExpirationDate());
    return existedLot;
  }

  public List<LotStockDto> getExpiredLots(UUID facilityId) {
    if (facilityConfigHelper.isLocationManagementEnabled(facilityId)) {
      return siglusLotRepository.queryExpiredLotsWithLocation(facilityId);
    }
    return siglusLotRepository.queryExpiredLots(facilityId);
  }

  public void removeExpiredLots(List<RemovedLotDto> lots, boolean hasLocation) {
    // check whether the lots expired
    List<UUID> lotIds = lots.stream().map(RemovedLotDto::getLotId).collect(Collectors.toList());
    if (siglusLotRepository.existsNotExpiredLotsByIds(lotIds)) {
      throw new BusinessDataException(Message.createFromMessageKeyStr("exists not expired lots"));
    }

    // check quantity is smaller or equal than soh
    List<StockCardStockDto> stockCardStockDtos = siglusStockCardSummariesService.getLatestStockOnHandByIds(
        lots.stream().map(RemovedLotDto::getStockCardId).collect(Collectors.toList()), hasLocation);
    if (hasLocation) {
      Map<String, Integer> stockCardIdLocationCodeToSohMap = stockCardStockDtos.stream()
          .collect(Collectors.toMap(e -> e.getStockCardId().toString() + e.getLocationCode(),
              StockCardStockDto::getStockOnHand));
      if (lots.stream().anyMatch(lot -> lot.getQuantity() > stockCardIdLocationCodeToSohMap.getOrDefault(
          lot.getStockCardId().toString() + lot.getLocationCode(), 0))) {
        throw new BusinessDataException(Message.createFromMessageKeyStr("not have enough soh"));
      }
    } else {
      Map<UUID, Integer> stockMap = stockCardStockDtos.stream()
          .collect(Collectors.toMap(StockCardStockDto::getStockCardId, StockCardStockDto::getStockOnHand));
      if (lots.stream().anyMatch(lot -> lot.getQuantity() > stockMap.getOrDefault(lot.getStockCardId(), 0))) {
        throw new BusinessDataException(Message.createFromMessageKeyStr("not have enough soh"));
      }
    }

    // send stock event to remove expired lots
    buildDiscardStockEventDtos(lots).forEach(
        stockEventDto -> siglusStockEventsService.processStockEvent(stockEventDto, hasLocation)
    );
  }

  private UUID getFacilityId(StockEventDto eventDto) {
    if (eventDto.getFacilityId() != null) {
      return eventDto.getFacilityId();
    }
    return authenticationHelper.getCurrentUser().getHomeFacilityId();
  }

  private void validateLotMustBeNull(StockEventLineItemDto stockEventLineItem) {
    if (StringUtils.isNotBlank(stockEventLineItem.getLotCode()) || stockEventLineItem.getLotId() != null) {
      throw new ValidationMessageException(new Message(ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY));
    }
  }

  private void fillLotIdIfNull(UUID facilityId, OrderableDto orderable,
      StockEventLineItemDto eventLineItem) {
    if (eventLineItem.getLotId() != null) {
      // already done or nothing we can do since lot info is missing
      return;
    }
    if (isBlank(eventLineItem.getLotCode())) {
      throw new ValidationMessageException("siglusapi.error.stockManagement.lotcode.cannot.null.or.cannot.empty");
    }
    UUID lotId = createNewLotOrReturnExisted(facilityId, orderable.getTradeItemIdentifier(), eventLineItem.getLotCode(),
        eventLineItem.getExpirationDate()).getId();
    eventLineItem.setLotId(lotId);
  }

  private LotDto findExistedLot(String lotCode, String tradeItemId) {
    LotSearchParams lotSearchParams = new LotSearchParams();
    lotSearchParams.setLotCode(lotCode);
    lotSearchParams.setTradeItemId(singletonList(UUID.fromString(tradeItemId)));
    List<LotDto> existedLots = lotReferenceDataService.getLots(lotSearchParams);
    return existedLots.stream().filter(lotDto -> lotDto.getLotCode().equals(lotCode)).findFirst().orElse(null);
  }

  private List<StockEventDto> buildDiscardStockEventDtos(List<RemovedLotDto> lots) {
    StockCardLineItemReason reason = stockCardLineItemReasonRepository.findByName(
        AdjustmentReason.EXPIRED_RETURN_TO_SUPPLIER_AND_DISCARD.getName());
    if (ObjectUtils.isEmpty(reason)) {
      throw new BusinessDataException(
          Message.createFromMessageKeyStr("Missing discard expired lots reason"));
    }
    Multimap<Tuple2<UUID, UUID>, RemovedLotDto> facilityWithProgramMap = ArrayListMultimap.create();
    lots.forEach(lot -> {
      Tuple2<UUID, UUID> key = Tuple2.of(lot.getFacilityId(), lot.getProgramId());
      facilityWithProgramMap.put(key, lot);
    });
    List<StockEventDto> eventDtos = new ArrayList<>();
    facilityWithProgramMap.asMap().forEach((key, values) -> {
      RemovedLotDto dto = values.stream().findFirst().get();
      eventDtos.add(StockEventDto.builder()
          .facilityId(key._1())
          .programId(key._2())
          .lineItems(values.stream()
              .map(item -> item.toStockEventLineItemDto(reason.getId())).collect(Collectors.toList()))
          .userId(authenticationHelper.getCurrentUser().getId())
          .type(MovementType.ADJUSTMENT.toString())
          .signature(dto.getSignature())
          .documentNumber(dto.getDocumentNumber())
          .build());
    });

    return eventDtos;
  }
}
