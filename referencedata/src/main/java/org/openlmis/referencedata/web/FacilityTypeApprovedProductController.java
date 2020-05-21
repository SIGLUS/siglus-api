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

import static org.openlmis.referencedata.domain.RightName.FACILITY_APPROVED_ORDERABLES_MANAGE;
import static org.openlmis.referencedata.web.FacilityTypeApprovedProductController.RESOURCE_PATH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.ApprovedProductDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityTypeApprovedProductRepository;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.service.FacilityTypeApprovedProductBuilder;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.util.UuidUtil;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeApprovedProductMessageKeys;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping(RESOURCE_PATH)
public class FacilityTypeApprovedProductController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory
      .getXLogger(FacilityTypeApprovedProductController.class);

  public static final String RESOURCE_PATH = API_PATH + "/facilityTypeApprovedProducts";

  @Autowired
  private FacilityTypeApprovedProductRepository repository;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private FacilityTypeApprovedProductBuilder facilityTypeApprovedProductBuilder;

  /**
   * Allows creating new facilityTypeApprovedProduct.
   *
   * @param approvedProductDto A facilityTypeApprovedProduct bound to the request body.
   * @return the created facilityTypeApprovedProduct.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApprovedProductDto createFacilityTypeApprovedProduct(
        @RequestBody ApprovedProductDto approvedProductDto) {
    Profiler profiler = new Profiler("CREATE_FACILITY_TYPE_APPROVED_PRODUCT");
    profiler.setLogger(XLOGGER);

    checkAdminRight(FACILITY_APPROVED_ORDERABLES_MANAGE, profiler);

    if (null != approvedProductDto.getId()) {
      profiler.stop().log();
      throw new ValidationMessageException(
          FacilityTypeApprovedProductMessageKeys.ERROR_ID_PROVIDED);
    }

    profiler.start("BUILD_FTAP_FROM_DTO");
    XLOGGER.debug("Creating new facilityTypeApprovedProduct");
    FacilityTypeApprovedProduct facilityTypeApprovedProduct = facilityTypeApprovedProductBuilder
        .build(approvedProductDto);

    profiler.start("SAVE");
    FacilityTypeApprovedProduct save = repository.save(facilityTypeApprovedProduct);

    ApprovedProductDto dto = toDto(save, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Allows updating facilityTypeApprovedProduct.
   *
   * @param approvedProductDto A facilityTypeApprovedProduct bound to the request body.
   * @param facilityTypeApprovedProductId UUID of facilityTypeApprovedProduct
   *                                      which we want to update.
   * @return the updated facilityTypeApprovedProduct.
   */
  @PutMapping("/{id}")
  public ApprovedProductDto updateFacilityTypeApprovedProduct(
        @RequestBody ApprovedProductDto approvedProductDto,
        @PathVariable("id") UUID facilityTypeApprovedProductId) {
    Profiler profiler = new Profiler("UPDATE_FACILITY_TYPE_APPROVED_PRODUCT");
    profiler.setLogger(XLOGGER);

    checkAdminRight(FACILITY_APPROVED_ORDERABLES_MANAGE, profiler);

    if (!UuidUtil.sameId(approvedProductDto.getId(), facilityTypeApprovedProductId)) {
      profiler.stop().log();
      throw new ValidationMessageException(
          FacilityTypeApprovedProductMessageKeys.ERROR_ID_MISMATCH);
    }

    profiler.start("GET_PREVIOUS_FTAP");
    FacilityTypeApprovedProduct currentVersion = repository
        .findFirstByIdentityIdOrderByIdentityVersionNumberDesc(facilityTypeApprovedProductId);
    long versionNumber = 1;

    if (null != currentVersion) {
      profiler.start("DEACTIVATE_PREVIOUS_FTAP_VERSIONS");
      repository.deactivatePreviousVersions(facilityTypeApprovedProductId);

      versionNumber = currentVersion.getVersionNumber() + 1;
    }

    profiler.start("BUILD_FTAP_FROM_DTO");
    ApprovedProductDto data = new ApprovedProductDto(approvedProductDto);
    data.setVersionNumber(versionNumber);

    FacilityTypeApprovedProduct newVersion = facilityTypeApprovedProductBuilder.build(data);

    profiler.start("SAVE");
    FacilityTypeApprovedProduct save = repository.save(newVersion);

    ApprovedProductDto dto = toDto(save, profiler);

    profiler.stop().log();
    return dto;
  }

  /**
   * Get chosen facilityTypeApprovedProduct.
   *
   * @param id UUID of facilityTypeApprovedProduct which we want to get
   * @param versionNumber specify which version should be returned. If null provided, the latest
   *                  version will be returned.
   * @return the FacilityTypeApprovedProduct.
   */
  @GetMapping("/{id}")
  public ApprovedProductDto getFacilityTypeApprovedProduct(@PathVariable("id") UUID id,
      @RequestParam(name = "versionNumber", required = false) Long versionNumber) {
    Profiler profiler = new Profiler("GET_FACILITY_TYPE_APPROVED_PRODUCT");
    profiler.setLogger(XLOGGER);

    FacilityTypeApprovedProduct facilityTypeApprovedProduct;

    if (null == versionNumber) {
      profiler.start("FIND_FTAP_BY_ID");
      facilityTypeApprovedProduct = repository
          .findFirstByIdentityIdOrderByIdentityVersionNumberDesc(id);
    } else {
      profiler.start("FIND_FTAP_BY_ID_AND_VERSION_NAME");
      facilityTypeApprovedProduct =
          repository.findByIdentityIdAndIdentityVersionNumber(id, versionNumber);
    }

    if (facilityTypeApprovedProduct == null) {
      profiler.stop().log();
      throw new NotFoundException(FacilityTypeApprovedProductMessageKeys.ERROR_NOT_FOUND);
    } else {
      ApprovedProductDto dto = toDto(facilityTypeApprovedProduct, profiler);

      profiler.stop().log();
      return dto;
    }
  }

  /**
   * Search approved products by search criteria.
   *
   * @param queryParams a map containing search parameters. Supported keys are:
   *                    * facilityType [required]
   *                    * program
   * @return a page of approved products matching the criteria
   */
  @GetMapping
  public Page<ApprovedProductDto> searchFacilityTypeApprovedProducts(
        @RequestParam MultiValueMap<String, Object> queryParams, Pageable pageable) {
    Profiler profiler = new Profiler("GET_FACILITY_TYPE_APPROVED_PRODUCT");
    profiler.setLogger(XLOGGER);

    profiler.start("PARSE_QUERY_PARAMETERS");
    QueryFacilityTypeApprovedProductSearchParams searchParams =
        new QueryFacilityTypeApprovedProductSearchParams(queryParams);

    profiler.start("SEARCH_FTAPS");
    Page<FacilityTypeApprovedProduct> ftaps = repository.searchProducts(searchParams, pageable);

    List<ApprovedProductDto> content = toDto(ftaps.getContent(), profiler);

    profiler.start("CREATE_PAGE");
    Page<ApprovedProductDto> page = Pagination.getPage(content, pageable, ftaps.getTotalElements());

    profiler.stop().log();
    return page;
  }

  /**
   * Search approved products by search criteria.
   *
   * @param body - specify criteria for approved products.
   * @return a page of approved products matching the criteria
   */
  @PostMapping("/search")
  public Page<ApprovedProductDto> searchFacilityTypeApprovedProducts(
      @RequestBody FacilityTypeApprovedProductSearchParams body) {
    Profiler profiler = new Profiler("SEARCH_FACILITY_TYPE_APPROVED_PRODUCT");
    profiler.setLogger(XLOGGER);

    profiler.start("SEARCH_FTAPS");
    Pageable pageable = body.getPageable();
    Page<FacilityTypeApprovedProduct> ftaps = repository.searchProducts(body, pageable);

    List<ApprovedProductDto> content = toDto(ftaps.getContent(), profiler);

    profiler.start("CREATE_PAGE");
    Page<ApprovedProductDto> page = Pagination.getPage(content, pageable, ftaps.getTotalElements());

    profiler.stop().log();
    return page;
  }

  /**
   * Allows deleting facilityTypeApprovedProduct.
   *
   * @param id UUID of facilityTypeApprovedProduct which we want to delete.
   * @param versionNumber Specify what version of the resource should be deleted.
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFacilityTypeApprovedProduct(@PathVariable("id") UUID id,
      @RequestParam(name = "versionNumber", required = false) Long versionNumber) {
    Profiler profiler = new Profiler("DELETE_FACILITY_TYPE_APPROVED_PRODUCT");
    profiler.setLogger(XLOGGER);

    checkAdminRight(FACILITY_APPROVED_ORDERABLES_MANAGE, profiler);

    if (null == versionNumber) {
      profiler.start("FIND_ALL_FTAPS_BY_ID");
      List<FacilityTypeApprovedProduct> facilityTypeApprovedProducts = repository
          .findByIdentityId(id);

      if (CollectionUtils.isEmpty(facilityTypeApprovedProducts)) {
        throw new NotFoundException(FacilityTypeApprovedProductMessageKeys.ERROR_NOT_FOUND);
      }

      profiler.start("DELETE_ALL_VERSIONS");
      repository.delete(facilityTypeApprovedProducts);
    } else {
      profiler.start("FIND_FTAP_BY_ID_AND_VERSION_NAME");
      FacilityTypeApprovedProduct facilityTypeApprovedProduct = repository
          .findByIdentityIdAndIdentityVersionNumber(id, versionNumber);

      if (facilityTypeApprovedProduct == null) {
        throw new NotFoundException(FacilityTypeApprovedProductMessageKeys.ERROR_NOT_FOUND);
      }

      profiler.start("DELETE");
      repository.delete(facilityTypeApprovedProduct);
    }

    profiler.stop().log();
  }


  /**
   * Get the audit information related to facility type approved products.
   *  @param author The author of the changes which should be returned.
   *               If null or empty, changes are returned regardless of author.
   * @param changedPropertyName The name of the property about which changes should be returned.
   *               If null or empty, changes associated with any and all properties are returned.
   * @param page A Pageable object that allows client to optionally add "page" (page number)
   *             and "size" (page size) query parameters to the request.
   */
  @GetMapping("/{id}/auditLog")
  public ResponseEntity<String> getFacilityTypeApprovedProductAuditLog(
      @PathVariable("id") UUID id,
      @RequestParam(name = "author", required = false, defaultValue = "") String author,
      @RequestParam(name = "changedPropertyName", required = false, defaultValue = "")
          String changedPropertyName,
      //Because JSON is all we formally support, returnJSON is excluded from our JavaDoc
      @RequestParam(name = "returnJSON", required = false, defaultValue = "true")
          boolean returnJson,
      Pageable page) {

    rightService.checkAdminRight(FACILITY_APPROVED_ORDERABLES_MANAGE);

    //Return a 404 if the specified instance can't be found
    FacilityTypeApprovedProduct instance = repository
        .findFirstByIdentityIdOrderByIdentityVersionNumberDesc(id);

    if (instance == null) {
      throw new NotFoundException(FacilityTypeApprovedProductMessageKeys.ERROR_NOT_FOUND);
    }

    return getAuditLogResponse(
        FacilityTypeApprovedProduct.class, id, author, changedPropertyName, page, returnJson
    );
  }

  private ApprovedProductDto toDto(FacilityTypeApprovedProduct prod, Profiler profiler) {
    profiler.start("EXPORT_FTAP_TO_DTO");

    ApprovedProductDto productDto = new ApprovedProductDto();
    prod.export(productDto);

    Orderable orderable = orderableRepository
        .findFirstByIdentityIdOrderByIdentityVersionNumberDesc(prod.getOrderableId());

    productDto.setOrderable(orderable);

    return productDto;
  }

  private List<ApprovedProductDto> toDto(Collection<FacilityTypeApprovedProduct> prods,
      Profiler profiler) {
    if (prods.isEmpty()) {
      return Collections.emptyList();
    }

    profiler.start("RETRIEVE_LATEST_ORDERABLES_BY_IDS");
    Set<UUID> orderableId = prods
        .stream()
        .map(FacilityTypeApprovedProduct::getOrderableId)
        .collect(Collectors.toSet());

    Map<UUID, Orderable> orderables = orderableRepository
        .findAllLatestByIds(orderableId, new PageRequest(0, orderableId.size()))
        .getContent()
        .stream()
        .collect(Collectors.toMap(Orderable::getId, Function.identity()));

    profiler.start("EXPORT_FTAPS_TO_DTO");
    List<ApprovedProductDto> dtos = new ArrayList<>();
    for (FacilityTypeApprovedProduct ftap : prods) {
      ApprovedProductDto productDto = new ApprovedProductDto();
      ftap.export(productDto);

      productDto.setOrderable(orderables.get(ftap.getOrderableId()));

      dtos.add(productDto);
    }

    return dtos;
  }

}
