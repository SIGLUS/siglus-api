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
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.hibernate.SQLQuery;
import org.hibernate.type.PostgresUUIDType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.custom.FacilityRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class FacilityRepositoryImpl implements FacilityRepositoryCustom {

  private static final String NATIVE_SELECT_BY_PARAMS = "SELECT DISTINCT f.id AS ID"
      + " FROM referencedata.facilities AS f"
      + " INNER JOIN referencedata.geographic_zones AS g ON f.geographiczoneid = g.id"
      + " INNER JOIN referencedata.facility_types AS t ON f.typeid = t.id";

  private static final String HQL_COUNT = "SELECT DISTINCT COUNT(*)"
      + " FROM Facility AS f"
      + " INNER JOIN f.geographicZone AS g"
      + " INNER JOIN f.type AS t";

  private static final String HQL_SELECT = "SELECT DISTINCT f"
      + " FROM Facility AS f"
      + " INNER JOIN f.geographicZone AS g"
      + " INNER JOIN f.type AS t";

  private static final String WHERE = "WHERE";
  private static final String AND = " AND ";
  private static final String DEFAULT_SORT = "f.name ASC";
  private static final String ORDER_BY = "ORDER BY";

  private static final String WITH_CODE = "UPPER(f.code) LIKE :code";
  private static final String WITH_NAME = "UPPER(f.name) LIKE :name";
  private static final String WITH_ZONE = "g.id IN (:zones)";
  private static final String WITH_IDS = "f.id IN (:ids)";
  private static final String WITH_TYPE = "t.code = :typeCode";
  private static final String WITH_EXTRA_DATA = "f.extradata @> (:extraData)\\:\\:jsonb";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all facilities with matched parameters.
   * Method is ignoring case for facility code and name.
   *
   * @param searchParams      Params to search facilities by.
   * @param geographicZoneIds Geographic zone IDs.
   * @param extraData         extra data
   * @param pageable object used to encapsulate the pagination related values: page, size and sort.
   * @return Page of Facilities matching the parameters.
   */
  public Page<Facility> search(SearchParams searchParams,
      Set<UUID> geographicZoneIds, String extraData, Pageable pageable) {
    if (null != extraData) {
      return searchWithExtraData(searchParams, geographicZoneIds, extraData, pageable);
    }
    return searchWithoutExtraData(searchParams, geographicZoneIds, pageable);
  }

  private Page<Facility> searchWithExtraData(SearchParams searchParams,
      Set<UUID> geographicZoneIds, String extraData, Pageable pageable) {

    Map<String, Object> params = Maps.newHashMap();
    String query =
        prepareQuery(NATIVE_SELECT_BY_PARAMS, searchParams, geographicZoneIds, extraData, params);

    Query nativeQuery = entityManager.createNativeQuery(query);
    params.forEach(nativeQuery::setParameter);

    SQLQuery sqlQuery = nativeQuery.unwrap(SQLQuery.class);
    sqlQuery.addScalar("ID", PostgresUUIDType.INSTANCE);

    // appropriate scalar is added to native query
    @SuppressWarnings("unchecked")
    List<UUID> ids = nativeQuery.getResultList();

    if (isEmpty(ids)) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    String hqlWithSort = Joiner.on(' ').join(Lists.newArrayList(HQL_SELECT, WHERE, WITH_IDS,
        ORDER_BY, PageableUtil.getOrderPredicate(pageable, "f.", DEFAULT_SORT)));

    List<Facility> facilities =  entityManager
        .createQuery(hqlWithSort, Facility.class)
        .setParameter("ids", ids)
        .setMaxResults(pageable.getPageSize())
        .setFirstResult(pageable.getOffset())
        .getResultList();

    return Pagination.getPage(facilities, pageable, ids.size());
  }

  private Page<Facility> searchWithoutExtraData(SearchParams searchParams,
      Set<UUID> geographicZoneIds, Pageable pageable) {

    Map<String, Object> params = Maps.newHashMap();
    Query countQuery = entityManager.createQuery(prepareQuery(
        HQL_COUNT, searchParams, geographicZoneIds, null, params), Long.class);
    params.forEach(countQuery::setParameter);
    Long count = (Long) countQuery.getSingleResult();

    if (count < 1) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    params = Maps.newHashMap();
    String hqlWithSort = Joiner.on(' ').join(Lists.newArrayList(
        prepareQuery(HQL_SELECT, searchParams, geographicZoneIds, null, params),
        ORDER_BY, PageableUtil.getOrderPredicate(pageable, "f.", DEFAULT_SORT)));

    Query searchQuery = entityManager.createQuery(hqlWithSort, Facility.class);
    params.forEach(searchQuery::setParameter);
    List<Facility> facilities =  searchQuery
        .setMaxResults(pageable.getPageSize())
        .setFirstResult(pageable.getOffset())
        .getResultList();

    return Pagination.getPage(facilities, pageable, count);
  }

  private String prepareQuery(String baseSql, SearchParams searchParams,
      Set<UUID> geographicZoneIds, String extraData, Map<String, Object> params) {

    List<String> sql = Lists.newArrayList(baseSql);
    List<String> where = Lists.newArrayList();

    if (isNotBlank(searchParams.getCode())) {
      where.add(WITH_CODE);
      params.put("code", "%" + searchParams.getCode().toUpperCase() + "%");
    }

    if (isNotBlank(searchParams.getName())) {
      where.add(WITH_NAME);
      params.put("name", "%" + searchParams.getName().toUpperCase() + "%");
    }

    if (isNotBlank(searchParams.getFacilityTypeCode())) {
      where.add(WITH_TYPE);
      params.put("typeCode", searchParams.getFacilityTypeCode());
    }

    if (isNotEmpty(geographicZoneIds)) {
      where.add(WITH_ZONE);
      params.put("zones", geographicZoneIds);
    }

    if (isNotBlank(extraData)) {
      where.add(WITH_EXTRA_DATA);
      params.put("extraData", extraData);
    }

    if (isNotEmpty(searchParams.getIds())) {
      where.add(WITH_IDS);
      params.put("ids", searchParams.getIds());
    }

    if (!where.isEmpty()) {
      sql.add(WHERE);
      sql.add(Joiner.on(AND).join(where));
    }

    return Joiner.on(' ').join(sql);
  }
}