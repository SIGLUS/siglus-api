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

package org.siglus.siglusapi.localmachine.event;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.web.util.OrderObjectReferenceDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.siglus.common.domain.OrderExternal;
import org.siglus.common.repository.OrderExternalRepository;
import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.domain.NotificationStatus;
import org.siglus.siglusapi.domain.NotificationType;
import org.siglus.siglusapi.repository.NotificationRepository;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  private final NotificationRepository notificationRepository;
  private final OrderExternalRepository orderExternalRepository;
  private final RequisitionRepository requisitionRepository;

  public void postInternalApproval(UUID userId, BasicRequisitionDto requisition, UUID notifySupervisoryNodeId) {
    saveNotificationFromRequisition(userId, requisition, notification -> {
      notification.setType(NotificationType.TODO);
      notification.setStatus(NotificationStatus.IN_APPROVAL);
      notification.setNotifySupervisoryNodeId(notifySupervisoryNodeId);
    });
  }

  public void postReject(UUID userId, BasicRequisitionDto requisition) {
    notificationRepository.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.IN_APPROVAL);
    saveNotificationFromRequisition(userId, requisition, notification -> {
      notification.setStatus(NotificationStatus.REJECTED);
      notification.setType(NotificationType.TODO);
    });
  }


  public void postFulfillment(UUID userId, UUID proofOfDeliveryId, Order order) {
    // supplier notification
    Notification supplierNotification = new Notification();
    supplierNotification.setRefId(order.getId());
    supplierNotification.setFacilityId(order.getFacilityId());
    supplierNotification.setProgramId(order.getProgramId());
    supplierNotification.setEmergency(order.getEmergency());
    supplierNotification.setStatus(NotificationStatus.ORDERED);
    supplierNotification.setType(NotificationType.TODO);
    supplierNotification.setProcessingPeriodId(order.getProcessingPeriodId());
    supplierNotification.setRequestingFacilityId(order.getRequestingFacilityId());
    supplierNotification.setProcessed(true);
    log.info("fulfill order notification: {}", supplierNotification);
    save(userId, supplierNotification);

    // client notification
    OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
    UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    Stream.of(NotificationType.values()).forEach(notificationType -> {
      Notification notification = new Notification();
      notification.setRefId(proofOfDeliveryId);
      notification.setFacilityId(requisition.getFacilityId());
      notification.setProgramId(requisition.getProgramId());
      notification.setEmergency(requisition.getEmergency());
      notification.setStatus(NotificationStatus.SHIPPED);
      notification.setType(notificationType);
      notification.setProcessingPeriodId(order.getProcessingPeriodId());
      notification.setRequestingFacilityId(requisition.getFacilityId());
      log.info("confirm shipment notification: {}", notification);
      save(userId, notification);
    });
  }

  public void postConfirmPod(UUID userId, org.openlmis.fulfillment.web.util.ProofOfDeliveryDto pod) {
    notificationRepository.updateLastNotificationProcessed(pod.getId(), NotificationStatus.SHIPPED);
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
    save(userId, notification);
  }

  private void saveNotificationFromRequisition(UUID userId, BasicRequisitionDto requisition,
      Consumer<Notification> setStatusAndNotifyFacility) {
    Notification notification = from(requisition);
    setStatusAndNotifyFacility.accept(notification);
    log.info("save notification: {} from requisition", notification);
    save(userId, notification);
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

  public void save(UUID userId, Notification notification) {
    simulateUserAuthHelper.simulateNewUserThenRollbackAuth(userId, notification, notificationRepository::save);
  }
}
