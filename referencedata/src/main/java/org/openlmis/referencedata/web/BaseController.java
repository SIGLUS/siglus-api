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

import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_FILE_IS_EMPTY;
import static org.openlmis.referencedata.util.messagekeys.CsvUploadMessageKeys.ERROR_INCORRECT_FILE_FORMAT;
import static org.openlmis.referencedata.web.BaseController.API_PATH;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.javers.core.changelog.SimpleTextChangeLog;
import org.javers.core.diff.Change;
import org.javers.core.json.JsonConverter;
import org.javers.repository.jql.QueryBuilder;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.service.ObjectReferenceExpander;
import org.openlmis.referencedata.service.RightService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping(API_PATH)
public abstract class BaseController {

  public static final DateTimeFormatter RFC_7231_FORMAT =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withZone(ZoneId.of("GMT"));
  public static final String API_PATH = "/api";

  @Autowired
  RightService rightService;

  @Resource(name = "javersProvider")
  private Javers javers;

  @Autowired
  private ObjectReferenceExpander objectReferenceExpander;

  protected void checkAdminRight(String rightName, Profiler profiler) {
    checkAdminRight(rightName, true, null, profiler);
  }

  protected void checkAdminRight(String rightName, boolean allowServiceTokens, Profiler profiler) {
    checkAdminRight(rightName, allowServiceTokens, null, profiler);
  }

  protected void checkAdminRight(String rightName, boolean allowServiceTokens, UUID expectedUserId,
      Profiler profiler) {
    profiler.start("CHECK_ADMIN");
    rightService.checkAdminRight(rightName, allowServiceTokens, expectedUserId);
  }

  protected <T> Page<T> toPage(List<T> originalList, Pageable pageable, Profiler profiler) {
    profiler.start("CREATE_PAGE");
    return Pagination.getPage(originalList, pageable);
  }

  protected <T> Page<T> toPage(List<T> originalList, Pageable pageable, long totalElements,
                               Profiler profiler) {
    profiler.start("CREATE_PAGE");
    return Pagination.getPage(originalList, pageable, totalElements);
  }

  protected ResponseEntity<String> getAuditLogResponse(Class type, UUID id, String author,
                                                       String changedPropertyName,
                                                       Pageable page, boolean returnJson) {
    String auditLogs = getAuditLog(type, id, author, changedPropertyName, page, returnJson);

    MediaType contentType = returnJson ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(contentType);

    return new ResponseEntity<>(auditLogs, headers, HttpStatus.OK);
  }

  /**
   * <p>
   * Convenience method intended to return audit log information via either JSON or raw text,
   * based on the value of the returnJson argument.
   * </p><p>
   * For testing and development, it’s useful to set this to false in order to retrieve a
   * human-readable response. For production, however, JSON should exclusively be returned.
   * </p>
   * See getAuditedChanges() for a list and explanation of the available parameters.
   */
  protected String getAuditLog(Class type, UUID id, String author, String changedPropertyName,
                               Pageable page, boolean returnJson) {
    if (returnJson) {
      return getAuditLogJson(type, id, author, changedPropertyName, page);
    } else {
      return getAuditLogText(type, id, author, changedPropertyName, page);
    }
  }

  /**
   * Return a list of audited changes via JSON.
   * @param type The type of class for which we wish to retrieve historical changes.
   */
  protected String getAuditLogJson(Class type) {
    return getAuditLogJson(type, null, null, null, null);
  }

  protected String getAuditLogJson(Class type, UUID id) {
    return getAuditLogJson(type, id, null, null, null);
  }

  /**
   * Return a list of changes via JSON.
   *
   * @param type The type of class for which we wish to retrieve historical changes.
   * @param id The ID of class for which we wish to retrieve historical changes.
   *           If null, entries are returned regardless of their ID.
   * @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object with PageNumber and PageSize values used for pagination.
   */
  protected String getAuditLogJson(Class type, UUID id, String author,
                                     String changedPropertyName, Pageable page) {
    List<Change> changes = getChangesByType(type, id, author, changedPropertyName, page);
    JsonConverter jsonConverter = javers.getJsonConverter();
    return jsonConverter.toJson(changes);
  }


  /**
   * Return a list of changes as a log (in other words, as a series of line entries).
   * @param type The type of class for which we wish to retrieve historical changes.
   */
  protected String getAuditLogText(Class type) {
    return getAuditLogText(type, null, null, null, null);
  }

  /**
   * Return a list of changes as a log (in other words, as a series of line entries).
   * @param type The type of class for which we wish to retrieve historical changes.
   * @param id The ID of class for which we wish to retrieve historical changes.
   *           If null, entries are returned regardless of their ID.
   */
  protected String getAuditLogText(Class type, UUID id) {
    return getAuditLogText(type, id, null, null, null);
  }

  /**
   * Return a list of changes as a log (in other words, as a series of line entries).
   * The available parameters and their means are the same as for the getChangesByClass() method.
   */
  protected String getAuditLogText(Class type, UUID id, String author,
                                   String changedPropertyName, Pageable page) {
    List<Change> changes = getChangesByType(type, id, author, changedPropertyName, page);
    return javers.processChangeList(changes, new SimpleTextChangeLog());
  }

  protected void expandDto(Object dto, BaseEntity entity, Set<String> expands) {
    objectReferenceExpander.expandDto(dto, entity, expands);
  }

  protected ZonedDateTime parseHttpDateToZonedDateTime(String httpDate) {
    return ZonedDateTime.parse(httpDate, RFC_7231_FORMAT);
  }

  void validateCsvFile(MultipartFile csvFile) {
    if (csvFile == null || csvFile.isEmpty()) {
      throw new ValidationMessageException(ERROR_FILE_IS_EMPTY);
    } else if (!csvFile.getOriginalFilename().endsWith(".csv")) {
      throw new ValidationMessageException(ERROR_INCORRECT_FILE_FORMAT);
    }
  }

  void throwValidationMessageExceptionIfErrors(BindingResult bindingResult) {
    if (bindingResult.getErrorCount() > 0) {
      throw new ValidationMessageException(new Message(bindingResult.getFieldError().getCode(),
          bindingResult.getFieldError().getArguments()));
    }
  }

  /*
    Return JaVers changes for the specified type, optionally filtered by id, author, and property.
  */
  private List<Change> getChangesByType(Class type, UUID id, String author,
                                        String changedPropertyName, Pageable page) {
    QueryBuilder queryBuilder;

    if (id != null) {
      queryBuilder = QueryBuilder.byInstanceId(id, type);
    } else {
      queryBuilder = QueryBuilder.byClass(type);
    }

    int skip = Pagination.getPageNumber(page);
    int limit = Pagination.getPageSize(page);

    queryBuilder = queryBuilder.withNewObjectChanges(true).skip(skip).limit(limit);

    if (StringUtils.isNotBlank(author)) {
      queryBuilder = queryBuilder.byAuthor(author);
    }
    if (StringUtils.isNotBlank(changedPropertyName)) {
      queryBuilder = queryBuilder.andProperty(changedPropertyName);
    }

    /* Depending on the business' preference, we can either use findSnapshots() or findChanges().
       Whereas the former returns the entire state of the object as it was at each commit, the later
       returns only the property and values which changed. */
    List<Change> changes = javers.findChanges(queryBuilder.build());

    changes.sort((o1, o2) -> -1 * o1.getCommitMetadata().get().getCommitDate()
                                  .compareTo(o2.getCommitMetadata().get().getCommitDate()));
    return changes;
  }
}
