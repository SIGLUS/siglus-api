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

package org.openlmis.notification.repository.custom.impl;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Pageable;

abstract class BaseCustomRepository<T> {

  @PersistenceContext
  private EntityManager entityManager;

  CriteriaBuilder getCriteriaBuilder() {
    return entityManager.getCriteriaBuilder();
  }

  Long countEntities(CriteriaQuery<Long> query) {
    return entityManager.createQuery(query).getSingleResult();
  }

  boolean isZeroEntities(Long count) {
    return ObjectUtils.compare(count, 0L) == 0;
  }

  List<T> getEntities(CriteriaQuery<T> query, Pageable pageable) {
    return entityManager
        .createQuery(query)
        .setMaxResults(pageable.getPageSize())
        .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
        .getResultList();
  }

  List<Order> getOrderBy(CriteriaBuilder builder, Root<T> root, Pageable pageable) {
    if (null == pageable.getSort()) {
      return Collections.emptyList();
    }

    return StreamSupport
        .stream(pageable.getSort().spliterator(), false)
        .map(order -> order.isAscending()
            ? builder.asc(getField(root, order.getProperty()))
            : builder.desc(getField(root, order.getProperty())))
        .collect(Collectors.toList());
  }

  Predicate addEqualFilter(Predicate predicate, CriteriaBuilder builder, Root<T> root,
      String field, Object filterValue) {
    return null == filterValue
        ? predicate
        : builder.and(predicate, builder.equal(getField(root, field), filterValue));
  }

  Predicate addLikeFilter(Predicate predicate, CriteriaBuilder builder, Root<T> root, String field,
      String filterValue) {
    return filterValue != null
        ? builder.and(predicate, builder.like(
        builder.upper(getField(root, field)), "%" + filterValue.toUpperCase() + "%"))
        : predicate;
  }

  Predicate addInFilter(Predicate predicate, CriteriaBuilder builder, Root<T> root, String field,
      Collection values) {
    return null == values || values.isEmpty()
        ? predicate
        : builder.and(predicate, getField(root, field).in(values));
  }

  Predicate addDateRangeFilter(Predicate predicate, CriteriaBuilder builder,
      Root<T> root, String field, ZonedDateTime startDate, ZonedDateTime endDate) {
    if (null != startDate && null != endDate) {
      return builder.and(predicate, builder.between(getField(root, field), startDate, endDate));
    }

    if (null != startDate) {
      return builder.and(predicate, builder.greaterThanOrEqualTo(getField(root, field), startDate));
    }

    if (null != endDate) {
      return builder.and(predicate, builder.lessThanOrEqualTo(getField(root, field), endDate));
    }

    return predicate;
  }

  private <Y> Expression<Y> getField(Root<T> root, String field) {
    String[] fields = field.split("\\.");

    if (fields.length < 2) {
      return root.get(field);
    }

    Path<Y> path = root.get(fields[0]);
    for (int i = 1, length = fields.length; i < length; ++i) {
      path = path.get(fields[i]);
    }

    return path;
  }

}
