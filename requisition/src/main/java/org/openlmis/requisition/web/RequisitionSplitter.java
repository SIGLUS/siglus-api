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

package org.openlmis.requisition.web;

import static org.openlmis.requisition.i18n.MessageKeys.LINE_ITEM_SUPPLIED_BY_OTHER_PARTNER;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.SupplyPartnerAssociationDto;
import org.openlmis.requisition.dto.SupplyPartnerDto;
import org.openlmis.requisition.dto.TogglzFeatureDto;
import org.openlmis.requisition.i18n.MessageService;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyPartnerReferenceDataService;
import org.openlmis.requisition.service.referencedata.TogglzReferenceDataService;
import org.openlmis.requisition.utils.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class RequisitionSplitter {
  static final String MULTIPLE_SUPPLIERS = "MULTIPLE_SUPPLIERS";

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Autowired
  private SupplyPartnerReferenceDataService supplyPartnerReferenceDataService;

  @Autowired
  private TogglzReferenceDataService togglzReferenceDataService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  private MessageService messageService;

  /**
   * Splits the given requisition into parts based on associations from supply partners. After the
   * split there will be:
   * <ul>
   * <li>X requisitions, where X is the number of partners handling the requisition</li>
   * <li>the original requisition but data for orderables that are supplied by partners will be
   * changed to zero and in the remarks column, there will be an explanation text.
   * </li>
   * </ul>
   * A requisition is splittable when all of the following rules apply:
   * <ul>
   * <li>The requisition was not split before</li>
   * <li>The requisition is not a part of another requisition</li>
   * <li>Parent node has a partner node</li>
   * <li>There is a supply partner with an entry for that partner node and requisition's
   * program</li>
   * <li>That entry has a facility that matches the requisition's facility</li>
   * <li>That entry has a product that matches any products in the requisition line items</li>
   * </ul>
   *
   * @return an instance of {@link RequisitionSplitResult}.
   */
  RequisitionSplitResult split(Requisition requisition, UUID supervisoryNodeId) {
    if (null == supervisoryNodeId || !isFeatureActive() || wasSplit(requisition)) {
      return new RequisitionSplitResult(requisition);
    }

    Map<VersionEntityReference, RequisitionLineItem> requisitionLineItems = requisition
        .getNonSkippedRequisitionLineItems()
        .stream()
        .collect(Collectors.toMap(RequisitionLineItem::getOrderable, Function.identity()));

    List<SupplyPartnerAssociationDto> associations = getAssociations(
        requisition, supervisoryNodeId, requisitionLineItems);

    if (associations.isEmpty()) {
      return new RequisitionSplitResult(requisition);
    }

    List<Requisition> partnerRequisitions = Lists.newArrayListWithCapacity(associations.size());
    createPartnerRequisitions(requisition, partnerRequisitions, associations, requisitionLineItems);

    adjustOriginalRequisition(partnerRequisitions, requisitionLineItems);

    return new RequisitionSplitResult(requisition, partnerRequisitions);
  }

  private List<SupplyPartnerAssociationDto> getAssociations(Requisition requisition,
      UUID supervisoryNodeId,
      Map<VersionEntityReference, RequisitionLineItem> requisitionLineItems) {
    Set<UUID> programIds = Sets.newHashSet(requisition.getProgramId());
    Set<UUID> partnerNodeIds = supervisoryNodeReferenceDataService
        .findOne(supervisoryNodeId)
        .getPartnerNodeIds();
    Set<UUID> facilityIds = Sets.newHashSet(requisition.getFacilityId());
    Set<UUID> orderableIds = requisitionLineItems
        .values()
        .stream()
        .map(line -> line.getOrderable().getId())
        .collect(Collectors.toSet());

    return supplyPartnerReferenceDataService
        .search(partnerNodeIds)
        .stream()
        .map(partner -> findAssociation(partner, programIds, partnerNodeIds,
            facilityIds, orderableIds))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private boolean isFeatureActive() {
    return togglzReferenceDataService
        .findAll()
        .stream()
        .filter(feature -> MULTIPLE_SUPPLIERS.equals(feature.getName()))
        .findFirst()
        .map(TogglzFeatureDto::isEnabled)
        .orElse(false);
  }

  private boolean wasSplit(Requisition requisition) {
    return requisition.hasOriginalRequisitionId()
        || requisitionRepository.existsByOriginalRequisitionId(requisition.getId());
  }

  private void createPartnerRequisitions(Requisition requisition, List<Requisition> list,
      List<SupplyPartnerAssociationDto> associations,
      Map<VersionEntityReference, RequisitionLineItem> requisitionLineItems) {
    for (SupplyPartnerAssociationDto association : associations) {
      List<RequisitionLineItem> partnerLineItems = association
          .getOrderableIds()
          .stream()
          .map(id -> requisitionLineItems
              .keySet()
              .stream()
              .filter(elem -> id.equals(elem.getId()))
              .findFirst()
              .map(requisitionLineItems::get)
              .orElse(null))
          .filter(Objects::nonNull)
          .map(this::createPartnerLineItem)
          .collect(Collectors.toList());

      list.add(createPartnerRequisition(requisition, association, partnerLineItems));
    }
  }

  private void adjustOriginalRequisition(List<Requisition> partnerRequisitions,
      Map<VersionEntityReference, RequisitionLineItem> requisitionLineItems) {
    String remarks = messageService
        .localize(new Message(LINE_ITEM_SUPPLIED_BY_OTHER_PARTNER))
        .asMessage();

    for (Requisition partnerRequisition : partnerRequisitions) {
      for (RequisitionLineItem lineItem : partnerRequisition.getRequisitionLineItems()) {
        RequisitionLineItem originalLineItem = requisitionLineItems
            .get(lineItem.getOrderable());
        originalLineItem.setSkipped(true);
        originalLineItem.setRequestedQuantity(null);
        originalLineItem.setRequestedQuantityExplanation(null);
        originalLineItem.setApprovedQuantity(null);
        originalLineItem.setRemarks(remarks);
      }
    }
  }

  private Requisition createPartnerRequisition(Requisition requisition,
      SupplyPartnerAssociationDto association, List<RequisitionLineItem> partnerLineItems) {
    Requisition partnerRequisition = new Requisition(requisition);

    partnerRequisition.setId(null);
    partnerRequisition.setRequisitionLineItems(partnerLineItems);
    partnerRequisition.setVersion(1L);
    partnerRequisition.setDraftStatusMessage("");
    partnerRequisition.setStatusChanges(Lists.newArrayList());
    partnerRequisition.setSupervisoryNodeId(association.getSupervisoryNodeId());
    partnerRequisition.setPreviousRequisitions(Lists.newArrayList());
    partnerRequisition.setAvailableProducts(Sets.newHashSet());

    partnerRequisition.setExtraData(requisition.getExtraData());
    partnerRequisition.setOriginalRequisitionId(requisition.getId());

    partnerLineItems.forEach(lineItem -> lineItem.setRequisition(partnerRequisition));

    return partnerRequisition;
  }

  private RequisitionLineItem createPartnerLineItem(RequisitionLineItem lineItem) {
    RequisitionLineItem partnerLineItem = new RequisitionLineItem(lineItem);
    partnerLineItem.setId(null);
    partnerLineItem.setRequisition(null);

    return partnerLineItem;
  }

  private SupplyPartnerAssociationDto findAssociation(SupplyPartnerDto partner,
      Set<UUID> programIds, Set<UUID> partnerNodeIds, Set<UUID> facilityIds,
      Set<UUID> orderableIds) {
    return partner
        .getAssociations()
        .stream()
        .filter(association -> containsValue(programIds, association.getProgramId()))
        .filter(association -> containsValue(partnerNodeIds, association.getSupervisoryNodeId()))
        .filter(association -> containsAtLeastOne(facilityIds, association.getFacilityIds()))
        .filter(association -> containsAtLeastOne(orderableIds, association.getOrderableIds()))
        .findFirst()
        .orElse(null);
  }

  private boolean containsValue(Set<UUID> set, UUID value) {
    return isEmpty(set) || set.contains(value);
  }

  private boolean containsAtLeastOne(Set<UUID> set, Set<UUID> values) {
    // disjoint returns true if collections have no elements in common but we want
    // set that have at least one element from the values set
    // that is why we negate the result of the method.
    return isEmpty(set) || !Collections.disjoint(set, values);
  }

}
