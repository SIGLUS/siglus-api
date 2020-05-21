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

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.springframework.data.domain.Pageable;

abstract class IdentitiesSearchableRepository<T> {

  static final Integer MAX_IDENTITIES_SIZE = 500;

  abstract <E> TypedQuery<E> prepareQuery(T searchParams, CriteriaQuery<E> query,
      boolean count, Collection<VersionIdentity> identities, Pageable pageable);

  Long getTotal(T searchParams, Set<Pair<UUID, Long>> identityPairs,
      List<VersionIdentity> identityList, CriteriaBuilder builder, Pageable pageable) {
    Long total = 0L;
    if (!isEmpty(identityPairs)) {
      identityList.addAll(convertPairToVersionIdentity(identityPairs));
      for (List<VersionIdentity> part : ListUtils.partition(identityList, MAX_IDENTITIES_SIZE)) {
        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        total += prepareQuery(searchParams, countQuery, true, part, pageable)
            .getSingleResult();
      }
    } else {
      CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
      total += prepareQuery(searchParams, countQuery, true, identityList, pageable)
          .getSingleResult();
    }
    return total;
  }

  List<VersionIdentity> getIdentities(T searchParams, List<VersionIdentity> identityList,
      CriteriaBuilder builder, Pageable pageable) {
    List<VersionIdentity> identities = new ArrayList<>();
    if (!isEmpty(identityList)) {
      for (List<VersionIdentity> part : ListUtils.partition(identityList, MAX_IDENTITIES_SIZE)) {
        CriteriaQuery<VersionIdentity> query = builder.createQuery(VersionIdentity.class);
        identities.addAll(prepareQuery(searchParams, query, false, part, pageable)
            .getResultList());
      }
    } else {
      CriteriaQuery<VersionIdentity> query = builder.createQuery(VersionIdentity.class);
      identities.addAll(prepareQuery(searchParams, query, false, identityList, pageable)
          .getResultList());
    }
    return identities;
  }

  private List<VersionIdentity> convertPairToVersionIdentity(Set<Pair<UUID, Long>> identityPairs) {
    if (identityPairs.isEmpty()) {
      return Collections.emptyList();
    }
    return identityPairs
        .stream()
        .map(identity -> new VersionIdentity(identity.getLeft(), identity.getRight()))
        .collect(Collectors.toList());
  }

}
