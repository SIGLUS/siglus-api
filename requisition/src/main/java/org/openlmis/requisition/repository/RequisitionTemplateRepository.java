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

package org.openlmis.requisition.repository;

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisitionTemplateRepository extends
    JpaRepository<RequisitionTemplate, UUID> {

  @Query("SELECT t FROM RequisitionTemplate AS t WHERE t.archived IS FALSE")
  List<RequisitionTemplate> getActiveTemplates();

  @Query("SELECT DISTINCT t"
      + " FROM RequisitionTemplate AS t"
      + "   INNER JOIN FETCH t.templateAssignments AS a"
      + " WHERE a.programId = :programId"
      + "   AND a.facilityTypeId = :facilityTypeId"
      + "   AND t.archived IS FALSE")
  RequisitionTemplate findTemplate(@Param("programId") UUID program,
                                   @Param("facilityTypeId") UUID facilityType);

}
