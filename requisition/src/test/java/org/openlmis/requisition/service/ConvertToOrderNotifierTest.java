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

package org.openlmis.requisition.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_CONVERT_TO_ORDER_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_CONVERT_TO_ORDER_SUBJECT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_SMS_CONVERT_TO_ORDER_CONTENT;

import java.util.Collections;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.i18n.MessageService;
import org.openlmis.requisition.service.notification.NotificationService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.utils.Message;

@SuppressWarnings({"PMD.UnusedPrivateField"})
@RunWith(MockitoJUnitRunner.class)
public class ConvertToOrderNotifierTest {

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private NotificationService notificationService;

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private ConvertToOrderNotifier convertToOrderNotifier;

  private UserDto user = DtoGenerator.of(UserDto.class);
  private Locale locale = Locale.ENGLISH;

  @Before
  public void setUp() {
    mockServices();
    mockMessages();
  }

  @Test
  public void shouldCallNotificationService() throws Exception {
    Requisition requisition = mock(Requisition.class);
    StatusChange initiateAuditEntry = mock(StatusChange.class);

    when(requisition.getStatusChanges()).thenReturn(Collections.singletonList(initiateAuditEntry));
    when(initiateAuditEntry.getStatus()).thenReturn(RequisitionStatus.INITIATED);
    when(initiateAuditEntry.getAuthorId()).thenReturn(user.getId());

    convertToOrderNotifier.notifyConvertToOrder(requisition, locale);

    verify(notificationService).notify(refEq(user),
        eq(REQUISITION_EMAIL_CONVERT_TO_ORDER_SUBJECT),
        eq(REQUISITION_EMAIL_CONVERT_TO_ORDER_CONTENT),
        eq(REQUISITION_SMS_CONVERT_TO_ORDER_CONTENT),
        eq(ConvertToOrderNotifier.NOTIFICATION_TAG));
  }

  private void mockServices() {
    when(programReferenceDataService.findOne(any())).thenReturn(new ProgramDto());
    when(periodReferenceDataService.findOne(any())).thenReturn(new ProcessingPeriodDto());
    when(userReferenceDataService.findOne(eq(user.getId()))).thenReturn(user);
  }

  private void mockMessages() {
    Message convertToOrderSubject = new Message(REQUISITION_EMAIL_CONVERT_TO_ORDER_SUBJECT);
    Message.LocalizedMessage localizedMessage =
        convertToOrderSubject.new LocalizedMessage(REQUISITION_EMAIL_CONVERT_TO_ORDER_SUBJECT);
    when(messageService.localize(eq(convertToOrderSubject), eq(locale)))
        .thenReturn(localizedMessage);

    Message convertToOrderEmailContent =
        new Message(REQUISITION_EMAIL_CONVERT_TO_ORDER_CONTENT);
    localizedMessage = convertToOrderEmailContent
        .new LocalizedMessage(REQUISITION_EMAIL_CONVERT_TO_ORDER_CONTENT);
    when(messageService.localize(eq(convertToOrderEmailContent), eq(locale)))
        .thenReturn(localizedMessage);

    Message convertToOrderSmsContent =
        new Message(REQUISITION_SMS_CONVERT_TO_ORDER_CONTENT);
    localizedMessage = convertToOrderSmsContent
        .new LocalizedMessage(REQUISITION_SMS_CONVERT_TO_ORDER_CONTENT);
    when(messageService.localize(eq(convertToOrderSmsContent), eq(locale)))
        .thenReturn(localizedMessage);


  }
}
