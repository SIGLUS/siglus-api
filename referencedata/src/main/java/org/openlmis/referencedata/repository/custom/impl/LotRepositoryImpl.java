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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.repository.custom.LotRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class LotRepositoryImpl implements LotRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all lots with matched parameters.
   * Method is ignoring case for lot code.
   * To find all wanted lots by code and expiration date we use criteria query and like operator.
   *
   * @param tradeItems list of TradeItems associated with Lot.
   * @param expirationDate date of lot expiration.
   * @param lotCode Part of wanted code.
   * @return List of Facilities matching the parameters.
   */
  public Page<Lot> search(Collection<TradeItem> tradeItems, LocalDate expirationDate,
                          String lotCode, List<UUID> ids, Pageable pageable) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Lot> lotQuery = builder.createQuery(Lot.class);
    lotQuery = prepareQuery(lotQuery, tradeItems, expirationDate, lotCode, ids, false);

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(countQuery, tradeItems, expirationDate, lotCode, ids, true);

    Long count = entityManager.createQuery(countQuery).getSingleResult();

    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);
    List<Lot> orderableList = entityManager.createQuery(lotQuery)
        .setMaxResults(maxAndFirst.getLeft())
        .setFirstResult(maxAndFirst.getRight())
        .getResultList();
    return Pagination.getPage(orderableList, pageable, count);
  }

  private <T> CriteriaQuery<T> prepareQuery(CriteriaQuery<T> query, Collection<TradeItem>
      tradeItems, LocalDate expirationDate, String lotCode, List<UUID> ids, boolean count) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Root<Lot> root = query.from(Lot.class);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    Predicate predicate = builder.conjunction();

    if (!isEmpty(tradeItems)) {
      predicate = builder.and(predicate, root.get("tradeItem").in(tradeItems));
    }

    if (lotCode != null) {
      predicate = builder.and(predicate,
          builder.like(builder.upper(root.get("lotCode")), "%" + lotCode.toUpperCase() + "%"));
    }

    if (expirationDate != null) {
      predicate = builder.and(predicate, builder.equal(root.get("expirationDate"), expirationDate));
    }

    if (ids != null && ids.size() > 0) {
      predicate = builder.and(predicate, root.get("id").in(ids));
    }

    query.where(predicate);
    return query;
  }
}
