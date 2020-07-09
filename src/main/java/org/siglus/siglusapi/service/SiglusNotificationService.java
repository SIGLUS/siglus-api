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

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.fulfillment.service.FulfillmentPermissionService;
import org.openlmis.fulfillment.web.shipment.ShipmentDto;
import org.openlmis.referencedata.domain.RightAssignment;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.RightAssignmentRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.service.ReferencedataAuthenticationHelper;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApproveRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusNotificationService {

  public enum ViewableStatus {
    NOT_VIEWED, VIEWED, PROCESSED
  }

  private final NotificationRepository repo;

  private final NotificationMapper mapper;

  private final ReferencedataAuthenticationHelper authenticationHelper;

  private final OrderFulfillmentService orderService;

  private final SiglusRequisitionRequisitionService requisitionService;

  private final SupervisoryNodeRepository supervisoryNodeRepo;

  private final RightAssignmentRepository rightAssignRepo;

  public Page<NotificationDto> searchNotifications(Pageable pageable) {
    User currentUser = authenticationHelper.getCurrentUser();
    Set<RightAssignment> rightAssignments = currentUser.getRightAssignments();
    return repo.findViewable(pageable, currentUser.getHomeFacilityId(),
        getFilterByRights(rightAssignments)).map(mapper::from);
  }

  public ViewableStatus viewNotification(UUID notificationId) {
    Notification notification = repo.findOne(notificationId);
    if (notification.getViewed()) {
      return ViewableStatus.VIEWED;
    } else if (notification.getProcessed()) {
      return ViewableStatus.PROCESSED;
    }
    notification.setViewed(true);
    repo.save(notification);
    return ViewableStatus.NOT_VIEWED;
  }

  public void postSubmit(BasicRequisitionDto requisition) {
    if (canBeAuthorizedBySubmitter(requisition)) {
      return;
    }
    saveNotificationFromRequisition(requisition, notification -> {
      notification.setRefStatus(NotificationStatus.SUBMITTED);
      notification.setNotifyFacilityId(requisition.getFacility().getId());
    });
  }

  public void postAuthorize(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), findCurrentUserFacilityId(),
        NotificationStatus.SUBMITTED);

    saveNotificationFromRequisition(requisition, notification -> {
      notification.setRefStatus(NotificationStatus.AUTHORIZED);
      if (canBeInternalApproved(requisition)) {
        notification.setNotifyFacilityId(requisition.getFacility().getId());
      } else {
        notification.setNotifyFacilityId(findSupervisorFacilityId(requisition));
      }
    });
  }

  public void postApprove(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), findCurrentUserFacilityId(),
        NotificationStatus.AUTHORIZED, NotificationStatus.IN_APPROVAL);

    saveNotificationFromRequisition(requisition, notification -> {
      if (requisition.getStatus() == RequisitionStatus.IN_APPROVAL) {
        notification.setRefStatus(NotificationStatus.IN_APPROVAL);
        notification.setNotifyFacilityId(findSupervisorFacilityId(requisition));
      } else {
        notification.setRefStatus(NotificationStatus.APPROVED);
        notification.setRefFacilityId(findCurrentUserFacilityId());
        notification.setNotifyFacilityId(findCurrentUserFacilityId());
      }
    });
  }

  public void postReject(BasicRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), findCurrentUserFacilityId(),
        NotificationStatus.AUTHORIZED, NotificationStatus.IN_APPROVAL);

    saveNotificationFromRequisition(requisition, notification -> {
      notification.setRefStatus(NotificationStatus.REJECTED);
      notification.setNotifyFacilityId(requisition.getFacility().getId());
    });
  }

  public void postConvertToOrder(ApproveRequisitionDto requisition) {
    repo.updateLastNotificationProcessed(requisition.getId(), findCurrentUserFacilityId(),
        NotificationStatus.APPROVED);

    searchOrders(requisition).forEach(
        order -> {
          Notification notification = new Notification();
          notification.setRefId(order.getId());
          notification.setRefFacilityId(findCurrentUserFacilityId());
          notification.setRefProgramId(order.getProgram().getId());
          notification.setRefStatus(NotificationStatus.ORDERED);
          notification.setNotifyFacilityId(findCurrentUserFacilityId());
          repo.save(notification);
        }
    );
  }

  public void postConfirmShipment(ShipmentDto shipment) {
    OrderDto order = orderService.findOne(shipment.getOrder().getId());
    repo.updateLastNotificationProcessed(order.getId(), findCurrentUserFacilityId(),
        NotificationStatus.ORDERED);

    Notification notification = new Notification();
    notification.setRefId(shipment.getId());
    RequisitionV2Dto requisition = requisitionService.searchRequisition(order.getExternalId());
    notification.setRefFacilityId(requisition.getFacility().getId());
    notification.setRefProgramId(requisition.getProgram().getId());
    notification.setRefStatus(NotificationStatus.SHIPPED);
    notification.setNotifyFacilityId(requisition.getFacility().getId());
    repo.save(notification);
  }

  private void saveNotificationFromRequisition(BasicRequisitionDto requisition,
      Consumer<Notification> setStatusAndNotifyFacility) {
    Notification notification = from(requisition);
    setStatusAndNotifyFacility.accept(notification);
    repo.save(notification);
  }

  private Notification from(BasicRequisitionDto requisition) {
    Notification notification = new Notification();
    notification.setRefId(requisition.getId());
    notification.setRefFacilityId(requisition.getFacility().getId());
    notification.setRefProgramId(requisition.getProgram().getId());
    return notification;
  }

  private UUID findCurrentUserFacilityId() {
    return authenticationHelper.getCurrentUser().getHomeFacilityId();
  }

  private boolean canBeAuthorizedBySubmitter(BasicRequisitionDto requisition) {
    return rightAssignRepo.count((root, query, cb) -> cb.and(
        cb.equal(root.get("facilityId"), requisition.getFacility().getId()),
        cb.equal(root.get("programId"), requisition.getProgram().getId()),
        cb.equal(root.get("user").get("id"), authenticationHelper.getCurrentUser().getId()),
        cb.equal(root.get("rightName"), PermissionService.REQUISITION_AUTHORIZE)
    )) > 0;
  }

  private boolean canBeInternalApproved(BasicRequisitionDto requisition) {
    return rightAssignRepo.count((root, query, cb) -> cb.and(
        cb.equal(root.get("user").get("homeFacilityId"), requisition.getFacility().getId()),
        cb.equal(root.get("facilityId"), requisition.getFacility().getId()),
        cb.equal(root.get("programId"), requisition.getProgram().getId()),
        cb.equal(root.get("rightName"), PermissionService.REQUISITION_APPROVE)
    )) > 0;
  }

  private UUID findSupervisorFacilityId(BasicRequisitionDto requisition) {
    UUID supervisoryNodeId = requisitionService.searchRequisition(requisition.getId())
        .getSupervisoryNode();
    return supervisoryNodeRepo.findOne(supervisoryNodeId).getFacility().getId();
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
    return (root, query, cb) -> rightAssignments.stream()
        .map(this::mapRightNameToFilters)
        .flatMap(Collection::stream)
        .map(e -> cb.and(
            cb.equal(root.get("refFacilityId"), e.getFacilityId()),
            ofNullable(e.programId)
                .map(programId -> cb.equal(root.get("refProgramId"), programId))
                .orElse(cb.conjunction()),
            cb.equal(root.get("refStatus"), e.getStatus())
        ))
        .reduce(cb::or)
        .orElse(cb.disjunction());
  }

  private Collection<FacilityProgramStatusFilter> mapRightNameToFilters(RightAssignment right) {
    Set<FacilityProgramStatusFilter> statuses = new HashSet<>();
    switch (right.getRightName()) {
      case PermissionService.REQUISITION_CREATE:
        statuses.add(new FacilityProgramStatusFilter(right, NotificationStatus.REJECTED));
        break;
      case PermissionService.REQUISITION_AUTHORIZE:
        statuses.add(new FacilityProgramStatusFilter(right, NotificationStatus.SUBMITTED));
        break;
      case PermissionService.REQUISITION_APPROVE:
        statuses.add(new FacilityProgramStatusFilter(right, NotificationStatus.AUTHORIZED));
        statuses.add(new FacilityProgramStatusFilter(right, NotificationStatus.IN_APPROVAL));
        break;
      case PermissionService.ORDERS_EDIT:
        statuses.add(
            new FacilityProgramStatusFilter(right.getFacilityId(), NotificationStatus.APPROVED));
        break;
      case FulfillmentPermissionService.SHIPMENTS_EDIT:
        statuses.add(
            new FacilityProgramStatusFilter(right.getFacilityId(), NotificationStatus.ORDERED));
        break;
      case FulfillmentPermissionService.PODS_MANAGE:
        statuses.add(new FacilityProgramStatusFilter(right, NotificationStatus.SHIPPED));
        break;
      default:
    }
    return statuses;
  }

  @Setter
  @Getter
  private static class FacilityProgramStatusFilter {

    private UUID facilityId;

    private UUID programId;

    private NotificationStatus status;

    FacilityProgramStatusFilter(RightAssignment right, NotificationStatus status) {
      facilityId = right.getFacilityId();
      programId = right.getProgramId();
      this.status = status;
    }

    FacilityProgramStatusFilter(UUID facilityId, NotificationStatus status) {
      this.facilityId = facilityId;
      this.status = status;
    }

  }

}
