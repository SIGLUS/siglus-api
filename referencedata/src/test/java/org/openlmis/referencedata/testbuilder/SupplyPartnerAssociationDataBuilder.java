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

package org.openlmis.referencedata.testbuilder;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation;

public class SupplyPartnerAssociationDataBuilder {
  private UUID id = UUID.randomUUID();
  private Program program = new ProgramDataBuilder().build();
  private SupervisoryNode supervisoryNode = new SupervisoryNodeDataBuilder().build();
  private Set<Facility> facilities = Sets.newHashSet();
  private Set<Orderable> orderables = Sets.newHashSet();

  public SupplyPartnerAssociationDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }

  public SupplyPartnerAssociationDataBuilder withSupervisoryNode(SupervisoryNode supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
    return this;
  }

  public SupplyPartnerAssociationDataBuilder withFacility(Facility facility) {
    this.facilities.add(facility);
    return this;
  }

  public SupplyPartnerAssociationDataBuilder withOrderable(Orderable orderable) {
    this.orderables.add(orderable);
    return this;
  }

  public SupplyPartnerAssociation buildAsNew() {
    return new SupplyPartnerAssociation(program, supervisoryNode, facilities, orderables);
  }

  /**
   * Builds an instance of {@link SupplyPartnerAssociation}.
   */
  public SupplyPartnerAssociation build() {
    SupplyPartnerAssociation association = buildAsNew();
    association.setId(id);

    return association;
  }
}
