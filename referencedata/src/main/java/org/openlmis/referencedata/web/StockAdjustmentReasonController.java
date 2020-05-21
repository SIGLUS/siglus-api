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

import static org.openlmis.referencedata.domain.RightName.STOCK_ADJUSTMENT_REASONS_MANAGE;

import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.StockAdjustmentReason;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.StockAdjustmentReasonRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.StockAdjustmentReasonMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class StockAdjustmentReasonController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(
          StockAdjustmentReasonController.class);

  @Autowired
  private StockAdjustmentReasonRepository stockAdjustmentReasonRepository;

  /**
   * Allows creating new Stock Adjustment Reasons.
   *
   * @param stockAdjustmentReason A stockAdjustmentReason bound to the request body.
   * @return the created stockAdjustmentReason.
   */
  @RequestMapping(value = "/stockAdjustmentReasons", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public StockAdjustmentReason createStockAdjustmentReason(
          @RequestBody StockAdjustmentReason stockAdjustmentReason) {
    rightService.checkAdminRight(STOCK_ADJUSTMENT_REASONS_MANAGE);

    LOGGER.debug("Creating new stockAdjustmentReason");
    // Ignore provided id
    stockAdjustmentReason.setId(null);
    return stockAdjustmentReasonRepository.save(stockAdjustmentReason);
  }

  /**
   * Get all stockAdjustmentReasons.
   *
   * @return the StockAdjustmentReasons.
   */
  @RequestMapping(value = "/stockAdjustmentReasons", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Iterable<StockAdjustmentReason> getAllStockAdjustmentReasons() {
    Iterable<StockAdjustmentReason> stockAdjustmentReasons =
            stockAdjustmentReasonRepository.findAll();
    if (stockAdjustmentReasons == null) {
      throw new NotFoundException(StockAdjustmentReasonMessageKeys.ERROR_NOT_FOUND);
    } else {
      return stockAdjustmentReasons;
    }
  }

  /**
   * Get chosen stockAdjustmentReason.
   *
   * @param stockAdjustmentReasonId UUID of stockAdjustmentReason which we want to get.
   * @return the StockAdjustmentReason.
   */
  @RequestMapping(value = "/stockAdjustmentReasons/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public StockAdjustmentReason getChosenStockAdjustmentReason(
          @PathVariable("id") UUID stockAdjustmentReasonId) {
    StockAdjustmentReason stockAdjustmentReason =
            stockAdjustmentReasonRepository.findOne(stockAdjustmentReasonId);
    if (stockAdjustmentReason == null) {
      throw new NotFoundException(StockAdjustmentReasonMessageKeys.ERROR_NOT_FOUND);
    } else {
      return stockAdjustmentReason;
    }
  }

  /**
   * Get the audit information related to stock adjustment reason.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/stockAdjustmentReasons/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getStockAdjustmentReasonAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    rightService.checkAdminRight(RightName.STOCK_ADJUSTMENT_REASONS_MANAGE);

    //Return a 404 if the specified instance can't be found
    StockAdjustmentReason instance = stockAdjustmentReasonRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(StockAdjustmentReasonMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(
        StockAdjustmentReason.class, id, author, changedPropertyName, page, returnJson
    );
  }

  /**
   * Allows deleting stockAdjustmentReason.
   *
   * @param stockAdjustmentReasonId UUID of stockAdjustmentReason which we want to delete.
   */
  @RequestMapping(value = "/stockAdjustmentReasons/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ResponseBody
  public void deleteStockAdjustmentReason(
          @PathVariable("id") UUID stockAdjustmentReasonId) {
    rightService.checkAdminRight(STOCK_ADJUSTMENT_REASONS_MANAGE);

    StockAdjustmentReason stockAdjustmentReason =
            stockAdjustmentReasonRepository.findOne(stockAdjustmentReasonId);
    if (stockAdjustmentReason == null) {
      throw new NotFoundException(StockAdjustmentReasonMessageKeys.ERROR_NOT_FOUND);
    } else {
      stockAdjustmentReasonRepository.delete(stockAdjustmentReason);
    }
  }

  /**
   * Updates stockAdjustmentReason.
   *
   * @param stockAdjustmentReason DTO class used to update stockAdjustmentReason.
   */
  @RequestMapping(value = "/stockAdjustmentReasons/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public StockAdjustmentReason updateStockAdjustmentReason(
          @PathVariable("id") UUID stockAdjustmentReasonId,
          @RequestBody StockAdjustmentReason stockAdjustmentReason) {
    rightService.checkAdminRight(STOCK_ADJUSTMENT_REASONS_MANAGE);

    if (stockAdjustmentReason == null || stockAdjustmentReasonId == null) {
      LOGGER.debug("Update failed - stockAdjustmentReason id not specified");
      throw new ValidationMessageException(StockAdjustmentReasonMessageKeys.ERROR_ID_NULL);
    }

    StockAdjustmentReason storedStockAdjustmentReason =
            stockAdjustmentReasonRepository.findOne(stockAdjustmentReasonId);
    if (storedStockAdjustmentReason == null) {
      LOGGER.warn("Update failed - stockAdjustmentReason with id: {} not found",
              stockAdjustmentReasonId);
      throw new ValidationMessageException(new Message(
          StockAdjustmentReasonMessageKeys.ERROR_NOT_FOUND_WITH_ID, stockAdjustmentReasonId));
    }

    stockAdjustmentReasonRepository.save(stockAdjustmentReason);

    return stockAdjustmentReason;
  }


  /**
   * Retrieves StockAdjustmentReasons for a specified program.
   *
   * @param programId the program id.
   * @return a list of StockAdjustmentReasons.
   */
  @RequestMapping(value = "/stockAdjustmentReasons/search", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<StockAdjustmentReason> findStockAdjustmentReasonsByProgramId(
          @RequestParam("program") UUID programId) {
    return stockAdjustmentReasonRepository.findByProgramId(programId);
  }
}
