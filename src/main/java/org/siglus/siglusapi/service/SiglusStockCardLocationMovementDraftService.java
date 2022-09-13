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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_MOVEMENT_DRAFT_EXISTS;

import com.google.common.collect.Maps;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.constant.LocationConstants;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraft;
import org.siglus.siglusapi.domain.StockCardLocationMovementDraftLineItem;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.StockCardLocationMovementDraftDto;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockCardLocationMovementDraftRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.OperatePermissionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.ActiveDraftValidator;
import org.siglus.siglusapi.validator.StockCardLocationMovementDraftValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusStockCardLocationMovementDraftService {

  private final StockCardLocationMovementDraftRepository stockCardLocationMovementDraftRepository;
  private final StockCardLocationMovementDraftValidator stockCardLocationMovementDraftValidator;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final ActiveDraftValidator draftValidator;
  private final OperatePermissionService operatePermissionService;
  private final StockCardRepository stockCardRepository;
  private final StockCardLocationMovementLineItemRepository stockCardLocationMovementLineItemRepository;
  private final SiglusOrderableService siglusOrderableService;
  private final SiglusLotReferenceDataService siglusLotReferenceDataService;

  @Transactional
  public StockCardLocationMovementDraftDto createEmptyMovementDraft(
      StockCardLocationMovementDraftDto stockCardLocationMovementDraftDto) {
    stockCardLocationMovementDraftValidator.validateEmptyMovementDraft(stockCardLocationMovementDraftDto);
    checkIfMovementDraftExists(stockCardLocationMovementDraftDto);
    log.info("create stock movement draft");
    StockCardLocationMovementDraft emptyStockCardLocationMovementDraft = StockCardLocationMovementDraft
        .createEmptyStockMovementDraft(stockCardLocationMovementDraftDto);
    StockCardLocationMovementDraft savedDraft = stockCardLocationMovementDraftRepository
        .save(emptyStockCardLocationMovementDraft);
    return StockCardLocationMovementDraftDto.from(savedDraft);
  }

  private void checkIfMovementDraftExists(StockCardLocationMovementDraftDto stockManagementDraftDto) {
    List<StockCardLocationMovementDraft> drafts = stockCardLocationMovementDraftRepository
        .findByProgramIdAndFacilityId(stockManagementDraftDto.getProgramId(), stockManagementDraftDto.getFacilityId());
    if (CollectionUtils.isNotEmpty(drafts)) {
      throw new ValidationMessageException(
          new Message(ERROR_MOVEMENT_DRAFT_EXISTS, stockManagementDraftDto.getProgramId(),
              stockManagementDraftDto.getFacilityId()));
    }
  }

  public List<StockCardLocationMovementDraftDto> searchMovementDrafts(UUID programId) {
    UUID facilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    operatePermissionService.checkPermission(facilityId);
    draftValidator.validateProgramId(programId);
    draftValidator.validateFacilityId(facilityId);

    List<StockCardLocationMovementDraft> stockCardLocationMovementDrafts = stockCardLocationMovementDraftRepository
        .findByProgramIdAndFacilityId(programId, facilityId);

    return StockCardLocationMovementDraftDto.from(stockCardLocationMovementDrafts);
  }

  public StockCardLocationMovementDraftDto updateVirtualLocationMovementDraft(
      StockCardLocationMovementDraftDto movementDraftDto, UUID movementDraftId) {
    stockCardLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
    UUID facilityId = movementDraftDto.getFacilityId();
    operatePermissionService.checkPermission(facilityId);
    draftValidator.validateFacilityId(facilityId);

    List<UUID> stockCardIds = stockCardRepository.findByFacilityIdIn(facilityId)
        .stream().map(StockCard::getId).collect(Collectors.toList());
    List<StockCardLocationMovementLineItem> previousStockCardLocationMovementLineItemList =
        stockCardLocationMovementLineItemRepository.findPreviousRecordByStockCardId(stockCardIds);

    List<StockCardLocationMovementLineItem> virtualLocationMovementLineItem =
        previousStockCardLocationMovementLineItemList.stream()
            .filter(e -> Objects.equals(LocationConstants.VIRTUAL_LOCATION_CODE, e.getSrcLocationCode())
                && Objects.equals(LocationConstants.VIRTUAL_LOCATION_CODE, e.getDestLocationCode())
                && e.getQuantity() != 0)
            .collect(Collectors.toList());

    List<StockCardLocationMovementDraftLineItem> stockCardLocationMovementDraftLineItemList =
        getStockCardLocationMovementDraftLineItem(facilityId, virtualLocationMovementLineItem);

    StockCardLocationMovementDraft stockMovementDraft = StockCardLocationMovementDraft
        .createMovementDraft(movementDraftDto);

    stockCardLocationMovementDraftLineItemList.forEach(e -> e.setStockCardLocationMovementDraft(stockMovementDraft));
    stockMovementDraft.setLineItems(stockCardLocationMovementDraftLineItemList);
    log.info("save virtual location movement draft with id: {}", stockMovementDraft.getId());
    stockCardLocationMovementDraftRepository.save(stockMovementDraft);
    return StockCardLocationMovementDraftDto.from(stockMovementDraft);
  }

  private List<StockCardLocationMovementDraftLineItem> getStockCardLocationMovementDraftLineItem(UUID facilityId,
      List<StockCardLocationMovementLineItem> virtualLocationMovementLineItem) {
    List<StockCardLocationMovementDraftLineItem> stockCardLocationMovementDraftlineItemList = new LinkedList<>();

    List<UUID> virtualLocationMovementStockCardIds = virtualLocationMovementLineItem.stream().map(
            StockCardLocationMovementLineItem::getStockCardId)
        .collect(Collectors.toList());
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
    List<StockCard> virtualLocationMovementStockCardList = stockCardRepository
        .findByIdIn(virtualLocationMovementStockCardIds, pageable).getContent();
    Map<UUID, StockCard> stockCardIdToStockCardMap = Maps.uniqueIndex(
        virtualLocationMovementStockCardList, StockCard::getId);

    Set<UUID> orderableIds = virtualLocationMovementStockCardList.stream()
        .map(StockCard::getOrderableId)
        .collect(Collectors.toSet());
    MultiValueMap<String, Object> queryParams = new LinkedMultiValueMap<>();
    QueryOrderableSearchParams queryOrderableSearchParams = new QueryOrderableSearchParams(queryParams);
    queryOrderableSearchParams.setIds(orderableIds);
    Map<UUID, OrderableDto> orderableIdToOrderableDtoMap = Maps.uniqueIndex(
        siglusOrderableService.searchOrderables(queryOrderableSearchParams, pageable, facilityId)
            .getContent(), OrderableDto::getId);

    List<UUID> lotIds = virtualLocationMovementStockCardList.stream()
        .map(StockCard::getLotId)
        .collect(Collectors.toList());
    Map<UUID, LotDto> lotIdToLotDtoMap = Maps.uniqueIndex(siglusLotReferenceDataService.findByIds(lotIds),
        LotDto::getId);

    virtualLocationMovementLineItem.forEach(e -> {
      StockCard stockCard = stockCardIdToStockCardMap.get(e.getStockCardId());
      UUID orderableId = stockCard.getOrderableId();
      UUID lotId = stockCard.getLotId();
      OrderableDto orderable = orderableIdToOrderableDtoMap.get(orderableId);
      LotDto lotDto = lotIdToLotDtoMap.get(lotId);
      stockCardLocationMovementDraftlineItemList.add(StockCardLocationMovementDraftLineItem.builder()
          .orderableId(orderableId)
          .productCode(orderable.getProductCode())
          .productName(orderable.getFullProductName())
          .lotId(lotId)
          .lotCode(Objects.isNull(lotDto) ? null : lotDto.getLotCode())
          .srcArea(e.getSrcArea())
          .srcLocationCode(e.getSrcLocationCode())
          .expirationDate(Objects.isNull(lotDto) ? null : lotDto.getExpirationDate())
          .quantity(e.getQuantity())
          .stockOnHand(e.getQuantity())
          .build());
    });
    return stockCardLocationMovementDraftlineItemList;
  }

  public StockCardLocationMovementDraftDto searchMovementDraft(UUID id) {
    StockCardLocationMovementDraft movementDraft = stockCardLocationMovementDraftRepository.findOne(id);
    stockCardLocationMovementDraftValidator.validateMovementDraft(movementDraft);
    return StockCardLocationMovementDraftDto.from(movementDraft);
  }

  @Transactional
  public StockCardLocationMovementDraftDto updateMovementDraft(StockCardLocationMovementDraftDto movementDraftDto,
      UUID movementDraftId) {
    stockCardLocationMovementDraftValidator.validateMovementDraftAndLineItems(movementDraftDto, movementDraftId);
    StockCardLocationMovementDraft stockMovementDraft = StockCardLocationMovementDraft
        .createMovementDraft(movementDraftDto);
    log.info("update movement draft with id: {}", movementDraftId);
    StockCardLocationMovementDraft savedMovementDraft = stockCardLocationMovementDraftRepository
        .save(stockMovementDraft);
    return StockCardLocationMovementDraftDto.from(savedMovementDraft);
  }

  @Transactional
  public void deleteMovementDraft(UUID movementDraftId) {
    StockCardLocationMovementDraft movementDraft = stockCardLocationMovementDraftRepository.findOne(movementDraftId);
    stockCardLocationMovementDraftValidator.validateMovementDraft(movementDraft);
    log.info("delete movement draft with id: {}", movementDraftId);
    stockCardLocationMovementDraftRepository.delete(movementDraft);
  }
}
