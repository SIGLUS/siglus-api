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

package org.openlmis.referencedata.web;

import static org.openlmis.referencedata.domain.RightName.SYSTEM_NOTIFICATIONS_MANAGE;
import static org.openlmis.referencedata.web.SystemNotificationController.RESOURCE_PATH;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.SystemNotification;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.dto.SystemNotificationDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.SystemNotificationRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.SystemNotificationMessageKeys;
import org.openlmis.referencedata.validate.SystemNotificationValidator;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Transactional
@RestController
@RequestMapping(RESOURCE_PATH)
public class SystemNotificationController extends BaseController {

  public static final String RESOURCE_PATH = BaseController.API_PATH + "/systemNotifications";
  public static final String ID_URL = "/{id}";

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(SystemNotification.class);
  private static final String AUDIT_LOG_URL = ID_URL + "/auditLog";

  @Autowired
  private SystemNotificationRepository systemNotificationRepository;

  @Autowired
  private SystemNotificationValidator systemNotificationValidator;

  @Autowired
  private UserRepository userRepository;

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Retrieves all system notifications.
   *
   * @return List of system notifications.
   */
  @GetMapping
  public Page<SystemNotificationDto> getSystemNotifications(
      @RequestParam MultiValueMap<String, Object> requestParams, Pageable pageable) {
    Profiler profiler = new Profiler("GET_SYSTEM_NOTIFICATIONS");
    profiler.setLogger(XLOGGER);

    profiler.start("FIND_SYSTEM_NOTIFICATIONS");
    SystemNotificationSearchParams searchParams =
        new SystemNotificationSearchParams(requestParams);
    Page<SystemNotification> notifications =
        systemNotificationRepository.search(searchParams, pageable);

    profiler.start("CREATE_FINAL_RESULT_PAGE");
    Page<SystemNotificationDto> page =
        exportToDtosWithExpand(notifications, pageable, searchParams.getExpand(),
            searchParams.getIsDisplayed());

    profiler.stop().log();
    return page;
  }

  /**
   * Get chosen system notification.
   *
   * @param id UUID of system notification which we want to get
   * @return system notification.
   */
  @GetMapping(ID_URL)
  public SystemNotificationDto getSystemNotification(@PathVariable("id") UUID id) {
    Profiler profiler = new Profiler("GET_SYSTEM_NOTIFICATION");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SYSTEM_NOTIFICATIONS_MANAGE, profiler);

    SystemNotification notification = findSystemNotification(id, profiler);

