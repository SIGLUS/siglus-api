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

import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_REQUISITION_APPROVED_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_REQUISITION_APPROVED_SUBJECT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_SMS_REQUISITION_APPROVED_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_TYPE_EMERGENCY;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_TYPE_REGULAR;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.requisition.domain.BaseTimestampedEntity;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.notification.NotificationService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.requisition.web.RequisitionForConvertBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApprovedRequisitionNotifier extends BaseNotifier {
  
  static final String NOTIFICATION_TAG = "requisition-requisitionApproved";

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private RequisitionForConvertBuilder requisitionForConvertBuilder;
  
  @Value("${publicUrl}")
  private String publicUrl;

  /**
   * Notifies all the clerks that the requisition has been approved and is ready to be converted to
   * order.
   *
   * @param requisition  the requisition to notify the clerks for
   */
  public void notifyClerks(Requisition requisition, Locale locale) {
    String subject = getMessage(REQUISITION_EMAIL_REQUISITION_APPROVED_SUBJECT, locale);
    String emailContent = getMessage(REQUISITION_EMAIL_REQUISITION_APPROVED_CONTENT, locale);
    String smsContent = getMessage(REQUISITION_SMS_REQUISITION_APPROVED_CONTENT, locale);

    Map<String, String> messageParams = new HashMap<>();
    messageParams.put("requisitionType", getRequisitionType(requisition, locale));
    messageParams.put("finalApprovalDate", getFinalApprovalDate(requisition));
    messageParams.put("facility", getFacilityName(requisition));
    messageParams.put("url", getConvertToOrderUrl());
    messageParams.put("program", getProgram(requisition));
    messageParams.put("period", getPeriod(requisition));

    for (UserDto user : getClerks(requisition)) {
      messageParams.put("user", user.getUsername());
      notificationService.notify(user, subject,
          new StrSubstitutor(messageParams).replace(emailContent),
          new StrSubstitutor(messageParams).replace(smsContent), NOTIFICATION_TAG);
    }
  }

  private String getConvertToOrderUrl() {
    return publicUrl + "/#!/requisitions/convertToOrder";
  }

  private String getRequisitionType(Requisition requisition, Locale locale) {
    return getMessage(
        requisition.getEmergency() ? REQUISITION_TYPE_EMERGENCY : REQUISITION_TYPE_REGULAR,
        locale);
  }

  private String getFinalApprovalDate(Requisition requisition) {
    ZonedDateTime approvedDate = requisition.getStatusChanges().stream()
        .filter(statusChange -> statusChange.getStatus() == RequisitionStatus.APPROVED)
        .map(BaseTimestampedEntity::getCreatedDate)
        .findFirst()
        .orElse(ZonedDateTime.now());

    return approvedDate.format(getDateTimeFormatter());
  }

  private String getFacilityName(Requisition requisition) {
    return facilityReferenceDataService.findOne(requisition.getFacilityId()).getName();
  }

  private String getProgram(Requisition requisition) {
    return programReferenceDataService.findOne(requisition.getProgramId()).getName();
  }

  private String getPeriod(Requisition requisition) {
    return periodReferenceDataService.findOne(requisition.getProcessingPeriodId()).getName();
  }

  private Set<UserDto> getClerks(Requisition requisition) {
    UUID rightId = authenticationHelper.getRight(PermissionService.ORDERS_EDIT).getId();
    Set<UserDto> users = new HashSet<>();

    requisitionForConvertBuilder.getAvailableSupplyingDepots(requisition.getId())
        .forEach(warehouse -> users.addAll(userReferenceDataService.findUsers(
            rightId,
            null,
            null,
            warehouse.getId())));

    return new HashSet<>(users);
  }
}
