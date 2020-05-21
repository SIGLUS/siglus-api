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

package org.openlmis.requisition.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.dto.ToStringContractTest;

public class RoleAssignmentPermissionValidationDetailsTest
    extends ToStringContractTest<RoleAssignmentPermissionValidationDetails> {

  @Override
  protected Class<RoleAssignmentPermissionValidationDetails> getTestClass() {
    return RoleAssignmentPermissionValidationDetails.class;
  }

  @Override
  protected void prepare(EqualsVerifier<RoleAssignmentPermissionValidationDetails> verifier) {
    Requisition left = new RequisitionDataBuilder().build();
    Requisition right = new RequisitionDataBuilder().build();

    verifier.withPrefabValues(Requisition.class, left, right);
  }

  @Override
  protected Optional<RoleAssignmentPermissionValidationDetails> getInstance() {
    return Optional.of(new RoleAssignmentPermissionValidationDetails("", new Requisition()));
  }

  @Test
  public void containsPartnerRequisitionShouldReturnTrueIfRequisitionIsPartnerRequisition() {
    // given
    Requisition requisition = new RequisitionDataBuilder().build();
    requisition.setOriginalRequisitionId(UUID.randomUUID());

    // when
    RoleAssignmentPermissionValidationDetails details =
        new RoleAssignmentPermissionValidationDetails("", requisition);

    // then
    assertThat(details.containsPartnerRequisition()).isTrue();
  }

  @Test
  public void containsPartnerRequisitionShouldReturnTrueIfRequisitionIsNormalRequisition() {
    // given
    Requisition requisition = new RequisitionDataBuilder().build();

    // when
    RoleAssignmentPermissionValidationDetails details =
        new RoleAssignmentPermissionValidationDetails("", requisition);

    // then
    assertThat(details.containsPartnerRequisition()).isFalse();
  }
}
