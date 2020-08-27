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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Sets.newHashSet;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_INVENTORIES_EDIT;
import static org.siglus.common.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.PaginationConstants.DEFAULT_PAGE_NUMBER;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.PermissionMessageKeys.ERROR_NO_FOLLOWING_PERMISSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.util.Message;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.util.FormatHelper;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.service.client.SiglusStockCardStockManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class SiglusStockCardSummariesService {

  private static final String PROGRAM_ID = "programId";
  private static final String EXCLUDE_ARCHIVED = "excludeArchived";
  private static final String ARCHIVED_ONLY = "archivedOnly";

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  SiglusStockCardStockManagementService siglusStockManagementService;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  public Page<StockCardSummaryV2Dto> findSiglusStockCard(
      MultiValueMap<String, String> parameters, Pageable pageable) {
    Set<String> archivedProducts = null;
    if (Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED)) || Boolean
        .parseBoolean(parameters.getFirst(ARCHIVED_ONLY))) {
      archivedProducts = archiveProductService
          .searchArchivedProducts(UUID.fromString(parameters.getFirst(FACILITY_ID)));
    }
    StockCardSummariesV2SearchParams v2SearchParams = new StockCardSummariesV2SearchParams(
        parameters);
    List<UUID> orderableIds = v2SearchParams.getOrderableIds();
    UUID inputProgramId = getId(PROGRAM_ID, parameters);
    UUID userId = authenticationHelper.getCurrentUser().getId();
    Set<UUID> programIds = getProgramIds(inputProgramId, userId, parameters.getFirst(RIGHT_NAME),
        parameters.getFirst(FACILITY_ID));
    List<StockCardSummaryV2Dto> summaryV2Dtos = new ArrayList<>();
    for (UUID programId : programIds) {
      v2SearchParams.setProgramId(programId);
      Pageable searchPageable = pageable;
      if (archiveEmptyForArchiveOnly(parameters, archivedProducts)) {
        return Pagination.getPage(Collections.emptyList(), searchPageable);
      } else if (needFilterOrSearchAllProgram(orderableIds, parameters, archivedProducts,
          programIds)) {
        searchPageable = new PageRequest(DEFAULT_PAGE_NUMBER, Integer.MAX_VALUE);
      } else {
        return siglusStockManagementService.search(v2SearchParams, searchPageable);
      }
      getSummaries(parameters, archivedProducts, v2SearchParams, orderableIds, summaryV2Dtos,
          searchPageable);
    }
    return Pagination.getPage(summaryV2Dtos, pageable);
  }

  public Set<UUID> getProgramIds(UUID programId, UUID userId, String rightName,
      String facilityId) {
    Set<UUID> programIds = newHashSet();
    Set<PermissionStringDto> permissionStrings = permissionService
        .getPermissionStrings(userId).get();
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      Set<UUID> programsByRight = permissionStrings
          .stream()
          .filter(permissionStringDto -> permissionStringDto.getRightName().equals(rightName)
              && UUID.fromString(facilityId).equals(permissionStringDto.getFacilityId())
          )
          .map(PermissionStringDto::getProgramId)
          .collect(Collectors.toSet());
      if (CollectionUtils.isEmpty(programsByRight)) {
        throw new PermissionMessageException(
            new Message(ERROR_NO_FOLLOWING_PERMISSION, rightName, facilityId));
      }
      return programsByRight;
    }

    programIds.add(programId);

    return programIds;
  }

  private void getSummaries(MultiValueMap<String, String> parameters, Set<String> archivedProducts,
      StockCardSummariesV2SearchParams v2SearchParams, List<UUID> orderableIds,
      List<StockCardSummaryV2Dto> summaryV2Dtos, Pageable searchPageable) {
    List<StockCardSummaryV2Dto> summaries =
        siglusStockManagementService.search(v2SearchParams, searchPageable).getContent();
    summaries = filterByOrderableIds(orderableIds, summaries);
    if (Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED))) {
      summaryV2Dtos.addAll(summaries.stream()
          .filter(isNotArchived(archivedProducts))
          .collect(Collectors.toList()));
    } else if (Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY))) {
      summaryV2Dtos.addAll(summaries.stream()
          .filter(isArchived(archivedProducts))
          .collect(Collectors.toList()));
    } else {
      summaryV2Dtos.addAll(summaries);
    }
  }

  private boolean archiveEmptyForArchiveOnly(MultiValueMap<String, String> parameters,
      Set<String> archivedProducts) {
    return CollectionUtils.isEmpty(archivedProducts)
        && Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY));
  }

  private boolean needFilterOrSearchAllProgram(List<UUID> orderableIds,
      MultiValueMap<String, String> parameters,
      Set<String> archivedProducts, Set<UUID> programIds) {
    return !CollectionUtils.isEmpty(orderableIds)
        || filterForArchive(parameters, archivedProducts)
        || programIds.size() > 1;
  }

  private boolean filterForArchive(MultiValueMap<String, String> parameters,
      Set<String> archiveProducts) {
    return !CollectionUtils.isEmpty(archiveProducts)
        && (Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY))
        || Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED)));
  }

  private List<StockCardSummaryV2Dto> filterByOrderableIds(List<UUID> orderableIds,
      List<StockCardSummaryV2Dto> summaries) {
    if (CollectionUtils.isEmpty(orderableIds)) {
      return summaries;
    }
    return summaries.stream().filter(summaryV2Dto ->
            orderableIds.contains(summaryV2Dto.getOrderable().getId()))
        .collect(Collectors.toList());
  }

  public Page<StockCardSummaryV2Dto> searchStockCardSummaryV2Dtos(
      MultiValueMap<String, String> parameters, Pageable pageable) {
    try {
      // reason: support all program && archive
      return findSiglusStockCard(parameters, pageable);

    } catch (PermissionMessageException e) {
      if (parameters.getFirst(RIGHT_NAME).equals(STOCK_INVENTORIES_EDIT)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED));
      }

      throw e;
    }
  }

  private Predicate<StockCardSummaryV2Dto> isArchived(Set<String> archivedProducts) {
    return stockCard -> archivedProducts.contains(stockCard.getOrderable().getId().toString());
  }

  private Predicate<StockCardSummaryV2Dto> isNotArchived(Set<String> archivedProducts) {
    return stockCard -> !archivedProducts.contains(stockCard.getOrderable().getId().toString());
  }

  private UUID getId(String fieldName, MultiValueMap<String, String> parameters) {
    String id = parameters.getFirst(fieldName);
    return FormatHelper.formatId(id, fieldName);
  }

}
