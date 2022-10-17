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

package org.siglus.siglusapi.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FcRequisitionDto {

  private String requisitionNumber;

  private ZonedDateTime createdDate;

  private ZonedDateTime modifiedDate;

  private RequisitionStatus status;

  private Boolean emergency;

  private String facilityCode;

  private String facilityName;

  private String facilityDescription;

  private String requestingFacilityCode;

  private String requestingFacilityName;

  private String requestingFacilityType;

  private String requestingFacilityDescription;

  private String programCode;

  private String programName;

  private LocalDate periodStartDate;

  private LocalDate periodEndDate;

  private List<FcRequisitionLineItemDto> products;

  private List<Map<String, Object>> regimens;
}
