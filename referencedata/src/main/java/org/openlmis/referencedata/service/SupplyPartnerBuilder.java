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

import static org.openlmis.referencedata.util.Pagination.DEFAULT_PAGE_NUMBER;
import static org.openlmis.referencedata.util.Pagination.NO_PAGINATION;
import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_GLOBAL_UNIQUE;
import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_INVALID_FACILITY;
import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_INVALID_ORDERABLE;
import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_INVALID_SUPERVISORY_NODE;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.domain.SupplyPartner;
import org.openlmis.referencedata.domain.SupplyPartnerAssociation;
import org.openlmis.referencedata.dto.SupplyPartnerAssociationDto;
import org.openlmis.referencedata.dto.SupplyPartnerDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeApprovedProductRepository;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.repository.SupplyPartnerRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.OrderableMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys;
import org.openlmis.referencedata.web.FacilityTypeApprovedProductSearchParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class SupplyPartnerBuilder
    implements DomainResourceBuilder<SupplyPartnerDto, SupplyPartner> {

  @Autowired
  private SupplyPartnerRepository supplyPartnerRepository;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private FacilityTypeApprovedProductRepository facilityTypeApprovedProductRepository;

  @Override
  public SupplyPartner build(SupplyPartnerDto importer) {
    SupplyPartner supplyPartner;

    if (null == importer.getId()) {
      supplyPartner = new SupplyPartner();
    } else {
      supplyPartner = supplyPartnerRepository.findOne(importer.getId());

      if (null == supplyPartner) {
        supplyPartner = new SupplyPartner();
        supplyPartner.setId(importer.getId());
      }
    }

    supplyPartner.updateFrom(importer);
    addAssociations(importer.getAssociations(), supplyPartner);

    return supplyPartner;
  }

  private void addAssociations(List<SupplyPartnerAssociationDto> associations,
      SupplyPartner supplyPartner) {
    supplyPartner.removeAllAssociations();

    if (CollectionUtils.isEmpty(associations)) {
      return;
    }

    associations
        .stream()
        .map(this::createAssociation)
        .forEach(supplyPartner::addAssociation);
  }

  private SupplyPartnerAssociation createAssociation(SupplyPartnerAssociationDto dto) {
    if (dto.getFacilityIds().isEmpty()) {
      throw new ValidationMessageException(SupplyPartnerMessageKeys.ERROR_MISSING_FACILITIES);
    }

    if (dto.getOrderableIds().isEmpty()) {
      throw new ValidationMessageException(SupplyPartnerMessageKeys.ERROR_MISSING_ORDERABLES);
    }

    Program program = findResource(programRepository::findOne,
        dto.getProgram(), ProgramMessageKeys.ERROR_NOT_FOUND);

    SupervisoryNode supervisoryNode = findResource(supervisoryNodeRepository::findOne,
        dto.getSupervisoryNode(), SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    Set<Facility> facilities = new HashSet<>(findResources(facilityRepository::findAll,
        dto.getFacilityIds(), FacilityMessageKeys.ERROR_NOT_FOUND));

    Set<Orderable> orderables = new HashSet<>(findResources(
        ids -> orderableRepository.findAllLatestByIds(ids, new PageRequest(0, ids.size())),
        dto.getOrderableIds(), OrderableMessageKeys.ERROR_NOT_FOUND));

    validateFacilities(program, supervisoryNode, facilities);
    validateOrderables(program, facilities, orderables);
    validateGlobalAssociationUniqueness(program, supervisoryNode, facilities, orderables);

    return new SupplyPartnerAssociation(program, supervisoryNode, facilities, orderables);
  }

  /**
   * Facilities in associations should be related to the given regular supervisory node by
   * a requisition group. In other words, it should be impossible to add any facility but
   * only facilities from the requisition group which is connected to the given regular
   * supervisory node.
   */
  private void validateFacilities(Program program, SupervisoryNode supervisoryNode,
      Collection<Facility> facilities) {
    SupervisoryNode regularNode = Optional
        .ofNullable(supervisoryNode.getPartnerNodeOf())
        .orElseThrow(() -> new ValidationMessageException(
            new Message(ERROR_INVALID_SUPERVISORY_NODE, supervisoryNode.getCode())));

    Set<Facility> supervisedFacilities = regularNode.getAllSupervisedFacilities(program);

    for (Facility facility : facilities) {
      if (!supervisedFacilities.contains(facility)) {
        throw new ValidationMessageException(
            new Message(ERROR_INVALID_FACILITY, facility.getName()));
      }
    }

  }

  /**
   * Only orderables for which FTAPs exists could be in associations. A list of available FTAPs
   * should be retrieved based on facility types from facilities that are in the same association.
   */
  private void validateOrderables(Program program, Collection<Facility> facilities,
      Collection<Orderable> orderables) {
    Set<String> facilityTypeCodes = facilities
        .stream()
        .map(Facility::getType)
        .map(FacilityType::getCode)
        .collect(Collectors.toSet());

    String programCode = program.getCode().toString();

    FacilityTypeApprovedProductSearchParams searchParams =
        new FacilityTypeApprovedProductSearchParams(facilityTypeCodes, programCode, null, null,
            null, DEFAULT_PAGE_NUMBER, NO_PAGINATION);

    Set<UUID> approvedProductIds = facilityTypeApprovedProductRepository
        .searchProducts(searchParams, searchParams.getPageable())
        .getContent()
        .stream()
        .map(FacilityTypeApprovedProduct::getOrderableId)
        .collect(Collectors.toSet());

    Set<UUID> orderableIds = orderables
        .stream()
        .map(Orderable::getId)
        .collect(Collectors.toSet());
    orderableIds.removeAll(approvedProductIds);

    if (!orderableIds.isEmpty()) {
      UUID firstId = orderableIds.iterator().next();
      Orderable first = orderables
          .stream()
          .filter(elem -> firstId.equals(elem.getId()))
          .findFirst()
          .orElseThrow(() -> new ValidationMessageException(OrderableMessageKeys.ERROR_NOT_FOUND));

      throw new ValidationMessageException(
          new Message(ERROR_INVALID_ORDERABLE, first.getFullProductName()));
    }
  }

  /**
   * The given supply partner should have only one association for the given
   * program/node/facility/orderable combo, the check should be global. It means that other
   * supply partner cannot have the same combo.
   */
  private void validateGlobalAssociationUniqueness(Program program, SupervisoryNode supervisoryNode,
      Collection<Facility> facilities, Collection<Orderable> orderables) {

    // Builder retrieves all supply partners instead of finding those that match combo
    // (described in the method javadoc) because of number of operations needed to check.
    // To do this in the right way we would need to execute X * Y calls to the database
    // (where X - number of facilities, Y - number of orderables) to verify that there is
    // exactly one supply partner for the given combo. For now, we assume that number of this
    // resource will be small so currently, it is easier to simply retrieve all of them with
    // the single call to the database and validate global unique in the memory.
    List<SupplyPartnerAssociation> associations = supplyPartnerRepository
        .findAll()
        .stream()
        .map(SupplyPartner::getAssociations)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    for (Facility facility : facilities) {
      for (Orderable orderable : orderables) {
        boolean exists = associations
            .stream()
            .anyMatch(association -> association
                .match(program, supervisoryNode, facility, orderable));

        if (exists) {
          throw new ValidationMessageException(
              new Message(ERROR_GLOBAL_UNIQUE,
                  program.getName(), supervisoryNode.getCode(),
                  facility.getName(), orderable.getFullProductName()));
        }
      }
    }
  }
}
