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

import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_ACTION_REQUIRED_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_ACTION_REQUIRED_SUBJECT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_SMS_ACTION_REQUIRED_CONTENT;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RightDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.notification.NotificationService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.RightReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisingUsersReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApprovalNotifier extends BaseNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalNotifier.class);
  static final String NOTIFICATION_TAG = "requisition-actionRequired";

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private SupervisingUsersReferenceDataService supervisingUsersReferenceDataService;

  @Autowired
  private RightReferenceDataService rightReferenceDataService;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Value("${requisitionUri}")
  private String requisitionUri;
  
  @Value("${publicUrl}")
  private String publicUrl;

  /**
   * Notify requisition's creator that it was converted to order.
   *
   * @param requisition requisition that was converted
   */
  public void notifyApprovers(Requisition requisition, Locale locale) {
    Collection<UserDto> approvers = getApprovers(requisition);
    String reqType = getMessage(getEmergencyKey(requisition), locale);
    ProcessingPeriodDto period =
        periodReferenceDataService.findOne(requisition.getProcessingPeriodId());
    ProgramDto program = programReferenceDataService.findOne(requisition.getProgramId());
    FacilityDto facility = facilityReferenceDataService.findOne(requisition.getFacilityId());

    List<StatusChange> statusChanges = requisition.getStatusChanges();
    if (statusChanges == null) {
      LOGGER.warn("Could not find requisition audit data to notify for convert to order.");
      return;
    }

    Optional<StatusChange> submitAuditEntry = statusChanges.stream()
        .filter(statusChange -> statusChange.getStatus() == RequisitionStatus.SUBMITTED)
        .findFirst();
    if (!submitAuditEntry.isPresent()) {
      LOGGER.warn("Could not find requisition submitter to notify for requisition status change.");
      return;
    }
    ZonedDateTime submittedDate = submitAuditEntry.get().getCreatedDate();

    String subject = getMessage(REQUISITION_EMAIL_ACTION_REQUIRED_SUBJECT, locale);
    String emailContent = getMessage(REQUISITION_EMAIL_ACTION_REQUIRED_CONTENT, locale);
    String smsContent = getMessage(REQUISITION_SMS_ACTION_REQUIRED_CONTENT, locale);

    DateTimeFormatter dateTimeFormatter = getDateTimeFormatter();

    String url = publicUrl + MessageFormat.format(requisitionUri, requisition.getId());

    Map<String, String> valuesMap =
        getValuesMap(reqType, period, program, facility, submittedDate, dateTimeFormatter, url);

    StrSubstitutor sub = new StrSubstitutor(valuesMap);

    for (UserDto approver : approvers) {
      valuesMap.put("approver", approver.getUsername());
      notificationService.notify(approver, subject,
          sub.replace(emailContent), sub.replace(smsContent), NOTIFICATION_TAG);
    }
  }

  private Map<String, String> getValuesMap(String reqType, ProcessingPeriodDto period,
                                           ProgramDto program, FacilityDto facility,
                                           ZonedDateTime submittedDate,
                                           DateTimeFormatter dateTimeFormatter, String url) {
    Map<String, String> valuesMap = new HashMap<>();
    valuesMap.put("requisitionType", reqType);
    valuesMap.put("submittedDate", submittedDate.format(dateTimeFormatter));
    valuesMap.put("periodName", period.getName());
    valuesMap.put("programName", program.getName());
    valuesMap.put("facilityName", facility.getName());
    valuesMap.put("requisitionUrl", url);
    return valuesMap;
  }

  private Collection<UserDto> getApprovers(Requisition requisition) {
    RightDto right = rightReferenceDataService.findRight(PermissionService.REQUISITION_APPROVE);
    return supervisingUsersReferenceDataService
        .findAll(requisition.getSupervisoryNodeId(), right.getId(), requisition.getProgramId());
  }
}
