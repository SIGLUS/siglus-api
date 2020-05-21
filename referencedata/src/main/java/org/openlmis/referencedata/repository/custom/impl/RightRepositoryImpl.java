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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.referencedata.domain.Right;
import org.openlmis.referencedata.domain.RightType;
import org.openlmis.referencedata.repository.custom.RightRepositoryCustom;

public class RightRepositoryImpl implements RightRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Method returns all rights with matched parameters.
   * If all parameters are null, returns all rights.
   *
   * @param name name of right.
   * @param type type of right.
   * @return Set of rights
   */
  public Set<Right> searchRights(String name, RightType type) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Right> query = builder.createQuery(Right.class);
    Root<Right> root = query.from(Right.class);
    Predicate predicate = builder.conjunction();
    predicate = addEqualsFilter(predicate, builder, root, "name", name);
    predicate = addEqualsFilter(predicate, builder, root, "type", type);
    query.where(predicate);
    List<Right> results = entityManager.createQuery(query).getResultList();
    return new HashSet<>(results);
  }

  private Predicate addEqualsFilter(Predicate predicate, CriteriaBuilder builder, Root root,
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
}
