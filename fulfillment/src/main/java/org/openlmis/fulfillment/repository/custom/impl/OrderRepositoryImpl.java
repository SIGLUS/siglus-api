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

import static org.openlmis.fulfillment.domain.Order.ORDER_STATUS;
import static org.openlmis.fulfillment.domain.Order.PROCESSING_PERIOD_ID;
import static org.openlmis.fulfillment.domain.Order.PROGRAM_ID;
import static org.openlmis.fulfillment.domain.Order.REQUESTING_FACILITY_ID;
import static org.openlmis.fulfillment.domain.Order.SUPPLYING_FACILITY_ID;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.repository.custom.OrderRepositoryCustom;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OrderRepositoryImpl implements OrderRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Method returns all Orders with matched parameters. This method ignore if user has right for
   * order. Use it only with service based tokens.
   *
   * @param params search params (supplyingFacility, requestingFacility, program, statuses)
   * @param processingPeriodIds set of Processing Period UUIDs
   * @param pageable page parameters
   * @return List of Orders with matched parameters.
   */
  @Override
  public Page<Order> searchOrders(OrderSearchParams params, Set<UUID> processingPeriodIds,
      Pageable pageable) {
    return search(params, processingPeriodIds, pageable, Collections.emptySet(),
        Collections.emptySet());
  }

  /**
   * Method returns all Orders with matched parameters. It will filter out all orders that are not
   * part of {@code availableSupplyingFacilities} or {@code availableRequestingFacilities}. If both
   * sets are empty or {@code processingPeriodIds} is empty it will result in empty response.
   *
   * @param params search params (supplyingFacility, requestingFacility, program, statuses)
   * @param processingPeriodIds set of Processing Period UUIDs
   * @param pageable page parameters
   * @param availableSupplyingFacilities  a set of supplying facilities user has right for
   * @param availableRequestingFacilities a set of requesting facilities user has right for
   * @return Page of Orders with matched parameters.
   */
  @Override
  public Page<Order> searchOrders(OrderSearchParams params, Set<UUID> processingPeriodIds,
      Pageable pageable, Set<UUID> availableSupplyingFacilities,
      Set<UUID> availableRequestingFacilities) {
    if ((isEmpty(availableSupplyingFacilities) && isEmpty(availableRequestingFacilities))) {
      return Pagination.getPage(Collections.emptyList(), pageable);
    }
    return search(params, processingPeriodIds,
        pageable, availableSupplyingFacilities, availableRequestingFacilities);
  }

  private Page<Order> search(OrderSearchParams params, Set<UUID> processingPeriodIds,
      Pageable pageable, Set<UUID> availableSupplyingFacilities,
      Set<UUID> availableRequestingFacilities) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Order> query = builder.createQuery(Order.class);
    query = prepareQuery(query, params, processingPeriodIds, pageable, false,
        availableSupplyingFacilities, availableRequestingFacilities);
    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(countQuery, params, processingPeriodIds, pageable, true,
        availableSupplyingFacilities, availableRequestingFacilities);

    Pageable page = null != pageable ? pageable : new PageRequest(0, Integer.MAX_VALUE);

    Long count = entityManager.createQuery(countQuery).getSingleResult();
    List<Order> result = entityManager.createQuery(query)
        .setMaxResults(page.getPageSize())
        .setFirstResult(page.getPageSize() * page.getPageNumber())
        .getResultList();

    return new PageImpl<>(result, page, count);
  }

  /**
   * Retrieves the distinct UUIDs of the available requesting facilities.
   */
  @Override
  public List<UUID> getRequestingFacilities(List<UUID> supplyingFacilityIds) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<UUID> query = builder.createQuery(UUID.class);
    Root<Order> root = query.from(Order.class);

    if (!isEmpty(supplyingFacilityIds)) {
      Predicate predicate = builder.disjunction();
      for (Object elem : supplyingFacilityIds) {
        predicate = builder.or(predicate, builder.equal(root.get(SUPPLYING_FACILITY_ID), elem));
      }
      query.where(predicate);
    }

    query.select(root.get(REQUESTING_FACILITY_ID)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  private <T> CriteriaQuery<T> prepareQuery(CriteriaQuery<T> query, OrderSearchParams params,
      Set<UUID> processingPeriodIds, Pageable pageable, boolean count,
      Set<UUID> availableSupplyingFacilities, Set<UUID> availableRequestingFacilities) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Root<Order> root = query.from(Order.class);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    Predicate predicate = builder.conjunction();
    predicate =
        isEqual(SUPPLYING_FACILITY_ID, params.getSupplyingFacilityId(), root, predicate, builder);
    predicate =
        isEqual(REQUESTING_FACILITY_ID, params.getRequestingFacilityId(), root, predicate, builder);

    if (!(isEmpty(availableSupplyingFacilities) && isEmpty(availableRequestingFacilities))) {
      Predicate orPredicate = builder.disjunction();
      orPredicate = isOneOfOr(SUPPLYING_FACILITY_ID, availableSupplyingFacilities, root,
          orPredicate, builder);
      orPredicate = isOneOfOr(REQUESTING_FACILITY_ID, availableRequestingFacilities, root,
          orPredicate, builder);
      predicate = builder.and(predicate, orPredicate);
    }

    predicate = isEqual(PROGRAM_ID, params.getProgramId(), root, predicate, builder);
    predicate = isOneOf(PROCESSING_PERIOD_ID, processingPeriodIds, root, predicate, builder);
    predicate = isOneOf(ORDER_STATUS, params.getStatusAsEnum(), root, predicate, builder);

    query.where(predicate);

    if (!count && pageable != null && pageable.getSort() != null) {
      query = addSortProperties(query, root, pageable);
    }

    return query;
  }

  private Predicate isOneOf(String field, Collection collection, Root<Order> root,
                            Predicate predicate, CriteriaBuilder builder) {
    return !isEmpty(collection)
        ? builder.and(predicate, root.get(field).in(collection))
        : predicate;
  }

  private Predicate isOneOfOr(String field, Collection collection, Root<Order> root,
                            Predicate predicate, CriteriaBuilder builder) {
    return !isEmpty(collection)
        ? builder.or(predicate, root.get(field).in(collection))
        : predicate;
  }

  private Predicate isEqual(String field, Object value, Root<Order> root, Predicate predicate,
                            CriteriaBuilder builder) {
    return value != null
        ? builder.and(predicate, builder.equal(root.get(field), value))
        : predicate;
  }

  private <T> CriteriaQuery<T> addSortProperties(CriteriaQuery<T> query,
                                                 Root root, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    List<javax.persistence.criteria.Order> orders = new ArrayList<>();
    Iterator<Sort.Order> iterator = pageable.getSort().iterator();
    Sort.Order order;

    while (iterator.hasNext()) {
      order = iterator.next();
      String property = order.getProperty();

      Path path = root.get(property);
      if (order.isAscending()) {
        orders.add(builder.asc(path));
      } else {
        orders.add(builder.desc(path));
      }
    }
    return query.orderBy(orders);
  }
}
