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

import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.javers.core.metamodel.annotation.TypeName;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@TypeName("SupplyPartnerAssociation")
@Table(name = "supply_partner_associations", schema = "referencedata")
@EqualsAndHashCode(callSuper = true)
public final class SupplyPartnerAssociation extends BaseEntity {

  @Getter
  @ManyToOne
  @JoinColumn(name = "programId", nullable = false)
  private Program program;

  @Getter
  @ManyToOne
  @JoinColumn(name = "supervisoryNodeId", nullable = false)
  private SupervisoryNode supervisoryNode;

  @ManyToMany
  @JoinTable(name = "supply_partner_association_facilities",
      schema = "referencedata",
      joinColumns = @JoinColumn(name = "supplyPartnerAssociationId", nullable = false),
      inverseJoinColumns = @JoinColumn(name = "facilityId", nullable = false))
  @BatchSize(size = 25)
  private Set<Facility> facilities = Sets.newHashSet();

  @ManyToMany
  @JoinTable(name = "supply_partner_association_orderables",
      schema = "referencedata",
      joinColumns = @JoinColumn(name = "supplyPartnerAssociationId", nullable = false),
      inverseJoinColumns = {
          @JoinColumn(name = "orderableId", nullable = false),
          @JoinColumn(name = "orderableVersionNumber", nullable = false)
      })
  @BatchSize(size = 25)
  private Set<Orderable> orderables = Sets.newHashSet();

  /**
   * Checks if this association contains the given program, supervisory node, facility and
   * orderable.
   */
  public boolean match(Program program, SupervisoryNode supervisoryNode,
      Facility facility, Orderable orderable) {
    boolean matchProgram = Objects.equals(program, this.program);
    boolean matchNode = Objects.equals(supervisoryNode, this.supervisoryNode);
    boolean hasFacility = facilities.contains(facility);
    boolean hasOrderable = orderables.contains(orderable);

    return matchProgram && matchNode && hasFacility && hasOrderable;
  }

  /**
   * Exports current state of this object to exporter.
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setProgram(program);
    exporter.setSupervisoryNode(supervisoryNode);
    facilities.forEach(exporter::addFacility);
    orderables.forEach(exporter::addOrderable);
  }

  public interface Importer extends BaseImporter {

    UUID getProgramId();

    UUID getSupervisoryNodeId();

    Set<UUID> getFacilityIds();

    Set<UUID> getOrderableIds();

  }

  public interface Exporter extends BaseExporter {

    void setProgram(Program program);

    void setSupervisoryNode(SupervisoryNode supervisoryNode);

    void addFacility(Facility facility);

    void addOrderable(Orderable orderable);

  }

}
