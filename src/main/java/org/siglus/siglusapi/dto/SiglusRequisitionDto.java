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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.siglus.siglusapi.dto.constraint.ConsultationNumberConstraint;
import org.siglus.siglusapi.dto.constraint.PatientDataConstraint;
import org.siglus.siglusapi.dto.group.RequisitionActionGroup;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@PatientDataConstraint(groups = RequisitionActionGroup.class)
@ConsultationNumberConstraint(groups = RequisitionActionGroup.class)
public class SiglusRequisitionDto extends RequisitionV2Dto {

  private Boolean isFinalApproval;
  private Boolean isExternalApproval;
  private Boolean isApprovedByInternal;
  private transient List<KitUsageLineItemDto> kitUsageLineItems = newArrayList();
  private transient List<UsageInformationServiceDto> usageInformationLineItems = newArrayList();
  private transient List<TestConsumptionServiceDto> testConsumptionLineItems = newArrayList();
  @Valid
  @NotNull
  private transient List<PatientGroupDto> patientLineItems = Collections.emptyList();
  @Valid
  @NotNull
  private transient List<ConsultationNumberGroupDto> consultationNumberLineItems = Collections.emptyList();
  private transient List<RegimenLineDto> regimenLineItems = newArrayList();
  private transient List<RegimenSummaryLineDto> regimenSummaryLineItems = newArrayList();
  private transient List<RegimenDto> customRegimens = newArrayList();
  private transient SiglusUsageTemplateDto usageTemplate;
  private String requisitionNumber;

  public static SiglusRequisitionDto from(RequisitionV2Dto v2Dto) {
    SiglusRequisitionDto dto = new SiglusRequisitionDto();
    BeanUtils.copyProperties(v2Dto, dto);
    return dto;
  }

}
