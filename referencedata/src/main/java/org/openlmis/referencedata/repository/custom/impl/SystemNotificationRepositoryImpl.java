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

import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.and;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.openlmis.referencedata.domain.SystemNotification;
import org.openlmis.referencedata.repository.custom.SystemNotificationRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class SystemNotificationRepositoryImpl implements SystemNotificationRepositoryCustom {

  private static final String HQL_COUNT = "SELECT DISTINCT COUNT(*)"
      + " FROM SystemNotification AS sn"
      + " INNER JOIN sn.author AS a";

  private static final String HQL_SELECT = "SELECT DISTINCT sn"
      + " FROM SystemNotification AS sn"
      + " INNER JOIN sn.author AS a";

  private static final String WHERE = "WHERE";
  private static final String AND = " AND ";
  private static final String OR = " OR ";
  private static final String DEFAULT_SORT = "sn.active DESC, sn.expiryDate DESC";
  private static final String ORDER_BY = "ORDER BY";

  private static final String WITH_AUTHOR_ID = "a.id = :authorId";
  private static final String WITH_ACTIVE = "sn.active = :isDisplayed";
  private static final String NOW = "NOW()";

  private static final String START_DATE = "sn.startDate";
  private static final String EXPIRY_DATE = "sn.expiryDate";
  private static final String IS_NOT_NULL = " IS NOT NULL";
  private static final String IS_NULL = " IS NULL";
  private static final String DATE_AFTER_NOW = " > " + NOW;
  private static final String DATE_BEFORE_NOW = " < " + NOW;
  private static final String DATE_AFTER_OR_EQUAL_NOW = " >= " + NOW;
  private static final String DATE_BEFORE_OR_EQUAL_NOW = " <= " + NOW;

  private static final String NULL_START_DATE = START_DATE + IS_NULL;
  private static final String NULL_EXPIRY_DATE = EXPIRY_DATE + IS_NULL;
  private static final String NOT_NULL_START_DATE = START_DATE + IS_NOT_NULL;
  private static final String NOT_NULL_EXPIRY_DATE = EXPIRY_DATE + IS_NOT_NULL;

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all system notifications with matched parameters.
   */
  public Page<SystemNotification> search(
      SearchParams searchParams, Pageable pageable) {
    Map<String, Object> params = Maps.newHashMap();
    Query countQuery = entityManager.createQuery(prepareQuery(
        HQL_COUNT, params, searchParams), Long.class);
    params.forEach(countQuery::setParameter);
    Long count = (Long) countQuery.getSingleResult();

    if (count < 1) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    params = Maps.newHashMap();
    String hqlWithSort = Joiner.on(' ').join(Lists.newArrayList(
        prepareQuery(HQL_SELECT, params, searchParams),
        ORDER_BY, PageableUtil.getOrderPredicate(pageable, "sn.", DEFAULT_SORT)));

    Query searchQuery = entityManager.createQuery(hqlWithSort, SystemNotification.class);
    params.forEach(searchQuery::setParameter);
    List<SystemNotification> resultList =  searchQuery
        .setMaxResults(pageable.getPageSize())
        .setFirstResult(pageable.getOffset())
        .getResultList();

    return Pagination.getPage(resultList, pageable, count);
  }

  private String prepareQuery(String baseSql, Map<String, Object> params,
      SearchParams searchParams) {

    List<String> sql = Lists.newArrayList(baseSql);
    List<String> where = Lists.newArrayList();
    List<String> or = Lists.newArrayList();

    if (searchParams.getAuthorId() != null) {
      where.add(WITH_AUTHOR_ID);
      params.put("authorId", searchParams.getAuthorId());
    }

    if (searchParams.getIsDisplayed() != null) {
      where.add(WITH_ACTIVE);
      params.put("isDisplayed", searchParams.getIsDisplayed());

      if (searchParams.getIsDisplayed()) {
        where.add("((" + and(NULL_START_DATE, NULL_EXPIRY_DATE) + ")");
        or.add("(" + and(NULL_EXPIRY_DATE, NOT_NULL_START_DATE,
            START_DATE + DATE_BEFORE_OR_EQUAL_NOW) + ")");
        or.add("(" + and(NULL_START_DATE, NOT_NULL_EXPIRY_DATE,
            EXPIRY_DATE + DATE_AFTER_OR_EQUAL_NOW) + ")");
        or.add("(" + and(NOT_NULL_START_DATE, NOT_NULL_EXPIRY_DATE,
            EXPIRY_DATE + DATE_AFTER_OR_EQUAL_NOW, START_DATE + DATE_BEFORE_OR_EQUAL_NOW) + "))");
      } else {
        or.add("(" + and(NULL_START_DATE, NULL_EXPIRY_DATE) + ")");
        or.add("(" + and(NOT_NULL_START_DATE, START_DATE + DATE_AFTER_NOW) + ")");
        or.add("(" + and(NOT_NULL_EXPIRY_DATE, EXPIRY_DATE + DATE_BEFORE_NOW) + ")");
      }
    }

    if (!where.isEmpty()) {
      sql.add(WHERE);
      sql.add(Joiner.on(AND).join(where));
    }

    if (!or.isEmpty()) {
      sql.add(OR);
      sql.add(Joiner.on(OR).join(or));
    }

    return Joiner.on(' ').join(sql);
  }

}
