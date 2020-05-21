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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.AS;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.FROM;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.ID;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.LEFT_JOIN_FETCH;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.ORDER_BY;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.SELECT;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.SELECT_DISTINCT_COUNT;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.WHERE;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.and;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.asParameter;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.getField;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.in;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.isEqual;
import static org.openlmis.referencedata.repository.custom.impl.SqlConstants.join;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.StreamSupport;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.openlmis.referencedata.domain.SupplyLine;
import org.openlmis.referencedata.repository.custom.SupplyLineRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class SupplyLineRepositoryImpl implements SupplyLineRepositoryCustom {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupplyLineRepositoryImpl.class);

  private static final String SUPPLYING_FACILITY = "supplyingFacility";
  private static final String SUPPLYING_FACILITY_IDS = "supplyingFacilityIds";
  private static final String PROGRAM_ID = "programId";
  private static final String SUPERVISORY_NODE = "supervisoryNode";
  private static final String SUPERVISORY_NODE_ID = SUPERVISORY_NODE + "Id";
  private static final String MEMBER_FACILITIES = "memberFacilities";
  private static final String REQUISITION_GROUP = "requisitionGroup";

  private static final String SUPPLY_LINE_ALIAS = "sl";
  private static final String SUPERVISORY_NODE_ALIAS = "sn";
  private static final String REQUISITION_GROUP_ALIAS = "rg";

  private static final String SUPERVISORY_NODE_JOIN = join(LEFT_JOIN_FETCH,
      getField(SUPPLY_LINE_ALIAS, SUPERVISORY_NODE), AS, SUPERVISORY_NODE_ALIAS);
  private static final String REQUISITION_GROUP_JOIN = join(LEFT_JOIN_FETCH,
      getField(SUPERVISORY_NODE_ALIAS, REQUISITION_GROUP), AS, REQUISITION_GROUP_ALIAS);
  private static final String REQUISITION_GROUP_MEMBERS_JOIN = join(LEFT_JOIN_FETCH,
      getField(REQUISITION_GROUP_ALIAS, MEMBER_FACILITIES));

  private static final String FROM_SL = join(FROM, "SupplyLine", AS, SUPPLY_LINE_ALIAS);
  private static final String SELECT_SL = join(SELECT, SUPPLY_LINE_ALIAS, FROM_SL,
      SUPERVISORY_NODE_JOIN, REQUISITION_GROUP_JOIN, REQUISITION_GROUP_MEMBERS_JOIN);
  private static final String COUNT_SL = join(SELECT_DISTINCT_COUNT, FROM_SL);

  private static final String WITH_PROGRAM_ID =
      isEqual(getField(SUPPLY_LINE_ALIAS, "program", ID), asParameter(PROGRAM_ID));
  private static final String WITH_SUPERVISORY_NODE_ID =
      isEqual(getField(SUPPLY_LINE_ALIAS, SUPERVISORY_NODE, ID), asParameter(SUPERVISORY_NODE_ID));
  private static final String WITH_SUPPLYING_FACILITIES =
      join(getField(SUPPLY_LINE_ALIAS, SUPPLYING_FACILITY, ID), in(SUPPLYING_FACILITY_IDS));

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Method returns a page of supply lines matching parameters.
   * Result can be sorted by supplying facility name if
   * "supplyingFacilityName" asParameter is used in sort property in pageable object.
   * Using expand
   *
   * @param programId            UUID of the program
   * @param supervisoryNodeId    UUID of the supervisory node
   * @param supplyingFacilityIds UUIDs of the supplying facilities
   * @param pageable             pagination and sorting parameters
   * @return page of supply lines with matched parameters.
   */
  @Override
  public Page<SupplyLine> search(UUID programId, UUID supervisoryNodeId,
      Set<UUID> supplyingFacilityIds, Pageable pageable) {

    Profiler profiler = new Profiler("SEARCH_SUPPLY_LINES_WITH_EXPAND_REPOSITORY");
    profiler.setLogger(LOGGER);

    Map<String, Object> params = Maps.newHashMap();
    String whereStatement =
        prepareWhereStatement(programId, supervisoryNodeId, supplyingFacilityIds, params);

    Query countQuery = entityManager.createQuery(join(COUNT_SL, whereStatement), Long.class);
    params.forEach(countQuery::setParameter);
    Long count = (Long) countQuery.getSingleResult();

    if (count < 1) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    Query searchQuery = entityManager.createQuery(
        join(SELECT_SL, whereStatement, getOrderPredicate(pageable)), SupplyLine.class);
    params.forEach(searchQuery::setParameter);

    List<SupplyLine> result = searchQuery
        .setMaxResults(pageable != null ? pageable.getPageSize() : Integer.MAX_VALUE)
        .setFirstResult(pageable != null ? pageable.getOffset() : 0)
        .getResultList();

    return Pagination.getPage(result, pageable, count);
  }

  private String prepareWhereStatement(UUID programId, UUID supervisoryNodeId,
      Set<UUID> supplyingFacilityIds, Map<String, Object> params) {

    List<String> conditions = Lists.newArrayList();

    if (null != programId) {
      conditions.add(WITH_PROGRAM_ID);
      params.put(PROGRAM_ID, programId);
    }

    if (null != supervisoryNodeId) {
      conditions.add(WITH_SUPERVISORY_NODE_ID);
      params.put(SUPERVISORY_NODE_ID, supervisoryNodeId);
    }

    if (isNotEmpty(supplyingFacilityIds)) {
      conditions.add(WITH_SUPPLYING_FACILITIES);
      params.put(SUPPLYING_FACILITY_IDS, supplyingFacilityIds);
    }

    if (isEmpty(conditions)) {
      return "";
    }

    return join(WHERE, and(conditions));
  }

  private String getOrderPredicate(Pageable pageable) {
    if (null != pageable && null != pageable.getSort()) {

      List<String> orderPredicate = StreamSupport.stream(
          Spliterators.spliteratorUnknownSize(pageable.getSort().iterator(), Spliterator.ORDERED),
          false)
          .map(order -> join(
              getField(SUPPLY_LINE_ALIAS, order.getProperty()),
              order.getDirection().toString()))
          .collect(toList());

      return join(ORDER_BY, Joiner.on(", ").join(orderPredicate));
    }

    return "";
  }
}
