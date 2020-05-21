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

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@Table(name = "supply_lines", schema = "referencedata",
    uniqueConstraints = @UniqueConstraint(name = "supply_line_unique_program_supervisory_node",
        columnNames = { "supervisoryNodeId", "programId" }))
@NoArgsConstructor
@AllArgsConstructor
@TypeName("SupplyLine")
@NamedQueries({
    @NamedQuery(name = "SupplyLine.findSupplyingFacilities",
        query = "SELECT DISTINCT sl.supplyingFacility"
            + " FROM SupplyLine AS sl"
            + " WHERE sl.program.id = :programId AND sl.supervisoryNode.id = :supervisoryNodeId")
    })
public class SupplyLine extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "supervisoryNodeId", nullable = false)
  @Getter
  @Setter
  private SupervisoryNode supervisoryNode;

  @Column(columnDefinition = "text")
  @Getter
  @Setter
  private String description;

  @ManyToOne
  @JoinColumn(name = "programId", nullable = false)
  @Getter
  @Setter
  private Program program;

  @ManyToOne
  @JoinColumn(name = "supplyingFacilityId", nullable = false)
  @Getter
  @Setter
  private Facility supplyingFacility;

  /**
   * Required arguments constructor.
   */
  public SupplyLine(SupervisoryNode supervisoryNode, Program program, Facility supplyingFacility) {
    this.supervisoryNode = supervisoryNode;
    this.program = program;
    this.supplyingFacility = supplyingFacility;
  }

  /**
   * Static factory method for constructing a new supply line using an importer (DTO).
   *
   * @param importer the supply line importer (DTO)
   */
  public static SupplyLine newSupplyLine(Importer importer) {
    SupervisoryNode supervisoryNode = null;

    if (importer.getSupervisoryNode() != null) {
      supervisoryNode = SupervisoryNode.newSupervisoryNode(importer.getSupervisoryNode());
    }

    Program program = null;

    if (importer.getProgram() != null) {
      program = Program.newProgram(importer.getProgram());
    }

    Facility supplyingFacility = null;

    if (importer.getSupplyingFacility() != null) {
      supplyingFacility = Facility.newFacility(importer.getSupplyingFacility());
    }

    SupplyLine supplyLine = new SupplyLine(supervisoryNode, program, supplyingFacility);
    supplyLine.id = importer.getId();
    supplyLine.description = importer.getDescription();

    return supplyLine;
  }

  /**
   * Copy values of attributes into new or updated SupplyLine.
   *
   * @param supplyLine SupplyLine with new values.
   */
  public void updateFrom(SupplyLine supplyLine) {
    this.supervisoryNode = supplyLine.getSupervisoryNode();
    this.description = supplyLine.getDescription();
    this.program = supplyLine.getProgram();
    this.supplyingFacility = supplyLine.getSupplyingFacility();
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setSupervisoryNode(supervisoryNode);
    exporter.setDescription(description);
    exporter.setProgram(program);
    exporter.setSupplyingFacility(supplyingFacility);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SupplyLine)) {
      return false;
    }
    SupplyLine that = (SupplyLine) obj;
    return Objects.equals(supervisoryNode, that.supervisoryNode)
        && Objects.equals(program, that.program)
        && Objects.equals(supplyingFacility, that.supplyingFacility);
  }

  @Override
  public int hashCode() {
    return Objects.hash(supervisoryNode, program, supplyingFacility);
  }

  public interface Exporter {
    void setId(UUID id);

    void setSupervisoryNode(SupervisoryNode supervisoryNode);

    void setDescription(String description);

    void setProgram(Program program);

    void setSupplyingFacility(Facility supplyingFacility);
  }

  public interface Importer {
    UUID getId();

    SupervisoryNode.Importer getSupervisoryNode();

    String getDescription();

    Program.Importer getProgram();

    Facility.Importer getSupplyingFacility();
  }
}
