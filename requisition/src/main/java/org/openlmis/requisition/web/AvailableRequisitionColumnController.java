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

package org.openlmis.requisition.web;

import java.util.stream.Collectors;
import org.openlmis.requisition.domain.AvailableRequisitionColumn;
import org.openlmis.requisition.dto.AvailableRequisitionColumnDto;
import org.openlmis.requisition.repository.AvailableRequisitionColumnRepository;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availableRequisitionColumns")
public class AvailableRequisitionColumnController {

  private final XLogger logger = XLoggerFactory.getXLogger(getClass());

  @Autowired
  private AvailableRequisitionColumnRepository repository;

  /**
   * Returns a page of Available Requisition Columns.
   *
   * @param pageable holds pagination and sort parameters
   * @return page of Available Requisition Columns
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<AvailableRequisitionColumnDto> getAllColumns(Pageable pageable) {
    Profiler profiler = new Profiler("GET_ALL_AVAILABLE_REQUISITION_COLUMNS");
    profiler.setLogger(logger);

    profiler.start("GET_FROM_REPOSITORY");
    Page<AvailableRequisitionColumn> page = repository.findAll(pageable);

    profiler.start("TO_DTO");
    Page<AvailableRequisitionColumnDto> result = new PageImpl<>(page.getContent()
        .stream()
        .map(AvailableRequisitionColumnDto::newInstance)
        .collect(Collectors.toList()), pageable, page.getTotalElements());

    profiler.stop().log();
    return result;
  }
}
