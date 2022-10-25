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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessingPeriodRepository extends JpaRepository<ProcessingPeriod, UUID>,
    JpaSpecificationExecutor<ProcessingPeriod> {

  ProcessingPeriod findOneById(UUID id);

  List<ProcessingPeriod> findByProcessingSchedule(ProcessingSchedule schedule);

  default Optional<ProcessingPeriod> findPeriodByCodeAndMonth(Code code, YearMonth month) {
    Specification<ProcessingPeriod> spec = (root, query, cb) -> cb.and(
        cb.equal(root.get("processingSchedule").get("code"), code),
        cb.like(root.get("startDate").as(String.class), month.toString() + "%")
    );
    return Optional.ofNullable(findOne(spec));
  }

  @Query(value = "select\n"
      + "  startdate\n"
      + "from\n"
      + "  referencedata.processing_periods pp\n"
      + "left join referencedata.processing_schedules ps on\n"
      + "  pp.processingscheduleid = ps.id\n"
      + "where ps.code ='M1' and :currentDate >= pp.startdate and :currentDate <= pp.enddate ", nativeQuery = true)
  LocalDate getCurrentPeriodStartDate(@Param("currentDate") LocalDate currentDate);

  @Query(value = "select pp.* \n"
      + "from referencedata.processing_periods pp \n"
      + "left join referencedata.processing_schedules ps \n"
      + "on pp.processingscheduleid = ps.id \n"
      + "where ps.code ='M1' \n"
      + "and pp.startdate <= :currentDate ", nativeQuery = true)
  List<ProcessingPeriod> getUpToNowMonthlyPeriods(@Param("currentDate") LocalDate currentDate);
}
