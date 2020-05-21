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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.SupervisoryNode;

public class RequisitionGroupDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private String description;
  private SupervisoryNode supervisoryNode;
  private List<RequisitionGroupProgramSchedule> requisitionGroupProgramSchedules;
  private Set<Facility> memberFacilities;

  /**
   * Returns instance of {@link RequisitionGroupDataBuilder} with sample data.
   */
  public RequisitionGroupDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "RG" + instanceNumber;
    name = "Requisition Group " + instanceNumber;
    description = "some-description";
    supervisoryNode = new SupervisoryNodeDataBuilder().build();
    requisitionGroupProgramSchedules = Lists.newArrayList();
    memberFacilities = Sets.newHashSet();
  }

  /**
   * Builds instance of {@link RequisitionGroup}.
   */
  public RequisitionGroup build() {
    RequisitionGroup requisitionGroup = new RequisitionGroup(code, name, description,
        supervisoryNode, requisitionGroupProgramSchedules, memberFacilities);
    requisitionGroup.setId(id);

    return requisitionGroup;
  }

  /**
   * Builds instance of {@link RequisitionGroup} without id.
   */
  public RequisitionGroup buildAsNew() {
    return this.withoutId().build();
  }

  /**
   * Sets id for new {@link RequisitionGroup}.
   */
  public RequisitionGroupDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Sets null id for new {@link RequisitionGroup}.
   */
  public RequisitionGroupDataBuilder withoutId() {
    return withId(null);
  }

  /**
   * Sets supervisory node for new {@link RequisitionGroup}.
   */
  public RequisitionGroupDataBuilder withSupervisoryNode(SupervisoryNode node) {
    this.supervisoryNode = node;
    return this;
  }

  /**
   * Sets member facilities for new {@link RequisitionGroup}.
   */
  public RequisitionGroupDataBuilder withMemberFacility(Facility memberFacility) {
    this.memberFacilities.add(memberFacility);
    return this;
  }

  public RequisitionGroupDataBuilder withRequisitionGroupProgramSchedule(
      RequisitionGroupProgramSchedule schedule) {
    this.requisitionGroupProgramSchedules.add(schedule);
    return this;
  }

  /**
   * Sets name for new {@link RequisitionGroup}.
   */
  public RequisitionGroupDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets code for new {@link RequisitionGroup}.
   */
  public RequisitionGroupDataBuilder withCode(String code) {
    this.code = code;
    return this;
  }
}
