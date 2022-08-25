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
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static org.openlmis.referencedata.domain.Orderable.TRADE_ITEM;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
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
import org.siglus.common.dto.StockOnHandByLotDto;
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
import org.siglus.siglusapi.dto.android.StockCardExtensionDto;
import org.siglus.siglusapi.dto.android.StocksOnHand;
import org.siglus.siglusapi.dto.android.db.CalculatedStockOnHand;
import org.siglus.siglusapi.dto.android.db.PhysicalInventory;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLine;
import org.siglus.siglusapi.dto.android.db.PhysicalInventoryLineAdjustment;
import org.siglus.siglusapi.dto.android.db.ProductLot;
import org.siglus.siglusapi.dto.android.db.RequestedQuantity;
import org.siglus.siglusapi.dto.android.db.StockCard;
import org.siglus.siglusapi.dto.android.db.StockCardLineItem;
import org.siglus.siglusapi.dto.android.db.StockEvent;
import org.siglus.siglusapi.dto.android.db.StockEventLineItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public class StockManagementRepository extends BaseNativeRepository {

  private final EntityManager em;
  private final ObjectMapper json;
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

  private static final String LEFT_JOIN = "LEFT JOIN ";
  private static final String STOCK_CARD_ROOT = "stockmanagement.stock_cards sc";
  private static final String ORDERABLE_ROOT =
      "(select distinct on (id) * from referencedata.orderables order by id, versionnumber desc) o";
  private static final String LOT_ROOT = "referencedata.lots l";

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

  public PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, @Nonnull Set<UUID> orderableIds,
      LocalDate since, LocalDate till) {
    return getAllProductMovements(facilityId, since, till, orderableIds, null, null);
  }


  public Map<UUID, Integer> getStockOnHandByProduct(UUID facilityId, UUID programId, Iterable<UUID> orderableIds,
      LocalDate asOfDate) {
    String sql = "SELECT DISTINCT ON (csoh.stockcardid) sc.orderableid, csoh.stockonhand \n"
        + "    FROM stockmanagement.calculated_stocks_on_hand csoh \n"
        + "    LEFT JOIN stockmanagement.stock_cards sc ON csoh.stockcardid = sc.id \n"
        + "    WHERE sc.facilityid = :facilityId \n"
        + "    AND sc.programid = :programId\n"
        + "    AND sc.orderableid in :orderableIds\n"
        + "    AND csoh.occurreddate <= :asOfDate \n"
        + "    ORDER BY csoh.stockcardid, csoh.occurreddate DESC";
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("facilityId", facilityId);
    parameters.addValue("programId", programId);
    parameters.addValue("orderableIds", orderableIds);
    parameters.addValue("asOfDate", asOfDate);
    List<StockOnHandByLotDto> result = executeQuery(sql, parameters,
        ((rs, rowNum) -> StockOnHandByLotDto.builder()
            .orderableId(readUuid(rs, "orderableid"))
            .stockOnHand(readAsInt(rs, "stockonhand"))
            .build()
        )
    );
    return result.stream()
        .collect(groupingBy(StockOnHandByLotDto::getOrderableId, summingInt(StockOnHandByLotDto::getStockOnHand)));
  }

  @ParametersAreNullableByDefault
  private PeriodOfProductMovements getAllProductMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate till,
      @Nonnull Set<UUID> orderableIds, Instant syncSince, Instant syncTill) {
    List<ProductLotMovement> allLotMovements = findAllLotMovements(facilityId, since, till, orderableIds, syncSince,
        syncTill);
    StocksOnHand stocksOnHand = getStockOnHand(facilityId, till, orderableIds);
    Map<String, Integer> productInventories = stocksOnHand.getProductInventories();
    Map<ProductLotCode, Integer> lotInventories = stocksOnHand.getLotInventories();
    List<ProductMovement> productMovements = allLotMovements.stream()
        .collect(groupingBy(ProductLotMovement::getProductMovementKey))
        .entrySet().stream()
        .sorted(Entry.comparingByKey(EventTimeContainer.DESCENDING))
        .map(e -> toProductMovement(e.getKey(), e.getValue(), productInventories, lotInventories, facilityId))
        .sorted(EventTimeContainer.ASCENDING)
        .collect(toList());
    return new PeriodOfProductMovements(productMovements, stocksOnHand);
  }

  public StocksOnHand getStockOnHand(@Nonnull UUID facilityId) {
    return getStockOnHand(facilityId, null, emptySet());
  }

  public StocksOnHand getStockOnHand(@Nonnull UUID facilityId, @Nonnull LocalDate at) {
    return getStockOnHand(facilityId, at, emptySet());
  }

  @ParametersAreNullableByDefault
  private StocksOnHand getStockOnHand(@Nonnull UUID facilityId, LocalDate at, @Nonnull Set<UUID> orderableIds) {
    return new StocksOnHand(findAllLotStocks(facilityId, at, orderableIds));
  }

  public void batchCreateEvents(List<StockEvent> stockEvents) {
    String sql = "INSERT INTO stockmanagement.stock_events"
        + "(id, facilityid, programid, processeddate, signature, userid) "
        + "VALUES (:id, :facilityId, :programId, :serverProcessedAt, :signature, :userId)";
    namedJdbc.batchUpdate(sql, toParams(stockEvents));
  }

  public void batchCreateStockCards(List<StockCard> stockCards) {
    String sql = "INSERT INTO stockmanagement.stock_cards"
        + "(id, facilityid, programid, orderableid, lotid, origineventid) "
        + "VALUES (:id, :facilityId, :programId, :productId, :lotId, :stockEventId)";
    namedJdbc.batchUpdate(sql, toParams(stockCards));
  }

  public void batchCreateStockCardExtensions(List<StockCardExtensionDto> stockCardExtensionDtos) {
    String sql = "INSERT INTO siglusintegration.stock_card_extension"
        + "(id, stockcardid, createdate) "
        + "VALUES (:id, :stockCardId, :createDate)";
    namedJdbc.batchUpdate(sql, toParams(stockCardExtensionDtos));
  }

  public void batchCreateRequestedQuantities(List<RequestedQuantity> requestedQuantities) {
    String sql = "INSERT INTO siglusintegration.stock_event_product_requested"
        + "(id, orderableid, stockeventid, requestedquantity) "
        + "VALUES (:id, :productId, :stockEventId, :requested)";
    namedJdbc.batchUpdate(sql, toParams(requestedQuantities));
  }

  public void batchCreateEventLines(List<StockEventLineItem> eventLineItems) {
    String sql = "INSERT INTO stockmanagement.stock_event_line_items"
        + "(id, stockeventid, orderableid, lotid, occurreddate, quantity, sourceid, destinationid, reasonid, "
        + "extradata) "
        + "VALUES (:id, :lineDetail.stockEvent.id, :lineDetail.stockCard.productId, :lineDetail.stockCard.lotId, "
        + ":lineDetail.occurredDate, :lineDetail.quantity, :lineDetail.sourceId, :lineDetail.destinationId, "
        + ":lineDetail.reasonId, :lineDetail.extraData)";
    namedJdbc.batchUpdate(sql, toParams(eventLineItems));
  }

  public void batchCreateLines(List<StockCardLineItem> stockCardLineItems) {
    String sql = "INSERT INTO stockmanagement.stock_card_line_items"
        + "(id, origineventid, stockcardid, occurreddate, "
        + "processeddate, quantity, sourceid, destinationid, "
        + "reasonid, userid, documentnumber, signature, extradata) "
        + "VALUES (:id, :lineDetail.stockEvent.id, :lineDetail.stockCard.id, :lineDetail.occurredDate, "
        + ":lineDetail.serverProcessAt, :lineDetail.quantity, :lineDetail.sourceId, :lineDetail.destinationId, "
        + ":lineDetail.reasonId, :lineDetail.userId, :lineDetail.documentNumber, :lineDetail.signature, "
        + ":lineDetail.extraData)";
    namedJdbc.batchUpdate(sql, toParams(stockCardLineItems));
  }

  public void batchCreateInventories(List<PhysicalInventory> physicalInventories) {
    String sql = "INSERT INTO stockmanagement.physical_inventories"
        + "(id, facilityid, programid, stockeventid, occurreddate, signature, isdraft) "
        + "VALUES (:id, :facilityId, :programId, :eventId, :occurredDateForSql, :signature, false) ";
    namedJdbc.batchUpdate(sql, toParams(physicalInventories));
  }

  public void batchCreateInventoryLines(List<PhysicalInventoryLine> inventoryLines) {
    String sql = "INSERT INTO stockmanagement.physical_inventory_line_items"
        + "(id, orderableid, lotid, quantity, physicalinventoryid, previousstockonhandwhensubmitted) "
        + "VALUES (:id, :productId, :lotId, :adjustment, :physicalInventoryId, :inventoryBeforeAdjustment) ";
    namedJdbc.batchUpdate(sql, toParams(inventoryLines));
  }

  public void batchCreateInventoryLineAdjustments(List<PhysicalInventoryLineAdjustment> inventoryLineAdjustments) {
    String sql = "INSERT INTO stockmanagement.physical_inventory_line_item_adjustments"
        + "(id, quantity, reasonid, physicalinventorylineitemid, stockeventlineitemid, stockcardlineitemid) "
        + "VALUES (:id, :adjustment, :reasonId, :inventoryLineId, :eventLineId, :stockCardLineId)";
    namedJdbc.batchUpdate(sql, toParams(inventoryLineAdjustments));
  }

  public ProductLot getLot(OrderableDto product, Lot lot) {
    String lotCode = lot.getCode();
    String tradeItemId = product.getIdentifiers().get(TRADE_ITEM);
    ProductLotCode code = ProductLotCode.of(product.getProductCode(), lotCode);
    String lotQuery = "SELECT id, lotcode, tradeitemid, expirationdate FROM referencedata.lots "
        + "WHERE lotcode = ? AND tradeitemid = ?";
    return jdbc.query(lotQuery, productLotExtractor(code), lotCode, tradeItemId);
  }

  public StockCard getStockCard(UUID facilityId, UUID programId, UUID productId, ProductLot productLot) {
    StockCard querySample = StockCard.querySample(facilityId, programId, productId, productLot);
    String sql;
    if (!productLot.isLot()) {
      sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.stock_cards "
          + "WHERE facilityid = ?1 AND programid = ?2 AND orderableid = ?3 AND lotid IS NULL ";
    } else {
      sql = "SELECT CAST(id AS VARCHAR) FROM stockmanagement.stock_cards "
          + "WHERE facilityid = ?1 AND programid = ?2 AND orderableid = ?3 AND lotid = ?4 ";
    }
    Query query = em.createNativeQuery(sql);
    query.setParameter(1, wrap(querySample.getFacilityId()));
    query.setParameter(2, wrap(querySample.getProgramId()));
    query.setParameter(3, wrap(querySample.getProductId()));
    UUID lotId = productLot.getId();
    if (lotId != null) {
      query.setParameter(4, wrap(lotId));
    }
    @SuppressWarnings("unchecked")
    List<String> list = query.getResultList();
    if (list.isEmpty()) {
      return null;
    }
    return StockCard.fromDatabase(UUID.fromString(list.get(0)), querySample);
  }

  public List<CalculatedStockOnHand> findCalculatedStockOnHand(StockCard stockCard, EventTime earliestDate) {
    String sql = "SELECT DISTINCT ON (root.id) root.id, stockonhand, "
        + "l.extradata :: json ->> 'originEventTime' as recordedat, root.occurreddate, root.processeddate "
        + "FROM stockmanagement.calculated_stocks_on_hand root "
        + "LEFT JOIN stockmanagement.stock_card_line_items l "
        + "ON root.stockcardid = l.stockcardid AND root.occurreddate = l.occurreddate "
        + "WHERE l.stockcardid = ? "
        + "AND l.occurreddate = ? "
        + "ORDER BY root.id, l.processeddate DESC";
    return jdbc
        .query(sql, calculatedStockOnHandExtractor(stockCard), stockCard.getId(), earliestDate.getOccurredDate());
  }

  public void batchSaveStocksOnHand(List<CalculatedStockOnHand> calculatedList) {
    Instant stockOnHandProcessAt = Instant.now();
    String updateSql = "UPDATE stockmanagement.calculated_stocks_on_hand "
        + "SET stockonhand = :stockQuantity, processeddate = :processAt WHERE id = :id";
    @SuppressWarnings("unchecked")
    Map<String, Object>[] updateParams = calculatedList.stream()
        .filter(c -> c.getId() != null)
        .map(c -> ImmutableMap.of(
            "id", c.getId(),
            "stockQuantity", c.getInventoryDetail().getStockQuantity(),
            "processAt", Timestamp.from(stockOnHandProcessAt)
        )).toArray(Map[]::new);
    if (updateParams.length > 0) {
      namedJdbc.batchUpdate(updateSql, updateParams);
    }
    String insertSql = "INSERT INTO stockmanagement.calculated_stocks_on_hand"
        + "(id, stockonhand, occurreddate, stockcardid, processeddate) "
        + "VALUES (:id, :stockQuantity, :occurredDate, :stockCardId, :processAt)";
    @SuppressWarnings("unchecked")
    Map<String, Object>[] insertParams = calculatedList.stream()
        .filter(c -> c.getId() == null)
        .map(c -> ImmutableMap.of(
            "id", randomUUID(),
            "stockQuantity", c.getInventoryDetail().getStockQuantity(),
            "processAt", Timestamp.from(stockOnHandProcessAt),
            "occurredDate", java.sql.Date.valueOf(c.getInventoryDetail().getEventTime().getOccurredDate()),
            "stockCardId", c.getStockCard().getId())
        ).toArray(Map[]::new);
    if (insertParams.length > 0) {
      namedJdbc.batchUpdate(insertSql, insertParams);
    }
  }

  @Override
  protected SqlParameterSource[] toParams(Collection<?> entities) {
    return entities.stream().map(e -> new ToJsonBeanPropertySqlParameterSource(e, json))
        .toArray(SqlParameterSource[]::new);
  }

  private TypedParameterValue wrap(UUID id) {
    return new TypedParameterValue(StandardBasicTypes.UUID_CHAR, id);
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
      return ProductLot.fromDatabase(readId(rs), code.getProductCode(), readUuid(rs, "tradeitemid"), readLot(rs));
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
        .processedAt(rs.getTimestamp("processeddate").toInstant())
        .sourceFreeText(rs.getString("sourcefreetext"))
        .destinationFreeText(rs.getString("destinationfreetext"))
        .build();
  }

  @ParametersAreNullableByDefault
  private List<ProductLotStock> findAllLotStocks(@Nonnull UUID facilityId, LocalDate at,
      @Nonnull Set<UUID> orderableIds) {
    requireNonNull(facilityId, "facilityId should not be null");
    String select =
        "SELECT DISTINCT ON (root.stockcardid) o.code AS productcode, o.fullproductname AS productname, l.lotcode, "
            + "root.stockonhand, root.occurreddate, li.extradata :: json ->> 'originEventTime' as recordedat, "
            + "li.processeddate, l.expirationdate ";
    String root = "stockmanagement.calculated_stocks_on_hand root";
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    String where = generateWhere(facilityId, parameters, at, orderableIds);
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
  private boolean readIsInitInventory(ResultSet rs) {
    return rs.getBoolean("isinitinventory");
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
        .inventoryReason(readAsString(rs, "inventoryreason"))
        .inventoryReasonType(readAsString(rs, "inventoryreasontype"))
        .unsignedInventoryAdjustment(readAsInt(rs, "inventoryadjustment"))
        .isInitInventory(readIsInitInventory(rs))
        .build();
  }

  @SneakyThrows
  private Lot readLot(ResultSet rs) {
    return Lot.fromDatabase(readAsString(rs, "lotcode"), readAsDate(rs, "expirationdate"));
  }

  private ProductLotCode readProductLotCode(ResultSet rs) {
    return ProductLotCode.of(readAsString(rs, "productcode"), readAsString(rs, "lotcode"));
  }

  private <T> List<T> executeQuery(String sql, SqlParameterSource parameters, RowMapper<T> transformer) {
    return namedJdbc.query(sql, parameters, new RowMapperResultSetExtractor<>(transformer));
  }

  private static String generateWhere(@Nonnull UUID facilityId, @Nonnull MapSqlParameterSource parameters,
      @Nullable LocalDate at, @Nonnull Set<UUID> orderableIds) {
    return generateWhere(facilityId, parameters, null, at, orderableIds, null, null);
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
        + "COALESCE(root.extradata :: json ->> 'isInitInventory', 'false') as isinitinventory, "
        + "root.quantity, "
        + "srcorg.name AS srcname, "
        + "destorg.name AS destname, "
        + "reason.name AS adjustreason, "
        + "reason.reasontype AS adjustreasontype, "
        + "adjstreason.name AS inventoryreason, "
        + "adjstreason.reasontype AS inventoryreasontype,"
        + "pilia.quantity inventoryadjustment,"
        + "requested.requestedquantity, "
        + "root.signature, "
        + "root.documentnumber, "
        + "root.processeddate, "
        + "l.expirationdate, "
        + "srcfac.name AS srcfacname, "
        + "destfac.name AS destfacname, "
        + "root.sourcefreetext ,"
        + "root.destinationfreetext ";
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
      Map<String, Integer> productInventoryMap, Map<ProductLotCode, Integer> lotInventories, UUID facilityId) {
    ProductMovementBuilder movementBuilder = ProductMovement.builder()
        .productCode(key.getProductCode())
        .eventTime(key.getEventTime());
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
          .reduce((m1, m2) -> m1.merge(m2, facilityId))
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
      String msg = String.format("dirty data[%s@%s %s]", key.getProductCode(), facilityId, key.getEventTime());
      throw new IllegalStateException(msg);
    }
    movementBuilder.requestedQuantity(anyLot.getRequestedQuantity());
    movementBuilder.processedAt(anyLot.getProcessedAt());
    movementBuilder.signature(anyLot.getSignature());
    movementBuilder.sourcefreetext(anyLot.sourceFreeText);
    movementBuilder.destinationfreetext(anyLot.destinationFreeText);
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

    @Nullable
    private final Instant processedAt;

    private final MovementDetail movementDetail;

    @Nullable
    private final Integer requestedQuantity;

    private final String signature;

    @Nullable
    private final String documentNumber;

    private final String sourceFreeText;

    private final String destinationFreeText;

    public ProductMovementKey getProductMovementKey() {
      return ProductMovementKey.of(code.getProductCode(), eventTime);
    }

  }

  private static class ToJsonBeanPropertySqlParameterSource extends BeanPropertySqlParameterSource {

    private final ObjectMapper objectMapper;

    public ToJsonBeanPropertySqlParameterSource(Object object, ObjectMapper objectMapper) {
      super(object);
      this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
      Object value = super.getValue(paramName);
      if (value instanceof Map) {
        return objectMapper.writeValueAsString(value);
      }
      return value;
    }
  }

}
