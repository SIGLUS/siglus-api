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

package org.openlmis.referencedata.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.vividsolutions.jts.geom.Point;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.SupportedProgram;

@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public final class FacilityDto extends BasicFacilityDto {
  private String description;
  private FacilityOperatorDto operator;
  private LocalDate goLiveDate;
  private LocalDate goDownDate;
  private String comment;
  private Boolean openLmisAccessible;
  private Point location;
  private Map<String, Object> extraData;

  @Getter
  private Set<SupportedProgramDto> supportedPrograms;

  public FacilityDto(UUID id) {
    setId(id);
  }

  /**
   * Creates new instance of {@link FacilityDto} based on passed facility.
   */
  public static FacilityDto newInstance(Facility facility) {
    FacilityDto dto = new FacilityDto();
    facility.export(dto);

    return dto;
  }

  @JsonSetter("operator")
  public void setOperator(FacilityOperatorDto operator) {
    this.operator = operator;
  }

  @Override
  @JsonIgnore
  public void setOperator(FacilityOperator operator) {
    this.operator = new FacilityOperatorDto();
    operator.export(this.operator);
  }

  @Override
  public void setSupportedPrograms(Set<SupportedProgram> supportedPrograms) {
    if (supportedPrograms == null) {
      this.supportedPrograms = null;
    } else {
      this.supportedPrograms = supportedPrograms
          .stream()
          .map(supportedProgram -> {
            SupportedProgramDto supportedProgramDto = new SupportedProgramDto();
            supportedProgram.export(supportedProgramDto);

            return supportedProgramDto;
          })
          .collect(Collectors.toSet());
    }
  }
}
