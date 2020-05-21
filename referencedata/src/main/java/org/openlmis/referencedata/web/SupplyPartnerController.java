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

import static org.openlmis.referencedata.web.SupplyPartnerController.RESOURCE_PATH;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.openlmis.referencedata.domain.RightName;
import org.openlmis.referencedata.domain.SupplyPartner;
import org.openlmis.referencedata.dto.SupplyPartnerDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.SupplyPartnerRepository;
import org.openlmis.referencedata.service.SupplyPartnerBuilder;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Transactional
@RestController
@RequestMapping(RESOURCE_PATH)
public class SupplyPartnerController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(SupplyPartnerController.class);

  public static final String RESOURCE_PATH = BaseController.API_PATH + "/supplyPartners";

  public static final String ID_URL = "/{id}";
  private static final String AUDIT_LOG_URL = ID_URL + "/auditLog";

  @Autowired
  private SupplyPartnerRepository supplyPartnerRepository;

  @Autowired
  private SupplyPartnerBuilder supplyPartnerBuilder;

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Retrieves all supply partners.
   *
   * @return List of supply partners.
   */
  @GetMapping
  public Page<SupplyPartnerDto> getSupplyPartners(
      @RequestParam MultiValueMap<String, Object> requestParams,
      Pageable pageable) {
    Profiler profiler = new Profiler("GET_SUPPLY_PARTNERS");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SUPPLY_PARTNERS_MANAGE, profiler);

    profiler.start("FIND_SUPPLY_PARTNERS");
    SupplyPartnerSearchParams searchParams = new SupplyPartnerSearchParams(requestParams);
    Page<SupplyPartner> supplyPartners = supplyPartnerRepository.search(searchParams, pageable);

    List<SupplyPartnerDto> supplyPartnerDtos = toDtos(supplyPartners.getContent(), profiler);

    profiler.start("CREATE_FINAL_RESULT_PAGE");
    Page<SupplyPartnerDto> page = Pagination.getPage(supplyPartnerDtos, pageable,
        supplyPartners.getTotalElements());

    profiler.stop().log();
    return page;
  }

  /**
   * Allows creating new supply partner. If the id is specified, it will be ignored.
   *
   * @param supplyPartnerDto A supply partner bound to the request body.
   * @return created supply partner.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SupplyPartnerDto createSupplyPartner(@RequestBody SupplyPartnerDto supplyPartnerDto) {
    Profiler profiler = new Profiler("CREATE_SUPPLY_PARTNER");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SUPPLY_PARTNERS_MANAGE, profiler);

    profiler.start("BUILD_SUPPLY_PARTNER_FROM_DTO");
    if (null != supplyPartnerDto.getId()) {
      throw new ValidationMessageException(SupplyPartnerMessageKeys.ERROR_ID_PROVIDED);
    }

    SupplyPartner supplyPartner = supplyPartnerBuilder.build(supplyPartnerDto);

    profiler.start("SAVE");
    supplyPartner = supplyPartnerRepository.save(supplyPartner);

    SupplyPartnerDto dto = toDto(supplyPartner, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Get chosen supply partner.
   *
   * @param id UUID of supply partner which we want to get
   * @return supply partner.
   */
  @GetMapping(ID_URL)
  public SupplyPartnerDto getSupplyPartner(@PathVariable("id") UUID id) {
    Profiler profiler = new Profiler("GET_SUPPLY_PARTNER");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SUPPLY_PARTNERS_MANAGE, profiler);

    SupplyPartner supplyPartner = findSupplyPartner(id, profiler);

    SupplyPartnerDto dto = toDto(supplyPartner, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Allows updating an existing supply partner or create a new one with the given id value.
   *
   * @param id UUID of supply partner which we want to update.
   * @param supplyPartnerDto A supply partner bound to the request body.
   * @return the updated/created supply partner.
   */
  @PutMapping(ID_URL)
  public SupplyPartnerDto updateSupplyPartner(@PathVariable("id") UUID id,
      @RequestBody SupplyPartnerDto supplyPartnerDto) {
    Profiler profiler = new Profiler("UPDATE_SUPPLY_PARTNER");
    profiler.setLogger(XLOGGER);

    if (null != supplyPartnerDto.getId() && !Objects.equals(supplyPartnerDto.getId(), id)) {
      throw new ValidationMessageException(SupplyPartnerMessageKeys.ERROR_ID_MISMATCH);
    }

    checkAdminRight(RightName.SUPPLY_PARTNERS_MANAGE, profiler);

    profiler.start("BUILD_SUPPLY_PARTNER_FROM_DTO");
    SupplyPartner supplyPartner = supplyPartnerBuilder.build(supplyPartnerDto);

    profiler.start("SAVE_SUPPLY_PARTNER");
    supplyPartner = supplyPartnerRepository.save(supplyPartner);

    SupplyPartnerDto dto = toDto(supplyPartner, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Get the audit information related to supply partner.
   *
   * @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *                            If null or empty, changes associated with any and all properties
   *                            are returned.
   * @param pageable A Pageable object that allows client to optionally add "page" (page number)
   *                 and "size" (page size) query parameters to the request.
   */
  @GetMapping(AUDIT_LOG_URL)
  public ResponseEntity<String> getAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable pageable) {

    Profiler profiler = new Profiler("GET_AUDIT_LOG");
    profiler.setLogger(XLOGGER);

    checkAdminRight(RightName.SUPPLY_PARTNERS_MANAGE, profiler);
    findSupplyPartner(id, profiler);

    profiler.start("GET_AUDIT_LOG");
    ResponseEntity<String> response = getAuditLogResponse(
        SupplyPartner.class, id, author, changedPropertyName, pageable, returnJson
    );

    profiler.stop().log();
    return response;
  }

  private SupplyPartner findSupplyPartner(UUID id, Profiler profiler) {
    profiler.start("FIND_SUPPLY_PARTNER");
    SupplyPartner supplyPartner = supplyPartnerRepository.findOne(id);

    if (supplyPartner == null) {
      profiler.stop().log();
      throw new NotFoundException(
          new Message(SupplyPartnerMessageKeys.ERROR_NOT_FOUND_WITH_ID, id));
    }

    return supplyPartner;
  }

  private List<SupplyPartnerDto> toDtos(List<SupplyPartner> partners, Profiler profiler) {
    profiler.start("EXPORT_SUPPLY_PARTNERS_TO_DTOS");
    return partners
        .stream()
        .map(elem -> SupplyPartnerDto.newInstance(elem, serviceUrl))
        .collect(Collectors.toList());
  }

  private SupplyPartnerDto toDto(SupplyPartner supplyPartner, Profiler profiler) {
    profiler.start("EXPORT_SUPPLY_PARTNER_TO_DTO");
    return SupplyPartnerDto.newInstance(supplyPartner, serviceUrl);
  }

}
