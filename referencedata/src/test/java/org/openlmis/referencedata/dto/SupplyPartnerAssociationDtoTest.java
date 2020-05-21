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
import org.junit.Before;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.OrderableDataBuilder;
import org.openlmis.referencedata.testbuilder.ProgramDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.web.BaseController;
import org.openlmis.referencedata.web.FacilityController;
import org.openlmis.referencedata.web.OrderableController;

public class SupplyPartnerAssociationDtoTest {

  private static final String SERVICE_URL = "http://localhost";

  private SupplyPartnerAssociationDto dto = new SupplyPartnerAssociationDto();

  @Before
  public void setUp() throws Exception {
    dto.setServiceUrl(SERVICE_URL);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SupplyPartnerAssociationDto.class)
        .withRedefinedSuperclass()
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    ToStringTestUtils.verify(SupplyPartnerAssociationDto.class, dto);
  }

  @Test
  public void shouldGetProgramId() {
    Program program  = new ProgramDataBuilder().build();

    dto.setProgram(program);

    assertThat(dto.getProgramId()).isEqualTo(program.getId());
  }

  @Test
  public void shouldGetSupervisoryNodeId() {
    SupervisoryNode supervisoryNode  = new SupervisoryNodeDataBuilder().build();

    dto.setSupervisoryNode(supervisoryNode);

    assertThat(dto.getSupervisoryNodeId()).isEqualTo(supervisoryNode.getId());
  }

  @Test
  public void shouldGetFacilityIds() {
    Facility facility = new FacilityDataBuilder().build();

    dto.addFacility(facility);

    assertThat(dto.getFacilityIds()).hasSize(1).contains(facility.getId());
  }

  @Test
  public void shouldGetOrderableIds() {
    Orderable orderable = new OrderableDataBuilder().build();

    dto.addOrderable(orderable);

    assertThat(dto.getOrderableIds()).hasSize(1).contains(orderable.getId());
  }

  @Test
  public void shouldAddFacility() {
    Facility facility = new FacilityDataBuilder().build();
    ObjectReferenceDto facilityReference = new ObjectReferenceDto(SERVICE_URL,
        BaseController.API_PATH + FacilityController.RESOURCE_PATH, facility.getId());

    dto.addFacility(facility);
    assertThat(dto.getFacilities())
        .hasSize(1)
        .contains(facilityReference);
  }

  @Test
  public void shouldAddOrderable() {
    Orderable orderable = new OrderableDataBuilder().build();
    ObjectReferenceDto orderableReference = new ObjectReferenceDto(SERVICE_URL,
        BaseController.API_PATH + OrderableController.RESOURCE_PATH, orderable.getId());


    dto.addOrderable(orderable);
    assertThat(dto.getOrderables())
        .hasSize(1)
        .contains(orderableReference);
  }
}
