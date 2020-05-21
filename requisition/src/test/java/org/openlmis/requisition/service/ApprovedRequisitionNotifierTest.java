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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_REQUISITION_APPROVED_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_REQUISITION_APPROVED_SUBJECT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_SMS_REQUISITION_APPROVED_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_TYPE_EMERGENCY;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_TYPE_REGULAR;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.i18n.MessageService;
import org.openlmis.requisition.service.notification.NotificationService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.UserDtoDataBuilder;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.web.RequisitionForConvertBuilder;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class ApprovedRequisitionNotifierTest {

  private static final String SUBJECT = "Action Required";
  private static final String EMAIL_CONTENT = "Dear ${user}:\\n"
      + "This email is informing you that the ${requisitionType} requisition approved on "
      + "${finalApprovalDate} for the Period ${period} and ${program} at ${facility} is ready to "
      + "be converted to an order. Please login to convert the requisition to an order.\\n"
      + "${url}\\n"
      + "Thank you.";
  private static final String SMS_CONTENT = "The ${requisitionType} requisition approved on "
      + "${finalApprovalDate} for the Period ${period} and ${program} at ${facility} is ready to "
      + "be converted to an order.";

  @Mock
  private UserReferenceDataService userReferenceDataService;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private NotificationService notificationService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private MessageService messageService;

  @Mock
  private RequisitionForConvertBuilder requisitionForConvertBuilder;

  @InjectMocks
  private ApprovedRequisitionNotifier approvedRequisitionNotifier;
  
  private String publicUrl;

  private Requisition requisition = mock(Requisition.class);
  private UUID requisitionId = UUID.randomUUID();

  private UserDto clerkOne = new UserDtoDataBuilder().withUsername("ClerkOne").buildAsDto();
  private UserDto clerkTwo = new UserDtoDataBuilder().withUsername("ClerkTwo").buildAsDto();
  private UserDto clerkThree = new UserDtoDataBuilder().withUsername("ClerkThree").buildAsDto();
  private UserDto clerkFour = new UserDtoDataBuilder().withUsername("ClerkFour").buildAsDto();
  private FacilityDto warehouseOne = DtoGenerator.of(FacilityDto.class, 3).get(0);
  private FacilityDto warehouseTwo = DtoGenerator.of(FacilityDto.class, 3).get(1);
  private FacilityDto facility = DtoGenerator.of(FacilityDto.class, 3).get(2);
  private ProgramDto program = DtoGenerator.of(ProgramDto.class);
  private ProcessingPeriodDto processingPeriod = DtoGenerator.of(ProcessingPeriodDto.class);

  private StatusChange statusChange = mock(StatusChange.class);
  private ZonedDateTime createdDate = ZonedDateTime.parse("2017-05-08T10:15:30+01:00");

  private Message regularRequisitionMessage = new Message(REQUISITION_TYPE_REGULAR);
  private Message emergencyRequisitionMessage = new Message(REQUISITION_TYPE_EMERGENCY);

  private RightDto right = DtoGenerator.of(RightDto.class);

  private Locale locale = Locale.ENGLISH;

  @Before
  public void setUp() {
    publicUrl = System.getenv("PUBLIC_URL") != null ? System.getenv("PUBLIC_URL")
        : System.getenv("BASE_URL");
    ReflectionTestUtils.setField(approvedRequisitionNotifier, "publicUrl",
        publicUrl);
    prepareStatusChange();
    prepareRequisition();
    mockServices();
  }

  @Test
  public void notifyClerksShouldNotifyAllClerksOnce() {
    approvedRequisitionNotifier.notifyClerks(requisition, locale);

    verify(notificationService, times(1))
        .notify(eq(clerkOne), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkTwo), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkThree), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkFour), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
  }

  @Test
  public void notifyClerkShouldNotifyWithCorrectSubject() {
    approvedRequisitionNotifier.notifyClerks(requisition, locale);

    verify(notificationService, times(4))
        .notify(any(), eq(SUBJECT), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
  }

  @Test
  public void notifyClerkShouldNotifyWithCorrectMessageBody() {
    String expectedEmailContent = "Dear ClerkOne:\\n"
        + "This email is informing you that the regular requisition approved on May 8, 2017 "
        + "10:15:30 AM for the Period " + processingPeriod.getName()
        + " and " + program.getName() + " at " + facility.getName() + " is ready to be "
        + "converted to an order. Please login to convert the requisition to an order.\\n"
        + publicUrl + "/#!/requisitions/convertToOrder\\n"
        + "Thank you.";

    String expectedSmsContent = "The regular requisition approved on May 8, 2017 "
        + "10:15:30 AM for the Period " + processingPeriod.getName()
        + " and " + program.getName() + " at " + facility.getName() + " is ready to be "
        + "converted to an order.";

    approvedRequisitionNotifier.notifyClerks(requisition, locale);

    verify(notificationService).notify(eq(clerkOne), any(), eq(expectedEmailContent),
        eq(expectedSmsContent), eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
  }

  @Test
  public void notifyClerkShouldIgnoreUsersThatCanNotBeNotified() {
    clerkOne = new UserDtoDataBuilder().denyNotify().buildAsDto();

    approvedRequisitionNotifier.notifyClerks(requisition, locale);

    verify(notificationService, never())
        .notify(eq(clerkOne), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkTwo), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkThree), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkFour), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
  }

  @Test
  public void notifyClerkShouldIgnoreUsersThatAreNotVerified() {
    clerkOne = new UserDtoDataBuilder().asUnverified().buildAsDto();

    approvedRequisitionNotifier.notifyClerks(requisition, locale);

    verify(notificationService, never())
        .notify(eq(clerkOne), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkTwo), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkThree), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkFour), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
  }

  @Test
  public void notifyClerkShouldIgnoreUsersWithoutEmail() {
    clerkOne = new UserDtoDataBuilder().withoutEmail().buildAsDto();

    approvedRequisitionNotifier.notifyClerks(requisition, locale);

    verify(notificationService, never())
        .notify(eq(clerkOne), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkTwo), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkThree), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
    verify(notificationService, times(1))
        .notify(eq(clerkFour), any(), any(), any(),
          eq(ApprovedRequisitionNotifier.NOTIFICATION_TAG));
  }

  private void mockServices() {

    when(facilityReferenceDataService.findOne(facility.getId())).thenReturn(facility);
    when(programReferenceDataService.findOne(program.getId())).thenReturn(program);
    when(periodReferenceDataService.findOne(processingPeriod.getId())).thenReturn(processingPeriod);
    when(requisitionForConvertBuilder.getAvailableSupplyingDepots(eq(requisitionId)))
        .thenReturn(Arrays.asList(warehouseOne, warehouseTwo));
    when(authenticationHelper.getRight(PermissionService.ORDERS_EDIT)).thenReturn(right);
    when(userReferenceDataService.findUsers(
        right.getId(),
        null,
        null,
        warehouseOne.getId())
    ).thenReturn(Arrays.asList(clerkOne, clerkTwo, clerkThree));
    when(userReferenceDataService.findUsers(
        right.getId(),
        null,
        null,
        warehouseTwo.getId()
    )).thenReturn(Arrays.asList(clerkThree, clerkTwo, clerkFour));

    mockMessages();
  }

  private void mockMessages() {
    when(messageService.localize(eq(regularRequisitionMessage), eq(locale)))
        .thenReturn(regularRequisitionMessage.new LocalizedMessage("regular"));
    when(messageService.localize(eq(emergencyRequisitionMessage), eq(locale)))
        .thenReturn(emergencyRequisitionMessage.new LocalizedMessage("emergency"));

    Message requisitionApprovedSubject =
        new Message(REQUISITION_EMAIL_REQUISITION_APPROVED_SUBJECT);
    Message.LocalizedMessage localizedMessage =
        requisitionApprovedSubject.new LocalizedMessage(SUBJECT);
    when(messageService.localize(eq(requisitionApprovedSubject), eq(locale)))
        .thenReturn(localizedMessage);

    Message requisitionApprovedEmailContent =
        new Message(REQUISITION_EMAIL_REQUISITION_APPROVED_CONTENT);
    localizedMessage = requisitionApprovedEmailContent.new LocalizedMessage(EMAIL_CONTENT);
    when(messageService.localize(eq(requisitionApprovedEmailContent), eq(locale)))
        .thenReturn(localizedMessage);

    Message requisitionApprovedSmsContent =
        new Message(REQUISITION_SMS_REQUISITION_APPROVED_CONTENT);
    localizedMessage = requisitionApprovedSmsContent.new LocalizedMessage(SMS_CONTENT);
    when(messageService.localize(eq(requisitionApprovedSmsContent), eq(locale)))
        .thenReturn(localizedMessage);
  }

  private void prepareStatusChange() {
    when(statusChange.getStatus()).thenReturn(RequisitionStatus.APPROVED);
    when(statusChange.getCreatedDate()).thenReturn(createdDate);
  }

  private void prepareRequisition() {
    when(requisition.getId()).thenReturn(requisitionId);
    when(requisition.getFacilityId()).thenReturn(facility.getId());
    when(requisition.getProgramId()).thenReturn(program.getId());
    when(requisition.getProcessingPeriodId()).thenReturn(processingPeriod.getId());
    when(requisition.getEmergency()).thenReturn(false);
    when(requisition.getStatusChanges()).thenReturn(Arrays.asList(statusChange));
  }

}
