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

import static org.openlmis.referencedata.domain.RightName.SUPPLY_LINES_MANAGE;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.SupplyLine;
import org.openlmis.referencedata.dto.SupplyLineDto;
import org.openlmis.referencedata.dto.SupplyLineObjectReferenceDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.repository.SupplyLineRepository;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.SupplyLineMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("api/supplyLines")
public class SupplyLineController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SupplyLineController.class);

  @Value("${service.url}")
  private String sericeUrl;

  @Autowired
  private SupplyLineRepository supplyLineRepository;

  /**
   * Allows creating new supplyLines. If the id is specified, it will be ignored.
   *
   * @param supplyLineDto A supplyLine bound to the request body.
   * @return the created supplyLine.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SupplyLineDto createSupplyLine(@RequestBody SupplyLineDto supplyLineDto) {
    rightService.checkAdminRight(SUPPLY_LINES_MANAGE);
    LOGGER.debug("Creating new supplyLine");

    supplyLineDto.setId(null);
    SupplyLine supplyLine = SupplyLine.newSupplyLine(supplyLineDto);
    supplyLineRepository.save(supplyLine);
    LOGGER.debug("Created new supplyLine with id: " + supplyLine.getId());
    return exportToDto(supplyLine);
  }

  /**
   * Search supply lines by given parameters.
   *
   * @param queryMap map of query parameters (programId, supervisoryNodeId, supplyingFacilityId)
   * @param pageable pagination and sorting parameters
   * @return page of supply sine dtos.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public Page<SupplyLineObjectReferenceDto> search(
      @RequestParam MultiValueMap<String, Object> queryMap,
      Pageable pageable) {
    Profiler profiler = new Profiler("SEARCH_SUPPLY_LINES");
    profiler.setLogger(LOGGER);

    profiler.start("CREATE_SEARCH_PARAMS_CLASS");
    SupplyLineSearchParams params = new SupplyLineSearchParams(queryMap);

    profiler.start("REPOSITORY_SEARCH");
    Page<SupplyLine> result = supplyLineRepository.search(
        params.getProgramId(), params.getSupervisoryNodeId(), params.getSupplyingFacilityIds(),
        pageable);

    profiler.start("BUILD_DTO_WITH_EXPAND");
    Page<SupplyLineObjectReferenceDto> page =
        exportToDtoWithExpand(result, pageable, params.getExpand());

    profiler.stop().log();
    return page;
  }

  /**
   * Allows updating supplyLines.
   *
   * @param supplyLineDto A supplyLineDto bound to the request body.
   * @param id            UUID of supplyLine which we want to update.
   * @return the updated supplyLine.
   */
  @PutMapping("{id}")
  @ResponseStatus(HttpStatus.OK)
  public SupplyLineDto updateSupplyLine(@RequestBody SupplyLineDto supplyLineDto,
      @PathVariable UUID id) {
    rightService.checkAdminRight(SUPPLY_LINES_MANAGE);

    SupplyLine supplyLineToUpdate = supplyLineRepository.findOne(id);
    if (supplyLineToUpdate == null) {
      supplyLineToUpdate = new SupplyLine();
      LOGGER.debug("Creating new supplyLine");
    } else {
      LOGGER.debug("Updating supplyLine with id: " + id);
    }

    supplyLineToUpdate.updateFrom(SupplyLine.newSupplyLine(supplyLineDto));
    supplyLineRepository.save(supplyLineToUpdate);

    LOGGER.debug("Saved supplyLine with id: " + supplyLineToUpdate.getId());
    return exportToDto(supplyLineToUpdate);
  }

  /**
   * Get the audit information related to stock supply line.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @GetMapping("{id}/auditLog")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<String> getSupplyLineAuditLog(
      @PathVariable UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {
    rightService.checkAdminRight(SUPPLY_LINES_MANAGE);

    //Return a 404 if the specified instance can't be found
    SupplyLine instance = supplyLineRepository.findOne(id);
    if (instance == null) {
      throw new NotFoundException(SupplyLineMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(SupplyLine.class, id, author, changedPropertyName, page,
        returnJson);
  }

  /**
   * Get chosen supplyLine.
   *
   * @param id UUID of supplyLine which we want to get.
   * @return the SupplyLine.
   */
  @GetMapping("{id}")
  @ResponseStatus(HttpStatus.OK)
  public SupplyLineDto getSupplyLine(@PathVariable UUID id) {
    SupplyLine supplyLine = supplyLineRepository.findOne(id);
    if (supplyLine == null) {
      throw new NotFoundException(SupplyLineMessageKeys.ERROR_NOT_FOUND);
    } else {
      return exportToDto(supplyLine);
    }
  }

  /**
   * Allows deleting supplyLine.
   *
   * @param id UUID of supplyLine which we want to delete
   */
  @DeleteMapping("{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSupplyLine(@PathVariable UUID id) {
    rightService.checkAdminRight(SUPPLY_LINES_MANAGE);

    SupplyLine supplyLine = supplyLineRepository.findOne(id);
    if (supplyLine == null) {
      throw new NotFoundException(SupplyLineMessageKeys.ERROR_NOT_FOUND);
    } else {
      supplyLineRepository.delete(supplyLine);
    }
  }

  private SupplyLineObjectReferenceDto export(SupplyLine supplyLine, Set<String> expand) {
    SupplyLineObjectReferenceDto supplyLineDto = null;

    if (supplyLine != null) {
      supplyLineDto = SupplyLineObjectReferenceDto.newInstance(supplyLine, sericeUrl);
      expandDto(supplyLineDto, supplyLine, expand);
    }

    return supplyLineDto;
  }

  private SupplyLineDto exportToDto(SupplyLine supplyLine) {
    SupplyLineDto supplyLineDto = null;

    if (supplyLine != null) {
      supplyLineDto = new SupplyLineDto();
      supplyLine.export(supplyLineDto);
    }

    return supplyLineDto;
  }

  private Page<SupplyLineObjectReferenceDto> exportToDtoWithExpand(Page<SupplyLine> page,
      Pageable pageable, Set<String> expand) {
    List<SupplyLineObjectReferenceDto> list = page.getContent().stream()
        .map(supplyLine -> export(supplyLine, expand))
        .collect(Collectors.toList());
    return Pagination.getPage(list, pageable, page.getTotalElements());
  }
}
