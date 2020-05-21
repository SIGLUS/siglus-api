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
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.ObjectReferenceDtoDataBuilder;

public class SupervisoryNodeDtoTest extends ToStringContractTest<SupervisoryNodeDto> {

  @Override
  protected Class<SupervisoryNodeDto> getTestClass() {
    return SupervisoryNodeDto.class;
  }

  @Override
  protected void prepare(EqualsVerifier<SupervisoryNodeDto> verifier) {
    List<ObjectReferenceDto> facilities = DtoGenerator.of(ObjectReferenceDto.class, 2);

    verifier
        .withRedefinedSuperclass()
        .withPrefabValues(ObjectReferenceDto.class, facilities.get(0), facilities.get(1));
  }

  SupervisoryNodeDto dto = new SupervisoryNodeDto();

  @Test
  public void shouldGetParentNodeId() {
    ObjectReferenceDto parent  = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setParentNode(parent);

    assertThat(dto.getParentNodeId()).isEqualTo(parent.getId());
  }

  @Test
  public void shouldGetNullValueIfParentNodeIsNull() {
    dto.setParentNode(null);

    assertThat(dto.getParentNodeId()).isNull();
  }

  @Test
  public void shouldGetPartnerNodeIds() {
    ObjectReferenceDto partner = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setPartnerNodes(Sets.newHashSet(partner));

    assertThat(dto.getPartnerNodeIds()).hasSize(1).contains(partner.getId());
  }

  @Test
  public void shouldGetEmptySetIfPartnerNodeSetIsEmpty() {
    dto.setPartnerNodes(Sets.newHashSet());

    assertThat(dto.getPartnerNodeIds()).hasSize(0).isEmpty();
  }

  @Test
  public void shouldGetRequisitionGroupIds() {
    ObjectReferenceDto requisitionGroup = new ObjectReferenceDtoDataBuilder().buildAsDto();

    dto.setRequisitionGroup(requisitionGroup);

    assertThat(dto.getRequisitionGroupId()).isEqualTo(requisitionGroup.getId());
  }

  @Test
  public void shouldGetNullValueIfRequisitionGroupIsNull() {
    dto.setRequisitionGroup(null);

    assertThat(dto.getRequisitionGroupId()).isNull();
  }
}
