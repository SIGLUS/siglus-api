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

package org.openlmis.referencedata.testbuilder;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Program;

public class FacilityTypeApprovedProductsDataBuilder {

  private UUID id;
  private Long versionNumber;
  private UUID orderableId;
  private Program program;
  private FacilityType facilityType;
  private Double maxPeriodsOfStock;
  private Double minPeriodsOfStock;
  private Double emergencyOrderPoint;
  private Boolean active;
  private ZonedDateTime lastUpdated;

  /**
   * Returns instance of {@link FacilityTypeApprovedProductsDataBuilder} with sample data.
   */
  public FacilityTypeApprovedProductsDataBuilder() {
    id = UUID.randomUUID();
    versionNumber = 1L;
    orderableId = UUID.randomUUID();
    program = new ProgramDataBuilder().build();
    facilityType = new FacilityTypeDataBuilder().build();
    maxPeriodsOfStock = 3.0;
    minPeriodsOfStock = 1.0;
    emergencyOrderPoint = 0.0;
    active = true;
    lastUpdated = ZonedDateTime.now();
  }

  public FacilityTypeApprovedProductsDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  public FacilityTypeApprovedProductsDataBuilder withVersionNumber(Long versionNumber) {
    this.versionNumber = versionNumber;
    return this;
  }

  public FacilityTypeApprovedProductsDataBuilder withOrderableId(UUID orderableId) {
    this.orderableId = orderableId;
    return this;
  }

  public FacilityTypeApprovedProductsDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }

  public FacilityTypeApprovedProductsDataBuilder withFacilityType(FacilityType facilityType) {
    this.facilityType = facilityType;
    return this;
  }

  public FacilityTypeApprovedProductsDataBuilder withMaxPeriodsOfStock(Number maxPeriodsOfStock) {
    this.maxPeriodsOfStock = maxPeriodsOfStock.doubleValue();
    return this;
  }

  public FacilityTypeApprovedProductsDataBuilder withActive(Boolean active) {
    this.active = active;
    return this;
  }

  /**
   * Builds instance of {@link FacilityTypeApprovedProduct}.
   */
  public FacilityTypeApprovedProduct build() {
    FacilityTypeApprovedProduct ftap = new FacilityTypeApprovedProduct(id, versionNumber,
        orderableId, program, facilityType, maxPeriodsOfStock, active);
    ftap.setMinPeriodsOfStock(minPeriodsOfStock);
    ftap.setEmergencyOrderPoint(emergencyOrderPoint);
    ftap.setLastUpdated(lastUpdated);

    return ftap;
  }
}
