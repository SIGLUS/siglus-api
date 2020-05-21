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

package org.openlmis.requisition.repository.custom.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.requisition.utils.DateHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

abstract class BaseCustomRepository<T> {

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private DateHelper dateHelper;

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
    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);
    return entityManager
        .createQuery(query)
        .setMaxResults(maxAndFirst.getLeft())
        .setFirstResult(maxAndFirst.getRight())
        .getResultList();
  }

  Predicate addEqualFilter(Predicate predicate, CriteriaBuilder builder, Root<T> root,
      String field, Object filterValue) {
    return null == filterValue
        ? predicate
        : builder.and(predicate, builder.equal(getField(root, field), filterValue));
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

  ZonedDateTime setStartDateParam(ZonedDateTime dateFrom) {
    return dateFrom != null ? setStartDateParam(dateFrom.toLocalDate()) : null;
  }

  ZonedDateTime setStartDateParam(LocalDate dateFrom) {
    return dateFrom != null ? dateFrom.atStartOfDay(dateHelper.getZone()) : null;
  }

  ZonedDateTime setEndDateParam(ZonedDateTime dateTo) {
    return dateTo != null ? setEndDateParam(dateTo.toLocalDate()) : null;
  }

  ZonedDateTime setEndDateParam(LocalDate dateTo) {
    return dateTo != null
        ? ZonedDateTime.of(dateTo, LocalTime.MAX, dateHelper.getZone()) : null;
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
