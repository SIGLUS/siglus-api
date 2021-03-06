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

package org.siglus.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.referencedata.BaseDto;
import org.springframework.beans.BeanUtils;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class RequisitionTemplateExtensionDto extends BaseDto {

  private UUID requisitionTemplateId;

  private boolean enableConsultationNumber = false;

  private boolean enableKitUsage = false;

  private boolean enableProduct = false;

  @JsonProperty("enablePatient")
  private boolean enablePatientLineItem = false;

  private boolean enableRegimen = false;

  private boolean enableRapidTestConsumption = false;

  private boolean enableUsageInformation = false;

  private boolean enableQuicklyFill = false;

  public static RequisitionTemplateExtensionDto from(
      RequisitionTemplateExtension entity) {
    if (entity == null) {
      return null;
    }
    RequisitionTemplateExtensionDto dto = new RequisitionTemplateExtensionDto();
    BeanUtils.copyProperties(entity, dto);
    return dto;
  }

}
