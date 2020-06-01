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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.custom.UserContactDetailsRepositoryCustom;
import org.openlmis.notification.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class UserContactDetailsRepositoryImpl
    extends BaseCustomRepository<UserContactDetails>
    implements UserContactDetailsRepositoryCustom {

  private static final String ID = "referenceDataUserId";
  private static final String EMAIL = "emailDetails.email";

  /**
   * Method returns all matching user contact details. If all parameters are null, returns all user
   * contact details. For email: matches values that equal or contain the searched value. Case
   * insensitive. Other fields: entered string value must equal to searched value.
   *
   * @return Page of user contact details
   */
  public Page<UserContactDetails> search(String email, Collection<UUID> ids, Pageable pageable) {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = createQuery(builder, Long.class, email, ids, pageable);
    Long count = countEntities(countQuery);

    if (isZeroEntities(count)) {
      return Pagination.getPage(Collections.emptyList(), pageable, count);
    }

    CriteriaQuery<UserContactDetails> query = createQuery(builder, UserContactDetails.class, email,
        ids, pageable);
    List<UserContactDetails> entities = getEntities(query, pageable);

    return Pagination.getPage(entities, pageable, count);
  }

  private <T> CriteriaQuery<T> createQuery(CriteriaBuilder builder, Class<T> type, String email,
      Collection<UUID> ids, Pageable pageable) {

    CriteriaQuery<T> query = builder.createQuery(type);
    Root<UserContactDetails> root = query.from(UserContactDetails.class);
    boolean count = Long.class.isAssignableFrom(type);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    query.where(getFilters(builder, root, email, ids));

    if (!count) {
      query.orderBy(getOrderBy(builder, root, pageable));
    }

    return query;
  }

  private Predicate getFilters(CriteriaBuilder builder, Root<UserContactDetails> root,
      String email, Collection<UUID> ids) {

    Predicate predicate = builder.conjunction();
    predicate = addLikeFilter(predicate, builder, root, EMAIL, email);
    predicate = addInFilter(predicate, builder, root, ID, ids);

    return predicate;
  }

}
