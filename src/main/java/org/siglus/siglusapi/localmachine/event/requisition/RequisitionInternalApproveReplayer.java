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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.repository.RequisitionRepository;
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
import org.siglus.siglusapi.repository.AgeGroupLineItemRepository;
import org.siglus.siglusapi.repository.ConsultationNumberLineItemRepository;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.PatientLineItemRepository;
import org.siglus.siglusapi.repository.RegimenLineItemRepository;
import org.siglus.siglusapi.repository.RegimenSummaryLineItemRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.repository.TestConsumptionLineItemRepository;
import org.siglus.siglusapi.repository.UsageInformationLineItemRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionInternalApproveReplayer {
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
  private final RequisitionService requisitionService;

  @EventListener(classes = {RequisitionInternalApproveApplicationEvent.class})
  public void replay(RequisitionInternalApproveApplicationEvent emited) {
    try {
      log.info("start test replay requisitionId = " + emited.getRequisition().getId());
      doReplay(emited);
      log.info("end test replay requisitionId = " + emited.getRequisition().getId());
    } catch (Exception e) {
      log.error("fail to save requisition internal approve event, msg = " + e.getMessage(), e);
      throw (IllegalStateException) (new IllegalStateException().initCause(e));
    }
  }

  public void doReplay(RequisitionInternalApproveApplicationEvent emited) {
    Requisition newRequisition = RequisitionBuilder.newRequisition(emited.getRequisition().getFacilityId(),
        emited.getRequisition().getProgramId(), emited.getRequisition().getEmergency());

    newRequisition.setTemplate(emited.getRequisition().getTemplate());
    newRequisition.setStatus(emited.getRequisition().getStatus());

    newRequisition.setProcessingPeriodId(emited.getRequisition().getProcessingPeriodId());
    newRequisition.setNumberOfMonthsInPeriod(emited.getRequisition().getNumberOfMonthsInPeriod());
    newRequisition.setDraftStatusMessage(emited.getRequisition().getDraftStatusMessage());
    newRequisition.setReportOnly(emited.getRequisition().getReportOnly());
    newRequisition.setCreatedDate(emited.getRequisition().getCreatedDate());
    newRequisition.setModifiedDate(emited.getRequisition().getModifiedDate());
    newRequisition.setVersion(emited.getRequisition().getVersion());
    newRequisition.setSupervisoryNodeId(emited.getRequisition().getSupervisoryNodeId());
    newRequisition.setStatusChanges(new ArrayList<>());
    buildStatusChanges(newRequisition, emited.getRequisition().getStatusChanges().stream()
        .filter(item -> item.getStatus() == RequisitionStatus.INITIATED).findFirst());
    buildStatusChanges(newRequisition, emited.getRequisition().getStatusChanges().stream()
        .filter(item -> item.getStatus() == RequisitionStatus.SUBMITTED).findFirst());
    buildStatusChanges(newRequisition, emited.getRequisition().getStatusChanges().stream()
        .filter(item -> item.getStatus() == RequisitionStatus.AUTHORIZED).findFirst());
    buildStatusChanges(newRequisition, emited.getRequisition().getStatusChanges().stream()
        .filter(item -> item.getStatus() == RequisitionStatus.IN_APPROVAL).findFirst());

    buildRequisitionApprovedProduct(newRequisition, emited.getRequisition().getFacilityId(),
        emited.getRequisition().getProgramId());
    newRequisition.setExtraData(emited.getRequisition().getExtraData());

    buildRequisitionLineItems(newRequisition, emited.getRequisition());

    Requisition requisition = requisitionRepository.saveAndFlush(newRequisition);
    log.info("new requisition =" + requisition.getId());

    buildRequisitionExtension(emited, requisition);

    Map<UUID, UUID> lineItemIdToOrderableId = emited.getRequisition().getRequisitionLineItems().stream()
        .collect(toMap(RequisitionLineItem::getId, item -> item.getOrderable().getId()));

    Map<UUID, RequisitionLineItemExtension> orderableIdToLineItemExtension = emited.getLineItemExtensions().stream()
        .collect(toMap(item -> lineItemIdToOrderableId.get(item.getRequisitionLineItemId()), identity()));
    buildRequisitionLineItemsExtension(requisition, orderableIdToLineItemExtension);

    buildRequisitionUsageSections(requisition, emited);
  }

  private void buildRequisitionApprovedProduct(Requisition requisition, UUID homeFacilityId, UUID programId) {
    ApproveProductsAggregator approvedProductsContainKit = requisitionService
        .getApproveProduct(homeFacilityId, programId, requisition.getReportOnly());
    List<ApprovedProductDto> approvedProductDtos = approvedProductsContainKit.getFullSupplyProducts();
    ApproveProductsAggregator approvedProducts = new ApproveProductsAggregator(approvedProductDtos, programId);
    Set<ApprovedProductReference> availableProductIdentities = approvedProducts.getApprovedProductReferences();
    requisition.setAvailableProducts(availableProductIdentities);
  }

  private void buildRequisitionUsageSections(Requisition requisition,
      RequisitionInternalApproveApplicationEvent event) {
    buildAgeGroupLineItem(requisition, event);
    buildConsultationNumberLineItem(requisition, event);
    buildPatientLineItem(requisition, event);
    buildTestConsumptionLineItem(requisition, event);
    buildUsageInformationLineItem(requisition, event);
    buildRegimenLineItem(requisition, event);
    buildRegimenSummaryLineItem(requisition, event);
    buildKitUsage(requisition, event);
  }

  private void buildKitUsage(Requisition requisition, RequisitionInternalApproveApplicationEvent event) {
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

  private void buildRegimenSummaryLineItem(Requisition requisition, RequisitionInternalApproveApplicationEvent event) {
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

  private void buildRegimenLineItem(Requisition requisition, RequisitionInternalApproveApplicationEvent event) {
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
      RequisitionInternalApproveApplicationEvent event) {
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

  private void buildTestConsumptionLineItem(Requisition requisition, RequisitionInternalApproveApplicationEvent event) {
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

  private void buildPatientLineItem(Requisition requisition, RequisitionInternalApproveApplicationEvent event) {
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
      RequisitionInternalApproveApplicationEvent event) {
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

  private void buildAgeGroupLineItem(Requisition requisition, RequisitionInternalApproveApplicationEvent event) {
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

  private void buildRequisitionExtension(RequisitionInternalApproveApplicationEvent emited, Requisition requisition) {
    RequisitionExtension requisitionExtension = new RequisitionExtension();
    requisitionExtension.setRequisitionId(requisition.getId());
    requisitionExtension.setRequisitionNumber(emited.getRequisitionExtension().getRequisitionNumber());
    requisitionExtension.setIsApprovedByInternal(emited.getRequisitionExtension().getIsApprovedByInternal());
    requisitionExtension.setFacilityId(emited.getRequisitionExtension().getFacilityId());
    requisitionExtension.setActualStartDate(emited.getRequisitionExtension().getActualStartDate());
    requisitionExtension.setRequisitionNumberPrefix(emited.getRequisitionExtension().getRequisitionNumberPrefix());

    requisitionExtensionRepository.save(requisitionExtension);
  }

  private void buildStatusChanges(Requisition requisition, Optional<StatusChange> statusChangeOld) {
    if (!statusChangeOld.isPresent()) {
      return;
    }
    StatusChange statusChange = new StatusChange();
    statusChange.setStatus(statusChangeOld.get().getStatus());
    statusChange.setRequisition(requisition);
    statusChange.setAuthorId(statusChangeOld.get().getAuthorId());
    statusChange.setSupervisoryNodeId(statusChangeOld.get().getSupervisoryNodeId());
    statusChange.setStatusMessage(statusChangeOld.get().getStatusMessage());
    statusChange.setCreatedDate(statusChangeOld.get().getCreatedDate());
    statusChange.setModifiedDate(statusChangeOld.get().getModifiedDate());
    requisition.getStatusChanges().add(statusChange);
  }

  private void buildRequisitionLineItemsExtension(Requisition requisition,
      Map<UUID, RequisitionLineItemExtension> requisitionLineItemExtensionMap) {
    if (isEmpty(requisition.getRequisitionLineItems())) {
      return;
    }
    log.info("requisition line size: {}", requisition.getRequisitionLineItems().size());

    requisition.getRequisitionLineItems().forEach(requisitionLineItem -> {
      RequisitionLineItemExtension requisitionLineItemExtensionOld =
          requisitionLineItemExtensionMap.getOrDefault(requisitionLineItem.getOrderable().getId(),
              new RequisitionLineItemExtension());

      RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
      extension.setRequisitionLineItemId(requisitionLineItem.getId());
      extension.setAuthorizedQuantity(requisitionLineItemExtensionOld.getAuthorizedQuantity());
      extension.setExpirationDate(requisitionLineItemExtensionOld.getExpirationDate());
      requisitionLineItemExtensionRepository.save(extension);
    });
    requisitionLineItemExtensionRepository.flush();
  }

  private void buildRequisitionLineItems(Requisition requisition, Requisition requisitionOld) {
    if (isEmpty(requisitionOld.getRequisitionLineItems())) {
      return;
    }
    Map<UUID, VersionEntityReference> productIdToApproveds = requisition.getAvailableProducts().stream().collect(
        toMap(product -> product.getOrderable().getId(), ApprovedProductReference::getFacilityTypeApprovedProduct)
    );
    List<RequisitionLineItem> requisitionLineItems = new ArrayList<>();
    for (RequisitionLineItem oldLineItem : requisitionOld.getRequisitionLineItems()) {
      RequisitionLineItem lineItem = new RequisitionLineItem();
      lineItem.setRequisition(requisition);
      lineItem.setOrderable(new VersionEntityReference(oldLineItem.getOrderable().getId(),
          oldLineItem.getOrderable().getVersionNumber()));
      lineItem.setBeginningBalance(oldLineItem.getBeginningBalance());
      lineItem.setTotalLossesAndAdjustments(oldLineItem.getTotalLossesAndAdjustments());
      lineItem.setTotalReceivedQuantity(oldLineItem.getTotalReceivedQuantity());
      lineItem.setTotalConsumedQuantity(oldLineItem.getTotalConsumedQuantity());
      lineItem.setStockOnHand(oldLineItem.getStockOnHand());
      lineItem.setRequestedQuantity(oldLineItem.getRequestedQuantity());
      VersionEntityReference approvedProduct = productIdToApproveds.get(lineItem.getOrderable().getId());
      lineItem.setFacilityTypeApprovedProduct(approvedProduct);
      lineItem.setSkipped(oldLineItem.getSkipped());
      requisitionLineItems.add(lineItem);
    }
    requisition.setRequisitionLineItems(requisitionLineItems);
  }
}
