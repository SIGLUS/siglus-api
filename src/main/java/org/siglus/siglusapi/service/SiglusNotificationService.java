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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.ProofOfDeliveryService;
import org.openlmis.fulfillment.service.referencedata.PeriodReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.ProcessingPeriodDto;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionGroupDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.RoleReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.domain.NotificationType;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusShipmentRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.siglus.siglusapi.util.PermissionString;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public class SiglusNotificationService {

  @FunctionalInterface
  interface PredicateEvaluator {

    Predicate mapRightToPredicate(PermissionString permissionString, Root<Notification> root,
        CriteriaBuilder cb, UUID currentUserFacilityId, Set<UUID> currentUserSupervisoryNodeIds,
        boolean canEditShipments, List<RequisitionGroupDto> requisitionGroups);
  }

  public enum ViewableStatus {
    NOT_VIEWED, VIEWED, PROCESSED
  }

  private static final String FACILITY_ID = "facilityId";

  private static final String PROGRAM_ID = "programId";

  private static final String STATUS = "status";

  private static final String NOTIFICATION_TYPE = "type";

  // can't find any official document to describe this size, but if you try to reduce with cb.and or
  // cb. or more than 200 predicates in a single thread, it will throw StackOverflow.
  private static final int MAX_PREDICATES_PER_THREAD = 103;

  private final NotificationRepository repo;

  private final NotificationMapper mapper;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusOrderService siglusOrderService;

  private final SiglusRequisitionRequisitionService requisitionService;

  private final SiglusProofOfDeliveryRepository siglusProofOfDeliveryRepository;

  private final SiglusShipmentRepository siglusShipmentRepository;

  private final SiglusFacilityReferenceDataService facilityReferenceDataService;

  private final OrderExternalRepository orderExternalRepository;

  private final RoleReferenceDataService roleService;

  private final SiglusRequisitionGroupService siglusRequisitionGroupService;

  private final EntityManager em;

  private final ExecutorService executor;

  private final ProofOfDeliveryService proofOfDeliveryService;

  private final RequisitionController requisitionController;

  private final PeriodReferenceDataService periodService;

  private final ProgramReferenceDataService programRefDataService;

  private final UserReferenceDataService userReferenceDataService;

  public Page<NotificationDto> searchNotifications(Pageable pageable, NotificationType type) {
    return repo
        .findViewable(pageable, type, getFilterByRights())
        .map(notification -> {
          ProcessingPeriodDto processingPeriod = periodService.findOne(notification.getProcessingPeriodId());
          org.siglus.siglusapi.dto.FacilityDto facility =
              facilityReferenceDataService.findOne(notification.getRequestingFacilityId());
          ProgramDto program = programRefDataService.findOne(notification.getProgramId());
          ZonedDateTime submitDate = null;
          String author = userReferenceDataService.findOne(notification.getOperatorId()).getUsername();
          if (notification.getStatus().isRequisitionPeriod()) {
            RequisitionV2Dto requisition = requisitionService.searchRequisition(notification.getRefId());
            Map<String, StatusLogEntry> statusChanges = requisition.getStatusChanges();
            submitDate = statusChanges.get(RequisitionStatus.SUBMITTED.name()).getChangeDate();
          }
          return mapper.from(notification, facility, program, processingPeriod, submitDate, author);
        });
  }

  @Transactional
  public ViewableStatus viewNotification(UUID notificationId) {
    Notification notification = repo.findOne(notificationId);
    if (Boolean.TRUE.equals(notification.getProcessed())) {
      return ViewableStatus.PROCESSED;
    }
    if (Boolean.TRUE.equals(notification.getViewed())) {
      return ViewableStatus.VIEWED;
    }
    notification.setViewed(true);
    notification.setViewedDate(ZonedDateTime.now());
    notification.setViewedUserId(authenticationHelper.getCurrentUserId().orElse(null));
    log.info("view notification: {}", notification);
    repo.save(notification);
    return ViewableStatus.NOT_VIEWED;
  }

  public void postSubmit(BasicRequisitionDto requisition) {
    if (canBeAuthorizedBySubmitter(requisition)) {
      return;
    }
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.REJECTED);
    saveNotificationFromRequisition(requisition, notification -> {
      notification.setStatus(NotificationStatus.SUBMITTED);
      notification.setType(NotificationType.TODO);
    });
  }

  public void postAuthorize(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.SUBMITTED,
        NotificationStatus.REJECTED);
    saveNotificationFromRequisition(requisition, notification -> {
      notification.setStatus(NotificationStatus.AUTHORIZED);
      notification.setNotifySupervisoryNodeId(findSupervisorNodeId(requisition));
      notification.setType(NotificationType.TODO);
    });
  }

  public void postApprove(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
        NotificationStatus.IN_APPROVAL);
    RequisitionStatus status = requisition.getStatus();
    if (status == RequisitionStatus.IN_APPROVAL) {
      saveNotificationFromRequisition(requisition, notification -> {
        notification.setType(NotificationType.TODO);
        notification.setStatus(NotificationStatus.IN_APPROVAL);
        notification.setNotifySupervisoryNodeId(findSupervisorNodeId(requisition));
      });
    } else if (status == RequisitionStatus.APPROVED) {
      saveNotificationFromRequisition(requisition, notification -> {
        notification.setType(NotificationType.TODO);
        notification.setStatus(NotificationStatus.APPROVED);
        notification.setFacilityId(findCurrentUserFacilityId());
      });
      saveNotificationFromRequisition(requisition, notification -> {
        notification.setType(NotificationType.UPDATE);
        notification.setStatus(NotificationStatus.APPROVED);
      });
    } else if (status == RequisitionStatus.RELEASED_WITHOUT_ORDER) {
      saveNotificationFromRequisition(requisition, notification -> {
        notification.setType(NotificationType.UPDATE);
        notification.setStatus(NotificationStatus.RELEASED_WITHOUT_ORDER);
      });
    }
  }

  public void postReject(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.AUTHORIZED,
        NotificationStatus.IN_APPROVAL);
    saveNotificationFromRequisition(requisition, notification -> {
      notification.setStatus(NotificationStatus.REJECTED);
      notification.setType(NotificationType.TODO);
    });
  }

  public void postDelete(UUID requisitionId) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaUpdate<Notification> update = cb.createCriteriaUpdate(Notification.class);
    Root<Notification> root = update.from(Notification.class);
    update.set(root.get("processed"), true);
    update.where(cb.and(
        cb.equal(root.get("refId"), requisitionId),
        root.get(STATUS).in(NotificationStatus.requisitionStatuses())
    ));
    em.createQuery(update).executeUpdate();
  }

  public void postConvertToOrder(ApproveRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.APPROVED);

    searchOrders(requisition).forEach(
        order -> {
          Notification notification = new Notification();
          notification.setRefId(order.getId());
          notification.setFacilityId(findCurrentUserFacilityId());
          notification.setProgramId(order.getProgram().getId());
          notification.setEmergency(requisition.getEmergency());
          notification.setStatus(NotificationStatus.ORDERED);
          notification.setType(NotificationType.TODO);
          notification.setProcessingPeriodId(order.getProcessingPeriod().getId());
          notification.setRequestingFacilityId(order.getRequestingFacility().getId());
          log.info("convert requisition to order notification: {}", notification);
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
    Shipment orderedShipment = siglusShipmentRepository.findShipmentByOrderId(order.getId());
    ProofOfDelivery proofOfDelivery = siglusProofOfDeliveryRepository.findByShipmentId(orderedShipment.getId());

    Notification requestNotification = new Notification();
    requestNotification.setRefId(proofOfDelivery.getId());
    requestNotification.setFacilityId(requisition.getFacilityId());
    requestNotification.setProgramId(requisition.getProgramId());
    requestNotification.setEmergency(requisition.getEmergency());
    requestNotification.setStatus(NotificationStatus.SHIPPED);
    requestNotification.setType(NotificationType.TODO);
    requestNotification.setProcessingPeriodId(order.getProcessingPeriod().getId());
    requestNotification.setRequestingFacilityId(requisition.getFacilityId());
    log.info("confirm shipment notification for request facility: {}", requestNotification);
    repo.save(requestNotification);

    Notification supplierNotification = new Notification();
    supplierNotification.setRefId(proofOfDelivery.getId());
    supplierNotification.setFacilityId(requisition.getFacilityId());
    supplierNotification.setProgramId(requisition.getProgramId());
    supplierNotification.setEmergency(requisition.getEmergency());
    supplierNotification.setStatus(NotificationStatus.SHIPPED);
    supplierNotification.setType(NotificationType.UPDATE);
    supplierNotification.setProcessingPeriodId(order.getProcessingPeriod().getId());
    supplierNotification.setRequestingFacilityId(requisition.getFacilityId());
    log.info("confirm shipment notification for supplier facility: {}", supplierNotification);
    repo.save(supplierNotification);
  }

  public void postConfirmPod(org.openlmis.fulfillment.web.util.ProofOfDeliveryDto pod) {
    repo.updateLastNotificationProcessed(pod.getId(), NotificationStatus.SHIPPED);
    Notification notification = new Notification();
    notification.setRefId(pod.getId());
    OrderObjectReferenceDto order = pod.getShipment().getOrder();
    notification.setProgramId(order.getProgram().getId());
    notification.setEmergency(order.getEmergency());
    notification.setProcessingPeriodId(order.getProcessingPeriod().getId());
    notification.setFacilityId(order.getSupplyingFacility().getId());
    notification.setRequestingFacilityId(order.getRequestingFacility().getId());
    notification.setStatus(NotificationStatus.RECEIVED);
    notification.setType(NotificationType.UPDATE);
    log.info("confirm pod notification: {}", notification);
    repo.save(notification);
  }

  public void postConfirmPod(UUID podId, Order order) {
    repo.updateLastNotificationProcessed(podId, NotificationStatus.SHIPPED);
    Notification notification = new Notification();
    notification.setRefId(podId);
    notification.setProgramId(order.getProgramId());
    notification.setEmergency(order.getEmergency());
    notification.setProcessingPeriodId(order.getProcessingPeriodId());
    notification.setFacilityId(order.getSupplyingFacilityId());
    notification.setRequestingFacilityId(order.getRequestingFacilityId());
    notification.setStatus(NotificationStatus.RECEIVED);
    notification.setType(NotificationType.UPDATE);
    log.info("confirm pod notification: {}", notification);
    repo.save(notification);
  }

  private void saveNotificationFromRequisition(BasicRequisitionDto requisition,
      Consumer<Notification> setStatusAndNotifyFacility) {
    Notification notification = from(requisition);
    setStatusAndNotifyFacility.accept(notification);
    log.info("save notification: {} from requisition", notification);
    repo.save(notification);
  }

  private Notification from(BasicRequisitionDto requisition) {
    Notification notification = new Notification();
    notification.setRefId(requisition.getId());
    notification.setFacilityId(requisition.getFacility().getId());
    notification.setProgramId(requisition.getProgram().getId());
    notification.setEmergency(requisition.getEmergency());
    notification.setProcessingPeriodId(requisition.getProcessingPeriod().getId());
    notification.setRequestingFacilityId(requisition.getFacility().getId());
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
    Profiler profiler = requisitionController.getProfiler("GET_REQUISITION", requisitionId);
    return requisitionController.findRequisition(requisitionId, profiler).getSupervisoryNodeId();
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
    Collection<PermissionString> permissionStrings = authenticationHelper.getCurrentUserPermissionStrings();
    log.info("current user has {} rights", permissionStrings.size());
    UUID currentUserFacilityId = findCurrentUserFacilityId();
    Set<UUID> currentUserSupervisoryNodeIds = findCurrentUserSupervisoryNodeIds();
    log.info("current user has supervisoryNode {}", currentUserSupervisoryNodeIds);
    List<RequisitionGroupDto> requisitionGroups = siglusRequisitionGroupService.getRequisitionGroups();
    boolean canEditShipments = permissionStrings.stream().anyMatch(
        rightAssignment ->
            org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_EDIT.equals(rightAssignment.getRightName()));
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
          log.error(e.getMessage());
          Thread.currentThread().interrupt();
        }
      }

      return predicate;
    };
  }

  UUID findSupervisoryNodeIdForInternalApprove(UUID currentUserFacilityId,
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

  Predicate mapRightToPredicate(
      PermissionString permissionString,
      Root<Notification> root,
      CriteriaBuilder cb,
      UUID currentUserFacilityId,
      Set<UUID> currentUserSupervisoryNodeIds,
      boolean canEditShipments,
      List<RequisitionGroupDto> requisitionGroups) {
    return Optional.ofNullable(permissionToPredicateEvaluator.get(permissionString.getRightName()))
        .map(
            evaluator ->
                evaluator.mapRightToPredicate(
                    permissionString,
                    root,
                    cb,
                    currentUserFacilityId,
                    currentUserSupervisoryNodeIds,
                    canEditShipments,
                    requisitionGroups))
        .orElse(null);
  }

  private final Map<String, PredicateEvaluator> permissionToPredicateEvaluator = ImmutableMap
      .<String, PredicateEvaluator>builder()
      .put(PermissionService.REQUISITION_VIEW, getPredicateEvaluatorForRequisitionView())
      .put(PermissionService.REQUISITION_CREATE, getPredicateEvaluatorForRequisitionCreate())
      .put(PermissionService.REQUISITION_AUTHORIZE, getPredicateEvaluatorForRequisitionAuthorize())
      .put(PermissionService.REQUISITION_APPROVE, getPredicateEvaluatorForRequisitionApprove())
      .put(PermissionService.ORDERS_EDIT, getPredicateEvaluatorForOrdersEdit())
      .put(org.openlmis.stockmanagement.service.PermissionService.STOCK_CARDS_VIEW,
          getPredicateEvaluatorForStockCardsView())
      .put(org.openlmis.fulfillment.service.PermissionService.PODS_MANAGE,
          getPredicateEvaluatorForPodManage())
      .put(org.openlmis.fulfillment.service.PermissionService.PODS_VIEW,
          getPredicateEvaluatorForPodView())
      .put(org.openlmis.fulfillment.service.PermissionService.SHIPMENTS_EDIT,
          getPredicateEvaluatorForShipmentsEdit())
      .build();

  PredicateEvaluator getPredicateEvaluatorForShipmentsEdit() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(STATUS), NotificationStatus.RECEIVED),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId)
    );
  }

  PredicateEvaluator getPredicateEvaluatorForPodView() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
        cb.equal(root.get(STATUS), NotificationStatus.SHIPPED),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId),
        cb.equal(root.get(NOTIFICATION_TYPE), NotificationType.UPDATE)
    );
  }

  PredicateEvaluator getPredicateEvaluatorForPodManage() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
        cb.equal(root.get(STATUS), NotificationStatus.SHIPPED),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId),
        cb.equal(root.get(NOTIFICATION_TYPE), NotificationType.TODO)
    );
  }

  PredicateEvaluator getPredicateEvaluatorForStockCardsView() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> {
      if (!canEditShipments) {
        return null;
      }
      return cb.and(
          cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
          cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
          cb.equal(root.get(STATUS), NotificationStatus.ORDERED),
          cb.equal(root.get(FACILITY_ID), currentUserFacilityId)
      );
    };
  }

  PredicateEvaluator getPredicateEvaluatorForOrdersEdit() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(STATUS), NotificationStatus.APPROVED),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId)
    );
  }

  PredicateEvaluator getPredicateEvaluatorForRequisitionApprove() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> {
      if (currentUserSupervisoryNodeIds.isEmpty()) {
        return null;
      }
      Predicate internalPredicate = null;
      Predicate externalPredicate = null;
      // if user has internal approve for specific program, should optimize
      // sn-1 & multiple for internal, sn-2 & multiple for external
      UUID nodeIdForInternalApprove = findSupervisoryNodeIdForInternalApprove(
          currentUserFacilityId, requisitionGroups,
          currentUserSupervisoryNodeIds, permissionString.getProgramId());
      Set<UUID> nodeIdsForExternalApprove = currentUserSupervisoryNodeIds
          .stream()
          .filter(id -> !id.equals(nodeIdForInternalApprove))
          .collect(toSet());

      if (nodeIdForInternalApprove != null) {
        internalPredicate = cb.and(
            cb.equal(root.get(FACILITY_ID), currentUserFacilityId),
            cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
            cb.equal(root.get(STATUS), NotificationStatus.AUTHORIZED)
        );
      }

      if (!CollectionUtils.isEmpty(nodeIdsForExternalApprove)) {
        externalPredicate = cb.and(
            cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
            cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
            root.get(STATUS)
                .in(NotificationStatus.AUTHORIZED, NotificationStatus.IN_APPROVAL),
            root.get("notifySupervisoryNodeId").in(nodeIdsForExternalApprove)
        );
      }

      if (internalPredicate != null && externalPredicate != null) {
        return cb.or(internalPredicate, externalPredicate);
      }

      if (internalPredicate != null) {
        return internalPredicate;
      }

      return externalPredicate;
    };
  }

  PredicateEvaluator getPredicateEvaluatorForRequisitionAuthorize() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
        cb.equal(root.get(STATUS), NotificationStatus.SUBMITTED),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId)
    );
  }

  PredicateEvaluator getPredicateEvaluatorForRequisitionCreate() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
        cb.equal(root.get(STATUS), NotificationStatus.REJECTED),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId)
    );
  }

  PredicateEvaluator getPredicateEvaluatorForRequisitionView() {
    return (permissionString, root, cb, currentUserFacilityId,
        currentUserSupervisoryNodeIds, canEditShipments, requisitionGroups) -> cb.and(
        cb.equal(root.get(FACILITY_ID), permissionString.getFacilityId()),
        cb.equal(root.get(PROGRAM_ID), permissionString.getProgramId()),
        root.get(STATUS).in(NotificationStatus.APPROVED,
            NotificationStatus.RELEASED_WITHOUT_ORDER),
        cb.equal(root.get(FACILITY_ID), currentUserFacilityId)
    );
  }

}
