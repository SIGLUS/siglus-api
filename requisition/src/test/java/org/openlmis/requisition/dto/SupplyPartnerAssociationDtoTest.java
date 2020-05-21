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

package org.openlmis.requisition.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.requisition.testutils.ObjectReferenceDtoDataBuilder;

public class SupplyPartnerAssociationDtoTest
    extends ToStringContractTest<SupplyPartnerAssociationDto> {

  private SupplyPartnerAssociationDto dto = new SupplyPartnerAssociationDto();

  @Override
  protected Class<SupplyPartnerAssociationDto> getTestClass() {
    return SupplyPartnerAssociationDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier<SupplyPartnerAssociationDto> verifier) {
    verifier.withRedefinedSuperclass();
  }

  @Test
  public void shouldGetProgramId() {
    ObjectReferenceDto program  = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setProgram(program);

    assertThat(dto.getProgramId()).isEqualTo(program.getId());
  }

  @Test
  public void shouldGetSupervisoryNodeId() {
    ObjectReferenceDto supervisoryNode  = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setSupervisoryNode(supervisoryNode);

    assertThat(dto.getSupervisoryNodeId()).isEqualTo(supervisoryNode.getId());
  }

  @Test
  public void shouldGetFacilityIds() {
    ObjectReferenceDto facility = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setFacilities(Lists.newArrayList(facility));

    assertThat(dto.getFacilityIds()).hasSize(1).contains(facility.getId());
  }

  @Test
  public void shouldGetOrderableIds() {
    ObjectReferenceDto orderable = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setOrderables(Lists.newArrayList(orderable));

    assertThat(dto.getOrderableIds()).hasSize(1).contains(orderable.getId());
  }

}
