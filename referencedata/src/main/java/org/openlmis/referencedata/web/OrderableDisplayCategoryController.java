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
import static org.openlmis.referencedata.dto.OrderableDisplayCategoryDto.newInstance;

import java.util.Collections;
import java.util.UUID;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;
import org.openlmis.referencedata.dto.OrderableDisplayCategoryDto;
import org.openlmis.referencedata.exception.IntegrityViolationException;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.OrderableDisplayCategoryRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.OrderableDisplayCategoryMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class OrderableDisplayCategoryController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      OrderableDisplayCategoryController.class);

  @Autowired
  private OrderableDisplayCategoryRepository orderableDisplayCategoryRepository;

  /**
   * Get all OrderableDisplayCategories.
   *
   * @return the OrderableDisplayCategories.
   */
  @RequestMapping(value = "/orderableDisplayCategories", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<OrderableDisplayCategoryDto> getAllOrderableDisplayCategories() {

    return newInstance(orderableDisplayCategoryRepository.findAll());
  }

  /**
   * Create a {@link OrderableDisplayCategory}.
   *
   * @param orderableDisplayCategoryDto a OrderableDisplayCategory bound to the request body.
   * @return the created OrderableDisplayCategory with id.
   */
  @RequestMapping(value = "/orderableDisplayCategories", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public OrderableDisplayCategoryDto createOrderableDisplayCategory(
      @RequestBody OrderableDisplayCategoryDto orderableDisplayCategoryDto) {
    OrderableDisplayCategory orderableDisplayCategory =
        OrderableDisplayCategory.newInstance(orderableDisplayCategoryDto);
    rightService.checkAdminRight(ORDERABLES_MANAGE);

    OrderableDisplayCategory found =
        orderableDisplayCategoryRepository.findByCode(orderableDisplayCategory.getCode());
    if (null != found) {
      found.updateFrom(orderableDisplayCategory);
    } else {
      found = orderableDisplayCategory;
    }

    orderableDisplayCategoryRepository.save(found);
    return newInstance(found);
  }

  /**
   * Updates the given {@link OrderableDisplayCategory}.
   * Uses the ID given to base it's update and ignores the code given.
   *
   * @param orderableDisplayCategoryDto   An OrderableDisplayCategory bound to the request body.
   * @param orderableDisplayCategoryId UUID of the OrderableDisplayCategory which we want to update.
   * @return the updated OrderableDisplayCategory.
   */
  @RequestMapping(value = "/orderableDisplayCategories/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public OrderableDisplayCategoryDto updateOrderableDisplayCategory(
      @RequestBody OrderableDisplayCategoryDto orderableDisplayCategoryDto,
      @PathVariable("id") UUID orderableDisplayCategoryId) {
    rightService.checkAdminRight(ORDERABLES_MANAGE);
    LOGGER.debug("Updating orderableDisplayCategory with id: %s", orderableDisplayCategoryId);

    OrderableDisplayCategory orderableDisplayCategoryToUpdate =
        orderableDisplayCategoryRepository.findOne(orderableDisplayCategoryId);

    if (null == orderableDisplayCategoryToUpdate) {
      throw new ValidationMessageException(new Message(
          OrderableDisplayCategoryMessageKeys.ERROR_NOT_FOUND_WITH_ID, orderableDisplayCategoryId));
    }
    OrderableDisplayCategory orderableDisplayCategory =
        OrderableDisplayCategory.newInstance(orderableDisplayCategoryDto);
    orderableDisplayCategoryToUpdate.updateFrom(orderableDisplayCategory);
    orderableDisplayCategoryRepository.save(orderableDisplayCategoryToUpdate);

    LOGGER.debug("Updated orderableDisplayCategory with id: %s", orderableDisplayCategoryId);
    return newInstance(orderableDisplayCategoryToUpdate);
  }

  /**
   * Get chosen OrderableDisplayCategory.
   *
   * @param orderableDisplayCategoryId UUID of the OrderableDisplayCategory which we want to get.
   * @return the OrderableDisplayCategory.
   */
  @RequestMapping(value = "/orderableDisplayCategories/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public OrderableDisplayCategoryDto getOrderableDisplayCategory(
      @PathVariable("id") UUID orderableDisplayCategoryId) {

    OrderableDisplayCategory orderableDisplayCategory = orderableDisplayCategoryRepository.findOne(
        orderableDisplayCategoryId);
    if (orderableDisplayCategory == null) {
      throw new NotFoundException(OrderableDisplayCategoryMessageKeys.ERROR_NOT_FOUND);
    } else {
      return newInstance(orderableDisplayCategory);
    }
  }

  /**
   * Allows deleting OrderableDisplayCategory.
   *
   * @param orderableDisplayCategoryId UUID of the OrderableDisplayCategory which we want to delete
   */
  @RequestMapping(value = "/orderableDisplayCategories/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteOrderableDisplayCategory(
      @PathVariable("id") UUID orderableDisplayCategoryId) {
    rightService.checkAdminRight(ORDERABLES_MANAGE);

    OrderableDisplayCategory orderableDisplayCategory = orderableDisplayCategoryRepository.findOne(
        orderableDisplayCategoryId);
    if (orderableDisplayCategory == null) {
      throw new NotFoundException(OrderableDisplayCategoryMessageKeys.ERROR_NOT_FOUND);
    } else {
      try {
        orderableDisplayCategoryRepository.delete(orderableDisplayCategory);
      } catch (DataIntegrityViolationException ex) {
        throw new IntegrityViolationException(new Message(
            OrderableDisplayCategoryMessageKeys.ERROR_DELETING_WITH_ID,
            orderableDisplayCategoryId), ex);
      }
    }
  }

  /**
   * Get the audit information related to orderable display category.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/orderableDisplayCategories/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getOrderableDisplayCategoryAuditLog(
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
    OrderableDisplayCategory instance = orderableDisplayCategoryRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(OrderableDisplayCategoryMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(
        OrderableDisplayCategory.class, id, author, changedPropertyName, page, returnJson
    );
  }

  /**
   * Finds OrderableDisplayCategories matching all of provided parameters.
   *
   * @param codeParam a code of the OrderableDisplayCategory.
   * @return a list of all OrderableDisplayCategories matching provided parameters.
   */
  @RequestMapping(value = "/orderableDisplayCategories/search", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<OrderableDisplayCategoryDto> searchOrderableDisplayCategories(
      @RequestParam(value = "code", required = false) String codeParam) {

    if (codeParam != null) {
      OrderableDisplayCategory orderableDisplayCategory = orderableDisplayCategoryRepository
          .findByCode(Code.code(codeParam));
      if (null == orderableDisplayCategory) {
        throw new NotFoundException(OrderableDisplayCategoryMessageKeys.ERROR_NOT_FOUND);
      }
      return Collections.singletonList(newInstance(orderableDisplayCategory));
    } else {
      return newInstance(orderableDisplayCategoryRepository.findAll());
    }
  }
}
