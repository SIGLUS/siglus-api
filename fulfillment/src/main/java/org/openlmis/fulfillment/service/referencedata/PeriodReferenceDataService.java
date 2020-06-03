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

package org.openlmis.fulfillment.service.referencedata;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openlmis.fulfillment.service.request.RequestParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class PeriodReferenceDataService extends BaseReferenceDataService<ProcessingPeriodDto> {

  private static final String START_DATE = "startDate";
  private static final String END_DATE = "endDate";

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
   * Gets filtered Processing Periods by start date and end date.
   *
   * @param startDate filter start date value
   * @param endDate   filter end date value
   * @return a list of filtered Processing Periods
   */
  public List<ProcessingPeriodDto> search(LocalDate startDate, LocalDate endDate) {
    return getPage(
        RequestParameters.init()
            .set(START_DATE, startDate)
            .set(END_DATE, endDate))
        .getContent();
  }

  /**
   * Finds periods by their ids.
   *
   * @param ids ids to look for.
   * @return a page of periods
   */
  public List<ProcessingPeriodDto> findByIds(Collection<UUID> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    return getPage(RequestParameters.init().set("id", ids)).getContent();
  }
}
