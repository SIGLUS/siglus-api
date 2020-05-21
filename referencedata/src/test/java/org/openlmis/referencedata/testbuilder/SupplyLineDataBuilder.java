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

import java.util.UUID;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyLine;

public class SupplyLineDataBuilder {

  private UUID id;
  private SupervisoryNode supervisoryNode;
  private String description;
  private Program program;
  private Facility supplyingFacility;

  /**
   * Returns instance of {@link SupplyLineDataBuilder} with sample data.
   */
  public SupplyLineDataBuilder() {
    id = UUID.randomUUID();
    supervisoryNode = new SupervisoryNodeDataBuilder().build();
    program = new ProgramDataBuilder().build();
    supplyingFacility = new FacilityDataBuilder().build();
    description = "description";
  }

  /**
   * Builds instance of {@link SupplyLine}.
   */
  public SupplyLine build() {
    SupplyLine supplyLine = buildAsNew();
    supplyLine.setId(id);

    return supplyLine;
  }

  /**
   * Builds instance of {@link SupplyLine} without id.
   */
  public SupplyLine buildAsNew() {
    SupplyLine supplyLine = new SupplyLine(supervisoryNode, description, program,
        supplyingFacility);

    return supplyLine;
  }

  /**
   * Sets supervisory node for new {@link SupplyLine}.
   */
  public SupplyLineDataBuilder withSupervisoryNode(SupervisoryNode supervisoryNode) {
    this.supervisoryNode = supervisoryNode;
    return this;
  }

  /**
   * Sets program for new {@link SupplyLine}.
   */
  public SupplyLineDataBuilder withProgram(Program program) {
    this.program = program;
    return this;
  }

  /**
   * Sets supplying facility for new {@link SupplyLine}.
   */
  public SupplyLineDataBuilder withSupplyingFacility(Facility facility) {
    this.supplyingFacility = facility;
    return this;
  }

  /**
   * Sets description for new {@link SupplyLine}.
   */
  public SupplyLineDataBuilder withDescription(String description) {
    this.description = description;
    return this;
  }
}
