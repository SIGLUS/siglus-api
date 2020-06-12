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

import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_ALREADY_ACTIVATED;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_ALREADY_ARCHIVED;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_STOCK_CARD_NOT_FOUND;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.common.domain.StockCardExtension;
import org.siglus.common.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiglusArchiveProductService {

  @Autowired
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Autowired
  private SiglusUnpackService unpackService;

  @Autowired
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Autowired
  private RequisitionRepository requisitionRepository;

  private static final String ARCHIVE = "archive";

  private static final String ACTIVATE = "activate";

  @Transactional
  public void archiveProduct(UUID facilityId, UUID orderableId) {
    List<StockCard> stockCards = stockCardRepository
        .findByFacilityIdAndOrderableId(facilityId, orderableId);
    if (CollectionUtils.isEmpty(stockCards)) {
      throw new ResourceNotFoundException(new Message(ERROR_ARCHIVE_STOCK_CARD_NOT_FOUND));
    }
    Set<UUID> orderablesInKit = unpackService.orderablesInKit();
    stockCards.forEach(stockCard -> {
      calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard);
      if (orderablesInKit.contains(stockCard.getOrderableId())) {
        throw new ValidationMessageException(
            new Message(ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT));
      }
      if (0 != stockCard.getStockOnHand()) {
        throw new ValidationMessageException(new Message(ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO));
      }
    });
    toggleStockCardArchiveState(stockCards, ARCHIVE);
    deleteArchivedItemInPhysicalInventoryDraft(facilityId, orderableId);
    deleteArchivedItemInStockManagementDraft(facilityId, orderableId);
    deleteArchivedItemInRequisitionDraft(facilityId, orderableId);
  }

  @Transactional
  public void activateProduct(UUID facilityId, UUID orderableId) {
    List<StockCard> stockCards = stockCardRepository
        .findByFacilityIdAndOrderableId(facilityId, orderableId);
    if (CollectionUtils.isEmpty(stockCards)) {
      throw new ResourceNotFoundException(new Message(ERROR_ARCHIVE_STOCK_CARD_NOT_FOUND));
    }
    toggleStockCardArchiveState(stockCards, ACTIVATE);
  }

  public void activateArchivedProducts(Collection<UUID> orderableIds, UUID facilityId) {
    Set<UUID> stockCardIds = stockCardRepository
        .findByOrderableIdInAndFacilityId(orderableIds, facilityId)
        .stream()
        .map(StockCard::getId)
        .collect(Collectors.toSet());
    List<StockCardExtension> stockCardExtensions = stockCardExtensionRepository
        .findByStockCardIdIn(stockCardIds);
    stockCardExtensions.forEach(stockCardExtension -> stockCardExtension.setArchived(false));
    stockCardExtensionRepository.save(stockCardExtensions);
  }

  private void toggleStockCardArchiveState(List<StockCard> stockCards, String toState) {
    stockCards.forEach(stockCard -> {
      StockCardExtension extension =
          stockCardExtensionRepository.findByStockCardId(stockCard.getId());
      if (extension.isArchived() && ARCHIVE.equals(toState)) {
        throw new ValidationMessageException(new Message(ERROR_ARCHIVE_ALREADY_ARCHIVED));
      }
      if (!extension.isArchived() && ACTIVATE.equals(toState)) {
        throw new ValidationMessageException(new Message(ERROR_ARCHIVE_ALREADY_ACTIVATED));
      }
      extension.setArchived(!extension.isArchived());
      stockCardExtensionRepository.save(extension);
    });
  }

  public boolean isArchived(UUID stockCardId) {
    StockCardExtension stockCardExtension =
        stockCardExtensionRepository.findByStockCardId(stockCardId);
    return stockCardExtension != null && stockCardExtension.isArchived();
  }

  private void deleteArchivedItemInPhysicalInventoryDraft(UUID facilityId, UUID orderableId) {
    PhysicalInventoryDto physicalInventoryDraft = siglusPhysicalInventoryService
        .getPhysicalInventoryForAllProducts(facilityId);
    if (null == physicalInventoryDraft) {
      return;
    }
    final List<PhysicalInventoryLineItemDto> lineItems = physicalInventoryDraft.getLineItems();
    if (CollectionUtils.isEmpty(lineItems)) {
      return;
    }
    List<PhysicalInventoryLineItemDto> filteredLineItems = lineItems
        .stream()
        .filter(lineItem -> !lineItem.getOrderableId().equals(orderableId))
        .collect(Collectors.toList());
    if (lineItems.size() == filteredLineItems.size()) {
      return;
    }
    if (CollectionUtils.isEmpty(filteredLineItems)) {
      siglusPhysicalInventoryService.deletePhysicalInventoryForAllProducts(facilityId);
    } else {
      physicalInventoryDraft.setLineItems(filteredLineItems);
      siglusPhysicalInventoryService.saveDraftForAllProducts(physicalInventoryDraft);
    }
  }

  private void deleteArchivedItemInStockManagementDraft(UUID facilityId, UUID orderableId) {
    List<StockManagementDraft> stockManagementDrafts = stockManagementDraftRepository
        .findByFacilityId(facilityId);
    if (CollectionUtils.isEmpty(stockManagementDrafts)) {
      return;
    }
    stockManagementDrafts.forEach(stockManagementDraft -> {
      List<StockManagementDraftLineItem> lineItems = stockManagementDraft.getLineItems();
      if (CollectionUtils.isEmpty(lineItems)) {
        return;
      }
      if (lineItems.removeIf(lineItem -> lineItem.getOrderableId().equals(orderableId))) {
        stockManagementDraftRepository.save(stockManagementDraft);
      }
    });
  }

  private void deleteArchivedItemInRequisitionDraft(UUID facilityId, UUID orderableId) {
    List<Requisition> requisitions = requisitionRepository
        .findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED);
    if (CollectionUtils.isEmpty(requisitions)) {
      return;
    }
    requisitions.forEach(requisition -> {
      List<RequisitionLineItem> lineItems = requisition.getRequisitionLineItems();
      if (CollectionUtils.isEmpty(lineItems)) {
        return;
      }
      if (lineItems.removeIf(lineItem -> lineItem.getOrderable().getId().equals(orderableId))) {
        requisitionRepository.save(requisition);
      }
    });
  }

  public Set<String> searchArchivedProducts(UUID facilityId) {
    return stockCardExtensionRepository.findArchivedProducts(facilityId);
  }
}
