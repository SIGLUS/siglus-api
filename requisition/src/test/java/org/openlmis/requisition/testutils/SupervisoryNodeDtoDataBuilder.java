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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.testutils.api.DtoDataBuilder;

public class SupervisoryNodeDtoDataBuilder implements DtoDataBuilder<SupervisoryNodeDto> {

  private UUID id;
  private String code;
  private ObjectReferenceDto facility;
  private String name;
  private String description;
  private Map<String, Object> extraData;
  private ObjectReferenceDto parentNode;
  private ObjectReferenceDto requisitionGroup;
  private Set<ObjectReferenceDto> childNodes;
  private ObjectReferenceDto partnerNodeOf;
  private Set<ObjectReferenceDto> partnerNodes;

  /**
   * Builder for {@link SupervisoryNodeDto}.
   */
  public SupervisoryNodeDtoDataBuilder() {
    id = UUID.randomUUID();
    code = "code";
    facility = new ObjectReferenceDtoDataBuilder().buildAsDto();
    name = "name";
    description = "description";
    extraData = new HashMap<>();
    parentNode = new ObjectReferenceDtoDataBuilder().buildAsDto();
    requisitionGroup = new ObjectReferenceDtoDataBuilder().buildAsDto();
    childNodes = new HashSet<>();
    partnerNodeOf = new ObjectReferenceDtoDataBuilder().buildAsDto();
    partnerNodes = new HashSet<>();
  }

  @Override
  public SupervisoryNodeDto buildAsDto() {
    SupervisoryNodeDto node = new SupervisoryNodeDto(
        code,
        facility,
        name,
        description,
        extraData,
        parentNode,
        requisitionGroup,
        childNodes,
        partnerNodeOf,
        partnerNodes);
    node.setId(id);
    return node;
  }

  public SupervisoryNodeDtoDataBuilder withId(UUID id) {
    this.id = id;
    return this;
  }
}
