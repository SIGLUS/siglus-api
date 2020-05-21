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

package org.openlmis.referencedata.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.repository.ProcessingScheduleRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
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
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

@Service
public class ProcessingPeriodService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingPeriodService.class);

  @Autowired
  private ProcessingPeriodRepository periodRepository;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private ProcessingScheduleRepository processingScheduleRepository;

  /**
   * Finds all ProcessingPeriods matching all of provided parameters.
   */
  public Page<ProcessingPeriod> searchPeriods(ProcessingPeriodSearchParams params,
                                              Pageable pageable) {
    Profiler profiler = new Profiler("SEARCH_PERIODS_BY_PARAMS");
    profiler.setLogger(LOGGER);

    params.validate();

    profiler.start("CHECK_IF_PROGRAM_EXISTS");
    existsById(programRepository, params.getProgramId(),
        ProgramMessageKeys.ERROR_NOT_FOUND_WITH_ID);

    profiler.start("CHECK_IF_FACILITY_EXISTS");
    existsById(facilityRepository, params.getFacilityId(),
        FacilityMessageKeys.ERROR_NOT_FOUND_WITH_ID);

    profiler.start("CHECK_IF_SCHEDULE_EXISTS");
    existsById(processingScheduleRepository,
        params.getProcessingScheduleId(), ProcessingScheduleMessageKeys.ERROR_NOT_FOUND_WITH_ID);

    LocalDate startDate = params.getStartDate();
    LocalDate endDate = params.getEndDate();
    Collection<UUID> ids = params.getIds();

    profiler.start("SEARCH_FOR_PERIODS");
    Page<ProcessingPeriod> periods =
        periodRepository.search(params.getProcessingScheduleId(), params.getProgramId(),
            params.getFacilityId(), startDate, endDate, ids, pageable);

    profiler.stop().log();
    return periods;
  }

  private <T> void existsById(CrudRepository<T, UUID> repository, UUID id, String errorKey) {
    boolean exists = id != null && repository.exists(id);
    if (null != id && !exists) {
      throw new NotFoundException(new Message(errorKey, id));
    }
  }
}
