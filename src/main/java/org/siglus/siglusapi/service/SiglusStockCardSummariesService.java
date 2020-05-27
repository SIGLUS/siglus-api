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

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_UUID_WRONG_FORMAT;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openlmis.referencedata.service.ReferencedataAuthenticationHelper;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.requisition.utils.Message;
import org.openlmis.stockmanagement.service.StockCardSummaries;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockCardSummariesV2SearchParams;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class SiglusStockCardSummariesService {

  private static final String PROGRAM_ID = "programId";
  // private static final String EXCLUDE_ARCHIVED = "excludeArchived";
  // private static final String ARCHIVED_ONLY = "archivedOnly";
  private static final String STOCK_CARDS_VIEW = "STOCK_CARDS_VIEW";

  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private ReferencedataAuthenticationHelper authenticationHelper;

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private PermissionService permissionService;

  //  @Autowired
  //  private SiglusArchiveProductService archiveProductService;

  public StockCardSummaries findSiglusStockCard(
      MultiValueMap<String, String> parameters) {
    UUID inputProgramId = getId(PROGRAM_ID, parameters);

    UUID userId = authenticationHelper.getCurrentUser().getId();
    List<UUID> programIds = getProgramIds(inputProgramId, userId);

    StockCardSummaries siglusSummaries = new StockCardSummaries();
    siglusSummaries.setStockCardsForFulfillOrderables(new ArrayList<>());
    siglusSummaries.setOrderableFulfillMap(new HashMap<>());
    for (UUID programId : programIds) {
      parameters.put(PROGRAM_ID, Collections.singletonList(programId.toString()));
      StockCardSummariesV2SearchParams v2SearchParams = new
          StockCardSummariesV2SearchParams(parameters);
      // call modify stockCardSummariesService for orderable support virtual program
      StockCardSummaries summaries = stockCardSummariesService.findStockCards(v2SearchParams);
      siglusSummaries.setAsOfDate(summaries.getAsOfDate());
      // TODO: move ArchiveProductService which depend on RequisitionRepository &&
      // SiglusPhysicalInventoryService && draft
      // if (Boolean.parseBoolean(parameters.getFirst(EXCLUDE_ARCHIVED))) {
      //   siglusSummaries.getStockCardsForFulfillOrderables()
      //       .addAll(summaries.getStockCardsForFulfillOrderables().stream()
      //           .filter(archiveProductService::isNotArchived).collect(Collectors.toList()));
      // } else if (Boolean.parseBoolean(parameters.getFirst(ARCHIVED_ONLY))) {
      //   siglusSummaries.getStockCardsForFulfillOrderables()
      //       .addAll(summaries.getStockCardsForFulfillOrderables().stream()
      //           .filter(archiveProductService::isArchived).collect(Collectors.toList()));
      // } else {
      //   siglusSummaries.getStockCardsForFulfillOrderables()
      //       .addAll(summaries.getStockCardsForFulfillOrderables());
      // }
      siglusSummaries.getStockCardsForFulfillOrderables()
          .addAll(summaries.getStockCardsForFulfillOrderables());
      siglusSummaries.getOrderableFulfillMap().putAll(summaries.getOrderableFulfillMap());
    }
    return siglusSummaries;
  }


  private List<UUID> getProgramIds(UUID programId, UUID userId) {
    List<UUID> programIds = new ArrayList<>();
    Set<PermissionStringDto> permissionStrings = permissionService
        .getPermissionStrings(userId).get();
    List<UUID> programsByRight = permissionStrings
        .stream()
        .filter(permissionStringDto -> permissionStringDto.getRightName().equals(STOCK_CARDS_VIEW))
        .map(PermissionStringDto::getProgramId)
        .collect(Collectors.toList());
    List<ProgramExtension> programExtensions = programExtensionRepository.findByIsVirtual(true);
    if (ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      return programsByRight
          .stream()
          .filter(programByRight -> isVirtual(programExtensions, programByRight))
          .collect(Collectors.toList());
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
    return formatId(id, fieldName);
  }

  private UUID formatId(String id, String fieldName) {
    if (null != id) {
      try {
        return UUID.fromString(id);
      } catch (IllegalArgumentException ex) {
        throw new ValidationMessageException(
            new Message(ERROR_UUID_WRONG_FORMAT, id, fieldName), ex);
      }
    }
    return null;
  }
}