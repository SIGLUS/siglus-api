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
import java.util.Collections;
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
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyPartner;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation;
import org.openlmis.referencedata.repository.custom.SupplyPartnerRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class SupplyPartnerRepositoryImpl implements SupplyPartnerRepositoryCustom {

  private static final String ID = "id";

  private static final String ASSOCIATIONS = "associations";
  private static final String SUPERVISORY_NODE = "supervisoryNode";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all supply partners with matched parameters.
   */
  public Page<SupplyPartner> search(SearchParams params, Pageable pageable) {
    Preconditions.checkNotNull(params);
    Preconditions.checkNotNull(pageable);

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(builder, countQuery, true, params);

    Long count = entityManager.createQuery(countQuery).getSingleResult();

    if (count == 0) {
      return Pagination.getPage(Collections.emptyList());
    }

    CriteriaQuery<SupplyPartner> query = builder.createQuery(SupplyPartner.class);
    query = prepareQuery(builder, query, false, params);

    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);

    List<SupplyPartner> resultList = entityManager.createQuery(query)
        .setMaxResults(maxAndFirst.getLeft())
        .setFirstResult(maxAndFirst.getRight())
        .getResultList();

    return Pagination.getPage(resultList, pageable, count);
  }

  private <E> CriteriaQuery<E> prepareQuery(CriteriaBuilder builder, CriteriaQuery<E> query,
      boolean count, SearchParams params) {

    Root<SupplyPartner> root = query.from(SupplyPartner.class);
    CriteriaQuery<E> newQuery;

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      newQuery = (CriteriaQuery<E>) countQuery.select(builder.count(root));
    } else {
      CriteriaQuery<SupplyPartner> typeQuery = (CriteriaQuery<SupplyPartner>) query;
      newQuery = (CriteriaQuery<E>) typeQuery.select(root);
    }

    Predicate where = builder.conjunction();
    Set<UUID> ids = Preconditions.checkNotNull(params.getIds());

    if (!ids.isEmpty()) {
      where = builder.and(where, root.get(ID).in(ids));
    }

    Set<UUID> supervisoryNodeIds = Preconditions.checkNotNull(params.getSupervisoryNodeIds());

    if (!supervisoryNodeIds.isEmpty()) {
      Join<SupplyPartner, SupplyPartnerAssociation> associationsJoin = root.join(ASSOCIATIONS);
      Join<SupplyPartnerAssociation, SupervisoryNode> supervisoryNodeJoin = associationsJoin
          .join(SUPERVISORY_NODE);
      where = builder.and(where, supervisoryNodeJoin.get(ID).in(supervisoryNodeIds));
    }

    newQuery.where(where);

    return newQuery;
  }

}
