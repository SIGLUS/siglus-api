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

package org.openlmis.referencedata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.GeographicLevel;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.dto.GeographicLevelDto;
import org.openlmis.referencedata.dto.GeographicZoneDto;
import org.openlmis.referencedata.dto.GeographicZoneSimpleDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.GeographicLevelRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.testbuilder.GeographicLevelDataBuilder;
import org.openlmis.referencedata.testbuilder.GeographicZoneDataBuilder;
import org.openlmis.referencedata.util.messagekeys.GeographicLevelMessageKeys;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;

@RunWith(MockitoJUnitRunner.class)
public class GeographicZoneBuilderTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private GeographicLevelRepository geographicLevelRepository;

  @Mock
  private GeographicZoneRepository geographicZoneRepository;

  @InjectMocks
  private GeographicZoneBuilder builder;

  private GeographicLevel level = new GeographicLevelDataBuilder().build();
  private GeographicZone parent = new GeographicZoneDataBuilder().build();
  private GeographicZone geographicZone = new GeographicZoneDataBuilder()
      .withLevel(level)
      .withParent(parent)
      .build();

  private GeographicZoneDto importer = new GeographicZoneDto();

  @Before
  public void setUp() {
    geographicZone.export(importer);
    importer.setId(null);

    when(geographicLevelRepository.findOne(level.getId())).thenReturn(level);
    when(geographicZoneRepository.findOne(parent.getId())).thenReturn(parent);
  }

  @Test
  public void shouldBuildDomainObjectBasedOnDataFromImporter() {
    GeographicZone geographicZone = builder.build(importer);

    assertThat(geographicZone)
        .isEqualToIgnoringGivenFields(importer, "level", "parent")
        .hasFieldOrPropertyWithValue("level", level)
        .hasFieldOrPropertyWithValue("parent", parent);
  }

  @Test
  public void shouldThrowExceptionIfGeographicLevelCouldNotBeFound() {
    when(geographicLevelRepository.findOne(level.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicLevelMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfParentCouldNotBeFound() {
    when(geographicZoneRepository.findOne(parent.getId())).thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicZoneMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfGeographicLevelIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicLevelMessageKeys.ERROR_NOT_FOUND);

    importer.getLevel().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfParentIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicZoneMessageKeys.ERROR_NOT_FOUND);

    importer.getParent().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfGeographicLevelImporterDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(GeographicLevelMessageKeys.ERROR_NOT_FOUND);

    importer.setLevel((GeographicLevelDto) null);
    builder.build(importer);
  }

  @Test
  public void shouldNotThrowExceptionIfParentImporterDoesNotExist() {
    importer.setParent((GeographicZoneSimpleDto) null);

    GeographicZone built = builder.build(importer);

    assertThat(built.getParent()).isNull();
  }

  @Test
  public void shouldUseInstanceFromDatabaseIfImporterHasIdSet() {
    importer.setId(UUID.randomUUID());
    when(geographicZoneRepository.findOne(importer.getId()))
        .thenReturn(geographicZone);

    GeographicZone built = builder.build(importer);
    assertThat(built.getId()).isEqualTo(geographicZone.getId());
  }

}
