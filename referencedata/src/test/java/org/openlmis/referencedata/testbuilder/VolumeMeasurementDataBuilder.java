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

import org.openlmis.referencedata.domain.measurement.VolumeMeasurement;

public class VolumeMeasurementDataBuilder {
  private String measurementUnitCode;
  private Double value;

  public VolumeMeasurementDataBuilder withValue(Double value) {
    this.value = value;
    return this;
  }

  public VolumeMeasurementDataBuilder withMeasurementUnitCode(String measurementUnitCode) {
    this.measurementUnitCode = measurementUnitCode;
    return this;
  }

  /**
   * Builds instance of {@link VolumeMeasurementDataBuilder} with sample data.
   */
  public VolumeMeasurementDataBuilder() {

    measurementUnitCode = "MLT";
    value = 1.0;
  }

  /**
   * Builds instance of {@link VolumeMeasurement}.
   */
  public VolumeMeasurement build() {
    return new VolumeMeasurement(value, measurementUnitCode);
  }
}
