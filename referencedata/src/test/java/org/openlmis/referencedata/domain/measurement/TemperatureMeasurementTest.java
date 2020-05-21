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

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.dto.TemperatureMeasurementDto;
import org.openlmis.referencedata.testbuilder.TemperatureMeasurementDataBuilder;

public class TemperatureMeasurementTest {

  @Test
  public void shouldCreateNewInstance() {
    TemperatureMeasurement importerAsDomain = new TemperatureMeasurementDataBuilder().build();
    TemperatureMeasurementDto importer = new TemperatureMeasurementDto();
    importerAsDomain.export(importer);

    TemperatureMeasurement newInstance = TemperatureMeasurement
            .newTemperatureMeasurement(importer);
    assertThat(newInstance).isEqualTo(importerAsDomain);
  }

  @Test
  public void shouldExportData() {
    TemperatureMeasurement instance = new TemperatureMeasurementDataBuilder()
            .build();

    TemperatureMeasurementDto exporter = new TemperatureMeasurementDto();
    instance.export(exporter);

    assertThat(exporter.getTemperatureMeasurementUnitCode())
            .isEqualTo(instance.getTemperatureMeasurementUnitCode());
    assertThat(exporter.getValue()).isEqualTo(instance.getValue());

  }

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(TemperatureMeasurement.class)
            .withRedefinedSuperclass()
            .verify();
  }

  @Test
  public void shouldImplementToString() {
    TemperatureMeasurement temperatureMeasurement = new TemperatureMeasurement();
    ToStringTestUtils.verify(TemperatureMeasurement.class, temperatureMeasurement);
  }
}
