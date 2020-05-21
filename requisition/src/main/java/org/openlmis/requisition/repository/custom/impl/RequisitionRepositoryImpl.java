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

package org.openlmis.requisition.repository.custom.impl;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.base.Joiner;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SQLQuery;
import org.hibernate.type.BooleanType;
import org.hibernate.type.LongType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.ZonedDateTimeType;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionPeriod;
import org.openlmis.requisition.domain.requisition.RequisitionPermissionString;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.repository.StatusChangeRepository;
import org.openlmis.requisition.repository.custom.RequisitionRepositoryCustom;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.utils.Pagination;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionRepositoryImpl
    extends BaseCustomRepository<Requisition>
    implements RequisitionRepositoryCustom {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(RequisitionRepositoryImpl.class);

  private static final String ORDER_BY = " ORDER BY ";

  private static final String FROM = " FROM requisition.requisitions r"
      + " INNER JOIN requisition.status_changes s ON r.id = s.requisitionid"
      + " WHERE r.status = 'APPROVED'"
      + " AND s.status = 'APPROVED'";

  private static final String SEARCH_APPROVED_SQL = "SELECT DISTINCT"
      + " r.id AS req_id, r.emergency AS req_emergency,"
      + " r.facilityid AS facility_id, r.programid AS program_id,"
      + " r.processingperiodid as period_id, r.supervisorynodeid as node_id,"
      + " s.createdDate as approved_date"
      + FROM;

  private static final String SELECT_COUNT_APPROVED_SQL = "SELECT DISTINCT COUNT(*)"
      + FROM;

  private static final String FACILITY_ID = "facilityId";
  private static final String PROGRAM_ID = "programId";
  private static final String EMERGENCY = "emergency";
  private static final String STATUS = "status";
  private static final String CREATED_DATE = "createdDate";
  private static final String PROCESSING_PERIOD_ID = "processingPeriodId";
  private static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";
  private static final String AUTHORIZED_DATE = "authorizedDate";
  private static final String MODIFIED_DATE = "modifiedDate";

  @PersistenceContext
  private EntityManager entityManager;

  @Autowired
  private StatusChangeRepository statusChangeRepository;

  /**
   * Method returns all Requisitions with matched parameters. User permission strings must not be
   * empty.
   *
   * @param params It contains parameters which have to be matched by requisition.
   * @param userPermissionStrings Permission strings of current user.
   * @param programNodePairs program / supervisoryNode pairs
   * @return Page of Requisitions with matched parameters.
   */
  @Override
  public Page<Requisition> searchRequisitions(RequisitionSearchParams params,
      List<String> userPermissionStrings, Set<Pair<UUID, UUID>> programNodePairs,
      Pageable pageable) {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(builder, countQuery, params,
        userPermissionStrings, programNodePairs, true, pageable);

    Long count = countEntities(countQuery);

    if (isZeroEntities(count)) {
      return Pagination.getPage(Collections.emptyList(), pageable, count);
    }

    CriteriaQuery<Requisition> requisitionQuery = builder.createQuery(Requisition.class);
    requisitionQuery = prepareQuery(builder, requisitionQuery, params,
        userPermissionStrings, programNodePairs, false, pageable);

    List<Requisition> requisitions = getEntities(requisitionQuery, pageable);
    return Pagination.getPage(requisitions, pageable, count);
  }


  /**
   * Method returns all Requisitions with matched parameters.
   *
   * @param processingPeriod ProcessingPeriod of searched Requisitions.
   * @param emergency        if {@code true}, the method will look only for emergency requisitions,
   *                         if {@code false}, the method will look only for standard requisitions,
   *                         if {@code null} the method will check all requisitions.
   * @return List of Requisitions with matched parameters.
   */
  @Override
  public List<Requisition> searchRequisitions(UUID processingPeriod, UUID facility,
      UUID program, Boolean emergency) {
    CriteriaBuilder builder = getCriteriaBuilder();

    CriteriaQuery<Requisition> query = builder.createQuery(Requisition.class);
    Root<Requisition> root = query.from(Requisition.class);

    Predicate predicate = builder.conjunction();
    predicate = addEqualFilter(predicate, builder, root, EMERGENCY, emergency);
    predicate = addEqualFilter(predicate, builder, root, PROCESSING_PERIOD_ID, processingPeriod);
    predicate = addEqualFilter(predicate, builder, root, FACILITY_ID, facility);
    predicate = addEqualFilter(predicate, builder, root, PROGRAM_ID, program);

    query.where(predicate);

    return entityManager.createQuery(query).getResultList();
  }

  /**
   * Method returns all ids and statues of requisitions with matched parameters.
   *
   * @param emergency        if {@code true}, the method will look only for emergency requisitions,
   *                         if {@code false}, the method will look only for standard requisitions,
   *                         if {@code null} the method will check all requisitions.
   * @return List of Map of UUID and Requisition Status with matched parameters.
   */
  @Override
  public List<RequisitionPeriod> searchRequisitionIdAndStatusPairs(UUID facility,
      UUID program, Boolean emergency) {
    CriteriaBuilder builder = getCriteriaBuilder();

    Profiler profiler = new Profiler("SEARCH_REQUISITION_ID_STATUS");
    profiler.setLogger(XLOGGER);

    CriteriaQuery<RequisitionPeriod> query = builder.createQuery(RequisitionPeriod.class);
    Root<Requisition> root = query.from(Requisition.class);
    query.multiselect(root.get("id"), root.get(STATUS), root.get(PROCESSING_PERIOD_ID));

    Predicate predicate = builder.conjunction();
    predicate = addEqualFilter(predicate, builder, root, EMERGENCY, emergency);
    predicate = addEqualFilter(predicate, builder, root, FACILITY_ID, facility);
    predicate = addEqualFilter(predicate, builder, root, PROGRAM_ID, program);
    query.where(predicate);

    profiler.start("EXECUTE_QUERY_TO_FIND_REQUISITION_PERIODS");
    List<RequisitionPeriod> requisitionIdStatusList = entityManager.createQuery(query)
        .getResultList();

    XLOGGER.exit(requisitionIdStatusList);
    profiler.stop().log();
    return requisitionIdStatusList;
  }

  /**
   * Get approved requisitions matching all of provided parameters.
   * Empty list is returned if:
   * - facilityIds collection is empty and filterBy is facilityName or facilityCode,
   * - programIds collection is empty and filterBy is programName,
   * - both facilityIds and programIds collections are empty and filterBy is all.
   *
   * @param facilityId             Desired facility UUID list.
   * @param programSupervisoryNode Desired set of program UUIDs.
   * @return List of requisitions with required fields for convert.
   */
  @Override
  public Page<Requisition> searchApprovedRequisitions(UUID facilityId,
      Set<Pair<UUID, UUID>> programSupervisoryNode, Pageable pageable) {
    XLOGGER.entry(facilityId, programSupervisoryNode, pageable);

    Query countQuery = createQuery(facilityId, programSupervisoryNode, true, pageable);
    Long count = (Long) countQuery.getSingleResult();

    if (count == 0) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    Query searchQuery = createQuery(facilityId, programSupervisoryNode, false, pageable);
    addScalars(searchQuery);

    // hibernate always returns a list of array of objects
    @SuppressWarnings("unchecked")
    List<Object[]> list = Collections.checkedList(searchQuery
            .setFirstResult(pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList(),
            Object[].class);

    List<Requisition> requisitions = list.stream().map(this::toRequisition)
        .collect(toList());

    XLOGGER.exit(requisitions);
    return Pagination.getPage(requisitions, pageable, count);
  }

  /**
   * Get all requisitions that match any of the program/supervisoryNode pairs, that can be
   * approved (AUTHORIZED, IN_APPROVAL). Pairs must not be null.
   *
   * @param programNodePairs program / supervisoryNode pairs
   * @return matching requisitions
   */
  @Override
  public Page<Requisition> searchApprovableRequisitionsByProgramSupervisoryNodePairs(
      Set<Pair<UUID, UUID>> programNodePairs, Pageable pageable) {
    XLOGGER.entry(programNodePairs, pageable);

    Profiler profiler = new Profiler("SEARCH_APPROBABLE_REQ_BY_PROGRAM_SUP_NODE_PAIRS");
    profiler.setLogger(XLOGGER);

    profiler.start("CREATE_BUILDER");
    CriteriaBuilder builder = getCriteriaBuilder();

    profiler.start("PREPARE_COUNT_QUERY");
    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareApprovableQuery(builder, countQuery, programNodePairs, true, pageable);

    profiler.start("EXECUTE_COUNT_QUERY");
    Long count = countEntities(countQuery);

    if (isZeroEntities(count)) {
      profiler.start("CREATE_RESULT_PAGE");
      Page<Requisition> page = Pagination.getPage(Collections.emptyList(), pageable, count);

      XLOGGER.exit(page);
      profiler.stop().log();

      return page;
    }

    profiler.start("GET_MAX_AND_FIRST");
    final Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);

    profiler.start("PREPARE_MAIN_QUERY");
    CriteriaQuery<Requisition> query = builder.createQuery(Requisition.class);
    query = prepareApprovableQuery(builder, query, programNodePairs, false, pageable);

    profiler.start("EXECUTE_MAIN_QUERY");
    List<Requisition> requisitions = entityManager.createQuery(query)
        .setMaxResults(maxAndFirst.getLeft())
        .setFirstResult(maxAndFirst.getRight())
        .getResultList();

    profiler.start("GET_REQUISITIONS_IDS");
    Set<UUID> requisitionIds = requisitions
        .stream()
        .map(BaseEntity::getId)
        .collect(Collectors.toSet());

    profiler.start("GET_STATUS_CHANGES_BY_REQ_IDS");
    Map<UUID, List<StatusChange>> allStatusChanges = statusChangeRepository
        .findByRequisitionIdIn(requisitionIds)
        .stream()
        .collect(Collectors.groupingBy(status -> status.getRequisition().getId()));

    profiler.start("MATCH_REQ_WITH_STATUS_CHANGES");
    requisitions
        .forEach(requisition -> {
          List<StatusChange> statusChanges = allStatusChanges.get(requisition.getId());
          requisition.setStatusChanges(statusChanges);
        });

    profiler.start("CREATE_RESULT_PAGE");
    Page<Requisition> page = Pagination.getPage(requisitions, pageable, count);

    XLOGGER.exit(page);
    profiler.stop().log();

    return page;
  }

  private <T> CriteriaQuery<T> prepareQuery(CriteriaBuilder builder, CriteriaQuery<T> query,
      RequisitionSearchParams params, List<String> userPermissionStrings,
      Set<Pair<UUID, UUID>> programNodePairs, boolean count, Pageable pageable) {

    Root<Requisition> root = query.from(Requisition.class);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    } else {
      query.orderBy(builder.asc(root.get(CREATED_DATE)));
    }

    Predicate predicate = builder.conjunction();
    predicate = addEqualFilter(predicate, builder, root, FACILITY_ID, params.getFacility());
    predicate = addEqualFilter(predicate, builder, root, PROGRAM_ID, params.getProgram());
    predicate = addEqualFilter(predicate, builder, root,
        PROCESSING_PERIOD_ID, params.getProcessingPeriod());
    predicate = addEqualFilter(predicate, builder, root,
        SUPERVISORY_NODE_ID, params.getSupervisoryNode());
    predicate = addInFilter(predicate, builder, root, STATUS, params.getRequisitionStatuses());
    predicate = addEqualFilter(predicate, builder, root, EMERGENCY, params.getEmergency());

    ZonedDateTime fromInitiatedDate = setStartDateParam(params.getInitiatedDateFrom());
    ZonedDateTime toInitiatedDate = setEndDateParam(params.getInitiatedDateTo());
    predicate = addDateRangeFilter(predicate, builder, root,
        CREATED_DATE, fromInitiatedDate, toInitiatedDate);


    ZonedDateTime fromModifiedDate = setStartDateParam(params.getModifiedDateFrom());
    ZonedDateTime toModifiedDate = setEndDateParam(params.getModifiedDateTo());
    predicate = addDateRangeFilter(predicate, builder, root,
        MODIFIED_DATE, fromModifiedDate, toModifiedDate);

    predicate = createPermissionPredicate(
        builder, root, predicate, userPermissionStrings, programNodePairs);

    query.where(predicate);

    query.distinct(true);

    if (!count && pageable != null && pageable.getSort() != null) {
      query = addSortProperties(builder, query, root, pageable);
    }

    return query;
  }

  private Predicate createPermissionPredicate(CriteriaBuilder builder, Root<Requisition> root,
      Predicate predicate, List<String> userPermissionStrings,
      Set<Pair<UUID, UUID>> programNodePairs) {
    if (userPermissionStrings.isEmpty() && programNodePairs.isEmpty()) {
      return predicate;
    }

    if (!userPermissionStrings.isEmpty() && programNodePairs.isEmpty()) {
      return builder.and(predicate,
          createPermissionStringsPredicate(root, userPermissionStrings));
    }

    if (userPermissionStrings.isEmpty()) {
      return builder.and(predicate,
          createProgramNodePairPredicate(builder, root, programNodePairs));
    }

    return builder.and(predicate,
        builder.or(
            createPermissionStringsPredicate(root, userPermissionStrings),
            createProgramNodePairPredicate(builder, root, programNodePairs)));
  }

  private Predicate createPermissionStringsPredicate(Root<Requisition> root,
      List<String> userPermissionStrings) {
    Join<Requisition, RequisitionPermissionString> permissionStringJoin = root
        .join("permissionStrings");
    Expression<String> permissionStringExp = permissionStringJoin.get("permissionString");

    return permissionStringExp.in(userPermissionStrings);
  }

  private <T> CriteriaQuery<T> prepareApprovableQuery(CriteriaBuilder builder,
      CriteriaQuery<T> query, Set<Pair<UUID, UUID>> programNodePairs,
      boolean isCountQuery, Pageable pageable) {

    Root<Requisition> root = query.from(Requisition.class);

    if (isCountQuery) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    Predicate pairPredicate = createProgramNodePairPredicate(builder, root, programNodePairs);

    Predicate statusPredicate = root
        .get(STATUS)
        .in(RequisitionStatus.AUTHORIZED, RequisitionStatus.IN_APPROVAL);

    Predicate predicate = builder.and(pairPredicate, statusPredicate);

    if (!isCountQuery) {
      Subquery<ZonedDateTime> subquery = query.subquery(ZonedDateTime.class);
      Root<StatusChange> subRoot = subquery.from(StatusChange.class);

      subquery.select(builder.greatest(subRoot.<ZonedDateTime>get(CREATED_DATE)));
      subquery.where(builder.and(
          builder.equal(subRoot.get(STATUS), RequisitionStatus.AUTHORIZED),
          builder.equal(subRoot.get("requisition"), root)));

      // partner requisitions (in the IN_APPROVAL status) does not have status changes
      ListJoin<Object, Object> statusChanges = root
          .joinList(Requisition.STATUS_CHANGES, JoinType.LEFT);

      statusChanges.on(builder.equal(statusChanges.get(STATUS), RequisitionStatus.AUTHORIZED));

      predicate = builder
          .and(predicate, builder
              .or(statusChanges.isNull(), statusChanges.get(CREATED_DATE).in(subquery)));
    }

    if (!isCountQuery && pageable != null && pageable.getSort() != null) {
      query = addSortProperties(builder, query, root, pageable);
    }

    return query.where(predicate);
  }

  private Predicate createProgramNodePairPredicate(CriteriaBuilder builder,
      Root<Requisition> root, Set<Pair<UUID, UUID>> programNodePairs) {
    Predicate[] combinedPredicates = new Predicate[programNodePairs.size()];

    int index = 0;
    for (Pair pair : programNodePairs) {
      Predicate predicate = builder.conjunction();
      predicate = addEqualFilter(predicate, builder, root, PROGRAM_ID, pair.getLeft());
      predicate = addEqualFilter(predicate, builder, root, SUPERVISORY_NODE_ID, pair.getRight());

      combinedPredicates[index++] = predicate;
    }

    return builder.or(combinedPredicates);
  }

  private <T> CriteriaQuery<T> addSortProperties(CriteriaBuilder builder,
      CriteriaQuery<T> query, Root<Requisition> root, Pageable pageable) {
    List<Order> orders = new ArrayList<>();
    Iterator<Sort.Order> iterator = pageable.getSort().iterator();
    Sort.Order order;

    while (iterator.hasNext()) {
      order = iterator.next();
      String property = order.getProperty();

      Path path;

      if (AUTHORIZED_DATE.equals(property)) {
        path = root
            .getJoins()
            .stream()
            .filter(item -> Requisition.STATUS_CHANGES.equals(item.getAttribute().getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Can't find statusChanges join"))
            .get(CREATED_DATE);
      } else {
        path = root.get(property);
      }

      if (order.isAscending()) {
        orders.add(builder.asc(path));
      } else {
        orders.add(builder.desc(path));
      }
    }

    return query.orderBy(orders);
  }

  private Query createQuery(UUID facilityId, Set<Pair<UUID, UUID>> programSupervisoryNode,
      Boolean count, Pageable pageable) {
    StringBuilder builder =
        new StringBuilder(count ? SELECT_COUNT_APPROVED_SQL : SEARCH_APPROVED_SQL);

    if (null != facilityId) {
      builder.append(String.format(" AND r.facilityid = '%s'", facilityId));
    }
    if (isNotEmpty(programSupervisoryNode)) {
      builder.append(" AND (");
      List<String> programAndSupervisoryNodeConditions = programSupervisoryNode.stream()
          .map(this::createQueryForProgramAndNode)
          .collect(toList());
      builder.append(Joiner.on(" OR ").join(programAndSupervisoryNodeConditions));
      builder.append(')');
    }

    if (!count && pageable.getSort() != null) {
      builder.append(ORDER_BY);
      builder.append(getOrderPredicate(pageable));
    }

    Query query = entityManager.createNativeQuery(builder.toString());

    if (count) {
      addScalarsForCount(query);
    }

    return query;
  }

  private void addScalars(Query query) {
    SQLQuery sql = query.unwrap(SQLQuery.class);
    sql.addScalar("req_id", PostgresUUIDType.INSTANCE);
    sql.addScalar("req_emergency", BooleanType.INSTANCE);
    sql.addScalar("facility_id", PostgresUUIDType.INSTANCE);
    sql.addScalar("program_id", PostgresUUIDType.INSTANCE);
    sql.addScalar("period_id", PostgresUUIDType.INSTANCE);
    sql.addScalar("node_id", PostgresUUIDType.INSTANCE);
    sql.addScalar("approved_date", ZonedDateTimeType.INSTANCE);
  }

  private void addScalarsForCount(Query query) {
    SQLQuery sql = query.unwrap(SQLQuery.class);
    sql.addScalar("count", LongType.INSTANCE);
  }

  private Requisition toRequisition(Object[] values) {
    Requisition requisition = new Requisition();

    requisition.setId((UUID) values[0]);
    requisition.setEmergency((Boolean) values[1]);
    requisition.setFacilityId((UUID) values[2]);
    requisition.setProgramId((UUID) values[3]);
    requisition.setProcessingPeriodId((UUID) values[4]);
    requisition.setSupervisoryNodeId((UUID) values[5]);

    StatusChange statusChange = new StatusChange();
    statusChange.setCreatedDate((ZonedDateTime) values[6]);
    statusChange.setStatus(RequisitionStatus.APPROVED);
    requisition.setStatusChanges(Collections.singletonList(statusChange));

    return requisition;
  }

  private String getOrderPredicate(Pageable pageable) {
    List<String> orderPredicate = new ArrayList<>();
    List<String> sql = new ArrayList<>();
    Iterator<Sort.Order> iterator = pageable.getSort().iterator();
    Sort.Order order;
    Sort.Direction sortDirection = Sort.Direction.ASC;

    while (iterator.hasNext()) {
      order = iterator.next();
      orderPredicate.add("r.".concat(order.getProperty()));
      sortDirection = order.getDirection();
    }

    sql.add(Joiner.on(",").join(orderPredicate));
    sql.add(sortDirection.name());

    return Joiner.on(' ').join(sql);
  }

  private String createQueryForProgramAndNode(Pair<UUID, UUID> programSupervisoryNode) {
    UUID programId = programSupervisoryNode.getLeft();
    UUID supervisoryNodeId = programSupervisoryNode.getRight();

    if (null != programId && null != supervisoryNodeId) {
      return String.format("(r.programId = '%s' AND r.supervisoryNodeId = '%s')",
          programId, supervisoryNodeId);
    } else if (null != programId) {
      return String.format("r.programId = '%s'", programId);
    } else if (null != supervisoryNodeId) {
      return String.format("r.supervisoryNodeId = '%s'", supervisoryNodeId);
    }
    return "";
  }
}
