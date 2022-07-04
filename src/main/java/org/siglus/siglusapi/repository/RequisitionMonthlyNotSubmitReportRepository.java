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

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyNotSubmitReport;
import org.siglus.siglusapi.domain.report.RequisitionMonthlyReportFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RequisitionMonthlyNotSubmitReportRepository extends
    JpaRepository<RequisitionMonthlyNotSubmitReport, UUID> {

  @Query(value = "SELECT f.id  AS ficilityid, "
      + "       f.name AS ficilityname, "
      + "       f.code      AS ficilitycode, "
      + "       ft.code      AS facilitytype, "
      + "       ftm.category AS facilitymergetype, "
      + "       z1.name district, "
      + "       z2.name province, "
      + "       vfs.districtfacilitycode, "
      + "       vfs.provincefacilitycode "
      + "FROM referencedata.facilities f "
      + "         LEFT JOIN referencedata.geographic_zones z1 ON f.geographiczoneid = z1.id "
      + "         LEFT JOIN referencedata.geographic_zones z2 ON z1.parentid = z2.id "
      + "         LEFT JOIN dashboard.vw_facility_supplier vfs ON vfs.facilitycode = f.code "
      + "         LEFT JOIN referencedata.facility_types ft ON f.typeid = ft.id "
      + "         LEFT JOIN siglusintegration.facility_type_mapping ftm "
      + "ON ftm.facilitytypecode = ft.code", nativeQuery = true)
  public List<RequisitionMonthlyReportFacility> queryAllFacilityInfo();


}
