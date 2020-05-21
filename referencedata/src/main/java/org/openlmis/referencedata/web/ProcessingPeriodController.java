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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.openlmis.referencedata.dto.ResultDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.service.ProcessingPeriodSearchParams;
import org.openlmis.referencedata.service.ProcessingPeriodService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.ProcessingPeriodMessageKeys;
import org.openlmis.referencedata.validate.ProcessingPeriodValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class ProcessingPeriodController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingPeriodController.class);

  public static final String RESOURCE_PATH = "/processingPeriods";

  @Autowired
  @Qualifier("beforeSavePeriodValidator")
  private ProcessingPeriodValidator validator;

  @Autowired
  private ProcessingPeriodRepository periodRepository;

  @Autowired
  private ProcessingPeriodService periodService;

  /**
   * Create a new processing period using the provided processing period DTO.
   *
   * @param periodDto     processing period DTO with which to create the processing period
   * @param bindingResult Object used for validation.
   * @return the new processing period.
   */
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public ProcessingPeriodDto createProcessingPeriod(
      @RequestBody ProcessingPeriodDto periodDto, BindingResult bindingResult) {
    rightService.checkAdminRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);
    ProcessingPeriod newPeriod = ProcessingPeriod.newPeriod(periodDto);
    LOGGER.debug("Creating new processingPeriod");
    validator.validate(newPeriod, bindingResult);
    if (bindingResult.getErrorCount() == 0) {
      periodRepository.save(newPeriod);

      return exportToDto(newPeriod);
    } else {
      ObjectError error = bindingResult.getAllErrors().get(0);
      throw new ValidationMessageException(new Message(error.getCode(), error.getArguments()));
    }
  }

  /**
   * Get all ProcessingPeriods matching all of provided parameters.
   *
   * @return the ProcessingPeriods.
   */
  @RequestMapping(value = RESOURCE_PATH, method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<ProcessingPeriodDto> getAllProcessingPeriods(
      @RequestParam MultiValueMap<String, Object> requestParams,
      @SortDefault(sort = "startDate") Pageable pageable) {

    Profiler profiler = new Profiler("SEARCH_PROCESSING_PERIODS");
    profiler.setLogger(LOGGER);

    ProcessingPeriodSearchParams params = new ProcessingPeriodSearchParams(requestParams);

    LOGGER.debug("period search start date {}", params.getStartDate());
    LOGGER.debug("period search end date {}", params.getEndDate());

    profiler.start("SEARCH_FOR_PERIODS");
    Page<ProcessingPeriod> periods = periodService.searchPeriods(params, pageable);

    profiler.start("EXPORT_PERIODS_TO_DTO");
    Page<ProcessingPeriodDto> dtos = exportToDto(periods, profiler, pageable);

    profiler.stop().log();
    return dtos;
  }

  /**
   * Update an existing ProcessingPeriod using the provided ProcessingPeriodDto. Note, if the role
   * does not exist, will create one.
   *
   * @param periodDto provided processing period DTO.
   * @param periodId  id of the ProcessingPeriod to update.
   * @return the updated role.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ProcessingPeriodDto updateProcessingPeriod(@RequestBody ProcessingPeriodDto periodDto,
                                                    @PathVariable("id") UUID periodId,
                                                    BindingResult bindingResult) {
    rightService.checkAdminRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);

    ProcessingPeriod updatedProcessingPeriod = ProcessingPeriod.newPeriod(periodDto);
    updatedProcessingPeriod.setId(periodId);

    validator.validate(updatedProcessingPeriod, bindingResult);
    if (bindingResult.getErrorCount() > 0) {
      ObjectError error = bindingResult.getAllErrors().get(0);
      throw new ValidationMessageException(new Message(error.getCode(), error.getArguments()));
    }

    periodRepository.save(updatedProcessingPeriod);
    return exportToDto(updatedProcessingPeriod);
  }

  /**
   * Get chosen ProcessingPeriodDto.
   *
   * @param periodId UUID of the ProcessingPeriodDto which we want to get
   * @return the ProcessingPeriod.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ProcessingPeriodDto getProcessingPeriod(@PathVariable("id") UUID periodId) {
    Profiler profiler = new Profiler("GET_PERIOD_BY_ID");
    profiler.setLogger(LOGGER);

    profiler.start("FIND_PERIOD_IN_DB");
    ProcessingPeriod period = periodRepository.findOne(periodId);
    if (period == null) {
      profiler.stop().log();
      throw new NotFoundException(ProcessingPeriodMessageKeys.ERROR_NOT_FOUND);
    }

    profiler.start("EXPORT_TO_DTO");
    ProcessingPeriodDto dto = exportToDto(period);

    profiler.stop().log();
    return dto;
  }

  /**
   * Returns total difference between start date and end date from given ProcessingPeriod rounded to
   * whole months.
   *
   * @param periodId UUID of given ProcessingPeriod.
   * @return String which contains number of months.
   */
  @RequestMapping(value = RESOURCE_PATH + "/{id}/duration", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResultDto<Integer> getDuration(@PathVariable("id") UUID periodId) {

    ProcessingPeriod period = periodRepository.findOne(periodId);

    LOGGER.debug("Returning total number of months of processingPeriod");

    return new ResultDto<>(period.getDurationInMonths());
  }

  /**
   * Get the audit information related to processing period.
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
  public ResponseEntity<String> getProcessingPeriodAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    rightService.checkAdminRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);

    //Return a 404 if the specified instance can't be found
    ProcessingPeriod instance = periodRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(ProcessingPeriodMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(ProcessingPeriod.class, id, author, changedPropertyName, page,
        returnJson);
  }

  private ProcessingPeriodDto exportToDto(ProcessingPeriod period) {
    ProcessingPeriodDto periodDto = new ProcessingPeriodDto();
    period.export(periodDto);
    return periodDto;
  }

  private Page<ProcessingPeriodDto> exportToDto(Page<ProcessingPeriod> periods, Profiler profiler,
                                                Pageable pageable) {
    List<ProcessingPeriodDto> dtos = periods.getContent()
        .stream()
        .map(this::exportToDto)
        .collect(toList());
    return toPage(dtos, pageable, periods.getTotalElements(), profiler);
  }
}
