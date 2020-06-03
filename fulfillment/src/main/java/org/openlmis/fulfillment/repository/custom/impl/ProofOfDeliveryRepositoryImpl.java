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

package org.openlmis.fulfillment.repository.custom.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.repository.custom.ProofOfDeliveryRepositoryCustom;
import org.openlmis.fulfillment.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

public class ProofOfDeliveryRepositoryImpl implements ProofOfDeliveryRepositoryCustom {

  private static final String POD_SELECT = "SELECT DISTINCT p"
      + " FROM ProofOfDelivery AS p"
      + " INNER JOIN FETCH p.shipment AS s"
      + " INNER JOIN FETCH s.order AS o"
      + " LEFT JOIN FETCH p.lineItems";

  private static final String POD_COUNT = "SELECT DISTINCT COUNT(*)"
      + " FROM ProofOfDelivery AS p"
      + " INNER JOIN p.shipment AS s"
      + " INNER JOIN s.order AS o";

  private static final String WHERE = "WHERE";
  private static final String AND = " AND ";
  private static final String ASC = "ASC";
  private static final String DESC = "DESC";
  private static final String ORDER_BY = "ORDER BY";

  private static final String WITH_SHIPMENT_ID = "s.id = :shipmentId";
  private static final String WITH_ORDER_ID = "o.id = :orderId";
  private static final String WITH_RECEIVING_FACILITIES =
      "o.receivingFacilityId IN (:receivingFacilityIds)";
  private static final String WITH_SUPPLYING_FACILITIES =
      "o.supplyingFacilityId IN (:supplyingFacilityIds)";
  private static final String WITH_PROGRAM_IDS =
      "o.programId IN (:programIds)";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all PODs with matched parameters.
   *
   * @param shipmentId           UUID of associated shipment
   * @param orderId              UUID of associated order
   * @param receivingFacilityIds list of UUIDs of receiving facility in associated order
   * @param supplyingFacilityIds list of UUIDs of supplying facility in associated order
   * @param pageable             pagination parameters
   * @return List of Facilities matching the parameters.
   */
  public Page<ProofOfDelivery> search(UUID shipmentId, UUID orderId, Set<UUID> receivingFacilityIds,
      Set<UUID> supplyingFacilityIds, Set<UUID> programIds, Pageable pageable) {

    TypedQuery countQuery = prepareQuery(POD_COUNT, shipmentId, orderId, receivingFacilityIds,
        supplyingFacilityIds, programIds, pageable, true);
    Long count = (Long) countQuery.getSingleResult();

    if (count > 0) {
      TypedQuery searchQuery = prepareQuery(POD_SELECT, shipmentId, orderId, receivingFacilityIds,
          supplyingFacilityIds, programIds, pageable, false);
      List<ProofOfDelivery> pods = searchQuery
          .setMaxResults(pageable.getPageSize())
          .setFirstResult(pageable.getOffset())
          .getResultList();
      return Pagination.getPage(pods, pageable, count);
    }

    return Pagination.getPage(emptyList(), pageable, count);
  }

  private TypedQuery prepareQuery(String select, UUID shipmentId, UUID orderId,
      Set<UUID> receivingFacilityIds, Set<UUID> supplyingFacilityIds, Set<UUID> programIds,
      Pageable pageable, boolean count) {

    List<String> sql = Lists.newArrayList(select);
    List<String> where = Lists.newArrayList();
    Map<String, Object> params = Maps.newHashMap();

    if (null != shipmentId) {
      where.add(WITH_SHIPMENT_ID);
      params.put("shipmentId", shipmentId);
    } else if (null != orderId) {
      where.add(WITH_ORDER_ID);
      params.put("orderId", orderId);
    }

    if (isNotEmpty(receivingFacilityIds)) {
      where.add(WITH_RECEIVING_FACILITIES);
      params.put("receivingFacilityIds", receivingFacilityIds);
    }

    if (isNotEmpty(supplyingFacilityIds)) {
      where.add(WITH_SUPPLYING_FACILITIES);
      params.put("supplyingFacilityIds", supplyingFacilityIds);
    }

    if (isNotEmpty(programIds)) {
      where.add(WITH_PROGRAM_IDS);
      params.put("programIds", programIds);
    }

    if (!where.isEmpty()) {
      sql.add(WHERE);
      sql.add(Joiner.on(AND).join(where));
    }

    String query = Joiner.on(' ').join(sql);
    if (!count && pageable.getSort() != null) {
      query = Joiner.on(' ').join(Lists.newArrayList(query, ORDER_BY,
          getOrderPredicate(pageable)));
    }

    Class resultClass = count ? Long.class : ProofOfDelivery.class;

    TypedQuery typedQuery = entityManager.createQuery(query, resultClass);
    params.forEach(typedQuery::setParameter);
    return typedQuery;
  }

  private String getOrderPredicate(Pageable pageable) {
    List<String> orderPredicate = new ArrayList<>();
    List<String> sql = new ArrayList<>();
    Iterator<Order> iterator = pageable.getSort().iterator();
    Sort.Order order;
    Sort.Direction sortDirection = Sort.Direction.ASC;

    while (iterator.hasNext()) {
      order = iterator.next();
      orderPredicate.add("p.".concat(order.getProperty()));
      sortDirection = order.getDirection();
    }

    sql.add(Joiner.on(",").join(orderPredicate));
    sql.add(sortDirection.isAscending() ? ASC : DESC);

    return Joiner.on(' ').join(sql);
  }
}
