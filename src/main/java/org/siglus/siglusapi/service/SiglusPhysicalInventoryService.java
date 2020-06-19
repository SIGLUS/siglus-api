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

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACCEPTABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.service.StockmanagementPermissionService;
import org.siglus.common.util.SupportedVirtualProgramsHelper;
import org.siglus.siglusapi.dto.InitialInventoryFieldDto;
import org.siglus.siglusapi.exception.NotAcceptableException;
import org.siglus.siglusapi.service.client.PhysicalInventoryStockManagementService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.util.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusPhysicalInventoryService {

  @Autowired
  private PhysicalInventoryStockManagementService physicalInventoryStockManagementService;

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private StockmanagementPermissionService permissionService;

  @Autowired
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private PhysicalInventoryService physicalInventoryService;

  @Autowired
  private SupportedVirtualProgramsHelper supportedVirtualProgramsHelper;

  public PhysicalInventoryDto createNewDraft(PhysicalInventoryDto dto) {
    return physicalInventoryStockManagementService.createEmptyPhysicalInventory(dto);
  }

  @Transactional
  public PhysicalInventoryDto createNewDraftForAllProducts(PhysicalInventoryDto dto) {
    Set<UUID> supportedVirtualPrograms = supportedVirtualProgramsHelper
        .findUserSupportedVirtualPrograms();
    List<PhysicalInventoryDto> inventories = supportedVirtualPrograms.stream().map(
        supportedVirtualProgram -> {
          dto.setProgramId(supportedVirtualProgram);
          return createNewDraft(dto);
        }).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      return getResultInventory(inventories);
    }
    return null;
  }

  public PhysicalInventoryDto saveDraft(PhysicalInventoryDto dto, UUID id) {
    physicalInventoryStockManagementService.savePhysicalInventory(id, dto);
    return dto;
  }

  @Transactional
  public PhysicalInventoryDto saveDraftForAllProducts(PhysicalInventoryDto dto) {
    deletePhysicalInventoryForAllProducts(dto.getFacilityId());
    createNewDraftForAllProducts(dto);
    Set<UUID> programIds = dto.getLineItems().stream()
        .map(PhysicalInventoryLineItemDto::getProgramId).collect(Collectors.toSet());
    List<PhysicalInventoryDto> inventories = programIds.stream()
        .map(programId -> getPhysicalInventoryDtos(programId, dto.getFacilityId(), Boolean.TRUE))
        .flatMap(Collection::stream).collect(Collectors.toList());
    inventories.forEach(inventory -> inventory.setLineItems(
        dto.getLineItems().stream()
            .filter(lineItem -> lineItem.getProgramId().equals(inventory.getProgramId()))
            .collect(Collectors.toList())));
    inventories = inventories.stream()
        .map(inventory -> saveDraft(inventory, inventory.getId()))
        .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      return getResultInventory(inventories);
    }
    return null;
  }

  public void deletePhysicalInventory(UUID id) {
    physicalInventoryStockManagementService.deletePhysicalInventory(id);
  }

  @Transactional
  public void deletePhysicalInventoryForAllProducts(UUID facilityId) {
    Set<UUID> supportedVirtualPrograms = supportedVirtualProgramsHelper
        .findUserSupportedVirtualPrograms();
    List<PhysicalInventoryDto> inventories = supportedVirtualPrograms.stream().map(
        supportedVirtualProgram -> getPhysicalInventoryDtos(supportedVirtualProgram,
            facilityId,
            Boolean.TRUE)).flatMap(Collection::stream).collect(Collectors.toList());
    inventories.forEach(inventory -> deletePhysicalInventory(inventory.getId()));
  }

  public PhysicalInventoryDto getPhysicalInventory(UUID id) {
    return physicalInventoryStockManagementService.getPhysicalInventory(id);
  }

  public PhysicalInventoryDto getPhysicalInventoryForAllProducts(UUID facilityId) {
    List<PhysicalInventoryDto> inventories = getPhysicalInventoryDtosForAllProducts(facilityId,
        Boolean.TRUE, Boolean.FALSE);
    if (CollectionUtils.isNotEmpty(inventories)) {
      return inventories.get(0);
    }
    return null;
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtos(UUID program, UUID facility,
      Boolean isDraft) {
    physicalInventoryService.checkPermission(program, facility);
    return physicalInventoryStockManagementService
        .searchPhysicalInventory(program, facility, isDraft);
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForAllProducts(
      UUID facilityId,
      Boolean isDraft,
      boolean canInitialInventory) {
    Set<UUID> supportedVirtualPrograms = supportedVirtualProgramsHelper
        .findUserSupportedVirtualPrograms();
    if (CollectionUtils.isEmpty(supportedVirtualPrograms)) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED,
              ALL_PRODUCTS_PROGRAM_ID));
    }
    List<PhysicalInventoryDto> inventories = supportedVirtualPrograms.stream().map(
        supportedVirtualProgram -> getPhysicalInventoryDtos(supportedVirtualProgram, facilityId,
            isDraft)).flatMap(Collection::stream).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      PhysicalInventoryDto resultInventory = getResultInventory(inventories);
      return Collections.singletonList(resultInventory);
    }
    PhysicalInventoryDto dto = createInitialInventoryDraftForAllProducts(supportedVirtualPrograms,
        facilityId, canInitialInventory);
    if (dto != null) {
      inventories.add(dto);
    }
    return inventories;
  }

  private PhysicalInventoryDto createInitialInventoryDraftForAllProducts(
      Set<UUID> supportedVirtualPrograms, UUID facilityId, boolean canInitialInventory) {
    if (canInitialInventory(facilityId).isCanInitialInventory()) {
      List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos =
          buildInitialInventoryLineItemDtos(supportedVirtualPrograms, facilityId);
      PhysicalInventoryDto dto = PhysicalInventoryDto.builder()
          .facilityId(facilityId)
          .programId(ALL_PRODUCTS_PROGRAM_ID)
          .lineItems(physicalInventoryLineItemDtos)
          .build();
      return saveDraftForAllProducts(dto);
    }
    if (canInitialInventory) {
      throw new NotAcceptableException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    return null;
  }

  private List<PhysicalInventoryLineItemDto> buildInitialInventoryLineItemDtos(
      Set<UUID> supportedVirtualPrograms, UUID facilityId) {
    List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos = new ArrayList<>();
    supportedVirtualPrograms.forEach(supportedVirtualProgram -> {
      List<OrderableDto> approvedProductDtos = approvedProductReferenceDataService
          .getApprovedProducts(facilityId, supportedVirtualProgram, null).getOrderablesPage()
          .getContent();
      approvedProductDtos.forEach(orderable -> {
        if (Boolean.parseBoolean(orderable.getExtraData().get(IS_BASIC))) {
          PhysicalInventoryLineItemDto lineItem = PhysicalInventoryLineItemDto.builder()
              .orderableId(orderable.getId())
              .programId(supportedVirtualProgram)
              .build();
          physicalInventoryLineItemDtos.add(lineItem);
        }
      });
    });
    return physicalInventoryLineItemDtos;
  }

  private PhysicalInventoryDto getResultInventory(List<PhysicalInventoryDto> inventories) {
    return PhysicalInventoryDto.builder()
        .id(ALL_PRODUCTS_UUID)
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .facilityId(inventories.get(0).getFacilityId())
        .occurredDate(inventories.get(0).getOccurredDate())
        .signature(inventories.get(0).getSignature())
        .documentNumber(inventories.get(0).getDocumentNumber())
        .isStarter(inventories.get(0).getIsStarter())
        .isDraft(inventories.get(0).getIsDraft())
        .lineItems(inventories.stream()
            .map(inventory -> {
              Optional<List<PhysicalInventoryLineItemDto>> optionalList = Optional
                  .ofNullable(inventory.getLineItems());
              optionalList
                  .ifPresent(physicalInventoryLineItemDtos -> physicalInventoryLineItemDtos.forEach(
                      physicalInventoryLineItemDto -> physicalInventoryLineItemDto
                          .setProgramId(inventory.getProgramId())));
              return optionalList.orElse(new ArrayList<>());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList()))
        .build();
  }

  public InitialInventoryFieldDto canInitialInventory(UUID facility) {
    boolean canEditPhysicalInventory = true;
    try {
      permissionService.canEditPhysicalInventory(null, facility);
    } catch (org.openlmis.stockmanagement.exception.PermissionMessageException ex) {
      canEditPhysicalInventory = false;
    }
    List<StockCard> stockCards = stockCardRepository.findByFacilityId(facility);
    boolean canInitialInventory = canEditPhysicalInventory && CollectionUtils.isEmpty(stockCards);
    return new InitialInventoryFieldDto(canInitialInventory);
  }

  public Set<String> findPhysicalInventoryDates(UUID facility,
      String startDate,
      String endDate) {
    List<PhysicalInventory> physicalInventories = physicalInventoriesRepository
        .findByFacilityIdAndStartDateAndEndDate(facility, startDate, endDate);
    return physicalInventories.stream()
        .map(physicalInventory -> physicalInventory.getOccurredDate().toString())
        .collect(Collectors.toSet());
  }

  public PhysicalInventoryDto findLatestPhysicalInventory(UUID facilityId) {
    List<PhysicalInventory> found = physicalInventoriesRepository.findByFacilityId(facilityId);

    if (found == null) {
      return null;
    }
    PhysicalInventory latest = found.stream().filter(item -> item.getOccurredDate() != null)
        .max(Comparator.comparing(PhysicalInventory::getOccurredDate)).orElse(null);
    if (latest == null) {
      return null;
    }

    return PhysicalInventoryDto.builder()
        .programId(latest.getProgramId())
        .facilityId(latest.getFacilityId())
        .isDraft(latest.getIsDraft())
        .occurredDate(latest.getOccurredDate())
        .documentNumber(latest.getDocumentNumber())
        .signature(latest.getSignature())
        .build();
  }
}
