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

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.domain.SupplyPartner;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation;
import org.openlmis.referencedata.testbuilder.SupplyPartnerAssociationDataBuilder;
import org.openlmis.referencedata.testbuilder.SupplyPartnerDataBuilder;

public class SupplyPartnerDtoTest {
  private static final String SERVICE_URL = "http://localhost";

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SupplyPartnerDto.class)
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    SupplyPartnerDto dto = new SupplyPartnerDto();
    ToStringTestUtils.verify(SupplyPartnerDto.class, dto);
  }

  @Test
  public void shouldCreateNewInstanceFromDomain() {
    SupplyPartner partner = new SupplyPartnerDataBuilder().build();
    SupplyPartnerDto expected = new SupplyPartnerDto();
    expected.setServiceUrl(SERVICE_URL);
    partner.export(expected);

    SupplyPartnerDto actual = SupplyPartnerDto.newInstance(partner, SERVICE_URL);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void shouldAddEntry() {
    SupplyPartnerAssociation association = new SupplyPartnerAssociationDataBuilder()
        .build();
    SupplyPartnerAssociationDto expected = new SupplyPartnerAssociationDto();
    expected.setServiceUrl(SERVICE_URL);
    association.export(expected);

    SupplyPartnerDto dto = new SupplyPartnerDto();
    dto.setServiceUrl(SERVICE_URL);
    dto.addEntry(association);

    assertThat(dto.getAssociationEntries()).hasSize(1).contains(expected);
  }

  @Test
  public void shouldReturnEmptyListOfEntriesIfAssociationListIsNull() {
    SupplyPartnerDto dto = new SupplyPartnerDto();
    dto.setAssociations(null);

    assertThat(dto.getAssociationEntries())
        .isNotNull()
        .isEmpty();
  }
}
