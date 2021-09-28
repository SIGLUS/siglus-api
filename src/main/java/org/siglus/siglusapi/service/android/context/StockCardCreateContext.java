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

package org.siglus.siglusapi.service.android.context;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.db.CalculatedStockOnHand;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory.Key;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.dto.android.db.StockCard;
import org.siglus.siglusapi.dto.android.db.StockEvent;
import org.siglus.siglusapi.dto.android.enumeration.AdjustmentReason;
import org.siglus.siglusapi.dto.android.enumeration.Destination;
import org.siglus.siglusapi.dto.android.enumeration.Source;

public final class StockCardCreateContext {

  @Getter
  private final FacilityDto facility;
  private final Map<NameBindToProgram, UUID> reasons;
  private final Map<NameBindToProgram, UUID> sources;
  private final Map<NameBindToProgram, UUID> destinations;
  private final Map<String, OrderableDto> approvedProducts;
  @Getter
  private final PeriodOfProductMovements allProductMovements;
  private final Map<ProductLotCode, ProductLot> lots = new HashMap<>();
  private final Map<ProductLotCode, StockCard> stockCards = new HashMap<>();
  private final Map<PhysicalInventory.Key, PhysicalInventory> physicalInventories = new HashMap<>();
  private final Map<CalculatedStockOnHand.Key, CalculatedStockOnHand> inventories = new HashMap<>();

  StockCardCreateContext(FacilityDto facility, Collection<ValidReasonAssignmentDto> reasons,
      Collection<ValidSourceDestinationDto> sources, Collection<ValidSourceDestinationDto> destinations,
      Collection<OrderableDto> approvedProducts, PeriodOfProductMovements allProductMovements) {
    this.facility = facility;
    this.reasons = reasons.stream().collect(toMap(NameBindToProgram::of, v -> v.getReason().getId()));
    this.sources = sources.stream().collect(toMap(NameBindToProgram::of, v -> v.getNode().getId()));
    this.destinations = destinations.stream().collect(toMap(NameBindToProgram::of, v -> v.getNode().getId()));
    this.approvedProducts = approvedProducts.stream().collect(toMap(BasicOrderableDto::getProductCode, identity()));
    this.allProductMovements = allProductMovements;
  }

  public Optional<UUID> findReasonId(UUID programId, String reason) {
    String reasonName = AdjustmentReason.valueOf(reason).getName();
    UUID reasonId = reasons.get(NameBindToProgram.of(programId, reasonName));
    return ofNullable(reasonId);
  }

  public Optional<UUID> findSourceId(UUID programId, String source) {
    String sourceName = Source.valueOf(source).getName();
    UUID sourceId = sources.get(NameBindToProgram.of(programId, sourceName));
    return ofNullable(sourceId);
  }

  public Optional<UUID> findDestinationId(UUID programId, String destination) {
    String destinationName = Destination.valueOf(destination).getName();
    UUID destinationId = destinations.get(NameBindToProgram.of(programId, destinationName));
    return ofNullable(destinationId);
  }

  public boolean isApprovedByCurrentFacility(String productCode) {
    return approvedProducts.containsKey(productCode);
  }

  public Optional<UUID> getProgramId(String productCode) {
    return ofNullable(approvedProducts.get(productCode))
        .flatMap(o -> o.getPrograms().stream().findAny())
        .map(ProgramOrderableDto::getProgramId);
  }

  public UUID getProductId(String productCode) {
    return ofNullable(approvedProducts.get(productCode)).map(BasicOrderableDto::getId)
        .orElseThrow(IllegalStateException::new);
  }

  public ProductLot getLot(String productCode, String lotCode) {
    if (lotCode == null) {
      return ProductLot.noLot(productCode);
    }
    ProductLotCode productLotCode = ProductLotCode.of(productCode, lotCode);
    return lots.get(productLotCode);
  }

  public UUID getLotId(String productCode, String lotCode) {
    ProductLotCode productLotCode = ProductLotCode.of(productCode, lotCode);
    return ofNullable(lots.get(productLotCode))
        .map(ProductLot::getId)
        .orElse(null);
  }

  public void newLot(ProductLot lot) {
    ProductLotCode productLotCode = ProductLotCode.of(lot.getProductCode(), lot.getLot().getCode());
    if (lots.containsKey(productLotCode)) {
      throw new IllegalStateException("Lot already existed!");
    }
    lots.put(productLotCode, lot);
  }

  public StockCard getStockCard(String productCode, String lotCode) {
    ProductLotCode productLotCode = ProductLotCode.of(productCode, lotCode);
    return stockCards.get(productLotCode);
  }

  public void newStockCard(StockCard stockCard) {
    ProductLotCode productLotCode = ProductLotCode.of(stockCard.getProductCode(), stockCard.getLotCode());
    if (stockCards.containsKey(productLotCode)) {
      throw new IllegalStateException("Stock card already existed!");
    }
    stockCards.put(productLotCode, stockCard);
  }

  public PhysicalInventory getPhysicalInventory(StockEvent stockEvent, LocalDate occurredDate) {
    PhysicalInventory physicalInventory = PhysicalInventory
        .of(stockEvent.getFacilityId(), stockEvent.getProgramId(), occurredDate, stockEvent);
    return physicalInventories.get(physicalInventory.getKey());
  }

  public void newPhysicalInventory(PhysicalInventory physicalInventory) {
    Key key = physicalInventory.getKey();
    if (physicalInventories.containsKey(key)) {
      throw new IllegalStateException("Inventory already existed!");
    }
    physicalInventories.put(key, physicalInventory);
  }

  public Map<String, OrderableDto> getApprovedProducts() {
    return unmodifiableMap(approvedProducts);
  }

  public OrderableDto getProduct(String productCode) {
    return approvedProducts.get(productCode);
  }

  public void addNewCalculatedStockOnHand(CalculatedStockOnHand newOne) {
    CalculatedStockOnHand existed = inventories.get(newOne.getKey());
    EventTime eventTime = newOne.getInventoryDetail().getEventTime();
    if (existed == null || existed.getInventoryDetail().getEventTime().compareTo(eventTime) < 0) {
      if (existed != null && existed.getId() != null) {
        newOne.setId(existed.getId());
      }
      inventories.put(newOne.getKey(), newOne);
    }
  }

  public List<CalculatedStockOnHand> getCalculatedStocksOnHand() {
    return new ArrayList<>(inventories.values());
  }

  @Data
  @AllArgsConstructor(staticName = "of")
  private static class NameBindToProgram {

    private final UUID programId;
    private final String name;

    static NameBindToProgram of(ValidReasonAssignmentDto v) {
      return new NameBindToProgram(v.getProgramId(), v.getReason().getName());
    }

    static NameBindToProgram of(ValidSourceDestinationDto v) {
      return new NameBindToProgram(v.getProgramId(), v.getName());
    }

  }

}
