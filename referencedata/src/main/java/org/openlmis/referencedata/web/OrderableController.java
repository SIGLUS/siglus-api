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

import static org.openlmis.referencedata.domain.RightName.ORDERABLES_MANAGE;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.service.OrderableService;
import org.openlmis.referencedata.util.OrderableBuilder;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.UuidUtil;
import org.openlmis.referencedata.util.messagekeys.OrderableMessageKeys;
import org.openlmis.referencedata.validate.OrderableValidator;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderableController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(OrderableController.class);
  public static final String RESOURCE_PATH = "/orderables";

  @Autowired
  private OrderableRepository repository;

  @Autowired
  private OrderableService orderableService;

  @Autowired
  private OrderableBuilder orderableBuilder;

  @Autowired
  private OrderableValidator validator;

  /**
   * Create an orderable.
   *
   * @return the orderable that was created.
   */
  @Transactional(isolation = Isolation.SERIALIZABLE)
  @PutMapping(RESOURCE_PATH)
  public ResponseEntity<OrderableDto> create(
      @RequestBody OrderableDto orderableDto,
      BindingResult bindingResult) {

    Profiler profiler = new Profiler("ORDERABLES_CREATE_PUT");
    profiler.setLogger(XLOGGER);

    rightService.checkAdminRight(ORDERABLES_MANAGE);

    profiler.start("VALIDATE_ORDERABLE");
    validator.validate(orderableDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    profiler.start("BUILD_ORDERABLE");
    Orderable orderable = orderableBuilder.newOrderable(orderableDto, null);

    profiler.start("SAVE_ORDERABLE");
    repository.save(orderable);

    profiler.stop().log();

    return ResponseEntity.ok()
            .headers(buildLastModifiedHeader(orderable.getLastUpdated()))
            .body(OrderableDto.newInstance(orderable));
  }

  /**
   * Update an Orderable.
   *
   * @param id the id of the Orderable to update.
   * @param orderableDto the contents of how the Orderable should be updated.
   * @param bindingResult the result of validation.
   * @return the orderable that was updated.
   */
  @Transactional(isolation = Isolation.SERIALIZABLE)
  @PutMapping(RESOURCE_PATH + "/{id}")
  public ResponseEntity<OrderableDto> update(
      @PathVariable("id") UUID id,
      @RequestBody OrderableDto orderableDto,
      BindingResult bindingResult) {

    rightService.checkAdminRight(ORDERABLES_MANAGE);

    if (!UuidUtil.sameId(id, orderableDto.getId())) {
      throw new ValidationMessageException(OrderableMessageKeys.ERROR_ID_MISMATCH);
    }

    validator.validate(orderableDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);
    Orderable foundOrderable = repository.findFirstByIdentityIdOrderByIdentityVersionNumberDesc(id);

    if (null == foundOrderable) {
      throw new NotFoundException(OrderableMessageKeys.ERROR_NOT_FOUND);
    }

    Orderable savedOrderable = repository
        .save(orderableBuilder.newOrderable(orderableDto, foundOrderable));
    XLOGGER.warn("Orderable updated: down stream services may not support versioned orderables: {}",
        id);

    return ResponseEntity.ok()
            .headers(buildLastModifiedHeader(savedOrderable.getLastUpdated()))
            .body(OrderableDto.newInstance(savedOrderable));
  }

  /**
   * Finds orderables matching all of the provided parameters. If no params provided, returns all.
   * If provided invalid param, throws {@link ValidationMessageException}. If provided request
   * param doesn't have value, it will search for empty value in database.
   *
   * @param queryParams request parameters (code, name, program, ids).
   * @param pageable object used to encapsulate the pagination related values: page and size.
   * @return a page of orderables
   */
  @GetMapping(RESOURCE_PATH)
  public ResponseEntity<Page<OrderableDto>> findAll(
      @RequestParam MultiValueMap<String, Object> queryParams,
      @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedDate,
      Pageable pageable) {
    XLOGGER.entry(queryParams, pageable);
    Profiler profiler = new Profiler("ORDERABLES_SEARCH");
    profiler.setLogger(XLOGGER);

    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    ZonedDateTime lastUpdated = orderableService.getLatestLastUpdatedDate(searchParams, profiler);

    if (lastUpdated == null) {
      Page<OrderableDto> emptyPage = Pagination.getPage(Collections.emptyList(), pageable);
      return ResponseEntity.ok()
              .body(emptyPage);
    }

    if (ifModifiedDate == null
        || wasModifiedSince(lastUpdated, parseHttpDateToZonedDateTime(ifModifiedDate))) {
      XLOGGER.info("search orderable query params: {}", queryParams);

      profiler.start("ORDERABLE_SERVICE_SEARCH");
      Page<Orderable> orderablesPage =
          orderableService.searchOrderables(searchParams, pageable);

      profiler.start("ORDERABLE_PAGINATION");
      Page<OrderableDto> page = Pagination.getPage(
          OrderableDto.newInstance(orderablesPage.getContent()),
          pageable,
          orderablesPage.getTotalElements());

      profiler.stop().log();
      XLOGGER.exit(page);
      return ResponseEntity.ok()
          .headers(buildLastModifiedHeader(lastUpdated))
          .body(page);
    } else {
      profiler.stop().log();
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .headers(buildLastModifiedHeader(lastUpdated))
          .build();
    }
  }

  /**
   * Search orderables by search criteria.
   *
   * @param body - specify criteria for orderables.
   * @return a page of orderables matching the criteria
   */
  @PostMapping(RESOURCE_PATH + "/search")
  public ResponseEntity<Page<OrderableDto>> searchOrderables(
      @RequestBody OrderableSearchParams body,
      @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
          String ifModifiedDate) {
    Profiler profiler = new Profiler("ORDERABLES_SEARCH_POST");
    profiler.setLogger(XLOGGER);

    Pageable pageable = body.getPageable();

    profiler.start("GET_LATEST_LAST_UPDATED_DATE");
    ZonedDateTime lastUpdated = orderableService
            .getLatestLastUpdatedDate(getQueryOrderableSearchParams(body), profiler);

    if (lastUpdated == null) {
      Page<OrderableDto> emptyPage = Pagination.getPage(Collections.emptyList(), pageable);
      return ResponseEntity.ok()
              .body(emptyPage);
    }

    if (ifModifiedDate == null
        || wasModifiedSince(lastUpdated, parseHttpDateToZonedDateTime(ifModifiedDate))) {
      profiler.start("SEARCH_ORDERABLES");
      Page<Orderable> orderablesPage = repository.search(body, pageable);

      profiler.start("EXPORT_TO_DTO");
      Page<OrderableDto> page = Pagination.getPage(
          OrderableDto.newInstance(orderablesPage.getContent()),
          pageable,
          orderablesPage.getTotalElements());

      profiler.stop().log();
      XLOGGER.exit(page);
      return ResponseEntity.ok()
          .headers(buildLastModifiedHeader(lastUpdated))
          .body(page);
    } else {
      profiler.stop().log();
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .headers(buildLastModifiedHeader(lastUpdated))
          .build();
    }
  }

  /**
   * Finds product with chosen id.
   *
   * @param productId id of the chosen product
   * @return chosen product
   */
  @GetMapping(RESOURCE_PATH + "/{id}")
  public ResponseEntity<OrderableDto> getChosenOrderable(
      @PathVariable("id") UUID productId,
      @RequestParam(required = false, value = "versionNumber") Long versionNumber,
      @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false)
          String ifModifiedDate) {
    Profiler profiler = new Profiler("GET_ORDERABLE");
    profiler.setLogger(XLOGGER);

    Orderable orderable;
    if (null == versionNumber) {
      profiler.start("FIND_ORDERABLE_BY_IDENTITY_ID");
      orderable = repository.findFirstByIdentityIdOrderByIdentityVersionNumberDesc(productId);
    } else {
      profiler.start("FIND_ORDERABLE_BY_IDENTITY_ID_AND_VERSION_NAME");
      orderable = repository.findByIdentityIdAndIdentityVersionNumber(productId, versionNumber);
    }

    profiler.stop().log();
    if (orderable == null) {
      throw new NotFoundException(OrderableMessageKeys.ERROR_NOT_FOUND);
    } else {
      return (ifModifiedDate == null
          || orderable.wasModifiedSince(parseHttpDateToZonedDateTime(ifModifiedDate)))
          ? ResponseEntity.ok()
              .headers(buildLastModifiedHeader(orderable.getLastUpdated()))
              .body(OrderableDto.newInstance(orderable))
          : ResponseEntity.status(HttpStatus.NOT_MODIFIED)
              .headers(buildLastModifiedHeader(orderable.getLastUpdated()))
              .build();
    }
  }

  /**
   * Get the audit information related to orderable.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getOrderableAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    rightService.checkAdminRight(ORDERABLES_MANAGE);

    //Return a 404 if the specified instance can't be found
    if (!repository.existsById(id)) {
      throw new NotFoundException(OrderableMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(Orderable.class, id, author, changedPropertyName, page, returnJson);
  }

  private HttpHeaders buildLastModifiedHeader(ZonedDateTime lastUpdated) {
    HttpHeaders headers = new HttpHeaders();
    headers.setLastModified(lastUpdated.toInstant().toEpochMilli());
    return headers;
  }

  public boolean wasModifiedSince(ZonedDateTime lastUpdated, ZonedDateTime date) {
    return date == null || lastUpdated == null || lastUpdated.isAfter(date);
  }

  private QueryOrderableSearchParams getQueryOrderableSearchParams(
      OrderableSearchParams searchParams) {

    Set<UUID> ids = searchParams.getIdentityPairs()
            .stream()
            .map(Pair::getKey)
            .collect(Collectors.toSet());

    LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();

    if (!ids.isEmpty()) {
      for (UUID id : ids) {
        queryMap.add("id", id.toString());
      }
    }
    queryMap.add("name", searchParams.getName());
    queryMap.add("code", searchParams.getCode());
    queryMap.add("program", searchParams.getProgramCode());

    return new QueryOrderableSearchParams(queryMap);
  }
}
