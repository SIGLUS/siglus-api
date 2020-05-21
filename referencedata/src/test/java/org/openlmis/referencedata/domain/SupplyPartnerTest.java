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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.referencedata.dto.SupplyPartnerDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.testbuilder.SupplyPartnerAssociationDataBuilder;
import org.openlmis.referencedata.testbuilder.SupplyPartnerDataBuilder;
import org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys;

public class SupplyPartnerTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SupplyPartner.class)
        .withRedefinedSuperclass()
        .withOnlyTheseFields("code")
        .withPrefabValues(SupplyPartnerAssociation.class,
            new SupplyPartnerAssociationDataBuilder().build(),
            new SupplyPartnerAssociation())
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in DTO
        .verify();
  }

  @Test
  public void shouldUpdateFromImporter() {
    SupplyPartner supplyPartner = new SupplyPartnerDataBuilder().build();
    SupplyPartnerDto importer = new SupplyPartnerDto();
    supplyPartner.export(importer);

    SupplyPartner newSupplyPartner = new SupplyPartner();
    newSupplyPartner.updateFrom(importer);

    assertThat(newSupplyPartner).isEqualToComparingOnlyGivenFields(supplyPartner, "name", "code");
  }

  @Test
  public void shouldThrowExceptionWhileUpdateFromIfCodeWasNotSet() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupplyPartnerMessageKeys.ERROR_CODE_REQUIRED);

    SupplyPartner supplyPartner = new SupplyPartnerDataBuilder().build();
    SupplyPartnerDto importer = new SupplyPartnerDto();
    supplyPartner.export(importer);

    SupplyPartner newSupplyPartner = new SupplyPartner();

    importer.setCode(null);
    newSupplyPartner.updateFrom(importer);
  }

  @Test
  public void shouldThrowExceptionWhileUpdateFromIfCodeWasBlank() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupplyPartnerMessageKeys.ERROR_CODE_REQUIRED);

    SupplyPartner supplyPartner = new SupplyPartnerDataBuilder().build();
    SupplyPartnerDto importer = new SupplyPartnerDto();
    supplyPartner.export(importer);

    SupplyPartner newSupplyPartner = new SupplyPartner();

    importer.setCode("          ");
    newSupplyPartner.updateFrom(importer);
  }

  @Test
  public void shouldThrowExceptionWhileUpdateFromIfNameWasNotSet() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupplyPartnerMessageKeys.ERROR_NAME_REQUIRED);

    SupplyPartner supplyPartner = new SupplyPartnerDataBuilder().build();
    SupplyPartnerDto importer = new SupplyPartnerDto();
    supplyPartner.export(importer);

    SupplyPartner newSupplyPartner = new SupplyPartner();

    importer.setName(null);
    newSupplyPartner.updateFrom(importer);
  }

  @Test
  public void shouldThrowExceptionWhileUpdateFromIfNameWasBlank() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupplyPartnerMessageKeys.ERROR_NAME_REQUIRED);

    SupplyPartner supplyPartner = new SupplyPartnerDataBuilder().build();
    SupplyPartnerDto importer = new SupplyPartnerDto();
    supplyPartner.export(importer);

    SupplyPartner newSupplyPartner = new SupplyPartner();

    importer.setName("       ");
    newSupplyPartner.updateFrom(importer);
  }

  @Test
  public void shouldExportCurrentState() {
    SupplyPartnerAssociation association = new SupplyPartnerAssociationDataBuilder()
        .build();
    SupplyPartner supplyPartner = new SupplyPartnerDataBuilder()
        .withAssociation(association)
        .build();
    SupplyPartnerDto exporter = new SupplyPartnerDto();
    supplyPartner.export(exporter);

    assertThat(exporter).isEqualToComparingOnlyGivenFields(supplyPartner, "name", "code");
    assertThat(exporter.getAssociationEntries()).hasSize(1);
    assertThat(exporter.getAssociationEntries().get(0).getId()).isEqualTo(association.getId());
  }
}
