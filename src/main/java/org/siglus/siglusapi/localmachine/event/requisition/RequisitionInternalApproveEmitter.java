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

package org.siglus.siglusapi.localmachine.event.requisition;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.RequisitionGroupMembersDto;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionGroupMembersRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionInternalApproveEmitter {
  private final RequisitionRepository requisitionRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;
  private final RequisitionLineItemExtensionRepository requisitionLineItemExtensionRepository;
  private final AgeGroupLineItemRepository ageGroupLineItemRepository;
  private final ConsultationNumberLineItemRepository consultationNumberLineItemRepository;
  private final UsageInformationLineItemRepository usageInformationLineItemRepository;
  private final PatientLineItemRepository patientLineItemRepository;
  private final TestConsumptionLineItemRepository testConsumptionLineItemRepository;
  private final RegimenLineItemRepository regimenLineItemRepository;
  private final RegimenSummaryLineItemRepository regimenSummaryLineItemRepository;
  private final KitUsageLineItemRepository kitUsageRepository;
  private final EventPublisher eventPublisher;
  private final RequisitionGroupMembersRepository requisitionGroupMembersRepository;

  public RequisitionInternalApproveApplicationEvent emit(UUID requisitionId) {
    RequisitionInternalApproveApplicationEvent event = getEvent(requisitionId);
    eventPublisher.emitGroupEvent(getGroupId(event), getReceiverId(event), event);
    return event;
  }

  public RequisitionInternalApproveApplicationEvent getEvent(UUID requisitionId) {
    RequisitionInternalApproveApplicationEvent event =
        new RequisitionInternalApproveApplicationEvent();
    log.info("get event of requisition internal approve, id = " + requisitionId);
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    event.setRequisition(requisition);

    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    event.setRequisitionExtension(requisitionExtension);

    List<UUID> lineItemIds =
        requisition.getRequisitionLineItems().stream().map(RequisitionLineItem::getId).collect(Collectors.toList());

    List<RequisitionLineItemExtension> lineItemExtensions =
        requisitionLineItemExtensionRepository.findLineItems(lineItemIds);
    event.setLineItemExtensions(lineItemExtensions);

    buildRequisitionUsage(requisitionId, event);

    return event;
  }

  private String getGroupId(RequisitionInternalApproveApplicationEvent event) {
    RequisitionExtension requisitionExtension = event.getRequisitionExtension();
    return requisitionExtension.getRequisitionNumberPrefix() + requisitionExtension.getRequisitionNumber();
  }

  private UUID getReceiverId(RequisitionInternalApproveApplicationEvent event) {
    List<RequisitionGroupMembersDto> parentFacility =
        requisitionGroupMembersRepository.findParentFacilityByRequisitionGroup(
            event.getRequisitionExtension().getFacilityId(),
            Collections.singleton(event.getRequisition().getProgramId()));
    if (CollectionUtils.isEmpty(parentFacility)) {
      throw new IllegalStateException(String.format("can't find event's reciver id, facilityId = %s, programId=%s",
          event.getRequisitionExtension().getFacilityId(), event.getRequisition().getProgramId()));
    }
    return parentFacility.get(0).getFacilityId();
  }

  private void buildRequisitionUsage(UUID id, RequisitionInternalApproveApplicationEvent event) {
    event.setAgeGroupLineItemRequisitionUsage(ageGroupLineItemRepository.findByRequisitionId(id));
    event.setConsultationNumberLineItemRequisitionUsage(consultationNumberLineItemRepository.findByRequisitionId(id));
    event.setPatientLineItemRequisitionUsage(patientLineItemRepository.findByRequisitionId(id));
    event.setTestConsumptionLineItemRequisitionUsage(testConsumptionLineItemRepository.findByRequisitionId(id));
    event.setUsageInformationLineItemRequisitionUsage(usageInformationLineItemRepository.findByRequisitionId(id));
    event.setKitUsageLineItemRequisitionUsage(kitUsageRepository.findByRequisitionId(id));

    event.setRegimenLineItemRequisitionUsage(regimenLineItemRepository.findByRequisitionId(id));
    event.setRegimenSummaryLineItemRequisitionUsage(regimenSummaryLineItemRepository.findByRequisitionId(id));
  }
}
