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

import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_ORDER_CREATION_BODY;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_POD_CONFIRMED_BODY;
import static org.openlmis.fulfillment.i18n.MessageKeys.FULFILLMENT_EMAIL_POD_CONFIRMED_SUBJECT;

import com.google.common.collect.Lists;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.ProofOfDeliveryDataBuilder;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.i18n.MessageService;
import org.openlmis.fulfillment.service.notification.NotificationService;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.service.referencedata.UserReferenceDataService;
import org.openlmis.fulfillment.testutils.FacilityDataBuilder;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.testutils.UserDataBuilder;
import org.openlmis.fulfillment.util.Message;

public class FulfillmentNotificationServiceTest {

  private static final String SUBJECT = "New order";
  private static final String KANKAO_HC = "Kankao HC";

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private NotificationService notificationService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private FulfillmentNotificationService fulfillmentNotificationService = new
      FulfillmentNotificationService();

  @Captor
  private ArgumentCaptor<String> subjectCaptor;

  @Captor
  private ArgumentCaptor<String> contentCaptor;

  private UUID userId = UUID.randomUUID();
  private ZonedDateTime date = ZonedDateTime.of(2018, 3, 1, 3, 0, 0, 0, ZoneId.systemDefault());
  private UserDto user;

  @Before
  public void setUp() {
    user = new UserDataBuilder()
        .withId(userId)
        .build();

    MockitoAnnotations.initMocks(this);
    when(userReferenceDataService.findOne(userId)).thenReturn(user);
    mockMessages();
  }

  @Test
  public void shouldSendOrderCreatedNotification() {
    Order order = new OrderDataBuilder()
        .withStatus(OrderStatus.RECEIVED)
        .withCreatedById(userId)
        .build();

    fulfillmentNotificationService.sendOrderCreatedNotification(order);

    verify(notificationService).notify(
        user,
        SUBJECT,
        "Create an order: " + order.getId() + " with status: RECEIVED"
    );
  }

  @Test
  public void shouldSendPodConfirmedNotification() {
    ProofOfDelivery pod = new ProofOfDeliveryDataBuilder()
        .withShipment(new ShipmentDataBuilder()
            .withShipDetails(new CreationDetails(userId, date))
            .build())
        .build();

    when(facilityReferenceDataService.findOne(pod.getReceivingFacilityId())).thenReturn(
        new FacilityDataBuilder().withName(KANKAO_HC).build());

    fulfillmentNotificationService.sendPodConfirmedNotification(pod);

    verify(notificationService).notify(eq(user), subjectCaptor.capture(), contentCaptor.capture());

    assertThat(subjectCaptor.getValue(), stringContainsInOrder(
        Lists.newArrayList(
            pod.getShipment().getOrder().getOrderCode(),
            KANKAO_HC)));
    assertThat(contentCaptor.getValue(), stringContainsInOrder(
        Lists.newArrayList(
            "John", "Doe",
            pod.getShipment().getOrder().getOrderCode(),
            pod.getShipment().getShippedDate().toLocalDate().toString(),
            KANKAO_HC,
            pod.getReceivedBy(),
            pod.getReceivedDate().toString()
        )
    ));
  }

  private void mockMessages() {
    Message orderCreationSubject = new Message(FULFILLMENT_EMAIL_ORDER_CREATION_SUBJECT);
    Message.LocalizedMessage localizedMessage =
        orderCreationSubject.new LocalizedMessage(SUBJECT);
    when(messageService.localize(orderCreationSubject)).thenReturn(localizedMessage);
    Message orderCreationBody = new Message(FULFILLMENT_EMAIL_ORDER_CREATION_BODY);
    localizedMessage = orderCreationBody
        .new LocalizedMessage("Create an order: {id} with status: {status}");
    when(messageService.localize(orderCreationBody)).thenReturn(localizedMessage);

    Message podConfirmedSubject = new Message(FULFILLMENT_EMAIL_POD_CONFIRMED_SUBJECT);
    Message.LocalizedMessage message = podConfirmedSubject
        .new LocalizedMessage("{orderCode} Received by {receivingFacility}");
    when(messageService.localize(podConfirmedSubject)).thenReturn(message);
    Message podConfirmedBody = new Message(FULFILLMENT_EMAIL_POD_CONFIRMED_BODY);
    message = podConfirmedBody
        .new LocalizedMessage("Dear {firstName} {lastName},\\n\\n \\\n"
        + "  You are receiving this notification because the shipment that you created to \\n"
        + "  fulfill {orderCode} on {shipmentDate} has arrived at {receivingFacility} \\n"
        + "  and has been confirmed by {receivedBy} on {receivedDate}. For details, \\n"
        + "  you can view the electronic copy of the Proof of Delivery at this link: {podUrl}");
    when(messageService.localize(podConfirmedBody)).thenReturn(message);
  }
}
