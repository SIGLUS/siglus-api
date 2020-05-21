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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.repository.custom.SupervisoryNodeRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.web.SupervisoryNodeSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class SupervisoryNodeRepositoryImpl implements SupervisoryNodeRepositoryCustom {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String FACILITY = "facility";
  private static final String PROGRAM = "program";
  private static final String REQUISITION_GROUP = "requisitionGroup";
  private static final String MEMBER_FACILITIES = "memberFacilities";
  private static final String GEOGRAPHIC_ZONE = "geographicZone";
  private static final String ID = "id";
  private static final String REQUISITION_GROUP_PROGRAM_SCHEDULE =
      "requisitionGroupProgramSchedules";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all supervisory nodes with matched parameters.
   * Method is ignoring case and using like operator for code and name.
   *
   * @param searchParams Search parameters.
   * @return List of Supervisory Nodes matching the parameters.
   */
  public Page<SupervisoryNode> search(@NotNull SupervisoryNodeSearchParams searchParams,
      Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<SupervisoryNode> nodeQuery = builder.createQuery(SupervisoryNode.class);
    nodeQuery = prepareQuery(nodeQuery, searchParams, false, builder);

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    countQuery = prepareQuery(countQuery, searchParams, true, builder);

    Long count = entityManager.createQuery(countQuery).getSingleResult();

    Pair<Integer, Integer> maxAndFirst = PageableUtil.querysMaxAndFirstResult(pageable);

    List<SupervisoryNode> supervisoryNodes = entityManager.createQuery(nodeQuery)
        .setMaxResults(maxAndFirst.getLeft())
        .setFirstResult(maxAndFirst.getRight())
        .getResultList();
    return Pagination.getPage(supervisoryNodes, pageable, count);
  }

  private <T> CriteriaQuery<T> prepareQuery(CriteriaQuery<T> query,
      SupervisoryNodeSearchParams searchParams, boolean count, CriteriaBuilder builder) {
    Root<SupervisoryNode> root = query.from(SupervisoryNode.class);

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      query = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    }

    Predicate predicate = builder.conjunction();

    String code = searchParams.getCode();
    if (code != null) {
      predicate = builder.and(predicate,
          builder.like(builder.upper(root.get(CODE)), "%" + code.toUpperCase() + "%"));
    }

    String name = searchParams.getName();
    if (name != null) {
      predicate = builder.and(predicate,
          builder.like(builder.upper(root.get(NAME)), "%" + name.toUpperCase() + "%"));
    }

    UUID facilityId = searchParams.getFacilityId();
    UUID programId = searchParams.getProgramId();
    if (facilityId != null || programId != null) {
      Join<SupervisoryNode, RequisitionGroup> requisitionGroupJoin =
          root.join(REQUISITION_GROUP, JoinType.LEFT);

      if (facilityId != null) {
        Join<RequisitionGroup, Facility> memberFacilitiesJoin =
            requisitionGroupJoin.join(MEMBER_FACILITIES, JoinType.LEFT);
        predicate = builder.and(predicate, builder.equal(memberFacilitiesJoin.get(ID), facilityId));
      }

      if (programId != null) {
        Join<RequisitionGroup, RequisitionGroupProgramSchedule> rgpsJoin =
            requisitionGroupJoin.join(REQUISITION_GROUP_PROGRAM_SCHEDULE, JoinType.LEFT);
        Join<RequisitionGroupProgramSchedule, Program> programJoin =
            rgpsJoin.join(PROGRAM, JoinType.LEFT);
        predicate = builder.and(predicate, builder.equal(programJoin.get(ID), programId));
      }
    }

    UUID zoneId = searchParams.getZoneId();
    if (zoneId != null) {
      Join<SupervisoryNode, Facility> facilityJoin = root.join(FACILITY, JoinType.LEFT);
      Join<Facility, GeographicZone> geographicZoneJoin =
          facilityJoin.join(GEOGRAPHIC_ZONE, JoinType.LEFT);
      predicate = builder.and(predicate, builder.equal(geographicZoneJoin.get(ID), zoneId));
    }

    Set<UUID> ids = searchParams.getIds();
    if (!isEmpty(ids)) {
      predicate = builder.and(predicate, root.get("id").in(ids));
    }

    query.where(predicate);
    return query;
  }
}