    SystemNotificationDto dto = toDto(notification, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Allows creating new system notification. If the id is specified, it will be ignored.
   *
   * @param systemNotificationDto A system notification bound to the request body.
   * @return created system notification.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SystemNotificationDto createSystemNotification(
      @RequestBody SystemNotificationDto systemNotificationDto, BindingResult bindingResult) {
    Profiler profiler = new Profiler("CREATE_SYSTEM_NOTIFICATION");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SYSTEM_NOTIFICATIONS_MANAGE, profiler);

    profiler.start("BUILD_SYSTEM_NOTIFICATION_FROM_DTO");
    if (null != systemNotificationDto.getId()) {
      throw new ValidationMessageException(SystemNotificationMessageKeys.ERROR_ID_PROVIDED);
    }

    SystemNotificationDto dto = validateAndSave(systemNotificationDto, bindingResult, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Allows updating an existing system notification or create a new one with the given id value.
   *
   * @param id UUID of system notification which we want to update.
   * @param systemNotificationDto A system notification bound to the request body.
   * @return the updated/created system notification.
   */
  @PutMapping(ID_URL)
  public SystemNotificationDto updateSystemNotification(@PathVariable("id") UUID id,
      @RequestBody SystemNotificationDto systemNotificationDto, BindingResult bindingResult) {
    Profiler profiler = new Profiler("UPDATE_SYSTEM_NOTIFICATION");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SYSTEM_NOTIFICATIONS_MANAGE, profiler);

    profiler.start("BUILD_SYSTEM_NOTIFICATION_FROM_DTO");
    if (null != systemNotificationDto.getId()
        && !Objects.equals(systemNotificationDto.getId(), id)) {
      throw new ValidationMessageException(SystemNotificationMessageKeys.ERROR_ID_MISMATCH);
    }

    SystemNotificationDto dto = validateAndSave(systemNotificationDto, bindingResult, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Allows deleting system notification.
   *
   * @param id UUID of system notification which we want to delete
   */
  @DeleteMapping(ID_URL)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSystemNotification(@PathVariable UUID id) {
    Profiler profiler = new Profiler("DELETE_SYSTEM_NOTIFICATION");
    profiler.setLogger(XLOGGER);

    rightService.checkAdminRight(SYSTEM_NOTIFICATIONS_MANAGE);

    SystemNotification notification = findSystemNotification(id, profiler);

    systemNotificationRepository.delete(notification);

    profiler.stop().log();
  }

  /**
   * Get the audit information related to system notifications.
   *
   * @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *                            If null or empty, changes associated with any and all properties
   *                            are returned.
   * @param pageable A Pageable object that allows client to optionally add "page" (page number)
   *                 and "size" (page size) query parameters to the request.
   */
  @GetMapping(AUDIT_LOG_URL)
  public ResponseEntity<String> getAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable pageable) {

    Profiler profiler = new Profiler("GET_AUDIT_LOG");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SYSTEM_NOTIFICATIONS_MANAGE, profiler);
    findSystemNotification(id, profiler);

    profiler.start("GET_AUDIT_LOG");
    ResponseEntity<String> response = getAuditLogResponse(
        SystemNotification.class, id, author, changedPropertyName, pageable, returnJson
    );

    profiler.stop().log();
    return response;
  }

  private SystemNotification findSystemNotification(UUID id, Profiler profiler) {
    profiler.start("FIND_SYSTEM_NOTIFICATION");
    SystemNotification systemNotification = systemNotificationRepository.findOne(id);

    if (systemNotification == null) {
      profiler.stop().log();
      throw new NotFoundException(
          new Message(SystemNotificationMessageKeys.ERROR_NOT_FOUND_WITH_ID, id));
    }

    return systemNotification;
  }

  private List<SystemNotification> notificationsToDisplay(Pageable pageable) {
    MultiValueMap<String, Object> paramsMap = new LinkedMultiValueMap<>();
    paramsMap.add("isDisplayed", true);
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(paramsMap);
    Page<SystemNotification> page = systemNotificationRepository.search(params, pageable);

    return page.getContent();
  }

  private SystemNotificationDto validateAndSave(SystemNotificationDto systemNotificationDto,
      BindingResult bindingResult, Profiler profiler) {
    profiler.start("SYSTEM_NOTIFICATION_VALIDATION");
    systemNotificationValidator.validate(systemNotificationDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    profiler.start("BUILD_SYSTEM_NOTIFICATION");
    User author = userRepository.findOne(systemNotificationDto.getAuthorId());
    SystemNotification systemNotification =
        SystemNotification.newInstance(systemNotificationDto, author);

    profiler.start("SAVE_SYSTEM_NOTIFICATION");
    systemNotificationRepository.save(systemNotification);

    return toDto(systemNotification, profiler);
  }

  private SystemNotificationDto toDto(SystemNotification notification, Profiler profiler) {
    profiler.start("EXPORT_SYSTEM_NOTIFICATION_TO_DTO");
    return SystemNotificationDto.newInstance(notification, serviceUrl, null);
  }

  private SystemNotificationDto exportToDtoWithExpand(SystemNotification systemNotification,
      Set<String> expand, Boolean isDisplayed) {
    SystemNotificationDto systemNotificationDto = null;

    if (systemNotification != null) {
      systemNotificationDto = SystemNotificationDto.newInstance(systemNotification, serviceUrl,
          isDisplayed);
      expandDto(systemNotificationDto, systemNotification, expand);
    }

    return systemNotificationDto;
  }

  private Page<SystemNotificationDto> exportToDtosWithExpand(Page<SystemNotification> page,
      Pageable pageable, Set<String> expand, Boolean isDisplayed) {

    List<SystemNotificationDto> list = page.getContent().stream()
        .map(systemNotification -> exportToDtoWithExpand(systemNotification, expand,
            verifyIfShouldBeDisplayed(isDisplayed, page, pageable, systemNotification)))
        .collect(Collectors.toList());
    return Pagination.getPage(list, pageable, page.getTotalElements());
  }

  private Boolean verifyIfShouldBeDisplayed(Boolean isDisplayed, Page<SystemNotification> page,
      Pageable pageable, SystemNotification notification) {
    if (isDisplayed != null) {
      if (isDisplayed) {
        return page.getContent().contains(notification);
      } else {
        return !page.getContent().contains(notification);
      }
    }
    return notificationsToDisplay(pageable).contains(notification);
  }
}
