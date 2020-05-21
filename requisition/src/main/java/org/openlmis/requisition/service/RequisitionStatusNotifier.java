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

import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_STATUS_UPDATE_CONTENT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_EMAIL_STATUS_UPDATE_SUBJECT;
import static org.openlmis.requisition.i18n.MessageKeys.REQUISITION_SMS_STATUS_UPDATE_CONTENT;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang.text.StrSubstitutor;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.StatusChange;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.service.notification.NotificationService;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.service.referencedata.UserReferenceDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequisitionStatusNotifier extends BaseNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequisitionStatusNotifier.class);
  static final String NOTIFICATION_TAG = "requisition-statusUpdate";

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private PeriodReferenceDataService periodReferenceDataService;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Value("${requisitionUri}")
  private String requisitionUri;
  
  @Value("${publicUrl}")
  private String publicUrl;

  /**
   * Notify user(s) that the requisition's status has changed.
   *
   * @param requisition a requisition that has just changed its status
   */
  public void notifyStatusChanged(Requisition requisition, Locale locale) {

    List<StatusChange> statusChanges = requisition.getStatusChanges();
    if (statusChanges == null) {
      LOGGER.error("Could not find status changes for requisition {} to "
          + "notify for requisition status change.", requisition.getId());
      return;
    }

    UserDto initiator = getInitiator(statusChanges, requisition.getId());

    Optional<StatusChange> submitAuditEntry = getSubmitAuditEntry(requisition, statusChanges);
    if (!submitAuditEntry.isPresent()) {
      return;
    }

    Optional<StatusChange> currentAuditEntry = getCurrentAuditEntry(requisition, statusChanges);
    if (!currentAuditEntry.isPresent()) {
      return;
    }
    DateTimeFormatter dateTimeFormatter = getDateTimeFormatter();

    Map<String, String> valuesMap = new HashMap<>();
    valuesMap.put("initiator", initiator.getUsername());
    valuesMap.put("requisitionType", getMessage(getEmergencyKey(requisition), locale));
    valuesMap.put("submittedDate", submitAuditEntry.get().getCreatedDate()
        .format(dateTimeFormatter));
    valuesMap.put("programName", getProgram(requisition).getName());
    valuesMap.put("periodName", getPeriod(requisition).getName());
    valuesMap.put("facilityName", getFacility(requisition).getName());
    valuesMap.put("requisitionStatus", requisition.getStatus().toString());
    valuesMap.put("author", getAuthor(currentAuditEntry.get()).getUsername());
    valuesMap.put("changeDate", currentAuditEntry.get().getCreatedDate().format(
        dateTimeFormatter));
    valuesMap.put("requisitionUrl", getRequisitionUrl(requisition));

    String subject = getMessage(REQUISITION_EMAIL_STATUS_UPDATE_SUBJECT, locale);
    String emailContent = getMessage(REQUISITION_EMAIL_STATUS_UPDATE_CONTENT, locale);
    String smsContent = getMessage(REQUISITION_SMS_STATUS_UPDATE_CONTENT, locale);

    StrSubstitutor sub = new StrSubstitutor(valuesMap);

    notificationService.notify(initiator, subject, sub.replace(emailContent),
        sub.replace(smsContent), NOTIFICATION_TAG);
  }

  private UserDto getInitiator(List<StatusChange> statusChanges, UUID requisitionId) {
    UUID initiatorId = getInitiatorId(statusChanges);
    if (initiatorId == null) {
      LOGGER.warn("Could not find initiator for requisition %s to notify "
          + "for requisition status change.", requisitionId);
      return null;
    }
    return userReferenceDataService.findOne(initiatorId);
  }

  private UUID getInitiatorId(List<StatusChange> statusChanges) {
    for (StatusChange statusChange : statusChanges) {
      if (statusChange.getStatus() == RequisitionStatus.INITIATED) {
        return statusChange.getAuthorId();
      }
    }
    return null;
  }

  private String getRequisitionUrl(Requisition requisition) {
    return publicUrl + MessageFormat.format(requisitionUri, requisition.getId());
  }

  private Optional<StatusChange> getSubmitAuditEntry(Requisition requisition,
                                                     List<StatusChange> statusChanges) {
    Optional<StatusChange> submitAuditEntry = statusChanges.stream()
        .filter(statusChange -> statusChange.getStatus() == RequisitionStatus.SUBMITTED)
        .findFirst();
    if (!submitAuditEntry.isPresent()) {
      LOGGER.warn("Could not find submitter for requisition " + requisition.getId() + " to notify "
          + "for requisition status change.");
      return Optional.empty();
    }
    return submitAuditEntry;
  }

  private Optional<StatusChange> getCurrentAuditEntry(Requisition requisition,
                                                      List<StatusChange> statusChanges) {
    Optional<StatusChange> currentAuditEntry = statusChanges.stream()
        .filter(statusChange -> statusChange.getStatus() == requisition.getStatus())
        .max(Comparator.comparing(StatusChange::getCreatedDate));

    if (!currentAuditEntry.isPresent() || currentAuditEntry.get().getAuthorId() == null) {
      LOGGER.warn("Could not find author of current status change for requisition "
          + requisition.getId() + " to notify for requisition status change.");
      return Optional.empty();
    }
    return currentAuditEntry;
  }

  private ProgramDto getProgram(Requisition requisition) {
    return programReferenceDataService.findOne(requisition.getProgramId());
  }

  private ProcessingPeriodDto getPeriod(Requisition requisition) {
    return periodReferenceDataService.findOne(
        requisition.getProcessingPeriodId());
  }

  private FacilityDto getFacility(Requisition requisition) {
    return facilityReferenceDataService.findOne(requisition.getFacilityId());
  }

  private UserDto getAuthor(StatusChange currentAuditEntry) {
    return userReferenceDataService.findOne(currentAuditEntry.getAuthorId());
  }
}
