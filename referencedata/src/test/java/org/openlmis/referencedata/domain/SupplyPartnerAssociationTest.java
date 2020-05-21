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
import org.junit.Test;
import org.openlmis.referencedata.dto.SupplyPartnerAssociationDto;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.OrderableDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.testbuilder.SupplyPartnerAssociationDataBuilder;

public class SupplyPartnerAssociationTest {

  private Program program = new ProgramDataBuilder().build();
  private SupervisoryNode supervisoryNode = new SupervisoryNodeDataBuilder().build();
  private Facility facility = new FacilityDataBuilder().build();
  private Orderable orderable = new OrderableDataBuilder().build();

  private SupplyPartnerAssociation association = new SupplyPartnerAssociationDataBuilder()
      .withProgram(program)
      .withSupervisoryNode(supervisoryNode)
      .withFacility(facility)
      .withOrderable(orderable)
      .build();

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SupplyPartnerAssociation.class)
        .withRedefinedSuperclass()
        .withPrefabValues(Program.class,
            new ProgramDataBuilder().build(),
            new ProgramDataBuilder().build())
        .withPrefabValues(SupervisoryNode.class,
            new SupervisoryNodeDataBuilder().build(),
            new SupervisoryNodeDataBuilder().build())
        .withPrefabValues(Facility.class,
            new FacilityDataBuilder().build(),
            new FacilityDataBuilder().build())
        .withPrefabValues(Orderable.class,
            new OrderableDataBuilder().build(),
            new OrderableDataBuilder().build())
        .suppress(Warning.NONFINAL_FIELDS) // we can't make fields as final in DTO
        .verify();
  }

  @Test
  public void shouldExportCurrentState() {
    SupplyPartnerAssociationDto exporter = new SupplyPartnerAssociationDto();
    association.export(exporter);

    assertThat(exporter.getProgramId()).isEqualTo(program.getId());
    assertThat(exporter.getSupervisoryNodeId()).isEqualTo(supervisoryNode.getId());
    assertThat(exporter.getFacilityIds()).hasSize(1).contains(facility.getId());
    assertThat(exporter.getOrderableIds()).hasSize(1).contains(orderable.getId());
  }

  @Test
  public void shouldMatchIfParamsAreCorrect() {
    assertThat(association.match(program, supervisoryNode, facility, orderable)).isTrue();
  }

  @Test
  public void shouldNotMatchIfProgramIsIncorrect() {
    Program another = new ProgramDataBuilder().build();
    assertThat(association.match(another, supervisoryNode, facility, orderable)).isFalse();
  }

  @Test
  public void shouldNotMatchIfSupervisoryNodeIsIncorrect() {
    SupervisoryNode another = new SupervisoryNodeDataBuilder().build();
    assertThat(association.match(program, another, facility, orderable)).isFalse();
  }

  @Test
  public void shouldNotMatchIfFacilityIsNotInAssociation() {
    Facility another = new FacilityDataBuilder().build();
    assertThat(association.match(program, supervisoryNode, another, orderable)).isFalse();
  }

  @Test
  public void shouldNotMatchIfOrderableIsNotInAssociation() {
    Orderable another = new OrderableDataBuilder().build();
    assertThat(association.match(program, supervisoryNode, facility, another)).isFalse();
  }
}
