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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.UserSearchParams;
import org.openlmis.referencedata.repository.custom.UserRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class UserRepositoryImpl implements UserRepositoryCustom {

  protected static final String USERNAME = "username";
  protected static final String FIRST_NAME = "firstName";
  protected static final String LAST_NAME = "lastName";
  protected static final String HOME_FACILITY_ID = "homeFacilityId";
  protected static final String ACTIVE = "active";
  protected static final String ID = "id";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Method returns all matching users sorted by username in alphabetically descending order.
   * If all parameters are null, returns all users.
   * For firstName, lastName, email: matches values that equal or contain
   * the searched value. Case insensitive.
   * Other fields: entered string value must equal to searched value.
   *
   * @param searchParams    user search params
   * @param foundUsers      list of already found users
   * @param pageable        pagination parameters
   * @return Page of users
   */
  public Page<User> searchUsers(UserSearchParams searchParams, List<User> foundUsers,
      Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(searchParams, foundUsers, countQuery, true, pageable);
    Long count = entityManager.createQuery(countQuery).getSingleResult();

    if (count == 0) {
      return Pagination.getPage(Collections.emptyList());
    }

    CriteriaQuery<User> query = builder.createQuery(User.class);
    query = prepareQuery(searchParams, foundUsers, query, false, pageable);
    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);

    List<User> result = entityManager
        .createQuery(query)
        .setMaxResults(maxAndFirst.getLeft())
        .setFirstResult(maxAndFirst.getRight())
        .getResultList();

    return Pagination.getPage(result, pageable, count);   
  }

  private <T> CriteriaQuery<T> prepareQuery(UserSearchParams searchParams, List<User> foundUsers,
      CriteriaQuery<T> query, boolean count, Pageable pageable) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Root<User> root = query.from(User.class);
    Predicate predicate = builder.conjunction();

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    predicate = addLikeFilter(predicate, builder, root, USERNAME, searchParams.getUsername());
    predicate = addLikeFilter(predicate, builder, root, FIRST_NAME, searchParams.getFirstName());
    predicate = addLikeFilter(predicate, builder, root, LAST_NAME, searchParams.getLastName());
    predicate = addEqualsFilter(predicate, builder, root, HOME_FACILITY_ID,
        searchParams.getHomeFacilityUuid());
    predicate = addEqualsFilter(predicate, builder, root, ACTIVE, searchParams.getActive());

    Set<UUID> ids = Sets.newHashSet();
    ids.addAll(searchParams.getIds());

    if (!CollectionUtils.isEmpty(foundUsers)) {
      ids.addAll(foundUsers.stream().map(User::getId).collect(Collectors.toSet()));
    }

    predicate = addInFilter(predicate, builder, root, ID, ids);

    query.where(predicate);

    if (!count && pageable != null && pageable.getSort() != null) {
      query = addSortProperties(query, root, pageable);
    }

    return query;
  }

  private <T> CriteriaQuery<T> addSortProperties(CriteriaQuery<T> query,
                                                 Root root, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
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
    return query.orderBy(orders);
  }

  private Predicate addEqualsFilter(Predicate predicate, CriteriaBuilder builder, Root<User> root,
                              String filterKey, Object filterValue) {
    if (filterValue != null) {
      return builder.and(
          predicate,
          builder.equal(
              root.get(filterKey), filterValue));
    } else {
      return predicate;
    }
  }

  private Predicate addLikeFilter(Predicate predicate, CriteriaBuilder builder, Root<User> root,
                                  String filterKey, String filterValue) {
    if (filterValue != null) {
      return builder.and(
              predicate,
              builder.like(
                  builder.upper(root.get(filterKey)), "%" + filterValue.toUpperCase() + "%"));
    } else {
      return predicate;
    }
  }

  private Predicate addInFilter(Predicate predicate, CriteriaBuilder builder, Root<User> root,
      String filterKey, Collection values) {
    if (null == values || values.isEmpty()) {
      return predicate;
    }

    return builder.and(predicate, root.get(filterKey).in(values));
  }

}
