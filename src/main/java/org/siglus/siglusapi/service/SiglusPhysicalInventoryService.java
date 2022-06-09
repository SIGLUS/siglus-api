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

import static java.util.Collections.emptyList;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_PROGRAM_NOT_SUPPORTED;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.AC;
import static org.siglus.siglusapi.constant.FacilityTypeConstants.CENTRAL;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_UUID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACCEPTABLE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.stockmanagement.domain.physicalinventory.PhysicalInventory;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.PhysicalInventoriesRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.PhysicalInventoryService;
import org.openlmis.stockmanagement.web.PhysicalInventoryController;
import org.siglus.siglusapi.domain.PhysicalInventoryLineItemsExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.InitialInventoryFieldDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.PhysicalInventoryLineItemsExtensionRepository;
import org.siglus.siglusapi.service.client.PhysicalInventoryStockManagementService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.PreserveStackTrace"})
public class SiglusPhysicalInventoryService {

  @Autowired
  private PhysicalInventoryStockManagementService physicalInventoryStockManagementService;

  @Autowired
  private PhysicalInventoriesRepository physicalInventoriesRepository;

  @Autowired
  private StockCardRepository stockCardRepository;

  @Autowired
  private SiglusApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private PhysicalInventoryService physicalInventoryService;

  @Autowired
  private SupportedProgramsHelper supportedProgramsHelper;

  @Autowired
  private PhysicalInventoryLineItemsExtensionRepository lineItemsExtensionRepository;

  @Autowired
  private PhysicalInventoryController inventoryController;

  @Autowired
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  public PhysicalInventoryDto createNewDraft(PhysicalInventoryDto dto) {
    return physicalInventoryStockManagementService.createEmptyPhysicalInventory(dto);
  }

  public PhysicalInventoryDto createNewDraftDirectly(PhysicalInventoryDto dto) {
    return inventoryController.createEmptyPhysicalInventory(dto);
  }

  public PhysicalInventoryDto createNewDraftForAllProducts(PhysicalInventoryDto dto) {
    return doCreateNewDraftForAllProducts(dto, false);
  }

  public void createNewDraftForAllProductsDirectly(PhysicalInventoryDto dto) {
    doCreateNewDraftForAllProducts(dto, true);
  }

  public PhysicalInventoryDto saveDraft(PhysicalInventoryDto dto, UUID id) {
    physicalInventoryStockManagementService.savePhysicalInventory(id, dto);
    return dto;
  }

