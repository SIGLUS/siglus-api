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
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;
import static org.siglus.siglusapi.i18n.PermissionMessageKeys.ERROR_NO_FOLLOWING_PERMISSION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openlmis.referencedata.service.ReferencedataAuthenticationHelper;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.utils.Pagination;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.openlmis.stockmanagement.util.Message;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.common.util.FormatHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

@Service
@SuppressWarnings({"PMD.PreserveStackTrace"})
public class SiglusStockCardSummariesService {

  private static final String PROGRAM_ID = "programId";
  private static final String EXCLUDE_ARCHIVED = "excludeArchived";
  private static final String ARCHIVED_ONLY = "archivedOnly";
  private static final String NON_EMPTY_ONLY = "nonEmptyOnly";

  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private ReferencedataAuthenticationHelper authenticationHelper;

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private StockCardSummariesV2DtoBuilder stockCardSummariesV2DtoBuilder;

  public StockCardSummaries findSiglusStockCard(
      MultiValueMap<String, String> parameters) {
    UUID inputProgramId = getId(PROGRAM_ID, parameters);

    UUID userId = authenticationHelper.getCurrentUser().getId();
    Set<UUID> programIds = getProgramIds(inputProgramId, userId, parameters.getFirst(RIGHT_NAME),
        parameters.getFirst(FACILITY_ID));

    StockCardSummaries siglusSummaries = new StockCardSummaries();
    siglusSummaries.setStockCardsForFulfillOrderables(new ArrayList<>());
    siglusSummaries.setOrderableFulfillMap(new HashMap<>());
    Set<String> archivedProducts = archiveProductService
        .searchArchivedProducts(UUID.fromString(parameters.getFirst(FACILITY_ID)));
    for (UUID programId : programIds) {
      parameters.put(PROGRAM_ID, Collections.singletonList(programId.toString()));
      StockCardSummariesV2SearchParams v2SearchParams = new
          StockCardSummariesV2SearchParams(parameters);
      // call modify stockCardSummariesService for orderable support virtual program
      StockCardSummaries summaries = stockCardSummariesService.findStockCards(v2SearchParams);
      siglusSummaries.setAsOfDate(summaries.getAsOfDate());
      if (Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED))) {
        siglusSummaries.getStockCardsForFulfillOrderables()
            .addAll(summaries.getStockCardsForFulfillOrderables()
                .stream()
                .filter(isNotArchived(archivedProducts))
                .collect(Collectors.toList()));
      } else if (Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY))) {
        siglusSummaries.getStockCardsForFulfillOrderables()
            .addAll(summaries.getStockCardsForFulfillOrderables()
                .stream()
                .filter(isArchived(archivedProducts))
                .collect(Collectors.toList()));
      } else {
        siglusSummaries.getStockCardsForFulfillOrderables()
            .addAll(summaries.getStockCardsForFulfillOrderables());
      }
      siglusSummaries.getOrderableFulfillMap().putAll(summaries.getOrderableFulfillMap());
    }
    return siglusSummaries;
  }

  public Page<StockCardSummaryV2Dto> searchStockCardSummaryV2Dtos(
      MultiValueMap<String, String> parameters, Pageable pageable) {
    try {
      // reason: support all program && archive
      StockCardSummaries summaries = findSiglusStockCard(parameters);

      List<StockCardSummaryV2Dto> dtos = stockCardSummariesV2DtoBuilder.build(
          summaries.getStockCardsForFulfillOrderables(),
          summaries.getOrderableFulfillMap(),
          Boolean.parseBoolean(parameters.getFirst(NON_EMPTY_ONLY)));

      return Pagination.getPage(dtos, pageable);
    } catch (PermissionMessageException e) {
      if (parameters.getFirst(RIGHT_NAME).equals(STOCK_INVENTORIES_EDIT)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED));
      }

      throw e;
    }
  }

  private Predicate<StockCard> isArchived(Set<String> archivedProducts) {
    return stockCard -> archivedProducts.contains(stockCard.getOrderableId().toString());
  }

  private Predicate<StockCard> isNotArchived(Set<String> archivedProducts) {
    return stockCard -> !archivedProducts.contains(stockCard.getOrderableId().toString());
  }

  private Set<UUID> getProgramIds(UUID programId, UUID userId, String rightName,
      String facilityId) {
    Set<UUID> programIds = newHashSet();
    Set<PermissionStringDto> permissionStrings = permissionService
        .getPermissionStrings(userId).get();
    List<ProgramExtension> programExtensions = programExtensionRepository.findByIsVirtual(true);
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
      return programsByRight
          .stream()
          .filter(programByRight -> isVirtual(programExtensions, programByRight))
          .collect(Collectors.toSet());
    }
    if (isVirtual(programExtensions, programId)) {
      programIds.add(programId);
    } else {
      programIds.add(programExtensionRepository.findByProgramId(programId).getParentId());
    }
    return programIds;
  }

  private boolean isVirtual(List<ProgramExtension> programVirtualExtensions, UUID programId) {
    for (ProgramExtension extension : programVirtualExtensions) {
      if (extension.getProgramId().equals(programId)) {
        return true;
      }
    }
    return false;
  }

  private UUID getId(String fieldName, MultiValueMap<String, String> parameters) {
    String id = parameters.getFirst(fieldName);
    return FormatHelper.formatId(id, fieldName);
  }

}
