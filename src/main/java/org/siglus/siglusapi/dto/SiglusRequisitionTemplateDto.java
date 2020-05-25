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

import static org.openlmis.requisition.web.ResourceNames.PROGRAMS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.RequisitionTemplateDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;

@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class SiglusRequisitionTemplateDto extends RequisitionTemplateDto {

  @Value("${service.url}")
  private String serviceUrl;

  private RequisitionTemplateExtensionDto extension;

  private Set<ObjectReferenceDto> associatePrograms;

  public static SiglusRequisitionTemplateDto from(RequisitionTemplateDto requisitionTemplateDto) {
    SiglusRequisitionTemplateDto dto = new SiglusRequisitionTemplateDto();
    BeanUtils.copyProperties(requisitionTemplateDto, dto);
    return dto;
  }

  @JsonIgnore
  public Set<UUID> getAssociateProgramsIds() {
    return Optional.ofNullable(associatePrograms)
        .orElse(Collections.emptySet())
        .stream()
        .map(ObjectReferenceDto::getId)
        .collect(Collectors.toSet());
  }

  @JsonIgnore
  public void setAssociateProgramsIds(Set<UUID> associateProgramsIds) {
    this.associatePrograms = Optional
        .ofNullable(associateProgramsIds)
        .orElse(Collections.emptySet())
        .stream()
        .map(elem -> new ObjectReferenceDto(elem, serviceUrl, PROGRAMS))
        .collect(Collectors.toSet());
  }

}
