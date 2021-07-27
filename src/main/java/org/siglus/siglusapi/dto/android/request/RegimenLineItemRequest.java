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

import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_COMMUNITY;
import static org.siglus.siglusapi.constant.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_PATIENT;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.android.androidenum.RegimenSummaryCode;
import org.springframework.util.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegimenLineItemRequest {

  @NotNull
  private String code;

  private String name;

  @NotNull
  @Min(value = 0)
  private Integer patientsOnTreatment;

  @NotNull
  @Min(value = 0)
  private Integer comunitaryPharmacy;

  public static List<RegimenLineItemRequest> from(List<RegimenLineItem> regimenLineItems,
      Map<UUID, RegimenDto> idToRegimenDto) {
    List<RegimenLineDto> regimenLineDtos = RegimenLineDto.from(regimenLineItems, idToRegimenDto);
    List<RegimenLineItemRequest> regimenLineItemRequests = regimenLineDtos.stream()
        .filter(regimenLineDto -> StringUtils.isEmpty(regimenLineDto.getName()))
        .map(regimenLineDto -> RegimenLineItemRequest.builder()
            .name(regimenLineDto.getRegimen().getFullProductName())
            .code(regimenLineDto.getRegimen().getCode())
            .comunitaryPharmacy(regimenLineDto.getColumns().get(COLUMN_NAME_COMMUNITY).getValue())
            .patientsOnTreatment(regimenLineDto.getColumns().get(COLUMN_NAME_PATIENT).getValue())
            .build())
        .collect(Collectors.toList());
    return regimenLineItemRequests;
  }

  public static List<RegimenLineItemRequest> from(List<RegimenSummaryLineItem> regimenSummaryLineItems) {
    List<RegimenSummaryLineDto> regimenSummaryLineDtos = RegimenSummaryLineDto.from(regimenSummaryLineItems);
    List<RegimenLineItemRequest> regimenLineItemRequests = regimenSummaryLineDtos.stream()
        .filter(regimenSummaryLineDto -> !regimenSummaryLineDto.getName().equals(FieldConstants.TOTAL))
        .map(
            regimenSummaryLineDto -> RegimenLineItemRequest.builder()
                .code(RegimenSummaryCode.findKeyByValue(regimenSummaryLineDto.getName()))
                .comunitaryPharmacy(regimenSummaryLineDto.getColumns().get(COLUMN_NAME_COMMUNITY).getValue())
                .patientsOnTreatment(regimenSummaryLineDto.getColumns().get(COLUMN_NAME_PATIENT).getValue())
                .build()
        ).collect(Collectors.toList());
    return regimenLineItemRequests;
  }
}
