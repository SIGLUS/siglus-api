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

package org.openlmis.requisition.service.referencedata;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PeriodReferenceDataService extends BaseReferenceDataService<ProcessingPeriodDto> {

  @Override
  protected String getUrl() {
    return "/api/processingPeriods/";
  }

  @Override
  protected Class<ProcessingPeriodDto> getResultClass() {
    return ProcessingPeriodDto.class;
  }

  @Override
  protected Class<ProcessingPeriodDto[]> getArrayResultClass() {
    return ProcessingPeriodDto[].class;
  }

  /**
   * Retrieves periods from the reference data service by schedule ID and end date.
   *
   * @param processingScheduleId UUID of the schedule
   * @param endDate              the end date (only include periods previous to this date)
   * @return A list of periods matching search criteria
   */
  public Collection<ProcessingPeriodDto> search(UUID processingScheduleId, LocalDate endDate) {
    return search(processingScheduleId, endDate, new PageRequest(0, Integer.MAX_VALUE));
  }

  /**
   * Retrieves periods from the reference data service by schedule ID and end date.
   *
   * @param processingScheduleId UUID of the schedule
   * @param endDate              the end date (only include periods previous to this date)
   * @param pageable             contains pagination parameters
   * @return A list of periods matching search criteria
   */
  public List<ProcessingPeriodDto> search(UUID processingScheduleId, LocalDate endDate,
      Pageable pageable) {
    RequestParameters parameters = RequestParameters
        .init()
        .setPage(pageable)
        .set("processingScheduleId", processingScheduleId)
        .set("endDate", endDate);

    return getPage(parameters).getContent();
  }

  /**
   * This method retrieves processing periods for given ids.
   *
   * @param periodIds list of period ids.
   * @return List of ProcessingPeriodDto.
   */
  public List<ProcessingPeriodDto> search(Set<UUID> periodIds) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("id", periodIds);

    return getPage(parameters).getContent();
  }

  /**
   * Retrieves periods from the reference data service by program ID and facility ID.
   *
   * @param programId  UUID of the program
   * @param facilityId UUID of the facility
   * @return A list of periods matching search criteria
   */
  public Collection<ProcessingPeriodDto> searchByProgramAndFacility(UUID programId,
                                                                    UUID facilityId) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("programId", programId)
        .set("facilityId", facilityId);
    
    return getPage(parameters).getContent();
  }
}
