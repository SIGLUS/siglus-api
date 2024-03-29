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

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.SiglusReportTypeDto;
import org.springframework.beans.BeanUtils;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "report_types", schema = "siglusintegration")
public class SiglusReportType extends BaseEntity {

  private UUID facilityId;

  private String name;

  private String programCode;

  private Boolean active;

  private LocalDate startDate;

  public static SiglusReportType from(SiglusReportTypeDto dto) {
    SiglusReportType domain = new SiglusReportType();
    BeanUtils.copyProperties(dto, domain);
    return domain;
  }
}
