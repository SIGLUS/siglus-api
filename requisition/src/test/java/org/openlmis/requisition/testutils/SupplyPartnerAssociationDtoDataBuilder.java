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

package org.openlmis.requisition.testutils;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.SupplyPartnerAssociationDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;
import org.openlmis.requisition.testutils.api.RepositoryDataBuilder;

public class SupplyPartnerAssociationDtoDataBuilder implements
    DtoDataBuilder<SupplyPartnerAssociationDto>,
    RepositoryDataBuilder<SupplyPartnerAssociationDto> {
  private UUID id = UUID.randomUUID();
  private ObjectReferenceDto program = new ObjectReferenceDtoDataBuilder().buildAsDto();
  private ObjectReferenceDto supervisoryNode = new ObjectReferenceDtoDataBuilder().buildAsDto();
  private List<ObjectReferenceDto> facilities = Lists.newArrayList();
  private List<ObjectReferenceDto> orderables = Lists.newArrayList();

  public SupplyPartnerAssociationDtoDataBuilder withProgram(ObjectReferenceDto program) {
    this.program = program;
    return this;
  }

  public SupplyPartnerAssociationDtoDataBuilder withSupervisoryNode(
      ObjectReferenceDto supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
    return this;
  }

  public SupplyPartnerAssociationDtoDataBuilder withFacility(ObjectReferenceDto facility) {
    this.facilities.add(facility);
    return this;
  }

  public SupplyPartnerAssociationDtoDataBuilder withOrderable(ObjectReferenceDto orderable) {
    this.orderables.add(orderable);
    return this;
  }

  @Override
  public SupplyPartnerAssociationDto buildAsNew() {
    return new SupplyPartnerAssociationDto(program, supervisoryNode, facilities, orderables);
  }

  /**
   * Builds an instance of {@link SupplyPartnerAssociationDto}.
   */
  @Override
  public SupplyPartnerAssociationDto buildAsDto() {
    SupplyPartnerAssociationDto association = buildAsNew();
    association.setId(id);

    return association;
  }
}
