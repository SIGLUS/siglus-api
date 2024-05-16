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

package org.siglus.siglusapi.localmachine.event.requisition.web.approve;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.siglus.siglusapi.domain.AgeGroupLineItem;
import org.siglus.siglusapi.domain.ConsultationNumberLineItem;
import org.siglus.siglusapi.domain.KitUsageLineItem;
import org.siglus.siglusapi.domain.PatientLineItem;
import org.siglus.siglusapi.domain.RegimenLineItem;
import org.siglus.siglusapi.domain.RegimenSummaryLineItem;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.domain.TestConsumptionLineItem;
import org.siglus.siglusapi.domain.UsageInformationLineItem;
import org.siglus.siglusapi.localmachine.event.NotificationService;
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.siglus.siglusapi.service.android.RequisitionCreateService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionInternalApproveReplayer {

  private final SiglusRequisitionRepository requisitionRepository;
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
  private final RequisitionService requisitionService;
  private final RequisitionCreateService requisitionCreateService;
  private final NotificationService notificationService;

  @EventListener(classes = {RequisitionInternalApprovedEvent.class})
  public void replay(RequisitionInternalApprovedEvent event) {
    try {
      log.info("start replay requisitionId = " + event.getRequisition().getId());
      doReplay(event);
      log.info("end replay requisitionId = " + event.getRequisition().getId());
    } catch (Exception e) {
      log.error("fail to save requisition internal approve event, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  public void doReplay(RequisitionInternalApprovedEvent event) {
    // if this is a rejected requisition, then delete requisition before replaying
    deleteIfExistRequisition(event);
    Requisition newRequisition = RequisitionBuilder.newRequisition(event.getRequisition().getFacilityId(),
        event.getRequisition().getProgramId(), event.getRequisition().getEmergency());
    newRequisition.setTemplate(event.getRequisition().getTemplate());
    newRequisition.setStatus(event.getRequisition().getStatus());
    newRequisition.setProcessingPeriodId(event.getRequisition().getProcessingPeriodId());
    newRequisition.setNumberOfMonthsInPeriod(event.getRequisition().getNumberOfMonthsInPeriod());
    newRequisition.setDraftStatusMessage(event.getRequisition().getDraftStatusMessage());
    newRequisition.setReportOnly(event.getRequisition().getReportOnly());
    newRequisition.setCreatedDate(event.getRequisition().getCreatedDate());
    newRequisition.setModifiedDate(event.getRequisition().getModifiedDate());
    newRequisition.setVersion(event.getRequisition().getVersion());
    newRequisition.setSupervisoryNodeId(event.getRequisition().getSupervisoryNodeId());
    saveStatusChanges(newRequisition, event.getRequisition().getStatusChanges());

    buildRequisitionApprovedProduct(newRequisition, event.getRequisition().getFacilityId(),
        event.getRequisition().getProgramId());
    newRequisition.setExtraData(event.getRequisition().getExtraData());
    buildRequisitionLineItems(newRequisition, event.getRequisition());
    Requisition requisition = requisitionRepository.saveAndFlush(newRequisition);
    log.info(String.format("replay requisition internal approve event, new requisition id: %s, event requisition id: "
        + "%s", requisition.getId(), event.getRequisition().getId()));

    buildRequisitionExtension(event, requisition);

    Map<UUID, UUID> lineItemIdToOrderableId = event.getRequisition().getRequisitionLineItems().stream()
        .collect(toMap(RequisitionLineItem::getId, item -> item.getOrderable().getId()));
    if (CollectionUtils.isNotEmpty(event.getLineItemExtensions())) {
      Map<UUID, RequisitionLineItemExtension> orderableIdToLineItemExtension = event.getLineItemExtensions().stream()
          .collect(toMap(item -> lineItemIdToOrderableId.get(item.getRequisitionLineItemId()), identity()));
      buildRequisitionLineItemsExtension(requisition, orderableIdToLineItemExtension);
    }

    buildRequisitionUsageSections(requisition, event);
    UUID internalApprovalAuthorId = event.getRequisition().getStatusChanges().stream()
        .sorted(Comparator.comparing(StatusChange::getCreatedDate).reversed())
        .filter(item -> item.getStatus() == RequisitionStatus.IN_APPROVAL)
        .findFirst().orElseThrow(IllegalStateException::new)
        .getAuthorId();
    notificationService.postInternalApproval(internalApprovalAuthorId,
        requisitionCreateService.buildBaseRequisitionDto(requisition), requisition.getSupervisoryNodeId());
  }

  private void saveStatusChanges(Requisition requisition, List<StatusChange> statusChanges) {
    requisition.setStatusChanges(new ArrayList<>());
    statusChanges.forEach(statusChange -> buildStatusChanges(requisition, statusChange));
  }

  private void deleteIfExistRequisition(RequisitionInternalApprovedEvent event) {
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionNumber(
        event.getRequisitionExtension().getRealRequisitionNumber());
    if (requisitionExtension != null) {
      UUID requisitionId = requisitionExtension.getRequisitionId();
      requisitionRepository.deleteById(requisitionId);
      requisitionExtensionRepository.deleteByRequisitionId(requisitionId);
      requisitionLineItemExtensionRepository.deleteByRequisitionId(requisitionId);
      ageGroupLineItemRepository.deleteByRequisitionId(requisitionId);
      consultationNumberLineItemRepository.deleteByRequisitionId(requisitionId);
      usageInformationLineItemRepository.deleteByRequisitionId(requisitionId);
      patientLineItemRepository.deleteByRequisitionId(requisitionId);
      testConsumptionLineItemRepository.deleteByRequisitionId(requisitionId);
      regimenLineItemRepository.deleteByRequisitionId(requisitionId);
      regimenSummaryLineItemRepository.deleteByRequisitionId(requisitionId);
      kitUsageRepository.deleteByRequisitionId(requisitionId);
    }
  }

  private void buildRequisitionApprovedProduct(Requisition requisition, UUID homeFacilityId, UUID programId) {
    List<ApprovedProductDto> approvedProductDtos = requisitionService.getAllApprovedProducts(homeFacilityId, programId);
    ApproveProductsAggregator aggregator = new ApproveProductsAggregator(approvedProductDtos, programId);
    Set<ApprovedProductReference> availableProductIdentities = aggregator.getApprovedProductReferences();
    requisition.setAvailableProducts(availableProductIdentities);
  }

  private void buildRequisitionUsageSections(Requisition requisition,
      RequisitionInternalApprovedEvent event) {
    buildAgeGroupLineItem(requisition, event);
    buildConsultationNumberLineItem(requisition, event);
    buildPatientLineItem(requisition, event);
    buildTestConsumptionLineItem(requisition, event);
    buildUsageInformationLineItem(requisition, event);
    buildRegimenLineItem(requisition, event);
    buildRegimenSummaryLineItem(requisition, event);
    buildKitUsage(requisition, event);
  }

  private void buildKitUsage(Requisition requisition, RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getKitUsageLineItemRequisitionUsage())) {
      return;
    }
    List<KitUsageLineItem> list = new ArrayList<>();
    event.getKitUsageLineItemRequisitionUsage().stream().forEach(item -> {
      KitUsageLineItem newItem = new KitUsageLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    kitUsageRepository.save(list);
  }

  private void buildRegimenSummaryLineItem(Requisition requisition, RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getRegimenSummaryLineItemRequisitionUsage())) {
      return;
    }
    List<RegimenSummaryLineItem> list = new ArrayList<>();
    event.getRegimenSummaryLineItemRequisitionUsage().forEach(item -> {
      RegimenSummaryLineItem newItem = new RegimenSummaryLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    regimenSummaryLineItemRepository.save(list);
  }

  private void buildRegimenLineItem(Requisition requisition, RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getRegimenLineItemRequisitionUsage())) {
      return;
    }
    List<RegimenLineItem> list = new ArrayList<>();
    event.getRegimenLineItemRequisitionUsage().forEach(item -> {
      RegimenLineItem newItem = new RegimenLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    regimenLineItemRepository.save(list);
  }

  private void buildUsageInformationLineItem(Requisition requisition,
      RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getUsageInformationLineItemRequisitionUsage())) {
      return;
    }
    List<UsageInformationLineItem> list = new ArrayList<>();
    event.getUsageInformationLineItemRequisitionUsage().forEach(item -> {
      UsageInformationLineItem newItem = new UsageInformationLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    usageInformationLineItemRepository.save(list);
  }

  private void buildTestConsumptionLineItem(Requisition requisition, RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getTestConsumptionLineItemRequisitionUsage())) {
      return;
    }
    List<TestConsumptionLineItem> list = new ArrayList<>();
    event.getTestConsumptionLineItemRequisitionUsage().forEach(item -> {
      TestConsumptionLineItem newItem = new TestConsumptionLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    testConsumptionLineItemRepository.save(list);
  }

  private void buildPatientLineItem(Requisition requisition, RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getPatientLineItemRequisitionUsage())) {
      return;
    }
    List<PatientLineItem> list = new ArrayList<>();
    event.getPatientLineItemRequisitionUsage().forEach(item -> {
      PatientLineItem newItem = new PatientLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    patientLineItemRepository.save(list);
  }

  private void buildConsultationNumberLineItem(Requisition requisition,
      RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getConsultationNumberLineItemRequisitionUsage())) {
      return;
    }
    List<ConsultationNumberLineItem> list = new ArrayList<>();
    event.getConsultationNumberLineItemRequisitionUsage().forEach(item -> {
      ConsultationNumberLineItem newItem = new ConsultationNumberLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    consultationNumberLineItemRepository.save(list);
  }

  private void buildAgeGroupLineItem(Requisition requisition, RequisitionInternalApprovedEvent event) {
    if (CollectionUtils.isEmpty(event.getAgeGroupLineItemRequisitionUsage())) {
      return;
    }
    List<AgeGroupLineItem> list = new ArrayList<>();
    event.getAgeGroupLineItemRequisitionUsage().forEach(item -> {
      AgeGroupLineItem newItem = new AgeGroupLineItem();
      BeanUtils.copyProperties(item, newItem);
      newItem.setRequisitionId(requisition.getId());
      list.add(newItem);
    });
    ageGroupLineItemRepository.save(list);
  }

  private void buildRequisitionExtension(RequisitionInternalApprovedEvent event, Requisition requisition) {
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    requisitionExtension.setRequisitionId(requisition.getId());
    requisitionExtension.setRequisitionNumber(event.getRequisitionExtension().getRequisitionNumber());
    requisitionExtension.setIsApprovedByInternal(event.getRequisitionExtension().getIsApprovedByInternal());
    requisitionExtension.setFacilityId(event.getRequisitionExtension().getFacilityId());
    requisitionExtension.setActualStartDate(event.getRequisitionExtension().getActualStartDate());
    requisitionExtension.setRequisitionNumberPrefix(event.getRequisitionExtension().getRequisitionNumberPrefix());
    requisitionExtension.setCreatedByFacilityId(event.getRequisitionExtension().getCreatedByFacilityId());

    requisitionExtensionRepository.save(requisitionExtension);
  }

  private void buildStatusChanges(Requisition requisition, StatusChange eventStatusChange) {
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(eventStatusChange.getStatus());
    statusChange.setRequisition(requisition);
    statusChange.setAuthorId(eventStatusChange.getAuthorId());
    statusChange.setSupervisoryNodeId(eventStatusChange.getSupervisoryNodeId());
    statusChange.setCreatedDate(eventStatusChange.getCreatedDate());
    statusChange.setModifiedDate(eventStatusChange.getModifiedDate());
    requisition.getStatusChanges().add(statusChange);
    StatusMessage oldStatusMessage = eventStatusChange.getStatusMessage();
    if (oldStatusMessage == null) {
      return;
    }
    StatusMessage newStatusMessage = new StatusMessage();
    newStatusMessage.setRequisition(requisition);
    newStatusMessage.setStatusChange(statusChange);
    newStatusMessage.setId(oldStatusMessage.getAuthorId());
    newStatusMessage.setCreatedDate(oldStatusMessage.getCreatedDate());
    newStatusMessage.setModifiedDate(oldStatusMessage.getModifiedDate());
    newStatusMessage.setAuthorId(oldStatusMessage.getAuthorId());
    newStatusMessage.setAuthorFirstName(oldStatusMessage.getAuthorFirstName());
    newStatusMessage.setAuthorLastName(oldStatusMessage.getAuthorLastName());
    newStatusMessage.setStatus(oldStatusMessage.getStatus());
    newStatusMessage.setBody(oldStatusMessage.getBody());
    statusChange.setStatusMessage(newStatusMessage);
  }

  private void buildRequisitionLineItemsExtension(Requisition requisition,
      Map<UUID, RequisitionLineItemExtension> requisitionLineItemExtensionMap) {
    if (CollectionUtils.isEmpty(requisition.getRequisitionLineItems())) {
      return;
    }
    log.info("requisition line size: {}", requisition.getRequisitionLineItems().size());
    List<RequisitionLineItemExtension> extensions = new ArrayList<>();

    requisition
        .getRequisitionLineItems()
        .forEach(
            requisitionLineItem -> {
              RequisitionLineItemExtension requisitionLineItemExtension =
                  requisitionLineItemExtensionMap.getOrDefault(
                      requisitionLineItem.getOrderable().getId(),
                      new RequisitionLineItemExtension());

              RequisitionLineItemExtension newExtension =
                  new RequisitionLineItemExtension(
                      requisitionLineItem.getId(),
                      requisitionLineItemExtension.getAuthorizedQuantity(),
                      requisitionLineItemExtension.getSuggestedQuantity(),
                      requisitionLineItemExtension.getEstimatedQuantity(),
                      requisitionLineItemExtension.getExpirationDate());
              extensions.add(newExtension);
            });
    requisitionLineItemExtensionRepository.save(extensions);
    requisitionLineItemExtensionRepository.flush();
  }

  private void buildRequisitionLineItems(Requisition newRequisition, Requisition eventRequisition) {
    if (CollectionUtils.isEmpty(eventRequisition.getRequisitionLineItems())) {
      return;
    }
    Map<UUID, VersionEntityReference> productIdToApproveds = newRequisition.getAvailableProducts().stream().collect(
        toMap(product -> product.getOrderable().getId(), ApprovedProductReference::getFacilityTypeApprovedProduct)
    );
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    for (RequisitionLineItem eventLineItem : eventRequisition.getRequisitionLineItems()) {
      RequisitionLineItem lineItem = new RequisitionLineItem();
      lineItem.setRequisition(newRequisition);
      lineItem.setOrderable(new VersionEntityReference(eventLineItem.getOrderable().getId(),
          eventLineItem.getOrderable().getVersionNumber()));
      lineItem.setBeginningBalance(eventLineItem.getBeginningBalance());
      lineItem.setTotalLossesAndAdjustments(eventLineItem.getTotalLossesAndAdjustments());
      lineItem.setTotalReceivedQuantity(eventLineItem.getTotalReceivedQuantity());
      lineItem.setTotalConsumedQuantity(eventLineItem.getTotalConsumedQuantity());
      lineItem.setStockOnHand(eventLineItem.getStockOnHand());
      lineItem.setRequestedQuantity(eventLineItem.getRequestedQuantity());
      VersionEntityReference approvedProduct = productIdToApproveds.get(lineItem.getOrderable().getId());
      lineItem.setFacilityTypeApprovedProduct(approvedProduct);
      lineItem.setSkipped(eventLineItem.getSkipped());
      requisitionLineItems.add(lineItem);
    }
    newRequisition.setRequisitionLineItems(requisitionLineItems);
  }
}
