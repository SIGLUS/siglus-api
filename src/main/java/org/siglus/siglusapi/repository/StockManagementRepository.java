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

package org.siglus.siglusapi.repository;

import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.siglus.common.domain.referencedata.Orderable.TRADE_ITEM;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
import org.openlmis.requisition.dto.OrderableDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.EventTimeContainer;
import org.siglus.siglusapi.dto.android.InventoryDetail;
import org.siglus.siglusapi.dto.android.Lot;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.PeriodOfProductMovements;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovement.ProductMovementBuilder;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.db.CalculatedStockOnHand;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLineDetail;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.dto.android.db.StockCard;
import org.siglus.siglusapi.dto.android.db.StockEvent;
import org.siglus.siglusapi.dto.android.db.StockEventLineDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class StockManagementRepository {

  private final EntityManager em;
  private final ObjectMapper json;
  private final JdbcTemplate jdbc;

  private static final String LEFT_JOIN = "LEFT JOIN ";
  private static final String STOCK_CARD_ROOT = "stockmanagement.stock_cards sc";
  private static final String ORDERABLE_ROOT =
      "(select distinct on (id) * from referencedata.orderables order by id, versionnumber desc) o";
  private static final String LOT_ROOT = "referencedata.lots l";

  @ParametersAreNullableByDefault
  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since) {
    requireNonNull(facilityId);
    return getAllProductMovements(facilityId, since, null, emptySet());
  }

  @ParametersAreNullableByDefault
  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till) {
    requireNonNull(facilityId);
    return getAllProductMovements(facilityId, since, till, emptySet());
  }

  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, @Nonnull Set<UUID> orderableIds) {
    requireNonNull(facilityId);
    requireNonNull(orderableIds);
    return getAllProductMovements(facilityId, null, null, orderableIds);
  }

  @ParametersAreNullableByDefault
  private PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till,
      @Nonnull Set<UUID> orderableIds) {
    List<ProductLotMovement> allLotMovements = findAllLotMovements(facilityId, since, till, orderableIds);
    StocksOnHand stocksOnHand = getStockOnHand(facilityId, till, orderableIds);
    Map<String, Integer> productInventories = stocksOnHand.getProductInventories();
    Map<ProductLotCode, Integer> lotInventories = stocksOnHand.getLotInventories();
    List<ProductMovement> productMovements = allLotMovements.stream()
        .collect(groupingBy(ProductLotMovement::getProductMovementKey))
        .entrySet().stream()
        .sorted(Entry.comparingByKey(EventTimeContainer.DESCENDING))
        .map(e -> toProductMovement(e.getKey(), e.getValue(), productInventories, lotInventories))
        .sorted(EventTimeContainer.ASCENDING)
        .collect(toList());
    return new PeriodOfProductMovements(productMovements, stocksOnHand);
  }

  public ProductLot ensureLot(OrderableDto product, String lotCode, LocalDate expirationDate) {
    String tradeItemId = product.getIdentifiers().get(TRADE_ITEM);
    ProductLotCode code = ProductLotCode.of(product.getProductCode(), lotCode);
    ProductLot productLot = new ProductLot(code, expirationDate);
    String lotQuery = "SELECT id, expirationdate FROM referencedata.lots WHERE lotcode = ? AND tradeitemid = ?";
    List<Map<String, Object>> resultList = jdbc.queryForList(lotQuery, lotCode, tradeItemId);
    if (!resultList.isEmpty()) {
      Map<String, Object> result = resultList.get(0);
      LocalDate expirationDateInDb = ((java.sql.Date) result.get("expirationdate")).toLocalDate();
      productLot = new ProductLot(code, expirationDateInDb);
      productLot.setId((UUID) result.get("id"));
      return productLot;
    }
    String lotCreate = "INSERT INTO referencedata.lots"
        + "(id, lotcode, expirationdate, manufacturedate, tradeitemid, active) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, true) RETURNING CAST(id AS VARCHAR)";
    Query insert = em.createNativeQuery(lotCreate);
    insert.setParameter(1, lotCode);
    insert.setParameter(2, expirationDate);
    insert.setParameter(3, expirationDate);
    insert.setParameter(4, tradeItemId);
    productLot.setId(UUID.fromString((String) insert.getSingleResult()));
    return productLot;
  }

  public StockEvent createStockEvent(StockEvent stockEvent) {
    String sql = "INSERT INTO stockmanagement.stock_events"
        + "(id, facilityid, programid, processeddate, signature, userid) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, ?5) RETURNING CAST(id AS VARCHAR)";
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, stockEvent.getFacilityId());
    insert.setParameter(2, stockEvent.getProgramId());
    insert.setParameter(3, stockEvent.getProcessedAt());
    insert.setParameter(4, stockEvent.getSignature());
    insert.setParameter(5, wrap(stockEvent.getUserId()));
    stockEvent.setId(UUID.fromString((String) insert.getSingleResult()));
    return stockEvent;
  }

  public UUID createStockEventLine(StockEvent stockEvent, StockCard stockCard, StockEventLineDetail detail) {
    String sql = "INSERT INTO stockmanagement.stock_event_line_items"
        + "(id, stockeventid, orderableid, lotid, occurreddate, quantity, sourceid, destinationid, reasonid, "
        + "extradata) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9) RETURNING CAST(id AS VARCHAR)";
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, wrap(stockEvent.getId()));
    insert.setParameter(2, wrap(stockCard.getProductId()));
    insert.setParameter(3, wrap(stockCard.getLotId()));
    insert.setParameter(4, detail.getEventTime().getOccurredDate());
    insert.setParameter(5, Math.abs(detail.getQuantity()));
    insert.setParameter(6, wrap(detail.getSourceId()));
    insert.setParameter(7, wrap(detail.getDestinationId()));
    insert.setParameter(8, wrap(detail.getReasonId()));
    String extraData = generateExtraData(stockCard, detail);
    insert.setParameter(9, extraData);
    return UUID.fromString((String) insert.getSingleResult());
  }

  public StockCard getStockCard(StockCard stockCard) {
    UUID lotId = stockCard.getLotId();
    String sql;
    if (lotId != null) {
      sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.stock_cards "
          + "WHERE facilityid = ?1 "
          + "AND programid = ?2 "
          + "AND orderableid = ?3 "
          + "AND lotid = ?4 ";
    } else {
      sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.stock_cards "
          + "WHERE facilityid = ?1 "
          + "AND programid = ?2 "
          + "AND orderableid = ?3 "
          + "AND lotid IS NULL ";
    }
    Query query = em.createNativeQuery(sql);
    query.setParameter(1, wrap(stockCard.getFacilityId()));
    query.setParameter(2, wrap(stockCard.getProgramId()));
    query.setParameter(3, wrap(stockCard.getProductId()));
    if (lotId != null) {
      query.setParameter(4, wrap(lotId));
    }
    @SuppressWarnings("unchecked")
    List<String> list = query.getResultList();
    if (list.isEmpty()) {
      return null;
    }
    stockCard.setId(UUID.fromString(list.get(0)));
    return stockCard;
  }

  public StockCard createStockCard(StockCard stockCard) {
    String sql = "INSERT INTO stockmanagement.stock_cards"
        + "(id, facilityid, programid, orderableid, lotid, origineventid) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, ?5) RETURNING CAST(id AS VARCHAR)";
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, wrap(stockCard.getFacilityId()));
    insert.setParameter(2, wrap(stockCard.getProgramId()));
    insert.setParameter(3, wrap(stockCard.getProductId()));
    insert.setParameter(4, wrap(stockCard.getLotId()));
    insert.setParameter(5, wrap(stockCard.getEventId()));
    stockCard.setId(UUID.fromString((String) insert.getSingleResult()));
    return stockCard;
  }

  public UUID createStockCardLine(StockEvent stockEvent, StockCard stockCard, StockEventLineDetail detail) {
    String sql = "INSERT INTO stockmanagement.stock_card_line_items"
        + "(id, stockcardid, origineventid, occurreddate, processeddate, quantity, sourceid, destinationid, reasonid, "
        + "userid, extradata, documentnumber, signature) "
        + "VALUES (UUID_GENERATE_V4(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
    LocalDate occurredDate = detail.getEventTime().getOccurredDate();
    Timestamp recordedAt =
        detail.getEventTime().getRecordedAt() == null ? null : Timestamp.from(detail.getEventTime().getRecordedAt());
    String extraData = generateExtraData(stockCard, detail);
    List<Map<String, Object>> resultList = jdbc
        .queryForList(sql, stockCard.getId(), stockEvent.getId(), occurredDate, recordedAt,
            Math.abs(detail.getQuantity()), detail.getSourceId(), detail.getDestinationId(), detail.getReasonId(),
            stockEvent.getUserId(), extraData, detail.getDocumentNumber(), stockEvent.getSignature());
    return (UUID) resultList.get(0).get("id");
  }

  public PhysicalInventory getPhysicalInventory(PhysicalInventory physicalInventory) {
    String sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.physical_inventories "
        + "WHERE facilityid = ?1 "
        + "AND programid = ?2 "
        + "AND stockeventid = ?3 "
        + "AND occurreddate = ?4 ";
    Query query = em.createNativeQuery(sql);
    query.setParameter(1, wrap(physicalInventory.getFacilityId()));
    query.setParameter(2, wrap(physicalInventory.getProgramId()));
    query.setParameter(3, wrap(physicalInventory.getEventId()));
    query.setParameter(4, physicalInventory.getOccurredDate());
    @SuppressWarnings("unchecked")
    List<String> list = query.getResultList();
    if (list.isEmpty()) {
      return null;
    }
    physicalInventory.setId(UUID.fromString(list.get(0)));
    return physicalInventory;
  }

  public PhysicalInventory createPhysicalInventory(PhysicalInventory physicalInventory) {
    String sql = "INSERT INTO stockmanagement.physical_inventories"
        + "(id, facilityid, programid, stockeventid, occurreddate, signature, isdraft) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, ?5, false) "
        + "RETURNING CAST(id AS VARCHAR)";
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, wrap(physicalInventory.getFacilityId()));
    insert.setParameter(2, wrap(physicalInventory.getProgramId()));
    insert.setParameter(3, wrap(physicalInventory.getEventId()));
    insert.setParameter(4, physicalInventory.getOccurredDate());
    insert.setParameter(5, physicalInventory.getSignature());
    physicalInventory.setId(UUID.fromString((String) insert.getSingleResult()));
    return physicalInventory;
  }

  public void createPhysicalInventoryLine(PhysicalInventoryLineDetail lineDetail, UUID eventLineId,
      UUID stockCardLineId) {
    String insertInventoryLineSql = "INSERT INTO stockmanagement.physical_inventory_line_items"
        + "(id, orderableid, lotid, quantity, physicalinventoryid, previousstockonhandwhensubmitted) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, ?5) RETURNING CAST(id AS VARCHAR)";
    Query insert = em.createNativeQuery(insertInventoryLineSql);
    insert.setParameter(1, wrap(lineDetail.getProductId()));
    insert.setParameter(2, wrap(lineDetail.getLotId()));
    insert.setParameter(3, lineDetail.getAdjustment());
    insert.setParameter(4, wrap(lineDetail.getPhysicalInventoryId()));
    insert.setParameter(5, lineDetail.getInventoryBeforeAdjustment());
    UUID inventoryLineId = UUID.fromString((String) insert.getSingleResult());
    String insertAdjustmentSql = "INSERT INTO stockmanagement.physical_inventory_line_item_adjustments"
        + "(id, quantity, reasonid, physicalinventorylineitemid) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3)";
    Query adjustmentInsert = em.createNativeQuery(insertAdjustmentSql);
    adjustmentInsert.setParameter(1, lineDetail.getAdjustment());
    adjustmentInsert.setParameter(2, wrap(lineDetail.getReasonId()));
    adjustmentInsert.setParameter(3, wrap(inventoryLineId));
    adjustmentInsert.executeUpdate();
    String insertAdjustmentSql2 = "INSERT INTO stockmanagement.physical_inventory_line_item_adjustments"
        + "(id, quantity, reasonid, stockeventlineitemid) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3)";
    Query adjustmentInsert2 = em.createNativeQuery(insertAdjustmentSql2);
    adjustmentInsert2.setParameter(1, lineDetail.getAdjustment());
    adjustmentInsert2.setParameter(2, wrap(lineDetail.getReasonId()));
    adjustmentInsert2.setParameter(3, wrap(eventLineId));
    adjustmentInsert2.executeUpdate();
    String insertAdjustmentSql3 = "INSERT INTO stockmanagement.physical_inventory_line_item_adjustments"
        + "(id, quantity, reasonid, stockcardlineitemid) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3)";
    Query adjustmentInsert3 = em.createNativeQuery(insertAdjustmentSql3);
    adjustmentInsert3.setParameter(1, lineDetail.getAdjustment());
    adjustmentInsert3.setParameter(2, wrap(lineDetail.getReasonId()));
    adjustmentInsert3.setParameter(3, wrap(stockCardLineId));
    adjustmentInsert3.executeUpdate();
  }

  public void saveRequested(StockEvent stockEvent, UUID productId, Integer requested) {
    String sql = "INSERT INTO siglusintegration.stock_event_product_requested"
        + "(id, orderableid, stockeventid, requestedquantity) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3)";
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, wrap(productId));
    insert.setParameter(2, wrap(stockEvent.getId()));
    insert.setParameter(3, requested);
    insert.executeUpdate();
  }

  public void saveStockOnHand(CalculatedStockOnHand calculated) {
    String sql = "INSERT INTO stockmanagement.calculated_stocks_on_hand"
        + "(id, stockonhand, occurreddate, stockcardid, processeddate) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4)";
    Query insert = em.createNativeQuery(sql);
    InventoryDetail inventoryDetail = calculated.getInventoryDetail();
    insert.setParameter(1, inventoryDetail.getStockQuantity());
    insert.setParameter(2, inventoryDetail.getEventTime().getOccurredDate());
    insert.setParameter(3, wrap(calculated.getStockCard().getId()));
    insert.setParameter(4, inventoryDetail.getEventTime().getRecordedAt());
    insert.executeUpdate();
  }

  @SneakyThrows
  private String generateExtraData(StockCard stockCard, StockEventLineDetail detail) {
    Map<String, String> extraData = new LinkedHashMap<>();
    if (stockCard.getLotCode() != null) {
      extraData.put("lotCode", stockCard.getLotCode());
    }
    if (stockCard.getExpirationDate() != null) {
      extraData.put("expirationDate", stockCard.getExpirationDate().toString());
    }
    if (detail.getEventTime().getRecordedAt() != null) {
      extraData.put("originEventTime", detail.getEventTime().getRecordedAt().toString());
    }
    return json.writeValueAsString(extraData);
  }

  private TypedParameterValue wrap(UUID id) {
    return new TypedParameterValue(StandardBasicTypes.UUID_CHAR, id);
  }

  public StocksOnHand getStockOnHand(@Nonnull UUID facilityId) {
    return getStockOnHand(facilityId, null, emptySet());
  }

  @ParametersAreNullableByDefault
  private StocksOnHand getStockOnHand(@Nonnull UUID facilityId, LocalDate at, @Nonnull Set<UUID> orderableIds) {
    return new StocksOnHand(findAllLotStocks(facilityId, at, orderableIds));
  }

  @ParametersAreNullableByDefault
  private List<ProductLotMovement> findAllLotMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till,
      @Nonnull Set<UUID> orderableIds) {
    requireNonNull(facilityId, "facilityId should not be null");
    List<Object> params = new ArrayList<>(2);
    String sql = generateMovementQuery(facilityId, since, till, params, orderableIds);
    return executeQuery(sql, params, buildProductLotMovementFromResult());
  }

  private Function<Object[], ProductLotMovement> buildProductLotMovementFromResult() {
    return arr -> ProductLotMovement.builder()
        .code(ProductLotCode.of((String) arr[0], (String) arr[1]))
        .eventTime(EventTime.fromDatabase((java.sql.Date) arr[2], (String) arr[3], (Timestamp) arr[15]))
        .movementDetail(MovementDetail.builder()
            .unsignedAdjustment((Integer) arr[4])
            .source((String) arr[5])
            .destination((String) arr[6])
            .adjustmentReason((String) arr[7])
            .adjustmentReasonType((String) arr[8])
            .inventoryReason((String) arr[9])
            .inventoryReasonType((String) arr[10])
            .unsignedInventoryAdjustment((Integer) arr[11])
            .build()
        )
        .requestedQuantity((Integer) arr[12])
        .signature((String) arr[13])
        .documentNumber((String) arr[14])
        .lot(Lot.of((String) arr[1], (java.sql.Date) arr[16]))
        .build();
  }

  @ParametersAreNullableByDefault
  private List<ProductLotStock> findAllLotStocks(@Nonnull UUID facilityId, LocalDate at,
      @Nonnull Set<UUID> orderableIds) {
    requireNonNull(facilityId, "facilityId should not be null");
    String select = "SELECT DISTINCT ON (root.stockcardid) o.code AS productcode, l.lotcode, "
        + "root.stockonhand, root.occurreddate, li.extradata \\:\\: json ->> 'originEventTime' as recordedat, "
        + "li.processeddate, l.expirationdate ";
    String root = "stockmanagement.calculated_stocks_on_hand root";
    List<Object> params = new ArrayList<>(2);
    String where = generateWhere(facilityId, null, at, params, orderableIds);
    String orderBy = "ORDER BY root.stockcardid, root.occurreddate DESC, root.processeddate DESC, "
        + "li.occurreddate DESC, recordedat DESC";
    String sql = select + "FROM " + root + ' '
        + LEFT_JOIN + STOCK_CARD_ROOT + " on root.stockcardid = sc.id "
        + LEFT_JOIN + ORDERABLE_ROOT + " on sc.orderableid = o.id "
        + LEFT_JOIN + LOT_ROOT + " on sc.lotid = l.id "
        + "LEFT JOIN stockmanagement.stock_card_line_items li ON "
        + "li.stockcardid = sc.id AND li.occurreddate = root.occurreddate "
        + where
        + orderBy;
    return executeQuery(sql, params,
        (arr -> ProductLotStock.builder()
            .code(ProductLotCode.of((String) arr[0], (String) arr[1]))
            .stockQuantity((Integer) arr[2])
            .eventTime(EventTime.fromDatabase((java.sql.Date) arr[3], (String) arr[4], (Timestamp) arr[5]))
            .expirationDate((java.sql.Date) arr[6])
            .build()
        )
    );
  }

  private <T> List<T> executeQuery(String sql, List<Object> params, Function<Object[], T> transformer) {
    Query nativeQuery = em.createNativeQuery(sql);
    for (int i = 0, paramsSize = params.size(); i < paramsSize; i++) {
      Object param = params.get(i);
      nativeQuery.setParameter(i, param);
    }
    // there is no type-safe way in Hibernate 5.0
    @SuppressWarnings("unchecked")
    List<T> resultList = ((List<Object[]>) nativeQuery.getResultList()).stream().map(transformer).collect(toList());
    return resultList;
  }

  @SuppressWarnings("PMD.ConsecutiveLiteralAppends")
  private static String generateWhere(@Nonnull UUID facilityId, @Nullable LocalDate since, @Nullable LocalDate at,
      @Nonnull List<Object> params, @Nonnull Set<UUID> orderableIds) {
    StringBuilder where = new StringBuilder("WHERE sc.facilityid = ?0 ");
    params.add(facilityId);
    if (since != null) {
      where.append(' ').append("AND root.occurreddate >= ?").append(params.size()).append(' ');
      params.add(since);
    }
    if (at != null) {
      where.append(' ').append("AND root.occurreddate <= ?").append(params.size()).append(' ');
      params.add(at);
    }
    if (!orderableIds.isEmpty()) {
      where.append(' ').append("AND sc.orderableid in ?").append(params.size()).append(' ');
      params.add(orderableIds);
    }
    return where.toString();
  }

  private String generateMovementQuery(@Nonnull UUID facilityId, @Nullable LocalDate since, @Nullable LocalDate at,
      @Nonnull List<Object> params, @Nonnull Set<UUID> orderableIds) {
    String select = "SELECT o.code AS productcode, "
        + "l.lotcode, "
        + "root.occurreddate, "
        + "root.extradata \\:\\: json ->> 'originEventTime' as recordedat, "
        + "root.quantity, "
        + "srcorg.name AS srcname, "
        + "destorg.name AS destname, "
        + "reason.name AS adjustreason, "
        + "reason.reasontype AS adjustreasontype, "
        + "adjstreason.name AS inventoryReason, "
        + "adjstreason.reasontype AS inventoryReasontype,"
        + "pilia.quantity inventoryadjustment,"
        + "requested.requestedquantity, "
        + "root.signature, "
        + "root.documentnumber, "
        + "root.processeddate, "
        + "l.expirationdate ";
    String root = "stockmanagement.stock_card_line_items root";
    String eventRoot = "stockmanagement.stock_events se";
    String srcNodeRoot = "stockmanagement.nodes srcnode";
    String srcOrgRoot = "stockmanagement.organizations srcorg";
    String destNodeRoot = "stockmanagement.nodes destnode";
    String destOrgRoot = "stockmanagement.organizations destorg";
    String reasonRoot = "stockmanagement.stock_card_line_item_reasons reason";
    String lineAdjRoot = "stockmanagement.physical_inventory_line_item_adjustments pilia";
    String adjustmentReasonRoot = "stockmanagement.stock_card_line_item_reasons adjstreason";
    String requestedRoot = "siglusintegration.stock_event_product_requested requested";
    String where = generateWhere(facilityId, since, at, params, orderableIds);
    return select
        + "FROM " + root + ' '
        + LEFT_JOIN + STOCK_CARD_ROOT + " ON root.stockcardid = sc.id "
        + LEFT_JOIN + eventRoot + " ON sc.origineventid = se.id "
        + LEFT_JOIN + ORDERABLE_ROOT + " ON sc.orderableid = o.id "
        + LEFT_JOIN + LOT_ROOT + " ON sc.lotid = l.id "
        + LEFT_JOIN + srcNodeRoot + " ON root.sourceid = srcnode.id "
        + LEFT_JOIN + srcOrgRoot + " ON srcnode.referenceid = srcorg.id "
        + LEFT_JOIN + destNodeRoot + " ON root.destinationid = destnode.id "
        + LEFT_JOIN + destOrgRoot + " ON destnode.referenceid = destorg.id "
        + LEFT_JOIN + reasonRoot + " ON root.reasonid = reason.id "
        + LEFT_JOIN + lineAdjRoot + " ON pilia.stockcardlineitemid = root.id "
        + LEFT_JOIN + adjustmentReasonRoot + " ON pilia.reasonid = adjstreason.id "
        + LEFT_JOIN + requestedRoot
        + " on requested.stockeventid = root.origineventid and requested.orderableid = o.id "
        + where;
  }

  private ProductMovement toProductMovement(ProductMovementKey key, List<ProductLotMovement> productLotMovements,
      Map<String, Integer> productInventoryMap, Map<ProductLotCode, Integer> lotInventories) {
    ProductMovementBuilder movementBuilder = ProductMovement.builder()
        .productCode(key.getProductCode())
        .eventTime(key.getEventTime());
    Integer requestedQuantity = productLotMovements.stream().findAny().map(ProductLotMovement::getRequestedQuantity)
        .orElse(null);
    movementBuilder.requestedQuantity(requestedQuantity);
    ProductLotMovement anyLot;
    if (productLotMovements.size() == 1 && !productLotMovements.get(0).getCode().isLot()) {
      ProductLotMovement theOnlyLot = productLotMovements.get(0);
      MovementDetail movementDetail = theOnlyLot.getMovementDetail();
      movementBuilder.movementDetail(movementDetail);
      Integer stockQuantity = productInventoryMap.get(key.getProductCode());
      movementBuilder.stockQuantity(stockQuantity);
      productInventoryMap.put(key.getProductCode(), stockQuantity - movementDetail.getAdjustment());
      movementBuilder.documentNumber(theOnlyLot.getDocumentNumber());
      anyLot = theOnlyLot;
    } else if (productLotMovements.stream().allMatch(m -> m.getCode().isLot())) {
      List<LotMovement> lotMovements = productLotMovements.stream()
          .map(m -> toLotMovement(m, lotInventories))
          .sorted(comparing(m -> m.getLot().getCode()))
          .collect(toList());
      movementBuilder.lotMovements(lotMovements);
      MovementDetail movementDetail = productLotMovements.stream()
          .map(ProductLotMovement::getMovementDetail)
          .reduce(MovementDetail::merge)
          .orElseThrow(IllegalStateException::new);
      Integer stockQuantity = productInventoryMap.get(key.getProductCode());
      movementBuilder.stockQuantity(stockQuantity);
      productInventoryMap.put(key.getProductCode(), stockQuantity - movementDetail.getAdjustment());
      movementBuilder.movementDetail(movementDetail);
      anyLot = productLotMovements.stream().findAny().orElseThrow(IllegalStateException::new);
      String documentNumber = anyLot.getDocumentNumber();
      if (productLotMovements.stream().map(ProductLotMovement::getDocumentNumber)
          .allMatch(n -> Objects.equals(n, documentNumber))) {
        movementBuilder.documentNumber(documentNumber);
      }
    } else {
      throw new IllegalStateException("dirty data");
    }
    movementBuilder.signature(anyLot.getSignature());
    return movementBuilder.build();
  }

  private LotMovement toLotMovement(ProductLotMovement movement, Map<ProductLotCode, Integer> lotInventories) {
    MovementDetail movementDetail = movement.getMovementDetail();
    Integer stockQuantity = lotInventories.get(movement.getCode());
    lotInventories.put(movement.getCode(), stockQuantity - movementDetail.getAdjustment());
    return LotMovement.builder()
        .lot(movement.getLot())
        .movementDetail(movementDetail)
        .stockQuantity(stockQuantity)
        .documentNumber(movement.getDocumentNumber())
        .build();
  }

  @EqualsAndHashCode
  @ToString
  @Getter
  @Builder
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ProductLotMovement implements EventTimeContainer {

    private final ProductLotCode code;

    private final Lot lot;

    private final EventTime eventTime;

    private final MovementDetail movementDetail;

    @Nullable
    private final Integer requestedQuantity;

    private final String signature;

    @Nullable
    private final String documentNumber;

    public ProductMovementKey getProductMovementKey() {
      return ProductMovementKey.of(code.getProductCode(), eventTime);
    }

  }

}
