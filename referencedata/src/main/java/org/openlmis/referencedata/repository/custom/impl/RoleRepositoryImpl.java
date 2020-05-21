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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.Role;
import org.openlmis.referencedata.repository.custom.RoleRepositoryCustom;

public class RoleRepositoryImpl implements RoleRepositoryCustom {

  private static final String ID = "id";

  private static final String RIGHTS = "rights";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Retrieves all roles with matched parameters.
   */
  @Override
  public List<Role> search(SearchParams params) {
    Preconditions.checkNotNull(params);

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(builder, countQuery, true, params);

    Long count = entityManager
        .createQuery(countQuery)
        .getSingleResult();

    if (count == 0) {
      return Lists.newArrayList();
    }

    CriteriaQuery<Role> query = builder.createQuery(Role.class);
    query = prepareQuery(builder, query, false, params);

    return entityManager
        .createQuery(query)
        .getResultList();
  }

  private <E> CriteriaQuery<E> prepareQuery(CriteriaBuilder builder, CriteriaQuery<E> query,
      boolean count, SearchParams params) {

    Root<Role> root = query.from(Role.class);
    CriteriaQuery<E> newQuery;

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      newQuery = (CriteriaQuery<E>) countQuery.select(builder.count(root));
    } else {
      CriteriaQuery<Role> typeQuery = (CriteriaQuery<Role>) query;
      newQuery = (CriteriaQuery<E>) typeQuery.select(root);
    }

    Predicate where = builder.conjunction();
    Set<UUID> rightIds = Preconditions.checkNotNull(params.getRightIds());

    if (!rightIds.isEmpty()) {
      Join<Role, Right> rightJoin = root.join(RIGHTS);
      where = builder.and(where, rightJoin.get(ID).in(rightIds));
    }

    newQuery.where(where);

    return newQuery;
  }

}
