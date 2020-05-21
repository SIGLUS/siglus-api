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

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotNull;

import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.web.QueryOrderableSearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderableService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrderableService.class);

  private static final String GMT = "GMT";

  @Autowired
  private OrderableRepository orderableRepository;

  /**
   * Method returns all orderables with matched parameters.
   *
   * @param queryMap request parameters (code, name, description, program).
   * @param pageable the page to get, or one page with all if null.
   * @return the Page of orderables found, or an empty page.
   */
  public Page<Orderable> searchOrderables(@NotNull QueryOrderableSearchParams queryMap,
      Pageable pageable) {
    LOGGER.info("search orderable query params: {}", queryMap);

    if (queryMap.isEmpty()) {
      LOGGER.info("find all");
      return orderableRepository.findAllLatest(pageable);
    }

    Set<UUID> ids = queryMap.getIds();
    LOGGER.info("ids from query params: {}", ids);
    if (!ids.isEmpty()) {
      LOGGER.info("find all by ids");
      return orderableRepository.findAllLatestByIds(ids, pageable);
    }

    if (LOGGER.isInfoEnabled()) {
      String code = queryMap.getCode();
      String name = queryMap.getName();
      String programCode = queryMap.getProgramCode();

      LOGGER.info("search by code {}, name {}, and program code {}", code, name, programCode);
    }

    return orderableRepository.search(queryMap, pageable);
  }

  /**
   * Method returns the latest last updated date out of all orderables with matched parameters.
   *
   * @param queryParams request parameters (code, name, description, program).
   * @return the ZonedDateTime of latest last updated date.
   */
  public ZonedDateTime getLatestLastUpdatedDate(@NotNull QueryOrderableSearchParams queryParams,
      Profiler profiler) {

    if (queryParams.isEmpty()) {
      LOGGER.info("Find latest modified date out of all orderables");
      profiler.start("SEARCH_NO_PARAMS_OR_IDS");
      Timestamp timestamp = orderableRepository.findLatestModifiedDateOfAll();
      return getZoneDateTime(timestamp);
    }

    Set<UUID> ids = queryParams.getIds();
    if (!ids.isEmpty()) {
      LOGGER.info("Find latest modified date out of orderables from ids list");
      profiler.start("SEARCH_IDS ONLY");
      Timestamp timestamp = orderableRepository.findLatestModifiedDateByIds(ids);
      return getZoneDateTime(timestamp);
    }

    LOGGER.info("Find latest modified date out of orderables from query params");
    profiler.start("SEARCH_QUERY_PARAMS ONLY");
    return orderableRepository
            .findLatestModifiedDateByParams(queryParams);
  }

  private ZonedDateTime getZoneDateTime(Timestamp timestamp) {

    if (null != timestamp) {
      return ZonedDateTime.of(timestamp.toLocalDateTime(),
              ZoneId.of(GMT));
    }
    return null;
  }

}