  public PhysicalInventoryDto saveDraftForProductsInOneProgram(PhysicalInventoryDto dto) {
    saveDraft(dto, dto.getId());
    List<PhysicalInventoryDto> physicalInventoryDtos = Collections.singletonList(dto);
    if (CollectionUtils.isNotEmpty(physicalInventoryDtos)) {
      return getResultInventory(physicalInventoryDtos, updateExtension(dto, physicalInventoryDtos));
    }
    return null;
  }

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
      return getResultInventoryForAllProducts(inventories, updateExtension(dto, inventories));
    }
    return null;
  }

  public void checkDraftIsExist(UUID facilityId) {
    Set<UUID> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryDto> inventories = supportedPrograms.stream()
        .map(programId -> getPhysicalInventoryDtosDirectly(programId, facilityId, Boolean.TRUE))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(inventories)) {
      throw new ValidationMessageException("stockmanagement.error.physicalInventory.isSubmitted");
    }
  }

  public void deletePhysicalInventory(UUID id) {
    physicalInventoryStockManagementService.deletePhysicalInventory(id);
  }

  public void deletePhysicalInventoryDirectly(UUID id) {
    inventoryController.deletePhysicalInventory(id);
  }

  public void deletePhysicalInventoryForProductInOneProgram(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId, false);
  }

  public void deletePhysicalInventoryForProductInOneProgramDirectly(UUID facilityId, UUID programId) {
    doDeletePhysicalInventoryForProductInOneProgram(facilityId, programId, true);
  }

  public void deletePhysicalInventoryForAllProducts(UUID facilityId) {
    doDeletePhysicalInventoryForAllProducts(facilityId, false);
  }

  public void deletePhysicalInventoryForAllProductsDirectly(UUID facilityId) {
    doDeletePhysicalInventoryForAllProducts(facilityId, true);
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

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosDirectly(UUID program, UUID facility,
      Boolean isDraft) {
    return inventoryController.searchPhysicalInventory(program, facility, isDraft).getBody();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForProductsInOneProgram(
      UUID programId,
      UUID facilityId,
      Boolean isDraft) {
    physicalInventoryService.checkPermission(programId, facilityId);
    Set<UUID> supportedProgram = Collections.singleton(programId);

    List<PhysicalInventoryDto> inventories = supportedProgram.stream().map(
        supportedVirtualProgram -> getPhysicalInventoryDtos(supportedVirtualProgram, facilityId,
            isDraft)).flatMap(Collection::stream).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      List<UUID> updatePhysicalInventoryIds =
          inventories.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
      log.info("find physical inventory extension in one program: {}", updatePhysicalInventoryIds);
      List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
          .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
      PhysicalInventoryDto resultInventory = getResultInventory(inventories, extensions);
      return Collections.singletonList(resultInventory);
    }
    return Collections.emptyList();
  }

  public List<PhysicalInventoryDto> getPhysicalInventoryDtosForAllProducts(
      UUID facilityId,
      Boolean isDraft,
      boolean canInitialInventory) {
    try {
      Set<UUID> supportedPrograms = supportedProgramsHelper
          .findHomeFacilitySupportedProgramIds();
      if (CollectionUtils.isEmpty(supportedPrograms)) {
        throw new PermissionMessageException(
            new org.openlmis.stockmanagement.util.Message(ERROR_PROGRAM_NOT_SUPPORTED,
                ALL_PRODUCTS_PROGRAM_ID));
      }
      List<PhysicalInventoryDto> inventories = supportedPrograms.stream().map(
          supportedVirtualProgram -> getPhysicalInventoryDtos(supportedVirtualProgram, facilityId,
              isDraft)).flatMap(Collection::stream).collect(Collectors.toList());

      if (CollectionUtils.isNotEmpty(inventories)) {
        List<UUID> updatePhysicalInventoryIds =
            inventories.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
        log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
        List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
            .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
        PhysicalInventoryDto resultInventory = getResultInventoryForAllProducts(inventories, extensions);
        return Collections.singletonList(resultInventory);
      }
      PhysicalInventoryDto dto = createInitialInventoryDraftForAllProducts(supportedPrograms,
          facilityId, canInitialInventory);
      if (dto != null) {
        inventories.add(dto);
      }
      return inventories;
    } catch (PermissionMessageException e) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message(ERROR_PERMISSION_NOT_SUPPORTED), e);
    }
  }

  private PhysicalInventoryDto doCreateNewDraftForAllProducts(PhysicalInventoryDto dto, boolean directly) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    List<PhysicalInventoryDto> inventories = supportedPrograms.stream().map(
        supportedVirtualProgram -> {
          dto.setProgramId(supportedVirtualProgram);
          if (directly) {
            return createNewDraftDirectly(dto);
          } else {
            return createNewDraft(dto);
          }
        }).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(inventories)) {
      return getResultInventoryForAllProducts(inventories, emptyList());
    }
    return null;
  }

  private void doDeletePhysicalInventoryForProductInOneProgram(UUID facilityId, UUID programId, boolean directly) {
    doDeletePhysicalInventoryCore(facilityId, programId, directly);
  }

  private void doDeletePhysicalInventoryCore(UUID facilityId, UUID programId, boolean directly) {
    String physicalInventoryId = physicalInventoriesRepository.findIdByProgramIdAndFacilityIdAndIsDraft(
        programId, facilityId, Boolean.TRUE);
    if (physicalInventoryId == null) {
      return;
    }
    List<UUID> physicalInventoryIdList = new ArrayList<>(
        Collections.singleton(UUID.fromString(physicalInventoryId)))
        .stream().filter(Objects::nonNull).collect(Collectors.toList());
    if (directly) {
      physicalInventoryIdList.forEach(this::deletePhysicalInventoryDirectly);
    } else {
      physicalInventoryIdList.forEach(this::deletePhysicalInventory);
    }
    lineItemsExtensionRepository.deleteByPhysicalInventoryIdIn(physicalInventoryIdList);
  }

  private void doDeletePhysicalInventoryForAllProducts(UUID facilityId, boolean directly) {
    Set<UUID> supportedPrograms = supportedProgramsHelper
        .findHomeFacilitySupportedProgramIds();
    supportedPrograms.forEach(programId -> doDeletePhysicalInventoryCore(facilityId, programId, directly));
  }

  private List<PhysicalInventoryLineItemsExtension> updateExtension(
      PhysicalInventoryDto inventoryDto,
      List<PhysicalInventoryDto> updatedDto) {
    List<UUID> updatePhysicalInventoryIds =
        updatedDto.stream().map(PhysicalInventoryDto::getId).collect(Collectors.toList());
    log.info("find physical inventory extension: {}", updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> extensions = lineItemsExtensionRepository
        .findByPhysicalInventoryIdIn(updatePhysicalInventoryIds);
    List<PhysicalInventoryLineItemsExtension> updateExtensions = new ArrayList<>();
    updatedDto.forEach(dto -> {
      if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
        return;
      }
      dto.getLineItems().forEach(lineItem -> {
        PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
        if (extension == null) {
          extension = PhysicalInventoryLineItemsExtension.builder()
              .orderableId(lineItem.getOrderableId())
              .lotId(lineItem.getLotId())
              .physicalInventoryId(dto.getId())
              .build();
        }
        extension.setReasonFreeText(getFreeTextByInput(inventoryDto, lineItem));
        updateExtensions.add(extension);
      });
    });

    log.info("save physical inventory extension, size: {}", updateExtensions.size());
    return lineItemsExtensionRepository.save(updateExtensions);
  }

  private PhysicalInventoryLineItemsExtension getExtension(
      List<PhysicalInventoryLineItemsExtension> extensions, PhysicalInventoryLineItemDto lineItem) {
    return extensions.stream()
        .filter(extension -> compareTwoId(extension.getLotId(), lineItem.getLotId())
            && compareTwoId(extension.getOrderableId(), lineItem.getOrderableId()))
        .findFirst()
        .orElse(null);
  }

  private boolean compareTwoId(UUID oneId, UUID secondId) {
    return oneId == null ? oneId == secondId : oneId.equals(secondId);
  }

  private String getFreeTextByInput(PhysicalInventoryDto inventoryDto,
      PhysicalInventoryLineItemDto lineItem) {
    PhysicalInventoryLineItemDto lineItemDto = inventoryDto.getLineItems().stream()
        .filter(itemDto -> itemDto.getLotId() == lineItem.getLotId()
            && itemDto.getOrderableId() == lineItem.getOrderableId())
        .findFirst()
        .orElse(null);
    return lineItemDto == null ? null : lineItemDto.getReasonFreeText();
  }

  private String getFreeTextByExtension(List<PhysicalInventoryLineItemsExtension> extensions,
      PhysicalInventoryLineItemDto lineItem) {
    if (extensions.isEmpty()) {
      return null;
    }
    PhysicalInventoryLineItemsExtension extension = getExtension(extensions, lineItem);
    return extension == null ? null : extension.getReasonFreeText();
  }


  private PhysicalInventoryDto createInitialInventoryDraftForAllProducts(
      Set<UUID> supportedPrograms, UUID facilityId, boolean canInitialInventory) {
    if (canInitialInventory(facilityId).isCanInitialInventory()) {
      List<PhysicalInventoryLineItemDto> physicalInventoryLineItemDtos =
          buildInitialInventoryLineItemDtos(supportedPrograms, facilityId);
      PhysicalInventoryDto dto = PhysicalInventoryDto.builder()
          .facilityId(facilityId)
          .programId(ALL_PRODUCTS_PROGRAM_ID)
          .lineItems(physicalInventoryLineItemDtos)
          .build();
      return saveDraftForAllProducts(dto);
    }
    if (canInitialInventory) {
      throw new ValidationMessageException(new Message(ERROR_NOT_ACCEPTABLE));
    }
    return null;
  }

  private List<PhysicalInventoryLineItemDto> buildInitialInventoryLineItemDtos(
      Set<UUID> supportedVirtualProgramIds, UUID facilityId) {
    return supportedVirtualProgramIds.stream()
        .map(programId ->
            approvedProductReferenceDataService
                .getApprovedProducts(facilityId, programId, emptyList()).stream()
                .map(ApprovedProductDto::getOrderable)
                .filter(o -> o.getExtraData().containsKey(IS_BASIC))
                .filter(o -> Boolean.parseBoolean(o.getExtraData().get(IS_BASIC)))
                .map(o -> newLineItem(o.getId(), programId))
                .collect(Collectors.toList())
        )
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private PhysicalInventoryLineItemDto newLineItem(UUID orderableId, UUID programId) {
    return PhysicalInventoryLineItemDto.builder()
        .orderableId(orderableId)
        .programId(programId)
        .build();
  }

  private PhysicalInventoryDto getResultInventory(List<PhysicalInventoryDto> inventories,
      List<PhysicalInventoryLineItemsExtension> extensions) {
    PhysicalInventoryDto resultInventory = getResultInventoryForAllProducts(inventories, extensions);
    resultInventory.setId(inventories.get(0).getId());
    resultInventory.setProgramId(inventories.get(0).getProgramId());
    return resultInventory;
  }

  private PhysicalInventoryDto getResultInventoryForAllProducts(List<PhysicalInventoryDto> inventories,
      List<PhysicalInventoryLineItemsExtension> extensions) {
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
                      physicalInventoryLineItemDto -> {
                        physicalInventoryLineItemDto.setProgramId(inventory.getProgramId());
                        physicalInventoryLineItemDto.setReasonFreeText(
                            getFreeTextByExtension(extensions, physicalInventoryLineItemDto));
                      }));
              return optionalList.orElse(new ArrayList<>());
            })
            .flatMap(Collection::stream)
            .collect(Collectors.toList()))
        .build();
  }

  public InitialInventoryFieldDto canInitialInventory(UUID facility) {
    FacilityDto homeFacility = facilityReferenceDataService.findOne(facility);
    String code = homeFacility.getType().getCode();
    if (code != null && Arrays.asList(AC, CENTRAL).contains(code)) {
      return new InitialInventoryFieldDto(false);
    }
    int stockCardCount = stockCardRepository.countByFacilityId(facility);
    return new InitialInventoryFieldDto(0 == stockCardCount);
  }

  public Set<String> findPhysicalInventoryDates(UUID programId, UUID facility,
      String startDate,
      String endDate) {
    List<PhysicalInventory> physicalInventories =
        physicalInventoriesRepository.findByProgramIdAndFacilityIdAndStartDateAndEndDate(programId,
            facility, startDate, endDate);
    return physicalInventories.stream()
        .map(physicalInventory -> physicalInventory.getOccurredDate().toString())
        .collect(Collectors.toSet());
  }

  public PhysicalInventoryDto findLatestPhysicalInventory(UUID facilityId) {
    PhysicalInventory latest = physicalInventoriesRepository
        .findTopByFacilityIdAndIsDraftOrderByOccurredDateDesc(facilityId, false);
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
