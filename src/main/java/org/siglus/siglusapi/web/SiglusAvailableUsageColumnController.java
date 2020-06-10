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

package org.siglus.siglusapi.web;

import java.util.stream.Collectors;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.siglus.siglusapi.dto.AvailableUsageColumnDto;
import org.siglus.siglusapi.repository.AvailableUsageColumnRepository;
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
@RequestMapping("/api/siglusapi/availableUsageColumns")
public class SiglusAvailableUsageColumnController {

  @Autowired
  AvailableUsageColumnRepository repository;

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<AvailableUsageColumnDto> getAllColumns(Pageable pageable) {
    Page<AvailableUsageColumn> page = repository.findAll(pageable);
    return new PageImpl<>(page.getContent()
        .stream()
        .map(AvailableUsageColumnDto::newInstance)
        .collect(Collectors.toList()), pageable, page.getTotalElements());
  }

}
