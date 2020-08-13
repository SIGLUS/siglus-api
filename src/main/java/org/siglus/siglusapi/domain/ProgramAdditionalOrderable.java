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

package org.siglus.siglusapi.domain;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.ProgramAdditionalOrderableDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "program_additional_orderables", schema = "siglusintegration")
public class ProgramAdditionalOrderable extends BaseEntity {

  private UUID programId;

  private UUID additionalOrderableId;

  private UUID orderableOriginProgramId;

  public static List<ProgramAdditionalOrderable> from(List<ProgramAdditionalOrderableDto> dtos) {
    List<ProgramAdditionalOrderable> additionalOrderables = newArrayList();
    dtos.forEach(dto -> {
      ProgramAdditionalOrderable additionalOrderable = new ProgramAdditionalOrderable();
      BeanUtils.copyProperties(dto, additionalOrderable);
      additionalOrderables.add(additionalOrderable);
    });
    return additionalOrderables;
  }
}
