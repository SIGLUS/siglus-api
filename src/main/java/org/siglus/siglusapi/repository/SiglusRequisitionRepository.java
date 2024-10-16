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

package org.siglus.siglusapi.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.siglus.siglusapi.repository.dto.RequisitionOrderDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface SiglusRequisitionRepository extends JpaRepository<Requisition, UUID> {

  @Query(value = "select * from requisition.requisitions r where "
      + "r.status in ('IN_APPROVAL', 'APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER')"
      + "and r.modifieddate >= :date "
      + "and r.modifieddate <= now() "
      + "order by r.modifieddate, ?#{#pageable}", nativeQuery = true)
  Page<Requisition> searchAllForFc(@Param("date") LocalDate date, Pageable pageable);

  @Query(value = "select * from requisition.requisitions r where "
      + "r.status = 'IN_APPROVAL' "
      + "and r.modifieddate >= :date "
      + "and r.modifieddate <= now() "
      + "and r.supervisorynodeid in :fcSupervisoryNodeIds "
      + "order by r.modifieddate, ?#{#pageable}", nativeQuery = true)
  Page<Requisition> searchNeedApprovalForFc(@Param("date") LocalDate date,
      Pageable pageable, @Param("fcSupervisoryNodeIds") Set<UUID> fcSupervisoryNodeIds);

  @Query(value = "select r.* from requisition.requisitions r, referencedata.processing_periods p "
      + "where r.processingPeriodId = p.id "
      + "and r.status in ('APPROVED', 'RELEASED', 'RELEASED_WITHOUT_ORDER') "
      + "and r.supervisorynodeid in :supervisoryNodeIds "
      + "and r.programId = :programId "
      + "and p.enddate >= :firstDayOfThisMonth "
      + "and p.enddate < :firstDayOfNextMonth", nativeQuery = true)
  List<Requisition> searchForSuggestedQuantity(@Param("programId") UUID programId,
      @Param("supervisoryNodeIds") Set<UUID> supervisoryNodeIds,
      @Param("firstDayOfThisMonth") String firstDayOfThisMonth,
      @Param("firstDayOfNextMonth") String firstDayOfNextMonth);

  @Query(value = "select r.* from requisition.requisitions r "
      + "where r.facilityid = :facilityId "
      + "and r.programId = :programId "
      + "and r.processingperiodid = :periodId "
      + "and r.emergency = :emergency "
      + "and r.status in :statusSet ", nativeQuery = true)
  List<Requisition> searchAfterAuthorizedRequisitions(@Param("facilityId") UUID facilityId,
      @Param("programId") UUID programId, @Param("periodId") UUID periodId,
      @Param("emergency") Boolean emergency, @Param("statusSet") Set<String> statusSet);

  @Query(value = "select DISTINCT ON(t.programid) t.* from requisition.requisitions t "
      + "where t.facilityid = :facilityId "
      + "and t.emergency = false "
      + "order by t.programid,t.createddate desc ", nativeQuery = true)
  List<Requisition> findLatestRequisitionsByFacilityId(@Param("facilityId") UUID facilityId);

  @Query(value = "select DISTINCT ON(t.programid) t.* from requisition.requisitions t "
      + "JOIN referencedata.processing_periods p ON t.processingperiodid = p.id "
      + "where t.facilityid = :facilityId "
      + "and t.emergency = false "
      + "order by t.programid,p.startdate desc ", nativeQuery = true)
  List<Requisition> findRequisitionsWithLatestPeriodDateByFacilityId(@Param("facilityId") UUID facilityId);

  @Query(value = "select CAST(id AS varchar) from requisition.requisitions r "
      + "where r.facilityid = :facilityId "
      + "and r.programid = :programId "
      + "and r.processingperiodid = :processingPeriodId "
      + "and r.emergency = :emergency "
      + "and r.status in (:status) ",
      nativeQuery = true)
  List<String> findRequisitionIdsByOrderInfo(@Param("facilityId") UUID facilityId, @Param("programId") UUID programId,
      @Param("processingPeriodId") UUID periodId, @Param("emergency") boolean emergency,
      @Param("status") List<String> status);

  @Query(value = "select r.* from requisition.requisitions r "
      + "where r.programid = :programId "
      + "and r.facilityid in :facilityIds "
      + "and r.processingperiodid in :processingPeriodIds "
      + "and r.emergency = :emergency "
      + "and r.status in (:status) ",
      nativeQuery = true)
  List<Requisition> findRequisitionsByOrderInfo(@Param("facilityIds") List<UUID> facilityIds,
      @Param("programId") UUID programId, @Param("processingPeriodIds") List<UUID> periodIds,
      @Param("emergency") boolean emergency, @Param("status") List<String> status);

  @Query(name = "Order.findRequisitionOrderDtos", nativeQuery = true)
  List<RequisitionOrderDto> findRequisitionOrderDtoByRequisitionIds(
      @Param("requisitionIds") Iterable<UUID> requisitionIds);

  int countById(UUID id);

  List<Requisition> findByFacilityIdAndProgramIdAndStatus(
      @Param("facilityId") UUID facilityId,
      @Param("programId") UUID programId,
      @Param("status") RequisitionStatus status
  );

  @EntityGraph(value = "requisition-statusChange-graph")
  Set<Requisition> findAllByStatus(RequisitionStatus status);


  Set<Requisition> findAllByStatusIn(Set<RequisitionStatus> statusSet);

  void deleteById(UUID id);

}
