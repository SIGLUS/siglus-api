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
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.hibernate.SQLQuery;
import org.hibernate.type.PostgresUUIDType;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.repository.custom.ProcessingPeriodRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class ProcessingPeriodRepositoryImpl implements ProcessingPeriodRepositoryCustom {

  private static final String SELECT_PERIODS = "SELECT DISTINCT pp"
      + " FROM ProcessingPeriod AS pp";

  private static final String COUNT_PERIODS = "SELECT DISTINCT pp.id AS ID"
      + " FROM referencedata.processing_periods AS pp";

  private static final String SELECT_SCHEDULES = "pp.processingscheduleid IN (SELECT ps.id"
      + " FROM referencedata.processing_schedules AS ps"
      + " JOIN referencedata.requisition_group_program_schedules"
      + " ON referencedata.requisition_group_program_schedules.processingscheduleid = ps.id"
      + " JOIN referencedata.requisition_group_members"
      + " ON referencedata.requisition_group_members.requisitiongroupid"
      + " = referencedata.requisition_group_program_schedules.requisitiongroupid";

  private static final String WHERE = "WHERE";
  private static final String AND = " AND ";
  private static final String DEFAULT_SORT = "pp.startDate ASC";

  private static final String ORDER_BY = "ORDER BY";

  private static final String WITH_SCHEDULE_ID = "pp.processingscheduleid IN (:scheduleId)";
  private static final String WITH_START_DATE = "pp.startdate <= :endDate";
  private static final String WITH_END_DATE = "pp.enddate >= :startDate";
  private static final String WITH_IDS = "pp.id IN (:ids)";
  private static final String WITH_FACILITY =
      "referencedata.requisition_group_members.facilityid = :facilityId";
  private static final String WITH_PROGRAM =
      "referencedata.requisition_group_program_schedules.programid = :programId)";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all Processing Periods with matched parameters.
   * Method is searching
   *
   * @param scheduleId  UUID of processing schedule
   * @param programId  UUID of program
   * @param facilityId  UUID of facility
   * @param startDate Processing Period Start Date
   * @param endDate   Processing Period End Date
   * @param pageable  pagination and sorting parameters
   * @return Page of Processing Periods matching the parameters.
   */
  public Page<ProcessingPeriod> search(UUID scheduleId, UUID programId, UUID facilityId,
      LocalDate startDate, LocalDate endDate, Collection<UUID> ids, Pageable pageable) {

    Map<String, Object> params = Maps.newHashMap();
    Query nativeQuery = entityManager.createNativeQuery(prepareQuery(
        scheduleId, programId, facilityId, startDate, endDate, ids, params));
    params.forEach(nativeQuery::setParameter);

    SQLQuery countQuery = nativeQuery.unwrap(SQLQuery.class);
    countQuery.addScalar("ID", PostgresUUIDType.INSTANCE);

    // appropriate scalar is added to native query
    @SuppressWarnings("unchecked")
    List<UUID> periodIds = nativeQuery.getResultList();

    if (isEmpty(periodIds)) {
      return Pagination.getPage(Collections.emptyList(), pageable, 0);
    }

    String hqlWithSort = Joiner.on(' ').join(Lists.newArrayList(SELECT_PERIODS, WHERE, WITH_IDS,
        ORDER_BY, PageableUtil.getOrderPredicate(pageable, "pp.", DEFAULT_SORT)));

    List<ProcessingPeriod> periods = entityManager
        .createQuery(hqlWithSort, ProcessingPeriod.class)
        .setParameter("ids", periodIds)
        .setMaxResults(pageable.getPageSize())
        .setFirstResult(pageable.getOffset())
        .getResultList();

    return Pagination.getPage(periods, pageable, periodIds.size());
  }

  private String prepareQuery(UUID scheduleId, UUID programId, UUID facilityId,
      LocalDate startDate, LocalDate endDate, Collection<UUID> ids, Map<String, Object> params) {

    List<String> sql = Lists.newArrayList(COUNT_PERIODS);
    List<String> where = Lists.newArrayList();

    if (null != endDate) {
      where.add(WITH_START_DATE);
      params.put("endDate", endDate);
    }

    if (null != startDate) {
      where.add(WITH_END_DATE);
      params.put("startDate", startDate);
    }

    if (isNotEmpty(ids)) {
      where.add(WITH_IDS);
      params.put("ids", ids);
    }

    if (null == programId && null != scheduleId) {
      where.add(WITH_SCHEDULE_ID);
      params.put("scheduleId", scheduleId);
    }

    if (null != programId && null == scheduleId) {
      where.add(SELECT_SCHEDULES);

      if (null != facilityId) {
        where.add(WITH_FACILITY);
        params.put("facilityId", facilityId);
      }

      where.add(WITH_PROGRAM);
      params.put("programId", programId);
    }

    if (!where.isEmpty()) {
      sql.add(WHERE);
      sql.add(Joiner.on(AND).join(where));
    }

    return Joiner.on(' ').join(sql);
  }
}
