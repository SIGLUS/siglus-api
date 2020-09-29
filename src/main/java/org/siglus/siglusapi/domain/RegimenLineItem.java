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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.RegimenDto;
import org.siglus.siglusapi.dto.RegimenLineDto;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "regimen_line_items", schema = "siglusintegration")
public class RegimenLineItem extends BaseEntity {

  private UUID requisitionId;

  private UUID regimenId;

  @Column(name = "columnname")
  private String column;

  private Integer value;

  public static List<RegimenLineItem> from(List<RegimenLineDto> lineDtos, UUID requisitionId) {
    List<RegimenLineItem> lineItems = newArrayList();

    lineDtos.forEach(regimenLineDto -> {
      RegimenDto regimen = regimenLineDto.getRegimen();

      regimenLineDto.getColumns().forEach((columnName, regimenColumnDto) -> {
        RegimenLineItem regimenLineItem = RegimenLineItem.builder()
            .requisitionId(requisitionId)
            .regimenId(regimen == null ? null : regimen.getId())
            .column(columnName)
            .value(regimenColumnDto.getValue())
            .build();

        regimenLineItem.setId(regimenColumnDto.getId());
        lineItems.add(regimenLineItem);
      });

    });


    return lineItems;
  }
}
