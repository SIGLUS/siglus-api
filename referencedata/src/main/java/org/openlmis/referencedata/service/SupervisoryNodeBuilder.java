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

package org.openlmis.referencedata.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.dto.SupervisoryNodeDto;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.RequisitionGroupRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RequisitionGroupMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupervisoryNodeBuilder
    implements DomainResourceBuilder<SupervisoryNodeDto, SupervisoryNode> {

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private RequisitionGroupRepository requisitionGroupRepository;

  @Override
  public SupervisoryNode build(SupervisoryNodeDto importer) {
    final Facility facility = Optional
        .ofNullable(importer.getFacility())
        .map(obj -> findResource(
            facilityRepository::findOne, obj, FacilityMessageKeys.ERROR_NOT_FOUND))
        .orElse(null);
    final SupervisoryNode parent = Optional
        .ofNullable(importer.getParentNode())
        .map(obj -> findResource(
            supervisoryNodeRepository::findOne, obj, SupervisoryNodeMessageKeys.ERROR_NOT_FOUND))
        .orElse(null);
    final SupervisoryNode partnerNodeOf = Optional
        .ofNullable(importer.getPartnerNodeOf())
        .map(obj -> findResource(
            supervisoryNodeRepository::findOne, obj, SupervisoryNodeMessageKeys.ERROR_NOT_FOUND))
        .orElse(null);
    final RequisitionGroup requisitionGroup = Optional
        .ofNullable(importer.getRequisitionGroup())
        .map(obj -> findResource(
            requisitionGroupRepository::findOne, obj, RequisitionGroupMessageKeys.ERROR_NOT_FOUND))
        .orElse(null);
    final Set<SupervisoryNode> childNodes = Optional
        .ofNullable(importer.getChildNodes())
        .map(nodes -> nodes.stream().map(BaseDto::getId).collect(Collectors.toSet()))
        .map(ids -> findResources(
            supervisoryNodeRepository::findAll, ids, SupervisoryNodeMessageKeys.ERROR_NOT_FOUND))
        .map(HashSet::new)
        .orElse(new HashSet<>());
    final Set<SupervisoryNode> partnerNodes = Optional
        .ofNullable(importer.getPartnerNodes())
        .map(nodes -> nodes.stream().map(BaseDto::getId).collect(Collectors.toSet()))
        .map(ids -> findResources(
            supervisoryNodeRepository::findAll, ids, SupervisoryNodeMessageKeys.ERROR_NOT_FOUND))
        .map(HashSet::new)
        .orElse(new HashSet<>());

    SupervisoryNode supervisoryNode;

    if (null == importer.getId()) {
      supervisoryNode = new SupervisoryNode();
    } else {
      supervisoryNode = supervisoryNodeRepository.findOne(importer.getId());

      if (null == supervisoryNode) {
        supervisoryNode = new SupervisoryNode();
        supervisoryNode.setId(importer.getId());
      }
    }

    supervisoryNode.updateFrom(importer);
    supervisoryNode.setFacility(facility);
    supervisoryNode.assignParentNode(parent);
    supervisoryNode.setRequisitionGroup(requisitionGroup);
    supervisoryNode.assignChildNodes(childNodes);
    supervisoryNode.assignPartnerNodeOf(partnerNodeOf);
    supervisoryNode.assignPartnerNodes(partnerNodes);

    return supervisoryNode;

  }

}
