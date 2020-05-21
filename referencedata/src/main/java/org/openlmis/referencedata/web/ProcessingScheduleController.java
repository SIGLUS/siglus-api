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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.domain.RequisitionGroupProgramSchedule;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.dto.ProcessingScheduleDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProcessingScheduleRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.service.RequisitionGroupProgramScheduleService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProcessingScheduleMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
public class ProcessingScheduleController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingScheduleController.class);

  @Autowired
  private ProcessingScheduleRepository scheduleRepository;


  @Autowired
  private RequisitionGroupProgramScheduleService requisitionGroupProgramScheduleService;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  /**
   * Allows creating new ProcessingSchedules.
   *
   * @param schedule a ProcessingSchedule bound to the request body.
   * @return the created ProcessingSchedule.
   */
  @RequestMapping(value = "/processingSchedules", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public ProcessingSchedule createProcessingSchedule(
      @RequestBody ProcessingSchedule schedule) {
    rightService.checkAdminRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);
    LOGGER.debug("Creating new processingSchedule");
    // Ignore provided id
    schedule.setId(null);
    scheduleRepository.save(schedule);
    return schedule;
  }

  /**
   * Allows updating ProcessingSchedules.
   *
   * @param schedule   a ProcessingSchedule bound to the request body.
   * @param scheduleId the UUID of ProcessingSchedule which we want to update.
   * @return the updated ProcessingSchedule.
   */
  @RequestMapping(value = "/processingSchedules/{id}", method = RequestMethod.PUT)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ProcessingSchedule updateProcessingSchedule(
      @RequestBody ProcessingSchedule schedule, @PathVariable("id") UUID scheduleId) {
    rightService.checkAdminRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);
    LOGGER.debug("Updating processingSchedule");
    scheduleRepository.save(schedule);
    return schedule;
  }

  /**
   * Get all ProcessingSchedules.
   *
   * @return the ProcessingSchedules.
   */
  @GetMapping(value = "/processingSchedules")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<ProcessingScheduleDto> getAll(Pageable pageable) {
    Profiler profiler = new Profiler("GET_ALL_PROCESSING_SCHEDULES");
    profiler.setLogger(LOGGER);

    profiler.start("GET_SCHEDULES");
    Page<ProcessingSchedule> processingSchedulePage = scheduleRepository.findAll(pageable);

    profiler.start("TO_DTO");
    Page<ProcessingScheduleDto> dtos = exportToDto(processingSchedulePage, pageable, profiler);
    profiler.stop().log();

    return dtos;
  }

  /**
   * Fetches the correct schedule, based on the provided parameters.
   *
   * @param programId the UUID of the program.
   * @param facilityId the UUID of the facility.
   * @return the ProcessingSchedule for the specified parameters.
   */
  @RequestMapping(value = "/processingSchedules/search", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<ProcessingScheduleDto> search(
      @RequestParam("programId") UUID programId, @RequestParam("facilityId") UUID facilityId) {
    Profiler profiler = new Profiler("SEARCH_FOR_SCHEDULES");
    profiler.setLogger(LOGGER);

    profiler.start("FIND_PROGRAM_IN_DB");
    if (!programRepository.exists(programId)) {
      profiler.stop().log();
      throw new ValidationMessageException(
          new Message(ProgramMessageKeys.ERROR_NOT_FOUND_WITH_ID, programId));
    }

    profiler.start("FIND_FACILITY_IN_DB");
    if (!facilityRepository.exists(facilityId)) {
      profiler.stop().log();
      throw new ValidationMessageException(
          new Message(FacilityMessageKeys.ERROR_NOT_FOUND_WITH_ID, facilityId));
    }

    profiler.start("FIND_REQUISITION_GROUP_IN_DB");
    List<RequisitionGroupProgramSchedule> requisitionGroupProgramSchedules =
        requisitionGroupProgramScheduleService.searchRequisitionGroupProgramSchedules(
            programId, facilityId);

    List<ProcessingScheduleDto> schedules = new ArrayList<>();
    if (!requisitionGroupProgramSchedules.isEmpty()) {
      profiler.start("EXPORT_TO_DTO");
      ProcessingScheduleDto scheduleDto = exportToDto(requisitionGroupProgramSchedules.get(0)
          .getProcessingSchedule());
      schedules.add(scheduleDto);
    }

    profiler.stop().log();
    return schedules;
  }

  /**
   * Get chosen ProcessingSchedule.
   *
   * @param scheduleId UUID of the ProcessingSchedule which we want to get.
   * @return the ProcessingSchedule.
   */
  @RequestMapping(value = "/processingSchedules/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ProcessingSchedule getProcessingSchedule(
      @PathVariable("id") UUID scheduleId) {

    ProcessingSchedule schedule = scheduleRepository.findOne(scheduleId);
    if (schedule == null) {
      throw new NotFoundException(ProcessingScheduleMessageKeys.ERROR_NOT_FOUND);
    } else {
      return schedule;
    }
  }

  /**
   * Allows deleting ProcessingSchedule.
   *
   * @param scheduleId UUID of the ProcessingSchedule which we want to delete.
   */
  @RequestMapping(value = "/processingSchedules/{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProcessingSchedule(
      @PathVariable("id") UUID scheduleId) {
    rightService.checkAdminRight(RightName.PROCESSING_SCHEDULES_MANAGE_RIGHT);
    ProcessingSchedule schedule = scheduleRepository.findOne(scheduleId);
    if (schedule == null) {
      throw new NotFoundException(ProcessingScheduleMessageKeys.ERROR_NOT_FOUND);
    } else {
      scheduleRepository.delete(schedule);
    }
  }

  /**
   * Get the audit information related to processing schedule.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @RequestMapping(value = "/processingSchedules/{id}/auditLog", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public ResponseEntity<String> getProcessingScheduleAuditLog(
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
    ProcessingSchedule instance = scheduleRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(ProcessingScheduleMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(ProcessingSchedule.class, id, author, changedPropertyName, page,
        returnJson);
  }

  private ProcessingScheduleDto exportToDto(ProcessingSchedule processingSchedule) {
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingSchedule.export(processingScheduleDto);
    return processingScheduleDto;
  }

  private Page<ProcessingScheduleDto> exportToDto(Page<ProcessingSchedule> page, Pageable pageable,
                                                  Profiler profiler) {
    List<ProcessingScheduleDto> processingScheduleDtos = page.getContent()
        .stream()
        .map(this::exportToDto)
        .collect(Collectors.toList());
    return toPage(processingScheduleDtos, pageable, page.getTotalElements(), profiler);
  }
}