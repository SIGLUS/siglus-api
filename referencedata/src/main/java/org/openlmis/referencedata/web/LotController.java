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

import static org.openlmis.referencedata.domain.RightName.LOTS_MANAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.Lot;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.dto.LotDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.LotRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.service.LotSearchParams;
import org.openlmis.referencedata.service.LotService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.LotMessageKeys;
import org.openlmis.referencedata.validate.LotValidator;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class LotController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(LotController.class);

  @Autowired
  private LotRepository lotRepository;

  @Autowired
  private TradeItemRepository tradeItemRepository;

  @Autowired
  private LotValidator validator;

  @Autowired
  private LotService lotService;

  /**
   * Allows creating new Lots.
   *
   * @param lotDto a LotDto bound to the request body.
   * @return the created LotDto.
   */
  @RequestMapping(value = "/lots", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public LotDto createLot(@RequestBody LotDto lotDto, BindingResult bindingResult) {
    rightService.checkAdminRight(LOTS_MANAGE);

    validator.validate(lotDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);
    TradeItem tradeItem = tradeItemRepository.findOne(lotDto.getTradeItemId());
    Lot lotToSave = Lot.newLot(lotDto, tradeItem);
    lotToSave.setId(null);

    //XLOGGER.debug("Creating new Lot");
    lotToSave = lotRepository.save(lotToSave);
    return exportToDto(lotToSave);
  }

  /**
   * Allows updating Lots.
   *
   * @param lotDto   a LotDto bound to the request body.
   * @param lotId the UUID of Lot which we want to update.
   * @return the updated LotDto.
   */
  @RequestMapping(value = "/lots/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public LotDto updateLot(@RequestBody LotDto lotDto, @PathVariable("id") UUID lotId,
                       BindingResult bindingResult) {
    rightService.checkAdminRight(LOTS_MANAGE);

    Lot existingLot = lotRepository.findOne(lotId);
    if (existingLot == null) {
      throw new NotFoundException(new Message(LotMessageKeys.ERROR_NOT_FOUND_WITH_ID, lotId));
    }
    validator.validate(lotDto, bindingResult);
    throwValidationMessageExceptionIfErrors(bindingResult);
    TradeItem tradeItem = tradeItemRepository.findOne(lotDto.getTradeItemId());
    Lot lotToSave = Lot.newLot(lotDto, tradeItem);
    lotToSave.setId(lotId);

    XLOGGER.debug("Updating Lot");
    lotToSave = lotRepository.save(lotToSave);
    return exportToDto(lotToSave);
  }

  /**
   * Get chosen lot.
   *
   * @param lotId UUID of the lot to get.
   * @return chosen Lot.
   */
  @RequestMapping(value = "/lots/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public LotDto getLot(@PathVariable("id") UUID lotId) {

    Lot lot = lotRepository.findOne(lotId);
    if (lot == null) {
      throw new NotFoundException(LotMessageKeys.ERROR_NOT_FOUND);
    }
    return exportToDto(lot);
  }

  /**
   * Retrieves all Lots matching given parameters.
   * For lotCode: finds any values that have entered string value
   * in any position of searched field. Not case sensitive.
   * Other fields: entered string value must equal to searched value.
   *
   * @param requestParams  the request parameters
   * @param pageable Pageable object that allows client to optionally add "page" (page number).
   * @return List of matched Lots.
   */
  @GetMapping("/lots")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<LotDto> getLots(LotSearchParams requestParams, Pageable pageable) {
    XLOGGER.entry(requestParams, pageable);
    Profiler profiler = new Profiler("LOTS_SEARCH");
    profiler.setLogger(XLOGGER);

    profiler.start("LOT_SERVICE_SEARCH");
    Page<Lot> lotsPage = lotService.search(requestParams, pageable);

    profiler.start("LOT_TO_DTO");
    assert lotsPage != null;
    Page<LotDto> page = Pagination.getPage(LotDto.newInstance(
        lotsPage.getContent()),
        pageable,
        lotsPage.getTotalElements());

    profiler.stop().log();
    XLOGGER.exit(page);
    return page;
  }

  /**
   * Get the audit information related to lot.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/lots/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getLotAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    rightService.checkAdminRight(LOTS_MANAGE);

    //Return a 404 if the specified instance can't be found
    Lot instance = lotRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(LotMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(Lot.class, id, author, changedPropertyName, page, returnJson);
  }

  private LotDto exportToDto(Lot lot) {
    LotDto lotDto = new LotDto();
    lot.export(lotDto);
    return lotDto;
  }

  private List<LotDto> exportToDtos(List<Lot> lots) {
    List<LotDto> lotsDto = new ArrayList<>(lots.size());
    for (Lot lot : lots) {
      lotsDto.add(exportToDto(lot));
    }
    return lotsDto;
  }
}
