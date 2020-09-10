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

import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.fc.CpDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "cps", schema = "siglusintegration")
public class CpDomain extends BaseEntity {

  private String facilityCode;

  private String facilityName;

  private String productCode;

  private String productName;

  private String realProgramCode;

  private String realProgramName;

  private Integer cp;

  private Integer max;

  private String period;

  private Integer year;

  private String queryDate;

  private ZonedDateTime date;

  private ZonedDateTime lastUpdatedAt;

  public static List<CpDomain> from(List<CpDto> dtos) {
    List<CpDomain> cps = newArrayList();
    dtos.forEach(dto -> {
      CpDomain cp = new CpDomain();
      BeanUtils.copyProperties(dto, cp);
      cp.setFacilityCode(dto.getClientCode());
      cp.setFacilityName(dto.getClientDescription());
      cp.setProductCode(dto.getProductFnm());
      cp.setProductName(dto.getProductDescription());
      cp.setRealProgramCode(dto.getProgramCode());
      cp.setRealProgramName(dto.getProgramDescription());
      cps.add(cp);
    });
    return cps;
  }
}
