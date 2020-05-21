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

import com.google.common.collect.Sets;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.requisition.testutils.DtoGenerator;

public class RequisitionGroupDtoTest extends ToStringContractTest<RequisitionGroupDto> {

  @Override
  protected Class<RequisitionGroupDto> getTestClass() {
    return RequisitionGroupDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier<RequisitionGroupDto> verifier) {
    FacilityDto left = new FacilityDto();
    FacilityDto right = DtoGenerator.of(FacilityDto.class);

    verifier
        .withRedefinedSuperclass()
        .withPrefabValues(FacilityDto.class, left, right);
  }

  @Test
  public void shouldReturnTrueIfContainsFacilityWithId() {
    // given
    RequisitionGroupDto requisitionGroup = DtoGenerator.of(RequisitionGroupDto.class);
    FacilityDto facility = DtoGenerator.of(FacilityDto.class);

    requisitionGroup.setMemberFacilities(Sets.newHashSet(facility));

    // expect
    assertThat(requisitionGroup.hasFacility(facility.getId())).isTrue();
  }

  @Test
  public void shouldReturnFalseIfItDoesNotContainFacilityWithId() {
    // given
    RequisitionGroupDto requisitionGroup = DtoGenerator.of(RequisitionGroupDto.class);
    FacilityDto facility = DtoGenerator.of(FacilityDto.class);

    requisitionGroup.setMemberFacilities(Sets.newHashSet(facility));

    // expect
    assertThat(requisitionGroup.hasFacility(UUID.randomUUID())).isFalse();
  }
}
