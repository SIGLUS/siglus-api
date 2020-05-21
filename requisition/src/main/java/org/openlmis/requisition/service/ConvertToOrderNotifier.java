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

import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_CONVERT_TO_ORDER_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_CONVERT_TO_ORDER_SUBJECT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_SMS_CONVERT_TO_ORDER_CONTENT;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.notification.NotificationService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConvertToOrderNotifier extends BaseNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToOrderNotifier.class);
  static final String NOTIFICATION_TAG = "requisition-convertToOrder";

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  /**
   * Notify requisition's creator that it was converted to order.
   *  @param requisition requisition that was converted
   *
   */
  public void notifyConvertToOrder(Requisition requisition, Locale locale) {
    ProgramDto program = programReferenceDataService.findOne(requisition.getProgramId());
    ProcessingPeriodDto period = periodReferenceDataService.findOne(
        requisition.getProcessingPeriodId());

    List<StatusChange> statusChanges = requisition.getStatusChanges();
    if (statusChanges == null) {
      LOGGER.warn("Could not find requisition audit data to notify for convert to order.");
      return;
    }

    Optional<StatusChange> initiateAuditEntry = statusChanges.stream()
        .filter(statusChange -> statusChange.getStatus() == RequisitionStatus.INITIATED)
        .findFirst();
    if (!initiateAuditEntry.isPresent() || initiateAuditEntry.get().getAuthorId() == null) {
      LOGGER.warn("Could not find requisition initiator to notify for convert to order.");
      return;
    }

    UserDto initiator = userReferenceDataService.findOne(initiateAuditEntry.get().getAuthorId());

    String subject = getMessage(REQUISITION_EMAIL_CONVERT_TO_ORDER_SUBJECT, locale);
    String emailContent = getMessage(REQUISITION_EMAIL_CONVERT_TO_ORDER_CONTENT, locale,
        initiator.getFirstName(), initiator.getLastName(), program.getName(), period.getName());
    String smsContent = getMessage(REQUISITION_SMS_CONVERT_TO_ORDER_CONTENT, locale,
        program.getName(), period.getName());

    notificationService.notify(initiator, subject, emailContent, smsContent, NOTIFICATION_TAG);
  }
}
