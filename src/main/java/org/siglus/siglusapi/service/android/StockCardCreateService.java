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

import static java.util.Collections.singletonList;
import static java.util.Comparator.naturalOrder;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.siglus.siglusapi.constant.FieldConstants.INVENTORY;
import static org.siglus.siglusapi.service.android.MeService.KEY_PROGRAM_CODE;
import static org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder.getContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.sourcedestination.Node;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.EventTimeContainer;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.StockCardExtensionDto;
import org.siglus.siglusapi.dto.android.constraint.stockcard.LotStockConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductConsistentWithAllLots;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductMovementConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByLot;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByProduct;
import org.siglus.siglusapi.dto.android.db.CalculatedStockOnHand;
import org.siglus.siglusapi.dto.android.db.LineItemDetail;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLine;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLineAdjustment;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.dto.android.db.RequestedQuantity;
import org.siglus.siglusapi.dto.android.db.StockCard;
import org.siglus.siglusapi.dto.android.db.StockCardLineItem;
import org.siglus.siglusapi.dto.android.db.StockEvent;
import org.siglus.siglusapi.dto.android.db.StockEventLineItem;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.group.DatabaseCheckGroup;
import org.siglus.siglusapi.dto.android.group.SelfCheckGroup;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.LotConflictService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.slf4j.profiler.Profiler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class StockCardCreateService {

  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;
  private final StockManagementRepository repo;
  private final RequisitionService requisitionService;
  private final LotConflictService lotConflictService;
  private final SiglusLotReferenceDataService siglusLotReferenceDataService;
  private final SiglusValidSourceDestinationService validSourceDestinationService;

  @Transactional
  @Validated(PerformanceSequence.class)
  public void createStockCards(
      @Valid
      @NotEmpty
      @StockOnHandConsistentWithQuantityByProduct(groups = SelfCheckGroup.class)
      @StockOnHandConsistentWithQuantityByLot(groups = SelfCheckGroup.class)
      @ProductConsistentWithAllLots(groups = SelfCheckGroup.class)
      @LotStockConsistentWithExisted(groups = DatabaseCheckGroup.class)
      @ProductMovementConsistentWithExisted(groups = DatabaseCheckGroup.class)
      List<StockCardCreateRequest> requests,
      Profiler profiler
  ) {
    profiler.setLogger(log);
    profiler.start("load & filter");
    List<StockCardCreateRequest> filtered = filterOutExisted(requests);
    profiler.start("walk through lots");
    StockCardNativeCreateContext context = new StockCardNativeCreateContext();
    walkThroughLots(filtered, context);
    Map<Instant, List<StockCardCreateRequest>> mapRecordedAtToRequest = new LinkedHashMap<>();
    filtered.stream().collect(groupingBy(StockCardCreateRequest::getRecordedAt))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(e -> mapRecordedAtToRequest.put(e.getKey(), e.getValue()));
    Instant processedAt = Instant.now();
    for (Map.Entry<Instant, List<StockCardCreateRequest>> entry : mapRecordedAtToRequest.entrySet()) {
      processedAt = processedAt.plusMillis(1L);
      createEvents(entry.getValue(), context, processedAt);
    }
    filtered.sort(EventTimeContainer.ASCENDING);
    populateCalculatedStockOnHand(filtered);
    Profiler insertDataProfiler = profiler.startNested("insert data");
    insertData(context, insertDataProfiler);
    profiler.start("end createStockCards");
  }

  private List<StockCardCreateRequest> filterOutExisted(List<StockCardCreateRequest> requests) {
    List<ProductMovementKey> existed = getContext().getAllProductMovements().getProductMovements().stream()
        .map(ProductMovement::getProductMovementKey).collect(toList());
    return requests.stream().filter(r -> !existed.contains(r.getProductMovementKey()))
        .collect(toList());
  }

  private Map<ProductLotCode, LocalDate> getExpirationDatesFromRequest(List<StockCardCreateRequest> requests) {
    return requests.stream()
        .filter(request -> !request.getLotEvents().isEmpty())
        .map(request -> request.getLotEvents().stream()
            .collect(toMap(l -> ProductLotCode.of(request.getProductCode(), l.getLotCode()),
                StockCardLotEventRequest::getExpirationDate)))
        .reduce(new HashMap<>(), (m1, m2) -> {
          m2.forEach((k, v) -> m1.merge(k, v, (v1, v2) -> v1));
          return m1;
        });
  }

  private void walkThroughLots(List<StockCardCreateRequest> requests, StockCardNativeCreateContext context) {
    Map<ProductLotCode, LocalDate> codeToExpirationDates = getExpirationDatesFromRequest(requests);
    Map<ProductLotCode, EventTime> lotToEarliestDate = mapLotToEarliestDate(requests);
    lotToEarliestDate.forEach(
        (productLotCode, earliestDate) -> {
          String productCode = productLotCode.getProductCode();
          if (!productLotCode.isLot()) {
            loadStockCardToContext(ProductLot.noLot(productCode), earliestDate);
            return;
          }
          String lotCode = productLotCode.getLotCode();
          LocalDate expirationDate = codeToExpirationDates.get(productLotCode);
          Lot lot = Lot.of(lotCode, expirationDate);
          ProductLot cachedLot = getContext().getLot(productCode, lotCode);
          if (cachedLot != null) {
            return;
          }
          OrderableDto product = getContext().getProduct(productCode);
          ProductLot existedLot = repo.getLot(product, lot);
          if (existedLot == null) {
            ProductLot newLot = ProductLot.of(product, lot);
            context.lots.add(newLot);
            getContext().newLot(newLot);
            return;
          }
          loadStockCardToContext(existedLot, earliestDate);
          getContext().newLot(existedLot);
          UUID facilityId = getContext().getFacility().getId();
          lotConflictService.handleLotConflict(facilityId, lotCode, existedLot.getId(), expirationDate,
              requireNonNull(existedLot.getExpirationDate()));
        }
    );
  }

  private void loadStockCardToContext(ProductLot productLot, EventTime earliestDate) {
    if (getContext().getStockCard(productLot.getProductCode(), productLot.getLotCode()) != null) {
      return;
    }
    UUID facilityId = getContext().getFacility().getId();
    String productCode = productLot.getProductCode();
    UUID programId = getContext().getProgramId(productCode).orElseThrow(IllegalStateException::new);
    UUID productId = getContext().getProductId(productCode);
    StockCard stockCard = repo.getStockCard(facilityId, programId, productId, productLot);
    if (stockCard == null) {
      return;
    }
    getContext().newStockCard(stockCard);
    repo.findCalculatedStockOnHand(stockCard, earliestDate).forEach(getContext()::addNewCalculatedStockOnHand);
  }

  private Map<ProductLotCode, EventTime> mapLotToEarliestDate(List<StockCardCreateRequest> requests) {
    return requests.stream()
        .map(
            r -> {
              if (r.getLotEvents().isEmpty()) {
                Map<ProductLotCode, EventTime> map = new HashMap<>();
                map.put(ProductLotCode.noLot(r.getProductCode()), r.getEventTime());
                return map;
              }
              return r.getLotEvents().stream()
                  .collect(toMap(l -> ProductLotCode.of(r.getProductCode(), l.getLotCode()), l -> r.getEventTime()));
            })
        .reduce(new HashMap<>(), (m1, m2) -> {
          m2.forEach((k, v) -> m1
              .merge(k, v, (v1, v2) -> Stream.of(v1, v2).min(naturalOrder()).orElseThrow(IllegalStateException::new)));
          return m1;
        });
  }

  public List<OrderableDto> getAllApprovedProducts() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper.findHomeFacilitySupportedProgramIds().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private void createEvents(List<StockCardCreateRequest> requests, StockCardNativeCreateContext context,
      Instant processedAt) {
    String signature = requests.stream().filter(r -> r.getSignature() != null)
        .findFirst().map(StockCardCreateRequest::getSignature).orElse(null);
    UUID facilityId = getContext().getFacility().getId();
    UUID userId = authHelper.getCurrentUserId().orElseThrow(IllegalStateException::new);
    requests.stream()
        .collect(groupingBy(r -> getContext().getProgramId(r.getProductCode()).orElseThrow(IllegalStateException::new)))
        .forEach((programId, r) -> {
          StockEvent stockEvent = StockEvent.of(facilityId, programId, processedAt, signature, userId);
          context.stockEvents.add(stockEvent);
          createEvent(stockEvent, r, context);
        });
  }

  private void createEvent(StockEvent stockEvent, List<StockCardCreateRequest> requests,
      StockCardNativeCreateContext context) {
    requests.stream()
        .forEach(request -> {
          MovementType type = MovementType.valueOf(request.getType());
          if (type == MovementType.ISSUE) {
            OrderableDto product = getContext().getProduct(request.getProductCode());
            context.requestedQuantities.add(RequestedQuantity.of(stockEvent, product, request.getRequested()));
          }
          toAdjustments(request).forEach(adjustment -> {
            StockCard stockCard = getStockCard(request, adjustment, stockEvent, context);
            LineItemDetail lineDetail = LineItemDetail
                .of(stockEvent, stockCard, type, adjustment, request.isInitInventory(), getNewSourceId(stockCard));
            StockEventLineItem eventLine = StockEventLineItem.of(lineDetail);
            context.eventLineItems.add(eventLine);
            StockCardLineItem stockCardLine = StockCardLineItem.of(lineDetail);
            context.stockCardLineItems.add(stockCardLine);
            UUID eventLineId = eventLine.getId();
            UUID stockCardLineId = stockCardLine.getId();
            if (type != MovementType.PHYSICAL_INVENTORY || INVENTORY.equals(adjustment.getReasonName())) {
              return;
            }
            PhysicalInventory inventory = getPhysicalInventory(request.getOccurredDate(), stockEvent, context);
            PhysicalInventoryLine inventoryLine = PhysicalInventoryLine.of(inventory, stockCard, type, adjustment);
            context.inventoryLines.add(inventoryLine);
            context.inventoryLineAdjustments
                .addAll(PhysicalInventoryLineAdjustment.of(inventoryLine, eventLineId, stockCardLineId));
          });
        });
  }

  private UUID getNewSourceId(StockCard stockCard) {
    Collection<ValidSourceDestinationDto> sourcesForOneProgram = validSourceDestinationService.findSourcesForOneProgram(
        stockCard.getProgramId(), stockCard.getFacilityId());
    for (ValidSourceDestinationDto source : sourcesForOneProgram) {
      Node node = source.getNode();
      if (node.isRefDataFacility()) {
        return node.getId();
      }
    }
    return null;
  }

  private List<? extends StockCardAdjustment> toAdjustments(StockCardCreateRequest request) {
    if (request.getLotEvents().isEmpty()) {
      return singletonList(request);
    }
    return request.getLotEvents();
  }

  private StockCard getStockCard(StockCardCreateRequest request, StockCardAdjustment adjustment, StockEvent stockEvent,
      StockCardNativeCreateContext context) {
    String lotCode = null;
    String productCode = request.getProductCode();
    if (adjustment instanceof StockCardLotEventRequest) {
      lotCode = ((StockCardLotEventRequest) adjustment).getLotCode();
    }
    StockCard stockCard = getContext().getStockCard(productCode, lotCode);
    if (stockCard != null) {
      return stockCard;
    }
    String reasonName = request.getReasonName();
    if (!request.getLotEvents().isEmpty()) {
      reasonName = request.getLotEvents().get(0).getReasonName();
    }
    if ((INVENTORY.equals(reasonName)) && request.getQuantity().equals(request.getStockOnHand())) {
      request.setIsInitInventory(true);
    }
    UUID facilityId = getContext().getFacility().getId();
    UUID programId = getContext().getProgramId(productCode).orElseThrow(IllegalStateException::new);
    UUID productId = getContext().getProductId(productCode);
    ProductLot productLot = getContext().getLot(productCode, lotCode);
    stockCard = StockCard.of(facilityId, programId, productId, productLot, stockEvent);
    context.stockCards.add(stockCard);
    getContext().newStockCard(stockCard);
    StockCardExtensionDto stockCardExtensionDto = StockCardExtensionDto
        .of(stockCard.getId(), request.getOccurredDate());
    context.stockCardExtensionDtos.add(stockCardExtensionDto);
    return stockCard;
  }

  private PhysicalInventory getPhysicalInventory(LocalDate occurredDate, StockEvent stockEvent,
      StockCardNativeCreateContext context) {
    UUID facilityId = getContext().getFacility().getId();
    UUID programId = stockEvent.getProgramId();
    PhysicalInventory physicalInventory = getContext().getPhysicalInventory(stockEvent, occurredDate);
    if (physicalInventory != null) {
      return physicalInventory;
    }
    physicalInventory = PhysicalInventory.of(facilityId, programId, occurredDate, stockEvent);
    context.physicalInventories.add(physicalInventory);
    getContext().newPhysicalInventory(physicalInventory);
    return physicalInventory;
  }

  private List<OrderableDto> getProgramProducts(UUID homeFacilityId, ProgramDto program) {
    return requisitionService.getApprovedProductsWithoutAdditional(homeFacilityId, program.getId())
        .stream()
        .map(ApprovedProductDto::getOrderable)
        .map(orderable -> {
          orderable.getExtraData().put(KEY_PROGRAM_CODE, program.getCode());
          return orderable;
        })
        .collect(toList());
  }

  private void populateCalculatedStockOnHand(List<StockCardCreateRequest> filtered) {
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
        InventoryDetail inventoryDetail = InventoryDetail.of(adjustment.getStockOnHand(), eventTime);
        CalculatedStockOnHand calculated = CalculatedStockOnHand.of(stockCard, inventoryDetail);
        getContext().addNewCalculatedStockOnHand(calculated);
      });
    }
  }

  private void insertData(StockCardNativeCreateContext context, Profiler profiler) {
    profiler.setLogger(log);
    profiler.start("insert lots: " + context.lots.size());
    siglusLotReferenceDataService.batchSaveLot(LotDto.convertList(context.lots));
    profiler.start("insert stockEvents: " + context.stockEvents.size());
    repo.batchCreateEvents(context.stockEvents);
    profiler.start("insert stockCards: " + context.stockCards.size());
    repo.batchCreateStockCards(context.stockCards);
    profiler.start("insert requestedQuantities: " + context.requestedQuantities.size());
    repo.batchCreateRequestedQuantities(context.requestedQuantities);
    profiler.start("insert eventLineItems: " + context.eventLineItems.size());
    repo.batchCreateEventLines(context.eventLineItems);
    profiler.start("insert stockCardLineItems: " + context.stockCardLineItems.size());
    repo.batchCreateLines(context.stockCardLineItems);
    profiler.start("insert physicalInventories: " + context.physicalInventories.size());
    repo.batchCreateInventories(context.physicalInventories);
    profiler.start("insert inventoryLines: " + context.inventoryLines.size());
    repo.batchCreateInventoryLines(context.inventoryLines);
    profiler.start("insert inventoryLineAdjustments: " + context.inventoryLineAdjustments.size());
    repo.batchCreateInventoryLineAdjustments(context.inventoryLineAdjustments);
    profiler.start("insert stock on hand: " + getContext().getCalculatedStocksOnHand().size());
    repo.batchSaveStocksOnHand(getContext().getCalculatedStocksOnHand());
    profiler.start("insert stockCardExtension: " + context.stockCardExtensionDtos.size());
    repo.batchCreateStockCardExtensions(context.stockCardExtensionDtos);
  }

  private static class StockCardNativeCreateContext {

    private final List<ProductLot> lots = new ArrayList<>();
    private final List<StockEvent> stockEvents = new ArrayList<>();
    private final List<StockCard> stockCards = new ArrayList<>();
    private final List<RequestedQuantity> requestedQuantities = new ArrayList<>();
    private final List<StockEventLineItem> eventLineItems = new ArrayList<>();
    private final List<StockCardLineItem> stockCardLineItems = new ArrayList<>();
    private final List<PhysicalInventory> physicalInventories = new ArrayList<>();
    private final List<PhysicalInventoryLine> inventoryLines = new ArrayList<>();
    private final List<PhysicalInventoryLineAdjustment> inventoryLineAdjustments = new ArrayList<>();
    private final List<StockCardExtensionDto> stockCardExtensionDtos = new ArrayList<>();
  }

}
