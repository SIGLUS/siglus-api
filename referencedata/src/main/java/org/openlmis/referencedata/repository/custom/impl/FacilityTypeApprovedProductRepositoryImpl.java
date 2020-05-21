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
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SQLQuery;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.type.LongType;
import org.hibernate.type.PostgresUUIDType;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.custom.FacilityTypeApprovedProductRepositoryCustom;
import org.openlmis.referencedata.repository.custom.FacilityTypeApprovedProductRepositoryCustom.SearchParams;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@SuppressWarnings({"PMD.TooManyMethods"})
public class FacilityTypeApprovedProductRepositoryImpl
    extends IdentitiesSearchableRepository<SearchParams>
    implements FacilityTypeApprovedProductRepositoryCustom {

  private static final XLogger XLOGGER =
      XLoggerFactory.getXLogger(FacilityTypeApprovedProductRepositoryImpl.class);

  private static final String NATIVE_SELECT_FACILITY_TYPE_ID = "SELECT ft.id AS type_id"
      + " FROM referencedata.facility_types AS ft"
      + " INNER JOIN referencedata.facilities f ON f.typeId = ft.id AND f.id = '%s'";

  private static final String FROM_FTAP_TABLE =
      " FROM referencedata.facility_type_approved_products AS ftap";

  private static final String NATIVE_COUNT_FTAPS = "SELECT COUNT(*)" + FROM_FTAP_TABLE;

  private static final String NATIVE_SELECT_FTAP_IDENTITIES = "SELECT DISTINCT"
      + "   ftap.id AS id,"
      + "   ftap.versionNumber AS versionNumber"
      + FROM_FTAP_TABLE;
  private static final String NATIVE_PROGRAM_INNER_JOIN =
      " INNER JOIN referencedata.programs AS p ON p.id = ftap.programId";
  private static final String NATIVE_ORDERABLE_INNER_JOIN =
      " INNER JOIN (SELECT id, MAX(versionNumber) AS versionNumber"
          + "   FROM referencedata.orderables GROUP BY id) AS o"
          + "   ON o.id = ftap.orderableId";
  private static final String NATIVE_PROGRAM_ORDERABLE_INNER_JOIN =
      " INNER JOIN referencedata.program_orderables AS po"
          + " ON o.id = po.orderableId"
          + " AND o.versionNumber = po.orderableVersionNumber"
          + " AND p.id = po.programId"
          + " AND po.active IS TRUE";
  private static final String NATIVE_FACILITY_TYPE_INNER_JOIN =
      " INNER JOIN referencedata.facility_types AS ft ON ft.id = ftap.facilityTypeId";
  private static final String NATIVE_LATEST_FTAPS_INNER_JOIN =
      " INNER JOIN (SELECT id, MAX(versionNumber) AS versionNumber"
          + "   FROM referencedata.facility_type_approved_products GROUP BY id) AS latest"
          + "   ON ftap.id = latest.id AND ftap.versionNumber = latest.versionNumber";
  private static final String NATIVE_FTAP_ACTIVE_FLAG = " ftap.active = :active";

  private static final String NATIVE_PAGEABLE = " LIMIT :limit OFFSET :offset";

  private static final String WHERE = " WHERE ";
  private static final String IDENTITY = "identity";
  private static final String ID = "id";
  private static final String VERSION_NUMBER = "versionNumber";
  private static final String ACTIVE = "active";
  private static final String FTAP_ALIAS = "ftap";
  private static final String PROGRAM = "program";
  private static final String CODE = "code";
  private static final String ORDERABLE_ID = "orderableId";
  private static final String FACILITY_TYPE = "facilityType";
  private static final String PROGRAM_ALIAS = "p";
  private static final String FACILITY_TYPE_ALIAS = "ft";

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<FacilityTypeApprovedProduct> searchProducts(UUID facilityId, UUID programId,
      Boolean fullSupply, List<UUID> orderableIds, Boolean active, Pageable pageable) {

    Profiler profiler = new Profiler("FTAP_REPOSITORY_SEARCH");
    profiler.setLogger(XLOGGER);

    profiler.start("SEARCH_FACILITY_TYPE_ID");
    UUID facilityTypeId = getFacilityTypeId(facilityId, profiler);

    profiler.start("CALCULATE_FULL_LIST_SIZE");
    Query countNativeQuery = prepareNativeQuery(facilityTypeId, programId, fullSupply, orderableIds,
        active, true, pageable);

    int total = executeCountQuery(countNativeQuery);

    if (total <= 0) {
      profiler.stop().log();
      return Pagination.getPage(Collections.emptyList());
    }

    profiler.start("GET_VERSION_IDENTITY");
    Query nativeQuery = prepareNativeQuery(facilityTypeId, programId, fullSupply, orderableIds,
        active, false, pageable);
    List<VersionIdentity> identities = executeNativeQuery(nativeQuery);

    profiler.start("RETRIEVE_FTAPS");
    List<FacilityTypeApprovedProduct> ftaps = new ArrayList<>();
    for (List<VersionIdentity> partition : ListUtils.partition(identities, MAX_IDENTITIES_SIZE)) {
      ftaps.addAll(retrieveFtaps(partition));
    }

    profiler.stop().log();
    return Pagination.getPage(ftaps, pageable, total);
  }

  @Override
  public Page<FacilityTypeApprovedProduct> searchProducts(SearchParams searchParams,
      Pageable pageable) {
    Profiler profiler = new Profiler("FTAP_REPOSITORY_SEARCH_BY_PARAMS");
    profiler.setLogger(XLOGGER);

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    profiler.start("CALCULATE_FULL_LIST_SIZE");
    List<VersionIdentity> identityList = new ArrayList<>();
    Set<Pair<UUID, Long>> identityPairs = searchParams.getIdentityPairs();
    Long total = getTotal(searchParams, identityPairs, identityList, builder, pageable);

    if (total <= 0) {
      profiler.stop().log();
      return Pagination.getPage(Collections.emptyList(), pageable);
    }

    profiler.start("GET_VERSION_IDENTITY");
    List<VersionIdentity> identities = getIdentities(searchParams, identityList, builder, pageable);

    profiler.start("RETRIEVE_FTAPS");
    List<FacilityTypeApprovedProduct> ftaps = new ArrayList<>();
    for (List<VersionIdentity> partition : ListUtils.partition(identities, MAX_IDENTITIES_SIZE)) {
      ftaps.addAll(retrieveFtaps(partition));
    }

    profiler.stop().log();
    return Pagination.getPage(ftaps, pageable, total);
  }

  private UUID getFacilityTypeId(UUID facilityId, Profiler profiler) {
    String queryString = String.format(NATIVE_SELECT_FACILITY_TYPE_ID, facilityId);
    Query query = entityManager.createNativeQuery(queryString);

    SQLQuery sql = query.unwrap(SQLQuery.class);
    sql.addScalar("type_id", PostgresUUIDType.INSTANCE);

    try {
      return (UUID) query.getSingleResult();
    } catch (Exception ex) {
      profiler.stop().log();
      throw new ValidationMessageException(ex, FacilityMessageKeys.ERROR_NOT_FOUND);
    }
  }

  @Override
  <E> TypedQuery<E> prepareQuery(SearchParams searchParams, CriteriaQuery<E> query,
      boolean count, Collection<VersionIdentity> identities, Pageable pageable) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Root<FacilityTypeApprovedProduct> root = query.from(FacilityTypeApprovedProduct.class);
    root.alias(FTAP_ALIAS);

    CriteriaQuery<E> newQuery;

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      newQuery = (CriteriaQuery<E>) countQuery.select(builder.count(root));
    } else {
      CriteriaQuery<VersionIdentity> typeQuery = (CriteriaQuery<VersionIdentity>) query;
      newQuery = (CriteriaQuery<E>) typeQuery.select(root.get(IDENTITY));
    }

    Predicate predicate = builder.conjunction();

    String programCode = searchParams.getProgramCode();
    if (isNotBlank(programCode)) {
      Join<FacilityTypeApprovedProduct, Program> programsJoin =
          root.join(PROGRAM, JoinType.INNER);
      programsJoin.alias(PROGRAM_ALIAS);
      predicate = builder.and(predicate, builder.equal(builder.lower(programsJoin.get(CODE)),
          Code.code(programCode.toLowerCase())));
    }

    Set<UUID> orderableIds = searchParams.getOrderableIds();
    if (!isEmpty(orderableIds)) {
      predicate = builder.and(predicate, root.get(ORDERABLE_ID)).in(orderableIds);
    }

    Set<String> facilityTypeCodes = searchParams.getFacilityTypeCodes();
    if (!isEmpty(facilityTypeCodes)) {
      Join<FacilityTypeApprovedProduct, FacilityType> facilityTypeJoin = root
          .join(FACILITY_TYPE, JoinType.INNER);
      facilityTypeJoin.alias(FACILITY_TYPE_ALIAS);
      predicate = builder.and(predicate, facilityTypeJoin.get(CODE).in(facilityTypeCodes));
    }

    if (!isEmpty(identities)) {
      predicate = builder.and(predicate, builder.in(root.get(IDENTITY)).value(identities));
    } else {
      Subquery<String> latestFtapQuery = createFtapSubQuery(newQuery, builder);
      predicate = builder.and(predicate, builder.in(builder.concat(
          root.get(IDENTITY).get(ID).as(String.class),
          root.get(IDENTITY).get(VERSION_NUMBER).as(String.class)))
          .value(latestFtapQuery));
    }

    Boolean isActive = searchParams.getActive();
    if (null != isActive) {
      predicate = builder.and(predicate, builder.equal(root.get(ACTIVE),
          isActive));
    }

    newQuery.where(predicate);

    if (!count) {
      return entityManager.createQuery(query)
          .setMaxResults(pageable.getPageSize())
          .setFirstResult(pageable.getOffset());
    }

    return entityManager.createQuery(newQuery);
  }

  private Query prepareNativeQuery(UUID facilityTypeId, UUID programId, Boolean fullSupply,
      List<UUID> orderableIds, Boolean active, boolean count, Pageable pageable) {
    String startNativeQuery = count ? NATIVE_COUNT_FTAPS : NATIVE_SELECT_FTAP_IDENTITIES;
    StringBuilder builder = new StringBuilder(startNativeQuery);
    Map<String, Object> params = Maps.newHashMap();

    builder.append(NATIVE_PROGRAM_INNER_JOIN);
    if (null != programId) {
      builder.append(" AND p.id = :programId");
      params.put("programId", programId);
    }

    builder.append(NATIVE_ORDERABLE_INNER_JOIN);
    if (!isEmpty(orderableIds)) {
      builder.append(" AND o.id in (:orderableIds)");
      params.put("orderableIds", orderableIds);
    }

    builder.append(NATIVE_PROGRAM_ORDERABLE_INNER_JOIN);
    if (null != fullSupply) {
      builder.append(" AND po.fullSupply = :fullSupply");
      params.put("fullSupply", fullSupply);
    }

    builder.append(NATIVE_FACILITY_TYPE_INNER_JOIN);
    if (null != facilityTypeId) {
      builder.append(" AND ft.id = :facilityTypeId");
      params.put("facilityTypeId", facilityTypeId);
    }

    builder
        .append(NATIVE_LATEST_FTAPS_INNER_JOIN)
        .append(WHERE)
        .append(NATIVE_FTAP_ACTIVE_FLAG);
    params.put(ACTIVE, null == active || active);

    if (!count) {
      setPagination(builder, params, pageable);
    }

    return createNativeQuery(builder, params, count);
  }

  private void setPagination(StringBuilder builder, Map<String, Object> params, Pageable pageable) {
    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);
    Integer limit = maxAndFirst.getLeft();
    Integer offset = maxAndFirst.getRight();

    if (limit > 0) {
      builder.append(NATIVE_PAGEABLE);
      params.put("limit", limit);
      params.put("offset", offset);
    }
  }

  private Query createNativeQuery(StringBuilder builder, Map<String, Object> params,
      boolean count) {
    Query nativeQuery = entityManager.createNativeQuery(builder.toString());
    params.forEach(nativeQuery::setParameter);

    if (!count) {
      SQLQuery sqlQuery = nativeQuery.unwrap(SQLQuery.class);
      sqlQuery.addScalar(ID, PostgresUUIDType.INSTANCE);
      sqlQuery.addScalar(VERSION_NUMBER, LongType.INSTANCE);
    }

    return nativeQuery;
  }

  private int executeCountQuery(Query nativeQuery) {
    return ((Number) nativeQuery.getSingleResult()).intValue();
  }

  private List<VersionIdentity> executeNativeQuery(Query nativeQuery) {
    // appropriate configuration has been set in the native query
    @SuppressWarnings("unchecked")
    List<Object[]> identities = nativeQuery.getResultList();

    return identities
        .stream()
        .map(identity -> new VersionIdentity((UUID) identity[0], (Long) identity[1]))
        .collect(Collectors.toList());
  }

  private Subquery<String> createFtapSubQuery(CriteriaQuery query, CriteriaBuilder builder) {
    Subquery<String> latestFtapsQuery = query.subquery(String.class);
    Root<FacilityTypeApprovedProduct> latestFtapsRoot =
        latestFtapsQuery.from(FacilityTypeApprovedProduct.class);
    latestFtapsRoot.alias("latestFtap");

    latestFtapsQuery.select(
        builder.concat(
            latestFtapsRoot.get(IDENTITY).get(ID).as(String.class),
            builder.max(latestFtapsRoot.get(IDENTITY).get(VERSION_NUMBER)).as(String.class)));
    latestFtapsQuery.groupBy(latestFtapsRoot.get(IDENTITY).get(ID));

    return latestFtapsQuery;
  }

  // appropriate class has been passed in the EntityManager.createNativeQuery method
  @SuppressWarnings("unchecked")
  private List<FacilityTypeApprovedProduct> retrieveFtaps(Collection<VersionIdentity> identities) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<FacilityTypeApprovedProduct> criteriaQuery =
        criteriaBuilder.createQuery(FacilityTypeApprovedProduct.class);
    Root<FacilityTypeApprovedProduct> root = criteriaQuery.from(FacilityTypeApprovedProduct.class);
    criteriaQuery.select(root).where(root.get(IDENTITY).in(identities));

    return entityManager
        .createQuery(criteriaQuery)
        .unwrap(org.hibernate.Query.class)
        .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
        .list();
  }
}
