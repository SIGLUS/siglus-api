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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.siglus.siglusapi.service.android.MeService.KEY_PROGRAM_CODE;
import static org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder.getContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.LotConflict;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.EventTimeContainer;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.constraint.stockcard.LotStockConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductConsistentWithAllLots;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductMovementConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByLot;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByProduct;
import org.siglus.siglusapi.dto.android.db.CalculatedStockOnHand;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLineDetail;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.dto.android.db.StockCard;
import org.siglus.siglusapi.dto.android.db.StockEvent;
import org.siglus.siglusapi.dto.android.db.StockEventLineDetail;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.group.PerformanceGroup;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.repository.LotConflictRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.slf4j.profiler.Profiler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class StockCardCreateService {

  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;
  private final StockManagementRepository stockManagementRepository;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final AndroidHelper androidHelper;
  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;
  private final LotConflictRepository lotConflictRepo;

  @Transactional
  @Validated(PerformanceSequence.class)
  public void createStockCards(
      @Valid
      @NotEmpty
      @StockOnHandConsistentWithQuantityByProduct
      @StockOnHandConsistentWithQuantityByLot
      @ProductConsistentWithAllLots
      @LotStockConsistentWithExisted(groups = PerformanceGroup.class)
      @ProductMovementConsistentWithExisted(groups = PerformanceGroup.class)
          List<StockCardCreateRequest> requests) {
    Profiler profiler = new Profiler("createStockCards");
    profiler.setLogger(log);
    FacilityDto facility = getContext().getFacility();
    deletePhysicalInventoryDraftForAndroidUser(facility.getId());
    profiler.start("load");
    List<ProductMovementKey> existed = getContext().getAllProductMovements().getProductMovements().stream()
        .map(ProductMovement::getProductMovementKey).collect(toList());
    profiler.start("filter");
    List<StockCardCreateRequest> filtered = requests.stream().filter(r -> !existed.contains(r.getProductMovementKey()))
        .collect(toList());
    Profiler nested = profiler.startNested("insert lines");
    filtered.stream().map(this::walkThroughLots).collect(groupingBy(StockCardCreateRequest::getRecordedAt))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> {
          nested.start("insert " + entry.getKey());
          createEvents(entry.getValue());
        });
    filtered.sort(EventTimeContainer.ASCENDING);
    profiler.start("insert stock on hand");
    for (StockCardCreateRequest request : filtered) {
      String productCode = request.getProductCode();
      EventTime eventTime = request.getEventTime();
      toAdjustments(request).forEach(adjustment -> {
        String lotCode = null;
        if (adjustment instanceof StockCardLotEventRequest) {
          lotCode = ((StockCardLotEventRequest) adjustment).getLotCode();
        }
        StockCard stockCard = getContext().getStockCard(productCode, lotCode);
        if (stockCard == null) {
          throw new IllegalStateException();
        }
        InventoryDetail inventoryDetail = new InventoryDetail(adjustment.getStockOnHand(), eventTime);
        CalculatedStockOnHand calculated = CalculatedStockOnHand.of(stockCard, inventoryDetail);
        getContext().addNewCalculatedStockOnHand(calculated);
      });
    }
    getContext().getCalculatedStocksOnHand().forEach(stockManagementRepository::saveStockOnHand);
    profiler.stop().log();
  }

  private StockCardCreateRequest walkThroughLots(StockCardCreateRequest request) {
    OrderableDto product = getContext().getProduct(request.getProductCode());
    request.getLotEvents().forEach(
        l -> {
          String lotCode = l.getLotCode();
          LocalDate expirationDate = l.getExpirationDate();
          ProductLot cached = getContext().getLot(product.getProductCode(), lotCode);
          if (cached == null) {
            ProductLot existed = stockManagementRepository.ensureLot(product, lotCode, expirationDate);
            cached = existed;
            getContext().newLot(existed);
          }
          handleLotConflict(lotCode, expirationDate, cached);
        }
    );
    return request;
  }

  private void handleLotConflict(String lotCode, LocalDate expirationDate, ProductLot cached) {
    UUID facilityId = getContext().getFacility().getId();
    if (cached.getLot().getExpirationDate().equals(expirationDate)) {
      return;
    }
    log.info("the date of lot {} is different: [in-request: {}, in-db: {}]", lotCode, expirationDate,
        cached.getLot().getExpirationDate());
    LotConflict exitedConflict = lotConflictRepo.findLotConflictByFacilityIdAndLotId(facilityId, cached.getId());
    if (exitedConflict != null) {
      log.info("conflict is already recorded");
      return;
    }
    LotConflict lotConflict = LotConflict.of(facilityId, cached.getId(), lotCode, expirationDate);
    lotConflict = lotConflictRepo.save(lotConflict);
    log.info("record conflict with id {}", lotConflict.getId());
  }

  public List<OrderableDto> getAllApprovedProducts() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper.findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private void createEvents(List<StockCardCreateRequest> requests) {
    String signature = requests.stream().filter(r -> r.getSignature() != null)
        .findFirst().map(StockCardCreateRequest::getSignature).orElse(null);
    UUID facilityId = getContext().getFacility().getId();
    Instant processedAt = Instant.now();
    UUID userId = authHelper.getCurrentUserId().orElseThrow(IllegalStateException::new);
    requests.stream()
        .collect(groupingBy(r -> getContext().getProgramId(r.getProductCode()).orElseThrow(IllegalStateException::new)))
        .forEach((programId, r) -> {
          StockEvent stockEvent = StockEvent.of(facilityId, programId, processedAt, signature, userId);
          stockManagementRepository.createStockEvent(stockEvent);
          createEvent(stockEvent, r);
        });
  }

  private void createEvent(StockEvent stockEvent, List<StockCardCreateRequest> requests) {
    requests.stream()
        .forEach(r -> {
          MovementType type = MovementType.valueOf(r.getType());
          if (type == MovementType.ISSUE) {
            UUID productId = getContext().getProductId(r.getProductCode());
            stockManagementRepository.saveRequested(stockEvent, productId, r.getRequested());
          }
          toAdjustments(r).forEach(a -> {
            StockCard stockCard = getStockCard(r.getProductCode(), a, stockEvent);
            StockEventLineDetail lineDetail = StockEventLineDetail.of(r.getProductCode(), type, a);
            UUID eventLineId = stockManagementRepository.createStockEventLine(stockEvent, stockCard, lineDetail);
            UUID stockCardLineId = stockManagementRepository.createStockCardLine(stockEvent, stockCard, lineDetail);
            if (type == MovementType.PHYSICAL_INVENTORY && !"INVENTORY".equals(a.getReasonName())) {
              PhysicalInventory physicalInventory = getPhysicalInventory(r.getOccurredDate(), stockEvent);
              PhysicalInventoryLineDetail inventoryLineDetail = PhysicalInventoryLineDetail
                  .of(physicalInventory, stockCard, type, a);
              stockManagementRepository.createPhysicalInventoryLine(inventoryLineDetail, eventLineId, stockCardLineId);
            }
          });
        });
  }

  private List<? extends StockCardAdjustment> toAdjustments(StockCardCreateRequest request) {
    if (request.getLotEvents().isEmpty()) {
      return singletonList(request);
    }
    return request.getLotEvents();
  }

  private StockCard getStockCard(String productCode, StockCardAdjustment adjustment, StockEvent stockEvent) {
    String lotCode = null;
    LocalDate expirationDate = null;
    if (adjustment instanceof StockCardLotEventRequest) {
      lotCode = ((StockCardLotEventRequest) adjustment).getLotCode();
      expirationDate = ((StockCardLotEventRequest) adjustment).getExpirationDate();
    }
    StockCard stockCard = getContext().getStockCard(productCode, lotCode);
    if (stockCard != null) {
      return stockCard;
    }
    UUID facilityId = getContext().getFacility().getId();
    UUID programId = getContext().getProgramId(productCode).orElseThrow(IllegalStateException::new);
    UUID productId = getContext().getProductId(productCode);
    UUID lotId = getContext().getLotId(productCode, lotCode);
    stockCard =
        StockCard.of(facilityId, programId, productId, productCode, lotId, lotCode, expirationDate, stockEvent.getId());
    StockCard existed = stockManagementRepository.getStockCard(stockCard);
    if (existed != null) {
      getContext().newStockCard(existed);
      return existed;
    }
    stockCard = stockManagementRepository.createStockCard(stockCard);
    getContext().newStockCard(stockCard);
    return stockCard;
  }

  private PhysicalInventory getPhysicalInventory(LocalDate occurredDate, StockEvent stockEvent) {
    UUID facilityId = getContext().getFacility().getId();
    UUID programId = stockEvent.getProgramId();
    PhysicalInventory physicalInventory = getContext().getPhysicalInventory(stockEvent, occurredDate);
    if (physicalInventory != null) {
      return physicalInventory;
    }
    physicalInventory =
        PhysicalInventory.of(facilityId, programId, stockEvent.getId(), occurredDate, stockEvent.getSignature());
    PhysicalInventory existed = stockManagementRepository.getPhysicalInventory(physicalInventory);
    if (existed != null) {
      getContext().newPhysicalInventory(existed);
      return existed;
    }
    physicalInventory = stockManagementRepository.createPhysicalInventory(physicalInventory);
    getContext().newPhysicalInventory(physicalInventory);
    return physicalInventory;
  }

  private List<OrderableDto> getProgramProducts(UUID homeFacilityId,
      ProgramDto program) {
    return approvedProductDataService
        .getApprovedProducts(homeFacilityId, program.getId(), emptyList()).stream()
        .map(ApprovedProductDto::getOrderable)
        .map(orderable -> {
          orderable.getExtraData().put(KEY_PROGRAM_CODE, program.getCode());
          return orderable;
        })
        .collect(toList());
  }

  private void deletePhysicalInventoryDraftForAndroidUser(UUID facilityId) {
    if (androidHelper.isAndroid()) {
      siglusPhysicalInventoryService.deletePhysicalInventoryForAllProducts(facilityId);
    }
  }

}
