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

package org.openlmis.fulfillment.service;

import static org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptors;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_ORDER_CREATION_BODY;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_POD_CONFIRMED_BODY;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_POD_CONFIRMED_SUBJECT;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.i18n.MessageService;
import org.openlmis.fulfillment.service.notification.NotificationService;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.util.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FulfillmentNotificationService {

  private static final String RECEIVING_FACILITY = "receivingFacility";
  private static final String ORDER_CODE = "orderCode";
  private static final String SHIPMENT_DATE = "shipmentDate";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String POD_URL = "podUrl";
  private static final String POD_BASE = "/#!/pod/";
  
  @Autowired
  private NotificationService notificationService;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  protected MessageService messageService;

  @Value("${publicUrl}")
  private String publicUrl;
  
  /**
   * Send notification to the shipper of the associated Proof of Delivery informing them that the
   * Proof of Delivery has been confirmed.
   *
   * @param proofOfDelivery proof of delivery that was confirmed
   */
  public void sendPodConfirmedNotification(ProofOfDelivery proofOfDelivery) {
    UserDto user = userReferenceDataService.findOne(proofOfDelivery.getShipment().getShippedById());
    Map<String, String> messageParams = buildMessageParamsForPodMessage(user, proofOfDelivery);

    String subject = getContent(proofOfDelivery,
        FULFILLMENT_EMAIL_POD_CONFIRMED_SUBJECT, messageParams);
    String content = getContent(proofOfDelivery,
        FULFILLMENT_EMAIL_POD_CONFIRMED_BODY, messageParams);

    notificationService.notify(user, subject, content);
  }

  /**
   * Send notification to the warehouse clerk that the order has just been created in the system.
   *
   * @param order an order that was created
   */
  public void sendOrderCreatedNotification(Order order) {
    UserDto user = userReferenceDataService.findOne(order.getCreatedById());
    String subject = messageService
        .localize(new Message(FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT))
        .getMessage();

    String content = getContent(order, FULFILLMENT_EMAIL_ORDER_CREATION_BODY);

    notificationService.notify(user, subject, content);
  }

  private Map<String, String> buildMessageParamsForPodMessage(UserDto user, ProofOfDelivery pod) {
    Map<String, String> messageParams = new HashMap<>();
    String receivingFacility = facilityReferenceDataService.findOne(
        pod.getReceivingFacilityId()).getName();

    messageParams.put(RECEIVING_FACILITY, receivingFacility);
    messageParams.put(ORDER_CODE, pod.getShipment().getOrder().getOrderCode());
    messageParams.put(SHIPMENT_DATE, pod.getShipment().getShippedDate().toLocalDate().toString());
    messageParams.put(FIRST_NAME, user.getFirstName());
    messageParams.put(LAST_NAME, user.getLastName());
    messageParams.put(POD_URL, publicUrl + POD_BASE + pod.getId());

    return messageParams;
  }

  private String getContent(Object object, String messageKey) {
    return getContent(object, messageKey, Collections.emptyMap());
  }

  private String getContent(Object object, String messageKey, Map<String, String> messageParams) {
    String content = messageService
        .localize(new Message(messageKey))
        .getMessage();

    try {
      List<PropertyDescriptor> descriptors = Arrays
          .stream(getPropertyDescriptors(object.getClass()))
          .filter(d -> null != d.getReadMethod())
          .collect(Collectors.toList());

      for (PropertyDescriptor descriptor : descriptors) {
        String target = "{" + descriptor.getName() + "}";
        String replacement = String.valueOf(descriptor.getReadMethod().invoke(object));

        content = content.replace(target, replacement);
      }

      for (Map.Entry<String, String> entry : messageParams.entrySet()) {
        String target = "{" + entry.getKey() + "}";
        String replacement = entry.getValue();

        content = content.replace(target, replacement);
      }

    } catch (IllegalAccessException | InvocationTargetException exp) {
      throw new IllegalStateException("Can't get access to getter method", exp);
    }
    return content;
  }
}
