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

package org.siglus.siglusapi.domain.report;

import java.util.UUID;
import javax.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequisitionMonthlyReportFacility {

  @Column(name = "district")
  private String district;
  @Column(name = "province")
  private String province;
  @Column(name = "facilityname")
  private String facilityName;
  @Column(name = "ficilitycode")
  private String ficilityCode;

  @Column(name = "facilityid")
  private UUID facilityId;

  @Column(name = "facilitytype")
  private String facilityType;
  @Column(name = "facilitymergetype")
  private String facilityMergeType;
  @Column(name = "districtfacilitycode")
  private String districtFacilityCode;
  @Column(name = "provincefacilitycode")
  private String provinceFacilityCode;
}

