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

package org.siglus.common.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.common.domain.referencedata.ProcessingPeriod;
import org.siglus.common.domain.referencedata.ProcessingSchedule;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessingPeriodRepository extends JpaRepository<ProcessingPeriod, UUID>,
    JpaSpecificationExecutor<ProcessingPeriod> {

  List<ProcessingPeriod> findByProcessingSchedule(ProcessingSchedule schedule);

  default Optional<ProcessingPeriod> findPeriodByCodeAndName(Code code, String name) {
    Specification<ProcessingPeriod> spec = (root, query, cb) -> cb.and(
        cb.equal(root.get("code"), code),
        cb.equal(root.get("name"), name)
    );
    return Optional.ofNullable(findOne(spec));
  }

}
