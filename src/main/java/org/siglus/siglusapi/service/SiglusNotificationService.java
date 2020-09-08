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
import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.service.FulfillmentPermissionService;
import org.openlmis.fulfillment.service.FulfillmentProofOfDeliveryService;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.fulfillment.ProofOfDeliveryFulfillmentService;
import org.openlmis.requisition.service.referencedata.RequisitionGroupReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.service.StockmanagementPermissionService;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.dto.referencedata.RoleAssignmentDto;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.PermissionString;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public class SiglusNotificationService {

  public enum ViewableStatus {
    NOT_VIEWED, VIEWED, PROCESSED
  }

  private static final String REF_FACILITY_ID = "refFacilityId";

  private static final String REF_PROGRAM_ID = "refProgramId";

  private static final String REF_STATUS = "refStatus";

  private static final String NOTIFY_FACILITY_ID = "notifyFacilityId";

  // can't find any official document to describe this size, but if you try to reduce with cb.and or
  // cb. or more than 200 predicates in a single thread, it will throw StackOverflow.
  private static final int MAX_PREDICATES_PER_THREAD = 103;

  private final NotificationRepository repo;

  private final NotificationMapper mapper;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusOrderService siglusOrderService;

  private final SiglusRequisitionRequisitionService requisitionService;

  private final ProofOfDeliveryFulfillmentService podService;

  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  private final OrderExternalRepository orderExternalRepository;

  private final RoleReferenceDataService roleService;

  private final RequisitionGroupReferenceDataService requisitionGroupService;

  private final EntityManager em;

  private final ExecutorService executor;

  private final FulfillmentProofOfDeliveryService fulfillmentProofOfDeliveryService;

  private final RequisitionController requisitionController;

  public Page<NotificationDto> searchNotifications(Pageable pageable) {
    return repo
        .findViewable(pageable, findCurrentUserFacilityId(), getFilterByRights())
        .map(mapper::from);
  }

  public ViewableStatus viewNotification(UUID notificationId) {
    Notification notification = repo.findOne(notificationId);
    if (notification.getViewed()) {
      return ViewableStatus.VIEWED;
    }
    notification.setViewed(true);
    notification.setViewedDate(LocalDateTime.now());
    notification.setViewedUserId(authenticationHelper.getCurrentUserId().orElse(null));
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

    RequisitionStatus status = requisition.getStatus();
    if (status != RequisitionStatus.IN_APPROVAL && status != RequisitionStatus.APPROVED) {
      return;
    }
    saveNotificationFromRequisition(requisition, notification -> {
      if (status == RequisitionStatus.IN_APPROVAL) {
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
    SiglusOrderDto siglusOrderDto = siglusOrderService.searchOrderById(shipment.getOrder().getId());
    OrderDto order = siglusOrderDto.getOrder();
    repo.updateLastNotificationProcessed(order.getId(), NotificationStatus.ORDERED);
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
    RequisitionV2Dto requisition = requisitionService.searchRequisition(requisitionId);
    List<ProofOfDelivery> pods = getProofOfDeliveryByOrderId(order.getId());
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

  private List<ProofOfDelivery> getProofOfDeliveryByOrderId(UUID orderId) {
    return fulfillmentProofOfDeliveryService.search(null, orderId,
        new PageRequest(DEFAULT_PAGE_NUMBER, Integer.MAX_VALUE)).getContent();
  }

  private Notification newNotification() {
    Notification notification = new Notification();
    String currentFacilityName = facilityReferenceDataService.findOne(findCurrentUserFacilityId())
        .getName();
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
    return authenticationHelper.getCurrentUser().getHomeFacilityId();
  }

  private Set<UUID> findCurrentUserSupervisoryNodeIds() {
    return authenticationHelper.getCurrentUser().getRoleAssignments().stream()
        .filter(roleAssignment -> roleAssignment.getSupervisoryNodeId() != null)
        .filter(this::canThisRoleApproveRequisition)
        .map(RoleAssignmentDto::getSupervisoryNodeId)
        .collect(toSet());
  }

  private boolean canThisRoleApproveRequisition(RoleAssignmentDto roleAssignment) {
    return roleService.findOne(roleAssignment.getRoleId()).getRights().stream()
        .map(RightDto::getName)
        .anyMatch(PermissionService.REQUISITION_APPROVE::equals);
  }

  private boolean canBeAuthorizedBySubmitter(BasicRequisitionDto requisition) {
    return authenticationHelper.getCurrentUserPermissionStrings().stream()
        .anyMatch(permissionString ->
            requisition.getFacility().getId().equals(permissionString.getFacilityId())
                && requisition.getProgram().getId().equals(permissionString.getProgramId())
                && PermissionService.REQUISITION_AUTHORIZE.equals(permissionString.getRightName())
        );
  }

  private UUID findSupervisorNodeId(BasicRequisitionDto requisitionDto) {
    UUID requisitionId = requisitionDto.getId();
    Profiler profiler = requisitionController
        .getProfiler("GET_REQUISITION", requisitionId);
    return requisitionController.findRequisition(requisitionId, profiler)
        .getSupervisoryNodeId();
  }

  private List<BasicOrderDto> searchOrders(ApproveRequisitionDto approveRequisitionDto) {
    RequisitionV2Dto requisition = requisitionService
        .searchRequisition(approveRequisitionDto.getId());
    OrderSearchParams params = new OrderSearchParams(
        requisition.getSupplyingFacility(), requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), null, null, null);
    return siglusOrderService.searchOrders(params, null)
        .getContent()
        .stream()
        .filter(order -> requisition.getId().equals(order.getExternalId()))
        .collect(Collectors.toList());
  }

  private Specification<Notification> getFilterByRights() {
    Collection<PermissionString> permissionStrings = authenticationHelper
        .getCurrentUserPermissionStrings();
    log.info("current user has {} rights", permissionStrings.size());
    UUID currentUserFacilityId = findCurrentUserFacilityId();
    Set<UUID> currentUserSupervisoryNodeIds = findCurrentUserSupervisoryNodeIds();
    log.info("current user has supervisoryNode {}", currentUserSupervisoryNodeIds);
    // CAUTION: bad performance, about 5.4 MB data, using 2.5s
    List<RequisitionGroupDto> requisitionGroups = requisitionGroupService.findAll();
    boolean canEditShipments = permissionStrings.stream().anyMatch(
        rightAssignment ->
            FulfillmentPermissionService.SHIPMENTS_EDIT.equals(rightAssignment.getRightName()));
    return (root, query, cb) -> {
      List<Predicate> predicates = permissionStrings.stream()
          .map(right -> mapRightToPredicate(right, root, cb, currentUserFacilityId,
              currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
      List<List<Predicate>> subLists = Lists.partition(predicates, MAX_PREDICATES_PER_THREAD);
      Predicate predicate = cb.disjunction();
      List<Future<Predicate>> futures = new ArrayList<>();
      for (List<Predicate> list : subLists) {
        futures.add(executor.submit(() -> list.stream().reduce(cb::or).orElse(cb.disjunction())));
      }
      for (Future<Predicate> future : futures) {
        try {
          predicate = cb.or(predicate, future.get());
        } catch (Exception e) {
          throw new IllegalStateException("Unable to get result from sub thread", e);
        }
      }

      return predicate;
    };
  }

  private UUID findSupervisoryNodeIdForInternalApprove(UUID currentUserFacilityId,
      List<RequisitionGroupDto> requisitionGroups,
      Set<UUID> currentUserSupervisoryNodeIds, UUID programId) {
    RequisitionGroupDto requisitionGroupDto = requisitionGroups.stream()
        .filter(requisitionGroup -> currentUserSupervisoryNodeIds
            .contains(requisitionGroup.getSupervisoryNode().getId()))
        .filter(requisitionGroup -> requisitionGroup.supportsProgram(programId))
        .filter(requisitionGroup ->
            isInMemberFacilities(requisitionGroup.getMemberFacilities(), currentUserFacilityId))
        .findAny()
        .orElse(null);

    return requisitionGroupDto == null ? null : requisitionGroupDto.getSupervisoryNode().getId();
  }

  private boolean isInMemberFacilities(Set<FacilityDto> memberFacilities, UUID userFacilityId) {
    return memberFacilities.stream().map(FacilityDto::getId).anyMatch(userFacilityId::equals);
  }

  private Predicate mapRightToPredicate(PermissionString permissionString, Root<Notification> root,
      CriteriaBuilder cb, UUID currentUserFacilityId, Set<UUID> currentUserSupervisoryNodeIds,
      boolean canEditShipments, List<RequisitionGroupDto> requisitionGroups) {
    switch (permissionString.getRightName()) {
      case PermissionService.REQUISITION_CREATE:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), permissionString.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.REJECTED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case PermissionService.REQUISITION_AUTHORIZE:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), permissionString.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.SUBMITTED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case PermissionService.REQUISITION_APPROVE:
        if (currentUserSupervisoryNodeIds.isEmpty()) {
          return null;
        }
        Predicate internalPredicate = null;
        // if user has internal approve for specific program, should optimize
        // sn-1 & multiple for internal, sn-2 & multiple for external
        UUID nodeIdForInternalApprove = findSupervisoryNodeIdForInternalApprove(
            currentUserFacilityId, requisitionGroups,
            currentUserSupervisoryNodeIds, permissionString.getProgramId());
        Set<UUID> nodeIdsForExternalApprove = currentUserSupervisoryNodeIds
            .stream()
            .filter(id -> id != nodeIdForInternalApprove)
            .collect(toSet());

        if (nodeIdForInternalApprove != null) {
          internalPredicate = cb.and(
              cb.equal(root.get(REF_FACILITY_ID), currentUserFacilityId),
              cb.equal(root.get(REF_PROGRAM_ID), permissionString.getProgramId()),
              cb.equal(root.get(REF_STATUS), NotificationStatus.AUTHORIZED)
          );
        }

        Predicate externalPredicate = cb.and(
            cb.equal(root.get(REF_FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), permissionString.getProgramId()),
            root.get(REF_STATUS)
                .in(NotificationStatus.AUTHORIZED, NotificationStatus.IN_APPROVAL),
            root.get("supervisoryNodeId").in(nodeIdsForExternalApprove)
        );

        return internalPredicate == null ? externalPredicate :
            cb.or(internalPredicate, externalPredicate);

      case PermissionService.ORDERS_EDIT:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.APPROVED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case StockmanagementPermissionService.STOCK_CARDS_VIEW:
        if (!canEditShipments) {
          return null;
        }
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), permissionString.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.ORDERED),
            cb.equal(root.get(NOTIFY_FACILITY_ID), currentUserFacilityId)
        );
      case FulfillmentPermissionService.PODS_MANAGE:
        return cb.and(
            cb.equal(root.get(REF_FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(REF_PROGRAM_ID), permissionString.getProgramId()),
            cb.equal(root.get(REF_STATUS), NotificationStatus.SHIPPED)
        );
      default:
        return null;
    }
  }

}
