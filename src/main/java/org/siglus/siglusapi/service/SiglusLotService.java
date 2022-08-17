package org.siglus.siglusapi.service;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOT_ID_AND_CODE_SHOULD_EMPTY;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_TRADE_ITEM_IS_EMPTY;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotSearchParams;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusLotService {

  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final SiglusDateHelper dateHelper;
  private final SiglusLotReferenceDataService lotReferenceDataService;
  private final LotConflictService lotConflictService;

  /**
   * reason for create a new transaction:
   * Running this method in the super transaction will cause 'stockmanagement.error.event.lot.not.exist' execption.
   * detail steps：
   * 1. method createStockEventForOneProgram do something
   * 2. method createAndFillLotId insert a new lot, with new uuid(This method)
   * 3. method createStockEventForOneProgram call siglusCreateStockEvent and in stockEventProcessor.process build
   * context. When building context, it start a http request /api/lots/ to getLotsByIds(which beyond the super
   * transaction scope, so the http must see the change in step 2)
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
    if (eventLineItem.getLotId() != null || isBlank(eventLineItem.getLotCode())) {
      // already done or nothing we can do since lot info is missing
      return;
    }
    UUID lotId = createNewLotOrReturnExisted(facilityId, orderable, eventLineItem.getLotCode(),
        eventLineItem.getExpirationDate()).getId();
    eventLineItem.setLotId(lotId);
  }


  private LotDto createNewLotOrReturnExisted(UUID facilityId, OrderableDto orderable, String lotCode,
      LocalDate expirationDate) {
    String tradeItemId = orderable.getTradeItemIdentifier();
    if (null == tradeItemId) {
      throw new ValidationMessageException(new Message(ERROR_TRADE_ITEM_IS_EMPTY));
    }
    LotDto existedLot = findExistedLot(lotCode, tradeItemId);
    if (existedLot == null) {
      LotDto lotDto = new LotDto();
      lotDto.setId(Lot.of(lotCode, expirationDate).getUUid());
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

  private LotDto findExistedLot(String lotCode, String tradeItemId) {
    LotSearchParams lotSearchParams = new LotSearchParams();
    lotSearchParams.setLotCode(lotCode);
    lotSearchParams.setTradeItemId(singletonList(UUID.fromString(tradeItemId)));
    List<LotDto> existedLots = lotReferenceDataService.getLots(lotSearchParams);
    return existedLots.stream().filter(lotDto -> lotDto.getLotCode().equals(lotCode)).findFirst().orElse(null);
  }


}
