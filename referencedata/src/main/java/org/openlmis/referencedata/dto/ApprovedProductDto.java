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
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ApprovedProductDto
    extends BaseDto
    implements FacilityTypeApprovedProduct.Exporter, FacilityTypeApprovedProduct.Importer {

  private OrderableDto orderable;
  private ProgramDto program;
  private FacilityTypeDto facilityType;
  private Double maxPeriodsOfStock;
  private Double minPeriodsOfStock;
  private Double emergencyOrderPoint;
  private Boolean active;
  private MetadataDto meta = new MetadataDto();

  /**
   * A copy constructor.
   */
  public ApprovedProductDto(ApprovedProductDto original) {
    setId(original.getId());
    this.orderable = original.orderable;
    this.program = original.program;
    this.facilityType = original.facilityType;
    this.maxPeriodsOfStock = original.maxPeriodsOfStock;
    this.minPeriodsOfStock = original.minPeriodsOfStock;
    this.emergencyOrderPoint = original.emergencyOrderPoint;
    this.active = original.active;
    this.meta = new MetadataDto(original.meta);
  }

  @JsonSetter("orderable")
  public void setOrderable(OrderableDto orderable) {
    this.orderable = orderable;
  }

  @JsonIgnore
  public void setOrderable(Orderable orderable) {
    this.orderable = OrderableDto.newInstance(orderable);
  }

  @Override
  @JsonIgnore
  public UUID getOrderableId() {
    return Optional
        .ofNullable(orderable)
        .map(BaseDto::getId)
        .orElse(null);
  }

  @JsonSetter("program")
  public void setProgram(ProgramDto program) {
    this.program = program;
  }

  @Override
  @JsonIgnore
  public void setProgram(Program program) {
    this.program = ProgramDto.newInstance(program);
  }

  @JsonSetter("facilityType")
  public void setFacilityType(FacilityTypeDto facilityType) {
    this.facilityType = facilityType;
  }

  @Override
  @JsonIgnore
  public void setFacilityType(FacilityType facilityType) {
    this.facilityType = FacilityTypeDto.newInstance(facilityType);
  }

  @Override
  @JsonIgnore
  public void setVersionNumber(Long versionNumber) {
    meta.setVersionNumber(versionNumber);
  }

  @Override
  @JsonIgnore
  public Long getVersionNumber() {
    return Optional
        .ofNullable(meta)
        .map(MetadataDto::getVersionNumber)
        .orElse(null);
  }

  @Override
  @JsonIgnore
  public void setLastUpdated(ZonedDateTime lastUpdated) {
    meta.setLastUpdated(lastUpdated);
  }
}
