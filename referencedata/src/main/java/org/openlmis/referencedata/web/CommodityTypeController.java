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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.dto.CommodityTypeDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.CommodityTypeRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.CommodityTypeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys;
import org.openlmis.referencedata.validate.CommodityTypeValidator;
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
public class CommodityTypeController extends BaseController {

  public static final String RESOURCE_PATH = "/commodityTypes";

  @Autowired
  private CommodityTypeRepository repository;

  @Autowired
  private TradeItemRepository tradeItemRepository;

  @Autowired
  private CommodityTypeValidator validator;

  /**
   * Add or update a commodity type.
   *
   * @param commodityTypeDto the commodity type to add or update.
   *
   * @return CommodytyType that was created or updated
   */
  @Transactional
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.PUT)
  public CommodityTypeDto createOrUpdate(@RequestBody CommodityTypeDto commodityTypeDto,
                                         BindingResult bindingResult) {
    rightService.checkAdminRight(ORDERABLES_MANAGE);
    validator.validate(commodityTypeDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);

    CommodityType commodityType = CommodityType.newInstance(commodityTypeDto);

    if (null != commodityType.getId()) {
      UUID productId = commodityType.getId();
      CommodityType storedCommodityType = repository.findOne(productId);
      if (null != storedCommodityType) {
        commodityType.setChildren(storedCommodityType.getChildren());
      }
    }

    if (commodityType.getParent() != null) {
      CommodityType parent = repository.findOne(commodityType.getParent().getId());
      if (parent == null) {
        throw new ValidationMessageException(new Message(
            CommodityTypeMessageKeys.ERROR_PARENT_NOT_FOUND, commodityType.getParent()));
      }
      commodityType.assignParent(parent);
    }

    return CommodityTypeDto.newInstance(repository.save(commodityType));
  }

  /**
   * Update the {@link TradeItem} that may fulfill for a {@link CommodityType}.
   *
   * @param commodityTypeId the CommodityType's persistence Id.
   * @param tradeItemIds    the persistence id's of the TradeItems
   *                        {@link org.springframework.http.HttpStatus#NOT_FOUND}
   *                        if any of the given persistence ids are not found.
   */
  @Transactional
  @RequestMapping(value = RESOURCE_PATH + "/{id}/tradeItems", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  public void updateTradeItemAssociations(@PathVariable("id") UUID commodityTypeId,
                                          @RequestBody Set<UUID> tradeItemIds) {
    rightService.checkAdminRight(ORDERABLES_MANAGE);

    // ensure trade item list isn't null
    if (null == tradeItemIds) {
      throw new ValidationMessageException(CommodityTypeMessageKeys.ERROR_TRADE_ITEMS_NULL);
    }

    // ensure commodity type exists
    CommodityType commodityType = repository.findOne(commodityTypeId);
    if (null == commodityType) {
      throw new NotFoundException(CommodityTypeMessageKeys.ERROR_NOT_FOUND);
    }

    // create set of trade items from their ids, stop if any one is not found
    Set<TradeItem> tradeItems = new HashSet<>(tradeItemIds.size());
    for (UUID id : tradeItemIds) {
      TradeItem item = tradeItemRepository.findOne(id);
      if (null == item) {
        throw new NotFoundException(new Message(TradeItemMessageKeys.ERROR_NOT_FOUND_WITH_ID, id));
      }

      item.assignCommodityType(commodityType);

      tradeItems.add(item);
    }

    // update the trade items with new classifications
    tradeItemRepository.save(tradeItems);
  }

  /**
   * Gets the TradeItem's persistence ids that may fulfill for the given CommodityType.
   *
   * @param commodityTypeId persistence id of the CommodityType.
   * @return {@link org.springframework.http.HttpStatus#OK} and a set of persistence ids for the
   *     commodity type, or an empty set.
   */
  @Transactional
  @RequestMapping(value = RESOURCE_PATH + "/{id}/tradeItems", method = RequestMethod.GET)
  public Set<UUID> getTradeItems(@PathVariable("id") UUID commodityTypeId) {

    // ensure commodity type exists
    CommodityType commodityType = repository.findOne(commodityTypeId);
    if (null == commodityType) {
      throw new NotFoundException(CommodityTypeMessageKeys.ERROR_NOT_FOUND);
    }

    Set<UUID> ids = new HashSet<>();
    Iterable<TradeItem> items = tradeItemRepository.findByClassificationId(
        commodityType.getClassificationId());

    for (TradeItem item : items) {
      ids.add(item.getId());
    }

    return ids;
  }

  /**
   * Retrieves all Commodity types.
   */
  @Transactional
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.GET)
  public Page<CommodityTypeDto> retrieveAll(Pageable pageable) {

    Iterable<CommodityType> result = repository.findAll();

    return Pagination.getPage(CommodityTypeDto.newInstance(result), pageable);
  }

  /**
   * Get the audit information related to commodity types.
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
  public ResponseEntity<String> getCommodityTypeAuditLog(
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
    CommodityType instance = repository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(CommodityTypeMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(CommodityType.class, id, author, changedPropertyName, page,
        returnJson);
  }
}
