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
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.siglusapi.dto.fc.RegimenDto;

@Builder
@Entity
@Getter
@Setter
@AllArgsConstructor
@Table(name = "regimens", schema = "siglusintegration")
public class Regimen extends BaseEntity {

  @Column(nullable = false, unique = true, columnDefinition = "text")
  @Embedded
  private Code code;

  @Column(columnDefinition = "text")
  private String name;

  private Boolean active;

  private Boolean isCustom;

  private Integer displayOrder;

  @Column(nullable = false)
  private UUID programId;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "categoryId")
  protected RegimenCategory regimenCategory;

  @ManyToOne
  @JoinColumn(name = "dispatchLineId")
  protected RegimenDispatchLine regimenDispatchLine;

  public Regimen() {
    code = null;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Regimen)) {
      return false;
    }

    Regimen regimen = (Regimen) other;
    return code.equals(regimen.code);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(code);
  }

  public static Regimen from(RegimenDto regimenDto, UUID programId, RegimenCategory category) {
    return Regimen
        .builder()
        .code(Code.code(regimenDto.getCode()))
        .name(regimenDto.getDescription())
        .programId(programId)
        .regimenCategory(category)
        .build();
  }
}
