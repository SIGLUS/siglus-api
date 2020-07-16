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

package org.siglus.siglusapi.service;

import static java.util.stream.Collectors.toSet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.service.FulfillmentPermissionService;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ProofOfDeliveryFulfillmentService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.stockmanagement.service.StockmanagementPermissionService;
import org.siglus.common.domain.BaseEntity;
import org.siglus.common.domain.referencedata.Right;
import org.siglus.common.domain.referencedata.RightAssignment;
import org.siglus.common.domain.referencedata.SupervisionRoleAssignment;
import org.siglus.common.domain.referencedata.User;
import org.siglus.common.repository.FacilityRepository;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.repository.SiglusRightAssignmentRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusNotificationService {

  public enum ViewableStatus {
    NOT_VIEWED, VIEWED, PROCESSED
  }

  private static final String REF_FACILITY_ID = "refFacilityId";

  private static final String REF_PROGRAM_ID = "refProgramId";

  private static final String REF_STATUS = "refStatus";

  private static final String NOTIFY_FACILITY_ID = "notifyFacilityId";

  private final NotificationRepository repo;

  private final NotificationMapper mapper;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final OrderFulfillmentService orderService;

  private final SiglusRequisitionRequisitionService requisitionService;

  private final SiglusRightAssignmentRepository rightAssignRepo;

  private final SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  private final ProofOfDeliveryFulfillmentService podService;

  private final FacilityRepository facilityRepo;

  private final EntityManager em;

  public Page<NotificationDto> searchNotifications(Pageable pageable) {
    User currentUser = authenticationHelper.getCurrentUserDomain();
    Set<RightAssignment> rightAssignments = currentUser.getRightAssignments();
    return repo.findViewable(pageable, currentUser.getHomeFacilityId(),
        getFilterByRights(rightAssignments)).map(mapper::from);
  }

  public ViewableStatus viewNotification(UUID notificationId) {
    Notification notification = repo.findOne(notificationId);
    if (notification.getViewed()) {
      return ViewableStatus.VIEWED;
    }
    notification.setViewed(true);
    notification.setViewedDate(LocalDateTime.now());
    notification.setViewedUserId(authenticationHelper.getCurrentUserDomain().getId());
    repo.save(notification);
    if (notification.getProcessed()) {
      return ViewableStatus.PROCESSED;
    }
    return ViewableStatus.NOT_VIEWED;
  }

  public void postSubmit(BasicRequisitionDto requisition) {
    if (canBeAuthorizedBySubmitter(requisition)) {
      return;
    }
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.REJECTED);
    saveNotificationFromRequisition(requisition, notification -> {
      notification.setRefStatus(NotificationStatus.SUBMITTED);
      notification.setNotifyFacilityId(requisition.getFacility().getId());
    });
  }

  public void postAuthorize(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.SUBMITTED,
        NotificationStatus.REJECTED);

    saveNotificationFromRequisition(requisition, notification -> {
      notification.setRefStatus(NotificationStatus.AUTHORIZED);
      notification.setSupervisoryNodeId(findSupervisorNodeId(requisition));
    });
  }

  public void postApprove(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
        NotificationStatus.IN_APPROVAL);

    saveNotificationFromRequisition(requisition, notification -> {
      if (requisition.getStatus() == RequisitionStatus.IN_APPROVAL) {
        notification.setRefStatus(NotificationStatus.IN_APPROVAL);
        notification.setSupervisoryNodeId(findSupervisorNodeId(requisition));
      } else {
        notification.setRefStatus(NotificationStatus.APPROVED);
        notification.setRefFacilityId(findCurrentUserFacilityId());
        notification.setNotifyFacilityId(findCurrentUserFacilityId());
      }
    });
  }

  public void postReject(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
        NotificationStatus.IN_APPROVAL);

    saveNotificationFromRequisition(requisition, notification -> {
      notification.setRefStatus(NotificationStatus.REJECTED);
      notification.setNotifyFacilityId(requisition.getFacility().getId());
    });
  }

  public void postDelete(UUID requisitionId) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaUpdate<Notification> update = cb.createCriteriaUpdate(Notification.class);
    Root<Notification> root = update.from(Notification.class);
    update.set(root.get("processed"), true);
    update.where(cb.and(
        cb.equal(root.get("refId"), requisitionId),
        root.get(REF_STATUS).in(NotificationStatus.requisitionStatuses())
    ));
    em.createQuery(update).executeUpdate();
  }

  public void postConvertToOrder(ApproveRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.APPROVED);

    searchOrders(requisition).forEach(
        order -> {
          Notification notification = newNotification();
          notification.setRefId(order.getId());
          notification.setRefFacilityId(findCurrentUserFacilityId());
          notification.setRefProgramId(order.getProgram().getId());
          notification.setEmergency(requisition.getEmergency());
          notification.setRefStatus(NotificationStatus.ORDERED);
          notification.setNotifyFacilityId(findCurrentUserFacilityId());
          repo.save(notification);
        }
    );
  }

  public void postConfirmShipment(ShipmentDto shipment) {
    OrderDto order = orderService.findOne(shipment.getOrder().getId());
    repo.updateLastNotificationProcessed(order.getId(), NotificationStatus.ORDERED);
    RequisitionV2Dto requisition = requisitionService.searchRequisition(order.getExternalId());
    List<ProofOfDeliveryDto> pods = podService.getProofOfDeliveries(order.getId());
    pods.forEach(pod -> {
      Notification notification = newNotification();
      notification.setRefId(pod.getId());
      notification.setRefFacilityId(requisition.getFacility().getId());
      notification.setRefProgramId(requisition.getProgram().getId());
      notification.setEmergency(requisition.getEmergency());
      notification.setRefStatus(NotificationStatus.SHIPPED);
      repo.save(notification);
    });
  }

  public void postConfirmPod(org.openlmis.fulfillment.web.util.ProofOfDeliveryDto pod) {
    repo.updateLastNotificationProcessed(pod.getId(), NotificationStatus.SHIPPED);
  }

  private Notification newNotification() {
    Notification notification = new Notification();
    String currentFacilityName = facilityRepo.findOne(findCurrentUserFacilityId()).getName();
    notification.setSourceFacilityName(currentFacilityName);
    return notification;
  }

  private void saveNotificationFromRequisition(BasicRequisitionDto requisition,
      Consumer<Notification> setStatusAndNotifyFacility) {
    Notification notification = from(requisition);
    setStatusAndNotifyFacility.accept(notification);
    repo.save(notification);
  }

  private Notification from(BasicRequisitionDto requisition) {
    Notification notification = newNotification();
    notification.setRefId(requisition.getId());
    notification.setRefFacilityId(requisition.getFacility().getId());
    notification.setRefProgramId(requisition.getProgram().getId());
    notification.setEmergency(requisition.getEmergency());
    return notification;
  }

  private UUID findCurrentUserFacilityId() {
    return authenticationHelper.getCurrentUserDomain().getHomeFacilityId();
  }

  private Set<UUID> findCurrentUserSupervisoryNodeIds() {
    return authenticationHelper.getCurrentUserDomain().getRoleAssignments()
        .stream()
        .filter(roleAssignment -> roleAssignment instanceof SupervisionRoleAssignment)
        .map(roleAssignment -> (SupervisionRoleAssignment) roleAssignment)
        .filter(this::canThisRoleApproveRequisition)
        .map(SupervisionRoleAssignment::getSupervisoryNode)
        .map(BaseEntity::getId)
        .collect(toSet());
  }

  private boolean canThisRoleApproveRequisition(SupervisionRoleAssignment roleAssignment) {
    return roleAssignment.getRole().getRights().stream()
        .map(Right::getName)
        .anyMatch(PermissionService.REQUISITION_APPROVE::equals);
  }

  private boolean canBeAuthorizedBySubmitter(BasicRequisitionDto requisition) {
    return rightAssignRepo.count((root, query, cb) -> cb.and(
        cb.equal(root.get("facilityId"), requisition.getFacility().getId()),
        cb.equal(root.get("programId"), requisition.getProgram().getId()),
        cb.equal(root.get("user").get("id"), authenticationHelper.getCurrentUserDomain().getId()),
        cb.equal(root.get("rightName"), PermissionService.REQUISITION_AUTHORIZE)
    )) > 0;
  }

  private UUID findSupervisorNodeId(BasicRequisitionDto requisition) {
    UUID supervisoryNodeId = requisitionService.searchRequisition(requisition.getId())
        .getSupervisoryNode();
    if (supervisoryNodeId == null) {
      RequisitionV2Dto requisitionV2Dto = requisitionService.searchRequisition(requisition.getId());
      supervisoryNodeId = supervisoryNodeReferenceDataService.findSupervisoryNode(
          requisitionV2Dto.getProgramId(), requisitionV2Dto.getFacilityId()).getId();
    }
    return supervisoryNodeId;
  }

  private List<OrderDto> searchOrders(ApproveRequisitionDto approveRequisitionDto) {
    RequisitionV2Dto requisition = requisitionService
        .searchRequisition(approveRequisitionDto.getId());
    return orderService
        .search(requisition.getSupplyingFacility(), requisition.getFacilityId(),
            requisition.getProgramId(), requisition.getProcessingPeriodId(), null/*ignore status*/)
        .stream()
        .filter(order -> requisition.getId().equals(order.getExternalId()))
        .collect(Collectors.toList());
  }

  private Specification<Notification> getFilterByRights(Set<RightAssignment> rightAssignments) {
    log.info("current user has {} rights", rightAssignments.size());
    UUID currentUserFacilityId = findCurrentUserFacilityId();
    Set<UUID> currentUserSupervisoryNodeIds = findCurrentUserSupervisoryNodeIds();
    log.info("current user has supervisoryNode {}", currentUserSupervisoryNodeIds);
    boolean canEditShipments = rightAssignments.stream().anyMatch(
        rightAssignment -> FulfillmentPermissionService.SHIPMENTS_EDIT
            .equals(rightAssignment.getRightName()));
    return (root, query, cb) -> rightAssignments.stream()
        .map(right -> mapRightToPredicate(right, root, cb, currentUserFacilityId,
            currentUserSupervisoryNodeIds, canEditShipments))
        .filter(Objects::nonNull)
        .reduce(cb::or)
        .orElse(cb.disjunction());
  }

  private Predicate mapRightToPredicate(RightAssignment right, Root<Notification> root,
      CriteriaBuilder cb, UUID currentUserFacilityId, Set<UUID> currentUserSupervisoryNodeIds,
      boolean canEditShipments) {
    switch (right.getRightName()) {
      case PermissionService.REQUISITION_CREATE:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), right.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), right.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.REJECTED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case PermissionService.REQUISITION_AUTHORIZE:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), right.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), right.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.SUBMITTED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case PermissionService.REQUISITION_APPROVE:
        if (currentUserSupervisoryNodeIds.isEmpty()) {
          return null;
        }
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), right.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), right.getProgramId()),
            root.get(REF_STATUS)
                .in(NotificationStatus.AUTHORIZED, NotificationStatus.IN_APPROVAL),
            root.get("supervisoryNodeId").in(currentUserSupervisoryNodeIds)
        );
      case PermissionService.ORDERS_EDIT:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), right.getFacilityId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.APPROVED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case StockmanagementPermissionService.STOCK_CARDS_VIEW:
        if (!canEditShipments) {
          return null;
        }
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), right.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), right.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.ORDERED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case FulfillmentPermissionService.PODS_MANAGE:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), right.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), right.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.SHIPPED)
        );
      default:
        return null;
    }
  }

}
