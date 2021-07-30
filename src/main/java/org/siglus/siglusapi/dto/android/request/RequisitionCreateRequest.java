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

package org.siglus.siglusapi.dto.android.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidEndDate;
import org.siglus.siglusapi.dto.android.constraint.RequisitionValidStartDate;
import org.siglus.siglusapi.dto.android.group.PerformanceGroup;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RequisitionValidStartDate(groups = {PerformanceGroup.class})
@RequisitionValidEndDate
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionCreateRequest {

  @NotBlank
  private String programCode;

  @NotNull
  private Instant clientSubmittedTime;

  @NotNull
  private Boolean emergency;

  @NotNull
  private LocalDate actualStartDate;

  @NotNull
  private LocalDate actualEndDate;

  private Integer consultationNumber;

  @Valid
  private List<RequisitionLineItemRequest> products;

  @Valid
  private List<RequisitionSignatureRequest> signatures;

  private String comments;

  @Valid
  private List<RegimenLineItemRequest> regimenLineItems;

  @Valid
  private List<RegimenLineItemRequest> regimenSummaryLineItems;

  @Valid
  private List<PatientLineItemsRequest> patientLineItems;

  @Valid
  private List<UsageInformationLineItemRequest> usageInformationLineItems;

  @Valid
  private List<TestConsumptionLineItemRequest> testConsumptionLineItems;
}

