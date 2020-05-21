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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.domain.measurement.TemperatureMeasurement;
import org.openlmis.referencedata.domain.measurement.VolumeMeasurement;

@SuppressWarnings("PMD.TooManyMethods")
public class OrderableDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private Code productCode;
  private Dispensable dispensable;
  private String fullProductName;
  private String description;
  private long netContent;
  private long packRoundingTreshold;
  private boolean roundToZero;
  private List<ProgramOrderable> programOrderables;
  private Map<String, String> identifiers;
  private Map<String, Object> extraData;
  private Long versionNumber;
  private ZonedDateTime lastUpdated;
  private TemperatureMeasurement minimumTemperature;
  private TemperatureMeasurement maximumTemperature;
  private VolumeMeasurement inBoxCubeDimension;

  /**
   * Returns instance of {@link OrderableDataBuilder} with sample data.
   */
  public OrderableDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    productCode = Code.code("P" + instanceNumber);
    dispensable = Dispensable.createNew("pack");
    fullProductName = "product " + instanceNumber;
    description = "description";
    netContent = 10;
    packRoundingTreshold = 5;
    roundToZero = false;
    programOrderables = new ArrayList<>();
    identifiers = new HashMap<>();
    extraData = new HashMap<>();
    versionNumber = 1L;
    lastUpdated = ZonedDateTime.now();
    minimumTemperature = null;
    maximumTemperature = null;
    inBoxCubeDimension = null;
  }

  public OrderableDataBuilder withIdentifier(String key, Object valueToString) {
    this.identifiers.put(key, valueToString.toString());
    return this;
  }

  public OrderableDataBuilder withProductCode(Code productCode) {
    this.productCode = productCode;
    return this;
  }

  public OrderableDataBuilder withDispensable(Dispensable dispensable) {
    this.dispensable = dispensable;
    return this;
  }

  public OrderableDataBuilder withFullProductName(String fullProductName) {
    this.fullProductName = fullProductName;
    return this;
  }

  public OrderableDataBuilder withVersionNumber(Long versionNumber) {
    this.versionNumber = versionNumber;
    return this;
  }

  /**
   * Sets the value of minimumTemperature for new {@link Orderable}.
   */
  public OrderableDataBuilder withMinimumTemperature(
          String temperatureMeasurementUnitCode, Double value) {
    this.minimumTemperature = new TemperatureMeasurementDataBuilder()
            .withTemperatureMeasurementUnitCode(temperatureMeasurementUnitCode)
            .withValue(value)
            .build();
    return this;
  }

  /**
   * Sets the value of maximumTemperature for new {@link Orderable}.
   */
  public OrderableDataBuilder withMaximumTemperature(
          String temperatureMeasurementUnitCode, Double value) {
    this.maximumTemperature = new TemperatureMeasurementDataBuilder()
            .withTemperatureMeasurementUnitCode(temperatureMeasurementUnitCode)
            .withValue(value)
            .build();
    return this;
  }

  /**
   * Sets the value of inBoxCubeDimension for new {@link Orderable}.
   */
  public OrderableDataBuilder withInBoxCubeDimension(String measurementUnitCode, Double value) {
    this.inBoxCubeDimension = new VolumeMeasurementDataBuilder()
            .withMeasurementUnitCode(measurementUnitCode)
            .withValue(value)
            .build();
    return this;
  }

  /**
   * Sets the value of programOrderables for new {@link Orderable}.
   */
  public OrderableDataBuilder withProgramOrderables(List<ProgramOrderable> programOrderables) {
    Orderable orderable = build();
    this.programOrderables = programOrderables;
    this.id = orderable.getId();

    programOrderables.forEach(programOrderable -> {
      programOrderable.setProduct(orderable);
    });
    return this;
  }

  /**
   * Builds instance of {@link Orderable}.
   */
  public Orderable build() {
    Orderable orderable = buildAsNew();
    orderable.setId(id);

    return orderable;
  }

  /**
   * Builds instance of {@link Orderable} without id field.
   */
  public Orderable buildAsNew() {
    Orderable orderable = new Orderable(productCode, dispensable, netContent, packRoundingTreshold,
        roundToZero, null, versionNumber);
    orderable.setFullProductName(fullProductName);
    orderable.setDescription(description);
    orderable.setProgramOrderables(programOrderables);
    orderable.setIdentifiers(identifiers);
    orderable.setExtraData(extraData);
    orderable.setLastUpdated(lastUpdated);
    orderable.setMinimumTemperature(minimumTemperature);
    orderable.setMaximumTemperature(maximumTemperature);
    orderable.setInBoxCubeDimension(inBoxCubeDimension);
    return orderable;
  }
}
