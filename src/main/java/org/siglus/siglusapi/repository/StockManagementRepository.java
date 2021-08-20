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

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.LotMovement;
import org.siglus.siglusapi.dto.android.MovementDetail;
import org.siglus.siglusapi.dto.android.ProductLotCode;
import org.siglus.siglusapi.dto.android.ProductLotMovement;
import org.siglus.siglusapi.dto.android.ProductLotStock;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovement.ProductMovementBuilder;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.StockOnHand;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockManagementRepository {

  private final EntityManager em;

  private static final String LEFT_JOIN = "LEFT JOIN ";
  private static final String STOCK_CARD_ROOT = "stockmanagement.stock_cards sc";
  private static final String ORDERABLE_ROOT =
      "(select distinct on (id) * from referencedata.orderables order by id, versionnumber desc) o";
  private static final String LOT_ROOT = "referencedata.lots l";

  @ParametersAreNullableByDefault
  public List<ProductMovement> getLatestProductMovements(@Nonnull UUID facilityId) {
    return getAllProductMovements(facilityId, null, null);
  }

  @ParametersAreNullableByDefault
  public List<ProductMovement> getAllProductMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate at) {
    List<ProductLotMovement> allLotMovements = findAllLotMovements(facilityId, since, at);
    return allLotMovements.stream()
        .collect(groupingBy(ProductLotMovement::getProductMovementKey))
        .entrySet().stream()
        .map(e -> toProductMovement(e.getKey(), e.getValue()))
        .collect(toList());
  }

  @ParametersAreNullableByDefault
  public StockOnHand getStockOnHand(@Nonnull UUID facilityId) {
    return getStockOnHand(facilityId, null);
  }

  @ParametersAreNullableByDefault
  public StockOnHand getStockOnHand(@Nonnull UUID facilityId, LocalDate at) {
    return new StockOnHand(findAllLotStocks(facilityId, at));
  }

  @ParametersAreNullableByDefault
  private List<ProductLotMovement> findAllLotMovements(@Nonnull UUID facilityId, LocalDate since, LocalDate at) {
    requireNonNull(facilityId, "facilityId should not be null");
    List<Object> params = new ArrayList<>(2);
    String sql = generateMovementQuery(facilityId, since, at, params);
    List<ProductLotMovement> productLotMovements = executeQuery(sql, params, buildProductLotMovementFromResult());
    populateInventory(facilityId, at, productLotMovements);
    return productLotMovements;
  }

  private Function<Object[], ProductLotMovement> buildProductLotMovementFromResult() {
    return arr -> ProductLotMovement.builder()
        .code(ProductLotCode.of((String) arr[0], (String) arr[1]))
        .eventTime(EventTime.of(((java.sql.Date) arr[2]).toLocalDate(), Instant.parse((String) arr[3])))
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
        .build();
  }

  private void populateInventory(UUID facilityId, LocalDate at, List<ProductLotMovement> productLotMovements) {
    StockOnHand stockOnHand = getStockOnHand(facilityId, at);
    productLotMovements.stream()
        .filter(m -> stockOnHand.findInventory(m.getCode()) != null)
        .collect(groupingBy(ProductLotMovement::getCode))
        .forEach((key, value) -> {
          ProductLotStock productLotStock = stockOnHand.findInventory(key);
          Integer inventory = requireNonNull(productLotStock).getInventory();
          List<ProductLotMovement> movements = value.stream()
              .sorted(((o1, o2) -> EventTime.DESCENDING.compare(o1.getEventTime(), o2.getEventTime())))
              .collect(toList());
          for (ProductLotMovement movement : movements) {
            inventory = movement.populateInventory(inventory);
          }
        });
  }

  @ParametersAreNullableByDefault
  private List<ProductLotStock> findAllLotStocks(@Nonnull UUID facilityId, LocalDate at) {
    requireNonNull(facilityId, "facilityId should not be null");
    String select = "SELECT DISTINCT ON (stockcardid) o.code AS productcode, l.lotcode, "
        + "root.stockonhand, root.occurreddate, root.processeddate ";
    String root = "stockmanagement.calculated_stocks_on_hand root";
    List<Object> params = new ArrayList<>(2);
    String where = generateWhere(facilityId, null, at, params);
    String orderBy = "ORDER BY stockcardid, occurreddate DESC, processeddate DESC";
    String sql = select
        + "FROM " + root + ' '
        + LEFT_JOIN + STOCK_CARD_ROOT + " on root.stockcardid = sc.id "
        + LEFT_JOIN + ORDERABLE_ROOT + " on sc.orderableid = o.id "
        + LEFT_JOIN + LOT_ROOT + " on sc.lotid = l.id "
        + where
        + orderBy;
    return executeQuery(sql, params,
        (arr -> ProductLotStock.builder()
            .code(ProductLotCode.of((String) arr[0], (String) arr[1]))
            .inventory((Integer) arr[2])
            .eventTime(EventTime.of(((java.sql.Date) arr[3]).toLocalDate(), ((java.sql.Timestamp) arr[4]).toInstant()))
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

  @ParametersAreNullableByDefault
  @SuppressWarnings("PMD.ConsecutiveLiteralAppends")
  private static String generateWhere(@Nonnull UUID facilityId, LocalDate since, LocalDate at,
      @Nonnull List<Object> params) {
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
    return where.toString();
  }

  private String generateMovementQuery(UUID facilityId, LocalDate since, LocalDate at, List<Object> params) {
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
        + "requested.requestedquantity ";
    String root = "stockmanagement.stock_card_line_items root";
    String srcNodeRoot = "stockmanagement.nodes srcnode";
    String srcOrgRoot = "stockmanagement.organizations srcorg";
    String destNodeRoot = "stockmanagement.nodes destnode";
    String destOrgRoot = "stockmanagement.organizations destorg";
    String reasonRoot = "stockmanagement.stock_card_line_item_reasons reason";
    String lineAdjRoot = "stockmanagement.physical_inventory_line_item_adjustments pilia";
    String adjustmentReasonRoot = "stockmanagement.stock_card_line_item_reasons adjstreason";
    String requestedRoot = "siglusintegration.stock_event_product_requested requested";
    String where = generateWhere(facilityId, since, at, params);
    return select
        + "FROM " + root + ' '
        + LEFT_JOIN + STOCK_CARD_ROOT + " on root.stockcardid = sc.id "
        + LEFT_JOIN + ORDERABLE_ROOT + " on sc.orderableid = o.id "
        + LEFT_JOIN + LOT_ROOT + " on sc.lotid = l.id "
        + LEFT_JOIN + srcNodeRoot + " on root.sourceid = srcnode.id "
        + LEFT_JOIN + srcOrgRoot + " on srcnode.referenceid = srcorg.id "
        + LEFT_JOIN + destNodeRoot + " on root.destinationid = destnode.id "
        + LEFT_JOIN + destOrgRoot + " on destnode.referenceid = destorg.id "
        + LEFT_JOIN + reasonRoot + " on root.reasonid = reason.id "
        + LEFT_JOIN + lineAdjRoot + " on pilia.stockcardlineitemid = root.id "
        + LEFT_JOIN + adjustmentReasonRoot + " on pilia.reasonid = adjstreason.id "
        + LEFT_JOIN + requestedRoot + " on requested.stockeventid = root.origineventid "
        + where;
  }

  private ProductMovement toProductMovement(ProductMovementKey key, List<ProductLotMovement> productLotMovements) {
    ProductMovementBuilder movementBuilder = ProductMovement.builder()
        .productCode(key.getProductCode())
        .eventTime(key.getEventTime());
    Integer requestedQuantity = productLotMovements.stream().findAny().map(ProductLotMovement::getRequestedQuantity)
        .orElse(null);
    movementBuilder.requestedQuantity(requestedQuantity);
    if (productLotMovements.size() == 1 && productLotMovements.get(0).getCode().getLotCode() == null) {
      movementBuilder.movementDetail(productLotMovements.get(0).getMovementDetail());
    } else {
      List<LotMovement> lotMovements = productLotMovements.stream()
          .map(this::toLotMovement)
          .filter(lot -> lot.getLotCode() != null)
          .sorted(comparing(LotMovement::getLotCode))
          .collect(toList());
      movementBuilder.lotMovements(lotMovements);
      MovementDetail movementDetail = productLotMovements.stream()
          .filter(p -> p.getCode().getLotCode() != null)
          .map(ProductLotMovement::getMovementDetail)
          .reduce(MovementDetail::merge)
          .orElse(null);
      movementBuilder.movementDetail(movementDetail);
    }
    return movementBuilder.build();
  }

  private LotMovement toLotMovement(ProductLotMovement productLotMovement) {
    return LotMovement.builder()
        .lotCode(productLotMovement.getCode().getLotCode())
        .movementDetail(productLotMovement.getMovementDetail())
        .build();
  }

}
