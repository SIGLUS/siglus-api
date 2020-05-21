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

package org.openlmis.referencedata.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.vividsolutions.jts.geom.Polygon;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.referencedata.dto.GeographicZoneDto;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;

public class GeographicZoneTest {
  private static final String LEVEL = "level";
  private static final String PARENT = "parent";

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(GeographicZone.class)
        .withRedefinedSuperclass()
        .withPrefabValues(Polygon.class, mock(Polygon.class), mock(Polygon.class))
        .withPrefabValues(GeographicZone.class,
            new GeographicZoneDataBuilder().buildAsNew(),
            new GeographicZoneDataBuilder().build())
        .withOnlyTheseFields("code")
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in DTO
        .verify();
  }

  @Test
  public void shouldCreateNewInstance() {
    GeographicZone importerAsDomain = new GeographicZoneDataBuilder().build();
    GeographicZoneDto importer = new GeographicZoneDto();
    importerAsDomain.export(importer);

    GeographicZone newInstance = GeographicZone.newGeographicZone(importer);
    assertThat(newInstance).isEqualTo(importerAsDomain);
    assertThat(newInstance.getParent()).isNull();
  }

  @Test
  public void shouldCreateNewInstanceWithParent() {
    GeographicZone importerAsDomain = new GeographicZoneDataBuilder()
        .withParent(new GeographicZoneDataBuilder().build())
        .build();
    GeographicZoneDto importer = new GeographicZoneDto();
    importerAsDomain.export(importer);

    GeographicZone newInstance = GeographicZone.newGeographicZone(importer);
    assertThat(newInstance).isEqualTo(importerAsDomain);
    assertThat(newInstance.getParent())
        .isNotNull()
        .isEqualTo(importerAsDomain.getParent());
  }

  @Test
  public void shouldCreateNewInstanceWithExtraData() {
    GeographicZone importerAsDomain = new GeographicZoneDataBuilder()
        .withExtraData("abc", "def")
        .build();
    GeographicZoneDto importer = new GeographicZoneDto();
    importerAsDomain.export(importer);

    GeographicZone newInstance = GeographicZone.newGeographicZone(importer);
    assertThat(newInstance).isEqualTo(importerAsDomain);
    assertThat(newInstance.getExtraData())
        .isNotNull()
        .hasSize(1)
        .containsEntry("abc", "def");
  }

  @Test
  public void shouldUpdateFromImporter() {
    GeographicZone importerAsDomain = new GeographicZoneDataBuilder().build();
    GeographicZoneDto importer = new GeographicZoneDto();
    importerAsDomain.export(importer);

    GeographicZone existing = new GeographicZoneDataBuilder().build();
    existing.updateFrom(importer);

    assertThat(existing)
        .isEqualToIgnoringGivenFields(importerAsDomain, "id", LEVEL, PARENT);
  }

  @Test
  public void shouldExportData() {
    GeographicZone instance = new GeographicZoneDataBuilder()
        .build();

    GeographicZoneDto exporter = new GeographicZoneDto();
    instance.export(exporter);

    assertThat(exporter).isEqualToIgnoringGivenFields(instance, LEVEL, PARENT);
    assertThat(exporter.getLevel()).isEqualToIgnoringGivenFields(instance.getLevel());
    assertThat(exporter.getParent()).isNull();
  }

  @Test
  public void shouldExportDataWithParent() {
    GeographicZone instance = new GeographicZoneDataBuilder()
        .withParent(new GeographicZoneDataBuilder().build())
        .build();

    GeographicZoneDto exporter = new GeographicZoneDto();
    instance.export(exporter);

    assertThat(exporter).isEqualToIgnoringGivenFields(instance, LEVEL, PARENT);
    assertThat(exporter.getLevel()).isEqualToIgnoringGivenFields(instance.getLevel());
    assertThat(exporter.getParent())
        .isNotNull()
        .isEqualToIgnoringGivenFields(instance.getParent(), LEVEL, PARENT);
  }
}
