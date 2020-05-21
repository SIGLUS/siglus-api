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

package org.openlmis.referencedata.domain.measurement;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Embeddable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class VolumeMeasurement extends Measurement {

  public VolumeMeasurement(Double value, String measurementUnitCode) {
    super(value, measurementUnitCode);
  }

  @Override
  public List<String> getCodeListVersion() {
    return Stream.of(VolumeUnitCode.values())
                          .map(VolumeUnitCode::name)
                          .collect(Collectors.toList());
  }

  /**
   * Static factory method for constructing a new Volume Measurement using an importer (DTO).
   *
   * @param importer the Volume Measurement importer (DTO)
   */
  public static VolumeMeasurement newVolumeMeasurement(
          Importer importer) {
    VolumeMeasurement newVolumeMeasurement = new VolumeMeasurement();
    newVolumeMeasurement.setMeasurementUnitCode(importer.getMeasurementUnitCode());
    newVolumeMeasurement.setValue(importer.getValue());
    return newVolumeMeasurement;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setMeasurementUnitCode(getMeasurementUnitCode());
    exporter.setValue(getValue());
    exporter.setCodeListVersion(getCodeListVersion());
  }

  public interface Exporter extends Measurement.Exporter {

    void setCodeListVersion(List<String> codeListVersion);

  }

  public interface Importer extends Measurement.Importer {

    List<String> getCodeListVersion();

  }
}
