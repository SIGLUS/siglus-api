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

import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.notification.domain.Notification;
import org.openlmis.notification.repository.custom.NotificationRepositoryCustom;
import org.openlmis.notification.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class NotificationRepositoryImpl
    extends BaseCustomRepository<Notification>
    implements NotificationRepositoryCustom {

  /**
   * Method returns all matching notifications. If all parameters are null, returns notifications.
   *
   * @return Page of notifications.
   */
  public Page<Notification> search(SearchParams searchParams, Pageable pageable) {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = createQuery(builder, Long.class, searchParams, pageable);
    Long count = countEntities(countQuery);

    if (isZeroEntities(count)) {
      return Pagination.getPage(Collections.emptyList(), pageable, count);
    }

    CriteriaQuery<Notification> query = createQuery(builder, Notification.class,
        searchParams, pageable);
    List<Notification> entities = getEntities(query, pageable);

    return Pagination.getPage(entities, pageable, count);
  }

  private <T> CriteriaQuery<T> createQuery(CriteriaBuilder builder, Class<T> type,
      SearchParams searchParams, Pageable pageable) {

    CriteriaQuery<T> query = builder.createQuery(type);
    Root<Notification> root = query.from(Notification.class);
    boolean count = Long.class.isAssignableFrom(type);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    query.where(getFilters(builder, root, searchParams));

    if (!count) {
      query.orderBy(getOrderBy(builder, root, pageable));
    }

    return query;
  }

  private Predicate getFilters(CriteriaBuilder builder, Root<Notification> root,
      SearchParams searchParams) {

    Predicate predicate = builder.conjunction();
    predicate = addEqualFilter(predicate, builder, root, "userId", searchParams.getUserId());
    predicate = addDateRangeFilter(predicate, builder, root, "createdDate",
        searchParams.getSendingDateFrom(), searchParams.getSendingDateTo());

    return predicate;
  }

}
