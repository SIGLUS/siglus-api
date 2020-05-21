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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.domain.TradeItemClassification;
import org.openlmis.referencedata.repository.custom.TradeItemRepositoryCustom;

public class TradeItemRepositoryImpl implements TradeItemRepositoryCustom {

  private static final String CLASSIFICATION_ID = "classificationId";
  private static final String CLASSIFICATIONS = "classifications";

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Iterable<TradeItem> findByClassificationId(String classificationId) {
    return searchByClassificationId(classificationId, true);
  }

  @Override
  public Iterable<TradeItem> findByClassificationIdLike(String classificationId) {
    return searchByClassificationId(classificationId, false);
  }

  private Iterable<TradeItem> searchByClassificationId(String classificationId,
                                                       boolean fullMatch) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<TradeItem> query = criteriaBuilder.createQuery(TradeItem.class);
    Root<TradeItem> tiRoot = query.from(TradeItem.class);
    Join<TradeItem, TradeItemClassification> join = tiRoot.join(CLASSIFICATIONS, JoinType.LEFT);
    query.groupBy(tiRoot);

    if (fullMatch) {
      query.where(criteriaBuilder.equal(join.get(CLASSIFICATION_ID), classificationId));
    } else {
      query.where(criteriaBuilder.like(join.get(CLASSIFICATION_ID),
          '%' + classificationId + '%'));
    }

    return entityManager.createQuery(query).getResultList();
  }
}
