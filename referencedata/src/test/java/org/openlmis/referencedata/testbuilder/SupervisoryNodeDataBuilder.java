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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.ExtraDataEntity;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;

@SuppressWarnings("PMD.TooManyMethods")
public class SupervisoryNodeDataBuilder {

  private static int instanceNumber = 0;

  private UUID id;
  private String code;
  private String name;
  private String description;
  private Facility facility;
  private SupervisoryNode parentNode;
  private Set<SupervisoryNode> childNodes;
  private SupervisoryNode partnerNodeOf;
  private Set<SupervisoryNode> partnerNodes;
  private RequisitionGroup requisitionGroup;
  private Map<String, Object> extraData;

  /**
   * Returns instance of {@link SupervisoryNodeDataBuilder} with sample data.
   */
  public SupervisoryNodeDataBuilder() {
    instanceNumber++;

    id = UUID.randomUUID();
    code = "SN" + instanceNumber;
    name = "Supervisory Node " + instanceNumber;
    description = "desc";
    facility = new FacilityDataBuilder().build();
    childNodes = new HashSet<>();
    partnerNodes = new HashSet<>();
    extraData = new HashMap<>();
  }

  /**
   * Builds instance of {@link SupervisoryNode}.
   */
  public SupervisoryNode build() {
    SupervisoryNode node = new SupervisoryNode(code, name, description, facility, parentNode,
        partnerNodeOf, childNodes, partnerNodes, requisitionGroup, new ExtraDataEntity(extraData));
    node.setId(id);

    return node;
  }

  /**
   * Sets id with null value for new {@link SupervisoryNode}.
   */
  public SupervisoryNodeDataBuilder withoutId() {
    this.id = null;
    return this;
  }

  /**
   * Sets facility for new {@link SupervisoryNode}.
   */
  public SupervisoryNodeDataBuilder withFacility(Facility facility) {
    this.facility = facility;
    return this;
  }

  public SupervisoryNodeDataBuilder withRequisitionGroup(RequisitionGroup requisitionGroup) {
    this.requisitionGroup = requisitionGroup;
    return this;
  }

  public SupervisoryNodeDataBuilder withParentNode(SupervisoryNode parentNode) {
    this.parentNode = parentNode;
    return this;
  }

  public SupervisoryNodeDataBuilder withPartnerNodeOf(SupervisoryNode partnerNodeOf) {
    this.partnerNodeOf = partnerNodeOf;
    return this;
  }

  public SupervisoryNodeDataBuilder withChildNode(SupervisoryNode childNode) {
    this.childNodes.add(childNode);
    return this;
  }

  public SupervisoryNodeDataBuilder withPartnerNode(SupervisoryNode partnerNode) {
    this.partnerNodes.add(partnerNode);
    return this;
  }

  /**
   * Sets code for new {@link SupervisoryNode}.
   *
   * @param code  the code to be set
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withCode(String code) {
    this.code = code;
    return this;
  }

  /**
   * Sets the code to null for new {@link SupervisoryNode}.
   *
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withoutCode() {
    return withCode(null);
  }

  /**
   * Sets name for new {@link SupervisoryNode}.
   *
   * @param name the name to be set
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the name to null for new {@link SupervisoryNode}.
   *
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withoutName() {
    return withName(null);
  }

  /**
   * Sets the requisition group to null for new {@link SupervisoryNode}.
   *
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withoutRequisitionGroup() {
    return withRequisitionGroup(null);
  }

  /**
   * Sets description for new {@link SupervisoryNode}.
   *
   * @param description  the description to be set
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * Sets id for new {@link SupervisoryNode}.
   *
   * @param id the name to be set
   * @return the builder instance
   */
  public SupervisoryNodeDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }
}
