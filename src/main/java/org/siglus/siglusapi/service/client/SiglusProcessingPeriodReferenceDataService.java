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

package org.siglus.siglusapi.service.client;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SiglusProcessingPeriodReferenceDataService
    extends BaseReferenceDataService<ProcessingPeriodDto> {

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


  public ProcessingPeriodDto saveProcessingPeriod(ProcessingPeriodDto processingPeriodDto) {
    return postResult("", processingPeriodDto, getResultClass());
  }

  public List<ProcessingPeriodDto> findByIds(Collection<UUID> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    return getPage(RequestParameters.init().set("id", ids)).getContent();
  }

  public Page<ProcessingPeriodDto> searchProcessingPeriods(
      UUID scheduleId,
      UUID programId,
      UUID facilityId,
      LocalDate startDate,
      LocalDate endDate,
      Collection<UUID> ids,
      Pageable pageable) {
    RequestParameters parameters =
        RequestParameters.init()
            .set("processingScheduleId", scheduleId)
            .set("programId", programId)
            .set("facilityId", facilityId)
            .set("startDate", startDate)
            .set("endDate", endDate)
            .set("id", ids)
            .setPage(pageable);
    return getPage(parameters);
  }

}
