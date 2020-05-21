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
import org.openlmis.referencedata.dto.VolumeMeasurementDto;
import org.openlmis.referencedata.testbuilder.VolumeMeasurementDataBuilder;

public class VolumeMeasurementTest {

  @Test
  public void shouldCreateNewInstance() {
    VolumeMeasurement importerAsDomain = new VolumeMeasurementDataBuilder().build();
    VolumeMeasurementDto importer = new VolumeMeasurementDto();
    importerAsDomain.export(importer);

    VolumeMeasurement newInstance = VolumeMeasurement.newVolumeMeasurement(importer);
    assertThat(newInstance).isEqualTo(importerAsDomain);
  }

  @Test
  public void shouldExportData() {
    VolumeMeasurement instance = new VolumeMeasurementDataBuilder()
            .build();

    VolumeMeasurementDto exporter = new VolumeMeasurementDto();
    instance.export(exporter);

    assertThat(exporter.getMeasurementUnitCode()).isEqualTo(instance.getMeasurementUnitCode());
    assertThat(exporter.getValue()).isEqualTo(instance.getValue());

  }

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(VolumeMeasurement.class)
            .withRedefinedSuperclass()
            .verify();
  }

  @Test
  public void shouldImplementToString() {
    VolumeMeasurement volumeMeasurement = new VolumeMeasurement();
    ToStringTestUtils.verify(VolumeMeasurement.class, volumeMeasurement);
  }
}
