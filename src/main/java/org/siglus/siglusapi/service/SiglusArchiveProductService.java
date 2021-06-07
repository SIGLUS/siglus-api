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

import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_ALREADY_ARCHIVED;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.common.domain.ArchivedProduct;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods"})
public class SiglusArchiveProductService {

  @Autowired
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Autowired
  private SiglusUnpackService unpackService;

  @Autowired
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Autowired
  private SiglusOrderableService siglusOrderableService;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private ArchivedProductRepository archivedProductRepository;

  @Autowired
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Transactional
  public void archiveProduct(UUID facilityId, UUID orderableId) {
    Set<UUID> orderablesInKit = unpackService.orderablesInKit();
    String errorInfo = ", facilityId: " + facilityId + ", orderableId: " + orderableId;
    if (orderablesInKit.contains(orderableId)) {
      throw new ValidationMessageException(
          new Message(ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT) + errorInfo);

    }
    List<StockCard> stockCards = stockCardRepository
        .findByFacilityIdAndOrderableId(facilityId, orderableId);
    stockCards.forEach(stockCard -> {
      calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard);
      if (0 != stockCard.getStockOnHand()) {
        throw new ValidationMessageException(
            new Message(ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO) + errorInfo);
      }
    });
    ArchivedProduct archivedProduct = archivedProductRepository
        .findByFacilityIdAndOrderableId(facilityId, orderableId);
    if (archivedProduct != null) {
      throw new ValidationMessageException(new Message(ERROR_ARCHIVE_ALREADY_ARCHIVED) + errorInfo);
    }
    doArchiveProduct(facilityId, orderableId);
    deleteArchivedItemInPhysicalInventoryDraft(facilityId, orderableId);
    deleteArchivedItemInStockManagementDraft(facilityId, orderableId);
    deleteArchivedItemInRequisitionDraft(facilityId, orderableId);
  }

  @Transactional
  public void archiveAllProducts(UUID facilityId, List<String> productCodes) {
    if (shouldNotArchiveAllProducts(facilityId, productCodes)) {
      log.info("no change, all archive products are existed");
      return;
    }
    log.info("delete all archived products in facility: {}", facilityId);
    archivedProductRepository.deleteAllArchivedProductsByFacilityId(facilityId);
    getProductIds(productCodes).forEach(orderableId -> archiveProduct(facilityId, orderableId));
  }

  @Transactional
  public void activateProduct(UUID facilityId, UUID orderableId) {
    ArchivedProduct archivedProduct = archivedProductRepository
        .findByFacilityIdAndOrderableId(facilityId, orderableId);
    if (archivedProduct == null) {
      return;
    }
    log.info("activate product, facilityId: {}, orderableId: {}", facilityId, orderableId);
    archivedProductRepository.delete(archivedProduct);
  }

  @Transactional
  public void activateProducts(UUID facilityId, Set<UUID> orderableIds) {
    orderableIds.forEach(orderableId -> activateProduct(facilityId, orderableId));
  }

  public boolean isArchived(UUID stockCardId) {
    StockCard stockCard = stockCardRepository.findOne(stockCardId);
    ArchivedProduct archivedProduct = archivedProductRepository
        .findByFacilityIdAndOrderableId(stockCard.getFacilityId(), stockCard.getOrderableId());
    return archivedProduct != null;
  }

  public Set<String> searchArchivedProductsByFacilityId(UUID facilityId) {
    return archivedProductRepository.findArchivedProductsByFacilityId(facilityId);
  }

  public Set<String> searchArchivedProductsByFacilityIds(Set<UUID> facilityIds) {
    return archivedProductRepository.findArchivedProductsByFacilityIds(facilityIds);
  }

  private void doArchiveProduct(UUID facilityId, UUID orderableId) {
    ArchivedProduct newArchivedProduct = ArchivedProduct.builder().facilityId(facilityId)
        .orderableId(orderableId).build();
    log.info("archive product, facilityId: {}, orderableId: {}", facilityId, orderableId);
    archivedProductRepository.save(newArchivedProduct);
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

  private List<UUID> getProductIds(List<String> productCodes) {
    return productCodes.stream()
        .map(productCode -> siglusOrderableService.getOrderableByCode(productCode).getId())
        .collect(Collectors.toList());
  }

  private boolean shouldNotArchiveAllProducts(UUID facilityId, List<String> productCodes) {
    Set<String> archivedProductIds = archivedProductRepository
        .findArchivedProductsByFacilityId(facilityId);
    List<String> productIds = getProductIds(productCodes).stream().map(UUID::toString)
        .collect(Collectors.toList());
    return archivedProductIds.containsAll(productIds) && productIds.containsAll(archivedProductIds);
  }
}
