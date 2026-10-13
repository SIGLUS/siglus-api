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

import static org.siglus.siglusapi.constant.FieldConstants.SEPARATOR;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProcessingScheduleRepository extends JpaRepository<ProcessingSchedule, UUID> {

  @Query(value = "SELECT "
      + "       f2.code  AS facilitycode, "
      + "       f2.name  AS facilityname, "
      + "       CAST(f2.id AS varchar) AS facilityid, " // Cast UUID to String
      + "       p.name   AS programname, "
      + "       CAST(p.id AS varchar) AS programid, "   // Cast UUID to String
      + "       ps.name  AS scheduler, "
      + "       ps.code  AS code "
      + "FROM referencedata.requisition_group_members rgm "
      + "         JOIN referencedata.requisition_groups rg ON rgm.requisitiongroupid = rg.id "
      + "         JOIN referencedata.supervisory_nodes sn ON rg.supervisorynodeid = sn.id "
      + "         JOIN referencedata.facilities f ON sn.facilityid = f.id "
      + "         JOIN referencedata.facility_types ft ON ft.id = f.typeid "
      + "         JOIN referencedata.facilities f2 ON rgm.facilityid = f2.id "
      + "         JOIN referencedata.facility_types ft2 ON ft2.id = f2.typeid "
      + "         JOIN referencedata.requisition_group_program_schedules rgps ON rgps.requisitiongroupid = rg.id "
      + "         JOIN referencedata.processing_schedules ps ON ps.id = rgps.processingscheduleid "
      + "         JOIN referencedata.programs p ON p.id = rgps.programid "
      + "         JOIN referencedata.geographic_zones gz ON gz.id = f2.geographiczoneid "
      + "         JOIN referencedata.geographic_levels gl ON gz.levelid = gl.id "
      + "ORDER BY gz.code, f2.code, p.code DESC", nativeQuery = true)
  List<Map<String, Object>> getRawFacilityProgramCodes();

  /**
   * Default method to transform the raw query results into a Map.
   * Key: facilityid + programid
   * Value: code
   */
  default Map<String, String> getFacilityProgramCodeMap() {
    return getRawFacilityProgramCodes().stream()
        .collect(Collectors.toMap(
            // Concatenate facilityid and programid for the key
            row -> row.get("facilityid")
                + SEPARATOR + row.get("programid"),
            // Use code as the value
            row -> String.valueOf(row.get("code")),
            // Merge function in case of duplicate keys (keeps the first one found)
            (existing, replacement) -> existing
        ));
  }
}
