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

import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.fc.CmmDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "cmms", schema = "siglusintegration")
public class Cmm extends BaseEntity {

  private String facilityCode;

  private String facilityName;

  private String productCode;

  private String productName;

  private String realProgramCode;

  private String realProgramName;

  private Integer cmm;

  private Integer max;

  private String period;

  private Integer year;

  private Date date;

  private Date lastUpdatedAt;

  public static List<Cmm> from(List<CmmDto> dtos) {
    List<Cmm> cmms = newArrayList();
    dtos.forEach(dto -> {
      Cmm cmm = new Cmm();
      BeanUtils.copyProperties(dto, cmm);
      cmm.setFacilityCode(dto.getClientCode());
      cmm.setFacilityName(dto.getClientDescription());
      cmm.setProductCode(dto.getProductFnm());
      cmm.setProductName(dto.getProductDescription());
      cmm.setRealProgramCode(dto.getProgramCode());
      cmm.setRealProgramName(dto.getProgramDescription());
      cmms.add(cmm);
    });
    return cmms;
  }
}
