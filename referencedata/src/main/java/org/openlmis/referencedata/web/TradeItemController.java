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

import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.dto.TradeItemDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys;
import org.openlmis.referencedata.validate.TradeItemValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TradeItemController extends BaseController {

  @Autowired
  private TradeItemRepository repository;

  @Autowired
  private TradeItemValidator validator;

  /**
   * Create or update a trade item.
   *
   * @return the trade item that was created or updated.
   */
  @Transactional
  @RequestMapping(value = "/tradeItems", method = RequestMethod.PUT)
  public TradeItemDto createOrUpdate(@RequestBody TradeItemDto tradeItemDto,
                                     BindingResult bindingResult) {
    rightService.checkAdminRight(ORDERABLES_MANAGE);
    validator.validate(tradeItemDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    TradeItem tradeItem = TradeItem.newInstance(tradeItemDto);

    return TradeItemDto.newInstance(repository.save(tradeItem));
  }

  /**
   * Get the audit information related to trade item.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/tradeItems/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getTradeItemAuditLog(
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
    TradeItem instance = repository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(TradeItemMessageKeys.ERROR_NOT_FOUND_WITH_ID);
    }

    return getAuditLogResponse(TradeItem.class, id, author, changedPropertyName, page,
        returnJson);
  }

  /**
   * Retrieves trade items. Allows searching by classification id, either using a full
   * or a partial match.

   * @param classificationId the classification id to search by
   * @param fullMatch true to search by a full match, false to search by partial match
   * @return a list of matching trade items
   */
  @Transactional
  @RequestMapping(value = "/tradeItems", method = RequestMethod.GET)
  public Page<TradeItemDto> retrieveTradeItems(
      @RequestParam(required = false) String classificationId,
      @RequestParam(required = false, defaultValue = "false") boolean fullMatch,
      Pageable pageable) {

    Iterable<TradeItem> result;
    if (StringUtils.isBlank(classificationId)) {
      result = repository.findAll();
    } else {
      if (fullMatch) {
        result = repository.findByClassificationId(classificationId);
      } else {
        result = repository.findByClassificationIdLike(classificationId);
      }
    }

    return Pagination.getPage(TradeItemDto.newInstance(result), pageable);
  }
}
