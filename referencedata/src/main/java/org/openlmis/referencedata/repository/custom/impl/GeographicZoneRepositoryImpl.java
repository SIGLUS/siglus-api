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

package org.openlmis.referencedata.repository.custom.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.domain.GeographicLevel;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.repository.custom.GeographicZoneRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class GeographicZoneRepositoryImpl implements GeographicZoneRepositoryCustom {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String PARENT = "parent";
  private static final String LEVEL = "level";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve page of geographic zones with matched parameters.
   * Method is ignoring case for geographic code and name.
   *
   * @param name Part of wanted name.
   * @param code Part of wanted code.
   * @param parent Parent of geographic zone.
   * @param geographicLevel Wanted geographic zone level.
   * @return Page of Geographic Zones matching the parameters.
   */
  public Page<GeographicZone> search(String name, String code,
                                     GeographicZone parent, GeographicLevel geographicLevel,
                                     Pageable pageable) {
    if (StringUtils.isEmpty(code)
        && StringUtils.isEmpty(name)
        && parent == null
        && geographicLevel == null) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<GeographicZone> query = builder.createQuery(GeographicZone.class);
    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);

    query = prepareSearchQuery(query, name, code, parent, geographicLevel, pageable, false);
    countQuery = prepareSearchQuery(countQuery, name, code,
                                    parent, geographicLevel, pageable, true);

    Long count = entityManager.createQuery(countQuery).getSingleResult();

    List<GeographicZone> result = entityManager.createQuery(query)
        .setMaxResults(pageable.getPageSize())
        .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
        .getResultList();

    return Pagination.getPage(result, pageable, count);
  }

  private <T> CriteriaQuery<T> prepareSearchQuery(CriteriaQuery<T> query, String name,
                                            String code, GeographicZone parent,
                                            GeographicLevel geographicLevel, Pageable pageable,
                                            boolean count) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    Root<GeographicZone> root = query.from(GeographicZone.class);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    Predicate predicate = builder.conjunction();

    if (name != null) {
      predicate = builder.and(predicate,
          builder.like(builder.upper(root.get(NAME)), "%" + name.toUpperCase() + "%"));
    }

    if (code != null) {
      predicate = builder.and(predicate,
          builder.like(builder.upper(root.get(CODE)), "%" + code.toUpperCase() + "%"));
    }

    if (parent != null) {
      predicate = builder.and(predicate,
          builder.equal(root.get(PARENT), parent));
    }

    if (geographicLevel != null) {
      predicate = builder.and(predicate,
          builder.equal(root.get(LEVEL), geographicLevel));
    }

    query.where(predicate);

    if (!count && pageable.getSort() != null) {
      List<Order> orders = new ArrayList<>();
      Iterator<Sort.Order> iterator = pageable.getSort().iterator();
      Sort.Order order;
      while (iterator.hasNext()) {
        order = iterator.next();
        if (order.isAscending()) {
          orders.add(builder.asc(root.get(order.getProperty())));
        } else {
          orders.add(builder.desc(root.get(order.getProperty())));
        }
      }
      query.orderBy(orders);
    }

    return query;
  }
}
