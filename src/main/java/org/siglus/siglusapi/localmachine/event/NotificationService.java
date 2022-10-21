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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.fulfillment.domain.Order;
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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class NotificationService {
  private final SiglusSimulateUserAuthHelper simulateUserAuthHelper;
  private final NotificationRepository notificationRepository;
  private final OrderExternalRepository orderExternalRepository;
  private final RequisitionRepository requisitionRepository;

  public void postInternalApproval(UUID userId, BasicRequisitionDto requisition, UUID notifySupervisoryNodeId) {
    try {
      saveNotificationFromRequisition(userId, requisition, notification -> {
        notification.setType(NotificationType.TODO);
        notification.setStatus(NotificationStatus.IN_APPROVAL);
        notification.setNotifySupervisoryNodeId(notifySupervisoryNodeId);
      });
    } catch (Exception e) {
      log.error("Notification send failed, msg = " + e.getMessage(), e);
    }
  }

  public void postReject(UUID userId, BasicRequisitionDto requisition) {
    try {
      notificationRepository.updateLastNotificationProcessed(requisition.getId(), NotificationStatus.IN_APPROVAL);
      saveNotificationFromRequisition(userId, requisition, notification -> {
        notification.setStatus(NotificationStatus.REJECTED);
        notification.setType(NotificationType.TODO);
      });
    } catch (Exception e) {
      log.error("Notification send failed, msg = " + e.getMessage(), e);
    }
  }


  public void postFulfillment(UUID userId, UUID proofOfDeliveryId, Order order) {
    try {
      OrderExternal external = orderExternalRepository.findOne(order.getExternalId());
      UUID requisitionId = external == null ? order.getExternalId() : external.getRequisitionId();
      Requisition requisition = requisitionRepository.findOne(requisitionId);

      Notification requestNotification = new Notification();
      requestNotification.setRefId(proofOfDeliveryId);
      requestNotification.setFacilityId(requisition.getFacilityId());
      requestNotification.setProgramId(requisition.getProgramId());
      requestNotification.setEmergency(requisition.getEmergency());
      requestNotification.setStatus(NotificationStatus.SHIPPED);
      requestNotification.setType(NotificationType.TODO);
      requestNotification.setProcessingPeriodId(order.getProcessingPeriodId());
      requestNotification.setRequestingFacilityId(requisition.getFacilityId());
      log.info("confirm shipment notification: {}", requestNotification);
      save(userId, requestNotification);

      Notification supplierNotification = new Notification();
      supplierNotification.setRefId(proofOfDeliveryId);
      supplierNotification.setFacilityId(order.getSupplyingFacilityId());
      supplierNotification.setProgramId(requisition.getProgramId());
      supplierNotification.setEmergency(requisition.getEmergency());
      supplierNotification.setStatus(NotificationStatus.SHIPPED);
      supplierNotification.setType(NotificationType.UPDATE);
      supplierNotification.setProcessingPeriodId(order.getProcessingPeriodId());
      supplierNotification.setRequestingFacilityId(requisition.getFacilityId());
      log.info("confirm shipment notification for supplier facility: {}", supplierNotification);
      save(userId, supplierNotification);
    } catch (Exception e) {
      log.error("Notification send failed, msg = " + e.getMessage(), e);
    }
  }

  public void postConfirmPod(UUID userId, UUID proofOfDeliveryId, Order order) {
    try {
      notificationRepository.updateLastNotificationProcessed(proofOfDeliveryId, NotificationStatus.SHIPPED);
      Notification notification = new Notification();
      notification.setRefId(proofOfDeliveryId);
      notification.setProgramId(order.getProgramId());
      notification.setEmergency(order.getEmergency());
      notification.setProcessingPeriodId(order.getProcessingPeriodId());
      notification.setFacilityId(order.getSupplyingFacilityId());
      notification.setRequestingFacilityId(order.getRequestingFacilityId());
      notification.setStatus(NotificationStatus.RECEIVED);
      notification.setType(NotificationType.UPDATE);
      log.info("confirm pod notification: {}", notification);
      save(userId, notification);
    } catch (Exception e) {
      log.error("Notification send failed, msg = " + e.getMessage(), e);
    }
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
