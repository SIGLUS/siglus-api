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
import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
import org.siglus.common.constant.KitConstants;
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
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class StockManagementRepository {

  private final EntityManager em;
  private final ObjectMapper json;
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

  private static final String LEFT_JOIN = "LEFT JOIN ";
  private static final String STOCK_CARD_ROOT = "stockmanagement.stock_cards sc";
  private static final String ORDERABLE_ROOT =
      "(select distinct on (id) * from referencedata.orderables order by id, versionnumber desc) o";
  private static final String LOT_ROOT = "referencedata.lots l";

  private static final String INSERT_5_FIELDS =
      "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, ?5) RETURNING CAST(id AS VARCHAR)";

  public PeriodOfProductMovements getAllProductMovementsForSync(@Nonnull UUID facilityId, @Nonnull LocalDate since) {
    requireNonNull(facilityId);
    ZoneId zoneId = ZoneId.systemDefault();
    return getAllProductMovements(facilityId, null, null, emptySet(), since.atStartOfDay(zoneId).toInstant(),
        LocalDate.now().atStartOfDay(zoneId).toInstant());
  }

  @ParametersAreNullableByDefault
  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since) {
    requireNonNull(facilityId);
    return getAllProductMovements(facilityId, since, null, emptySet(), null, null);
  }

  @ParametersAreNullableByDefault
  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till) {
    requireNonNull(facilityId);
    return getAllProductMovements(facilityId, since, till, emptySet(), null, null);
  }

  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, @Nonnull Set<UUID> orderableIds) {
    requireNonNull(facilityId);
    requireNonNull(orderableIds);
    return getAllProductMovements(facilityId, null, null, orderableIds, null, null);
  }

  @ParametersAreNullableByDefault
  private PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till,
      @Nonnull Set<UUID> orderableIds, Instant syncSince, Instant syncTill) {
    List<ProductLotMovement> allLotMovements = findAllLotMovements(facilityId, since, till, orderableIds, syncSince,
        syncTill);
    StocksOnHand stocksOnHand = getStockOnHand(facilityId, till, orderableIds, syncSince, syncTill);
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

  public StockEvent createStockEvent(StockEvent stockEvent) {
    String sql = "INSERT INTO stockmanagement.stock_events"
        + "(id, facilityid, programid, processeddate, signature, userid) "
        + INSERT_5_FIELDS;
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

  public ProductLot getLot(OrderableDto product, Lot lot) {
    String lotCode = lot.getCode();
    String tradeItemId = product.getIdentifiers().get(TRADE_ITEM);
    ProductLotCode code = ProductLotCode.of(product.getProductCode(), lotCode);
    String lotQuery = "SELECT id, lotcode, expirationdate FROM referencedata.lots "
        + "WHERE lotcode = ? AND tradeitemid = ?";
    return jdbc.query(lotQuery, productLotExtractor(code), lotCode, tradeItemId);
  }

  public ProductLot createLot(OrderableDto product, Lot lot) {
    String tradeItemId = product.getIdentifiers().get(TRADE_ITEM);
    String lotCode = lot.getCode();
    ProductLotCode code = ProductLotCode.of(product.getProductCode(), lotCode);
    String lotCreate = "INSERT INTO referencedata.lots"
        + "(id, lotcode, expirationdate, manufacturedate, tradeitemid, active) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4, true) RETURNING CAST(id AS VARCHAR)";
    LocalDate expirationDate = lot.getExpirationDate();
    Query insert = em.createNativeQuery(lotCreate);
    insert.setParameter(1, lotCode);
    insert.setParameter(2, expirationDate);
    insert.setParameter(3, expirationDate);
    insert.setParameter(4, tradeItemId);
    ProductLot productLot = new ProductLot(code, expirationDate);
    productLot.setId(UUID.fromString((String) insert.getSingleResult()));
    return productLot;
  }

  public StockCard getStockCard(StockCard stockCard) {
    UUID lotId = stockCard.getLotId();
    String sql;
    if (lotId != null) {
      sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.stock_cards "
          + "WHERE facilityid = ?1 AND programid = ?2 AND orderableid = ?3 AND lotid = ?4 ";
    } else {
      sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.stock_cards "
          + "WHERE facilityid = ?1 AND programid = ?2 AND orderableid = ?3 AND lotid IS NULL ";
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

  public StockCard createStockCard(StockCard stockCard, UUID stockEventId) {
    String sql = "INSERT INTO stockmanagement.stock_cards"
        + "(id, facilityid, programid, orderableid, lotid, origineventid) "
        + INSERT_5_FIELDS;
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, wrap(stockCard.getFacilityId()));
    insert.setParameter(2, wrap(stockCard.getProgramId()));
    insert.setParameter(3, wrap(stockCard.getProductId()));
    insert.setParameter(4, wrap(stockCard.getLotId()));
    insert.setParameter(5, wrap(stockEventId));
    stockCard.setId(UUID.fromString((String) insert.getSingleResult()));
    return stockCard;
  }

  public UUID createStockCardLine(StockEvent stockEvent, StockCard stockCard, StockEventLineDetail detail) {
    String sql = "INSERT INTO stockmanagement.stock_card_line_items"
        + "(id, stockcardid, origineventid, occurreddate, processeddate, quantity, sourceid, destinationid, reasonid, "
        + "userid, extradata, documentnumber, signature) "
        + "VALUES (UUID_GENERATE_V4(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
    LocalDate occurredDate = detail.getEventTime().getOccurredDate();
    Timestamp recordedAt = Timestamp.from(stockEvent.getProcessedAt());
    String extraData = generateExtraData(stockCard, detail);
    List<Map<String, Object>> resultList = jdbc
        .queryForList(sql, stockCard.getId(), stockEvent.getId(), occurredDate, recordedAt,
            Math.abs(detail.getQuantity()), detail.getSourceId(), detail.getDestinationId(), detail.getReasonId(),
            stockEvent.getUserId(), extraData, detail.getDocumentNumber(), stockEvent.getSignature());
    return (UUID) resultList.get(0).get("id");
  }

  public PhysicalInventory getPhysicalInventory(PhysicalInventory physicalInventory) {
    String sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.physical_inventories "
        + "WHERE facilityid = ?1 AND programid = ?2 AND stockeventid = ?3 AND occurreddate = ?4 ";
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
        + INSERT_5_FIELDS;
    Query insert = em.createNativeQuery(insertInventoryLineSql);
    insert.setParameter(1, wrap(lineDetail.getProductId()));
    insert.setParameter(2, wrap(lineDetail.getLotId()));
    insert.setParameter(3, lineDetail.getAdjustment());
    insert.setParameter(4, wrap(lineDetail.getPhysicalInventoryId()));
    insert.setParameter(5, lineDetail.getInventoryBeforeAdjustment());
    UUID inventoryLineId = UUID.fromString((String) insert.getSingleResult());
    String insertAdjustmentSqlTemplate = "INSERT INTO stockmanagement.physical_inventory_line_item_adjustments"
        + "(id, quantity, reasonid, %s) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3)";
    ImmutableMap.of(
        "physicalinventorylineitemid", inventoryLineId,
        "stockeventlineitemid", eventLineId,
        "stockcardlineitemid", stockCardLineId
    ).forEach((k, v) -> {
      String sql = String.format(insertAdjustmentSqlTemplate, k);
      Query adjustmentInsert = em.createNativeQuery(sql);
      adjustmentInsert.setParameter(1, lineDetail.getAdjustment());
      adjustmentInsert.setParameter(2, wrap(lineDetail.getReasonId()));
      adjustmentInsert.setParameter(3, wrap(v));
      adjustmentInsert.executeUpdate();
    });
  }

  public void saveRequested(StockEvent stockEvent, UUID productId, Integer requested) {
    if (requested == null) {
      return;
    }
    String sql = "INSERT INTO siglusintegration.stock_event_product_requested"
        + "(id, orderableid, stockeventid, requestedquantity) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3)";
    Query insert = em.createNativeQuery(sql);
    insert.setParameter(1, wrap(productId));
    insert.setParameter(2, wrap(stockEvent.getId()));
    insert.setParameter(3, requested);
    insert.executeUpdate();
  }

  public List<CalculatedStockOnHand> findCalculatedStockOnHand(StockCard stockCard, EventTime earliestDate) {
    String sql = "SELECT id, stockonhand, occurreddate, processeddate FROM stockmanagement.calculated_stocks_on_hand "
        + "WHERE stockcardid = ? AND occurreddate>= ?";
    return jdbc
        .query(sql, calculatedStockOnHandExtractor(stockCard), stockCard.getId(), earliestDate.getOccurredDate());
  }

  public void saveStockOnHand(CalculatedStockOnHand calculated, Instant stockOnHandProcessAt) {
    Integer stockQuantity = calculated.getInventoryDetail().getStockQuantity();
    if (calculated.getId() != null) {
      jdbc.update("UPDATE stockmanagement.calculated_stocks_on_hand "
              + "SET stockonhand = ?, processeddate = ? WHERE id = ?",
          stockQuantity, Timestamp.from(stockOnHandProcessAt), calculated.getId());
      return;
    }
    String sql = "INSERT INTO stockmanagement.calculated_stocks_on_hand"
        + "(id, stockonhand, occurreddate, stockcardid, processeddate) "
        + "VALUES (UUID_GENERATE_V4(), ?1, ?2, ?3, ?4)";
    Query insert = em.createNativeQuery(sql);
    InventoryDetail inventoryDetail = calculated.getInventoryDetail();
    insert.setParameter(1, inventoryDetail.getStockQuantity());
    insert.setParameter(2, inventoryDetail.getEventTime().getOccurredDate());
    insert.setParameter(3, wrap(calculated.getStockCard().getId()));
    insert.setParameter(4, stockOnHandProcessAt);
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
    return getStockOnHand(facilityId, null, emptySet(), null, null);
  }

  public StocksOnHand getStockOnHand(@Nonnull UUID facilityId, @Nonnull LocalDate at) {
    return getStockOnHand(facilityId, at, emptySet(), null, null);
  }

  @ParametersAreNullableByDefault
  private StocksOnHand getStockOnHand(@Nonnull UUID facilityId, LocalDate at, @Nonnull Set<UUID> orderableIds,
      Instant syncSince, Instant syncTill) {
    return new StocksOnHand(findAllLotStocks(facilityId, at, orderableIds, syncSince, syncTill));
  }

  @ParametersAreNullableByDefault
  private List<ProductLotMovement> findAllLotMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till,
      @Nonnull Set<UUID> orderableIds, Instant syncSince, Instant syncTill) {
    requireNonNull(facilityId, "facilityId should not be null");
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    String sql = generateMovementQuery(facilityId, parameters, since, till, orderableIds, syncSince, syncTill);
    return executeQuery(sql, parameters, buildProductLotMovementFromResult());
  }

  private ResultSetExtractor<ProductLot> productLotExtractor(ProductLotCode code) {
    return rs -> {
      if (!rs.next()) {
        return null;
      }
      Lot lot = readLot(rs);
      ProductLot existed = new ProductLot(code, lot.getExpirationDate());
      existed.setId(readId(rs));
      return existed;
    };
  }

  private RowMapper<CalculatedStockOnHand> calculatedStockOnHandExtractor(StockCard stockCard) {
    return (rs, i) -> {
      CalculatedStockOnHand stockOnHand = CalculatedStockOnHand
          .of(stockCard, InventoryDetail.of(readAsInt(rs, "stockonhand"), readEventTime(rs)));
      stockOnHand.setId(readId(rs));
      return stockOnHand;
    };
  }

  private RowMapper<ProductLotMovement> buildProductLotMovementFromResult() {
    return (rs, i) -> ProductLotMovement.builder()
        .code(readProductLotCode(rs))
        .eventTime(readEventTime(rs))
        .movementDetail(readMovementDetail(rs))
        .requestedQuantity(readAsInt(rs, "requestedquantity"))
        .signature(readAsString(rs, "signature"))
        .documentNumber(readAsString(rs, "documentnumber"))
        .lot(readLot(rs))
        .build();
  }

  @ParametersAreNullableByDefault
  private List<ProductLotStock> findAllLotStocks(@Nonnull UUID facilityId, LocalDate at,
      @Nonnull Set<UUID> orderableIds, Instant syncSince, Instant syncTill) {
    requireNonNull(facilityId, "facilityId should not be null");
    String select =
        "SELECT DISTINCT ON (root.stockcardid) o.code AS productcode, o.fullproductname AS productname, l.lotcode, "
            + "root.stockonhand, root.occurreddate, li.extradata :: json ->> 'originEventTime' as recordedat, "
            + "li.processeddate, l.expirationdate ";
    String root = "stockmanagement.calculated_stocks_on_hand root";
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    String where = generateWhere(facilityId, parameters, null, at, orderableIds, syncSince, syncTill);
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
    return executeQuery(sql, parameters,
        ((rs, i) -> ProductLotStock.builder()
            .code(readProductLotCode(rs))
            .productName(readAsString(rs, "productname"))
            .stockQuantity(readAsInt(rs, "stockonhand"))
            .eventTime(readEventTime(rs))
            .expirationDate(readAsDate(rs, "expirationdate"))
            .build()
        )
    );
  }

  @SneakyThrows
  private UUID readId(ResultSet rs) {
    return rs.getObject("id", UUID.class);
  }

  @SneakyThrows
  private Integer readAsInt(ResultSet rs, String columnName) {
    int value = rs.getInt(columnName);
    if (rs.wasNull()) {
      return null;
    }
    return value;
  }

  @SneakyThrows
  private String readAsString(ResultSet rs, String columnName) {
    ResultSetMetaData rsMetaData = rs.getMetaData();
    int numberOfColumns = rsMetaData.getColumnCount();
    for (int i = 1; i < numberOfColumns + 1; i++) {
      if (columnName.equals(rsMetaData.getColumnName(i))) {
        return rs.getString(columnName);
      }
    }
    return null;
  }

  @SneakyThrows
  private java.sql.Date readAsDate(ResultSet rs, String columnName) {
    return rs.getDate(columnName);
  }

  @SneakyThrows
  private EventTime readEventTime(ResultSet rs) {
    java.sql.Date occurredDate = readAsDate(rs, "occurreddate");
    String recordedAtStr = readAsString(rs, "recordedat");
    Timestamp processedAtTs = rs.getTimestamp("processeddate");
    return EventTime.fromDatabase(occurredDate, recordedAtStr, processedAtTs);
  }

  @SneakyThrows
  private MovementDetail readMovementDetail(ResultSet rs) {
    return MovementDetail.builder()
        .unsignedAdjustment(readAsInt(rs, "quantity"))
        .sourceName(readAsString(rs, "srcname"))
        .sourceFacilityName(readAsString(rs, "srcfacname"))
        .destinationName(readAsString(rs, "destname"))
        .destinationFacilityName(readAsString(rs, "destfacname"))
        .adjustmentReason(readAsString(rs, "adjustreason"))
        .adjustmentReasonType(readAsString(rs, "adjustreasontype"))
        .inventoryReason(readAsString(rs, "inventoryReason"))
        .inventoryReasonType(readAsString(rs, "inventoryReasontype"))
        .unsignedInventoryAdjustment(readAsInt(rs, "inventoryadjustment"))
        .build();
  }

  @SneakyThrows
  private Lot readLot(ResultSet rs) {
    return Lot.of(readAsString(rs, "lotcode"), readAsDate(rs, "expirationdate"));
  }

  private ProductLotCode readProductLotCode(ResultSet rs) {
    return ProductLotCode.of(readAsString(rs, "productcode"), readAsString(rs, "lotcode"));
  }

  private <T> List<T> executeQuery(String sql, SqlParameterSource parameters, RowMapper<T> transformer) {
    return namedJdbc.query(sql, parameters, new RowMapperResultSetExtractor<>(transformer));
  }

  @SuppressWarnings("PMD.ConsecutiveLiteralAppends")
  private static String generateWhere(@Nonnull UUID facilityId, @Nonnull MapSqlParameterSource parameters,
      @Nullable LocalDate since, @Nullable LocalDate at, @Nonnull Set<UUID> orderableIds, Instant syncSince,
      Instant syncTill) {
    StringBuilder where = new StringBuilder(300);
    parameters.addValue("facilityId", facilityId);
    where.append("WHERE sc.facilityid = :facilityId ");
    if (since != null) {
      parameters.addValue("since", since);
      where.append(' ').append("AND root.occurreddate >= :since ");
    }
    if (at != null) {
      parameters.addValue("at", at);
      where.append(' ').append("AND root.occurreddate <= :at ");
    }
    if (!orderableIds.isEmpty()) {
      parameters.addValue("orderableIds", orderableIds);
      where.append(' ').append("AND sc.orderableid IN (:orderableIds) ");
    }
    if (syncSince != null) {
      parameters.addValue("syncSince", Timestamp.from(syncSince));
      where.append(' ').append("AND root.processeddate >= :syncSince ");
    }
    if (syncTill != null) {
      parameters.addValue("syncTill", Timestamp.from(syncTill));
      where.append(' ').append("AND root.processeddate < :syncTill ");
    }
    return where.toString();
  }

  private String generateMovementQuery(@Nonnull UUID facilityId, @Nonnull MapSqlParameterSource parameters,
      @Nullable LocalDate since, @Nullable LocalDate at, @Nonnull Set<UUID> orderableIds, Instant syncSince,
      Instant syncTill) {
    String select = "SELECT o.code AS productcode, "
        + "l.lotcode, "
        + "root.occurreddate, "
        + "root.extradata :: json ->> 'originEventTime' as recordedat, "
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
        + "l.expirationdate, "
        + "srcfac.name AS srcfacname, "
        + "destfac.name AS destfacname ";
    String root = "stockmanagement.stock_card_line_items root";
    String eventRoot = "stockmanagement.stock_events se";
    String srcNodeRoot = "stockmanagement.nodes srcnode";
    String srcOrgRoot = "stockmanagement.organizations srcorg";
    String srcFacilityRoot = "referencedata.facilities srcfac";
    String destNodeRoot = "stockmanagement.nodes destnode";
    String destOrgRoot = "stockmanagement.organizations destorg";
    String destFacilityRoot = "referencedata.facilities destfac";
    String reasonRoot = "stockmanagement.stock_card_line_item_reasons reason";
    String lineAdjRoot = "stockmanagement.physical_inventory_line_item_adjustments pilia";
    String adjustmentReasonRoot = "stockmanagement.stock_card_line_item_reasons adjstreason";
    String requestedRoot = "siglusintegration.stock_event_product_requested requested";
    String where = generateWhere(facilityId, parameters, since, at, orderableIds, syncSince, syncTill);
    return select
        + "FROM " + root + ' '
        + LEFT_JOIN + STOCK_CARD_ROOT + " ON root.stockcardid = sc.id "
        + LEFT_JOIN + eventRoot + " ON sc.origineventid = se.id "
        + LEFT_JOIN + ORDERABLE_ROOT + " ON sc.orderableid = o.id "
        + LEFT_JOIN + LOT_ROOT + " ON sc.lotid = l.id "
        + LEFT_JOIN + srcNodeRoot + " ON root.sourceid = srcnode.id "
        + LEFT_JOIN + srcOrgRoot + " ON srcnode.referenceid = srcorg.id "
        + LEFT_JOIN + srcFacilityRoot + " ON srcnode.referenceid = srcfac.id "
        + LEFT_JOIN + destNodeRoot + " ON root.destinationid = destnode.id "
        + LEFT_JOIN + destOrgRoot + " ON destnode.referenceid = destorg.id "
        + LEFT_JOIN + destFacilityRoot + " ON destnode.referenceid = destfac.id "
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
    if (isNoStock(productLotMovements)) {
      ProductLotMovement theOnlyLot = productLotMovements.get(0);
      MovementDetail movementDetail = theOnlyLot.getMovementDetail();
      movementBuilder.movementDetail(movementDetail);
      Integer stockQuantity = productInventoryMap.get(key.getProductCode());
      movementBuilder.stockQuantity(stockQuantity);
      productInventoryMap.put(key.getProductCode(), stockQuantity - movementDetail.getAdjustment());
      movementBuilder.documentNumber(theOnlyLot.getDocumentNumber());
      anyLot = theOnlyLot;
    } else if (isKit(key, productLotMovements) || productLotMovements.stream().allMatch(m -> m.getCode().isLot())) {
      if (!isKit(key, productLotMovements)) {
        List<LotMovement> lotMovements = productLotMovements.stream()
            .map(m -> toLotMovement(m, lotInventories))
            .sorted(comparing(m -> m.getLot().getCode()))
            .collect(toList());
        movementBuilder.lotMovements(lotMovements);
      }
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

  private boolean isKit(ProductMovementKey key, List<ProductLotMovement> productLotMovements) {
    return KitConstants.isKit(key.getProductCode()) && productLotMovements.stream().allMatch(m -> !m.getCode().isLot());
  }

  private boolean isNoStock(List<ProductLotMovement> productLotMovements) {
    return productLotMovements.size() == 1 && productLotMovements.get(0).getCode().isNoStock();
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
