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

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.referencedata.Code;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "regimen_categories", schema = "siglusintegration")
public class RegimenCategory extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Embedded
  private Code code;

  @Column(columnDefinition = "text")
  private String name;

  @Column(nullable = false)
  private Integer displayOrder;

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof RegimenCategory)) {
      return false;
    }

    RegimenCategory regimenCategory = (RegimenCategory) other;
    return code.equals(regimenCategory.code);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(code);
  }
}
