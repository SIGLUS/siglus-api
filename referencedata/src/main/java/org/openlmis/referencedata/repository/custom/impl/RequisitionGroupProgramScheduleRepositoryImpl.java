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

import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.repository.custom.RequisitionGroupProgramScheduleRepositoryCustom;


public class RequisitionGroupProgramScheduleRepositoryImpl implements
    RequisitionGroupProgramScheduleRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Retrieves requisition group program schedule list from reference data service
   * by program and facility. Both parameters are optional.
   *
   * @param programId  UUID of program of searched RequisitionGroupProgramSchedule
   * @param facilityId UUID of facility of searched RequisitionGroupProgramSchedule
   * @return Requisition Group Program Schedule list matching search criteria
   */
  public List<RequisitionGroupProgramSchedule> searchRequisitionGroupProgramSchedules(
      UUID programId, UUID facilityId) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<RequisitionGroupProgramSchedule> query = builder.createQuery(
        RequisitionGroupProgramSchedule.class
    );

    Root<RequisitionGroupProgramSchedule> rgps = query.from(RequisitionGroupProgramSchedule.class);

    Join<RequisitionGroupProgramSchedule, RequisitionGroup> rg = rgps.join("requisitionGroup");
    Join<RequisitionGroup, Facility> ft = rg.join("memberFacilities");

    Predicate conjunction = builder.conjunction();
    if (facilityId != null) {
      conjunction = builder.and(conjunction, builder.equal(ft.get("id"), facilityId));
    }
    if (programId != null) {
      conjunction = builder.and(conjunction, builder.equal(rgps.get("program").get("id"),
          programId));
    }

    query.select(rgps);
    query.where(conjunction);

    return entityManager.createQuery(query).getResultList();
  }
}
