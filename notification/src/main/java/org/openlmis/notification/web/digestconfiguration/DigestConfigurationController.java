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

package org.openlmis.notification.web.digestconfiguration;

import static org.openlmis.notification.i18n.MessageKeys.ERROR_DIGEST_CONFIGURATION_NOT_FOUND;
import static org.openlmis.notification.web.BaseController.API_PREFIX;
import static org.openlmis.notification.web.digestconfiguration.DigestConfigurationController.RESOURCE_URL;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.repository.DigestConfigurationRepository;
import org.openlmis.notification.util.Pagination;
import org.openlmis.notification.web.BaseController;
import org.openlmis.notification.web.NotFoundException;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Transactional
@RestController
@RequestMapping(RESOURCE_URL)
public class DigestConfigurationController extends BaseController {

  public static final String RESOURCE_URL = API_PREFIX + "/digestConfiguration";

  @Autowired
  private DigestConfigurationRepository digestConfigurationRepository;

  /**
   * Gets a page of {@link DigestConfigurationDto}.
   */
  @GetMapping
  public Page<DigestConfigurationDto> getDigestConfigurations(Pageable pageable) {
    Profiler profiler = getProfiler("GET_DIGEST_CONFIGURATIONS", pageable);

    profiler.start("CALL_DB");
    Page<DigestConfiguration> page = digestConfigurationRepository.findAll(pageable);

    profiler.start("CONVERT_TO_DTO");
    List<DigestConfigurationDto> content = page
        .getContent()
        .stream()
        .map(DigestConfigurationDto::newInstance)
        .collect(Collectors.toList());

    profiler.start("CREATE_PAGE");
    Page<DigestConfigurationDto> pageDto = Pagination
        .getPage(content, pageable, page.getTotalElements());

    return stopProfilerAndReturnValue(profiler, pageDto);
  }

  /**
   * Gets single {@link DigestConfigurationDto} based on id.
   */
  @GetMapping("/{id}")
  public DigestConfigurationDto getDigestConfiguration(@PathVariable("id") UUID id) {
    Profiler profiler = getProfiler("GET_DIGEST_CONFIGURATION", id);

    profiler.start("CALL_DB");
    DigestConfiguration configuration = digestConfigurationRepository.findOne(id);

    if (null == configuration) {
      NotFoundException exception = new NotFoundException(ERROR_DIGEST_CONFIGURATION_NOT_FOUND);
      stopProfilerAndThrowException(profiler, exception);
    }

    profiler.start("CONVERT_TO_DTO");
    DigestConfigurationDto dto = DigestConfigurationDto.newInstance(configuration);

    return stopProfilerAndReturnValue(profiler, dto);
  }

}
