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

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.CalculatedStockOnHand;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.repository.CalculatedStockOnHandRepository;
import org.openlmis.stockmanagement.repository.StockCardLineItemRepository;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.constant.KitConstants;
import org.siglus.siglusapi.constant.LocationConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.CalculatedStockOnHandByLocation;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.domain.StockCardLocationMovementLineItem;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.SiglusFacilityDto;
import org.siglus.siglusapi.dto.SiglusReportTypeDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.i18n.CsvUploadMessageKeys;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.CalculatedStockOnHandByLocationRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.LocationDraftRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.repository.StockCardLocationMovementLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.CsvValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class SiglusAdministrationsService {
  private final AppInfoRepository appInfoRepository;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final FacilityExtensionRepository facilityExtensionRepository;
  private final FacilityLocationsRepository facilityLocationsRepository;
  private final StockCardRepository stockCardRepository;
  private final CsvValidator csvValidator;
  private final SiglusReportTypeRepository siglusReportTypeRepository;
  private final StockCardLocationMovementLineItemRepository stockCardLocationMovementLineItemRepository;
  private final SiglusProcessingPeriodService siglusProcessingPeriodService;
  private final CalculatedStockOnHandRepository calculatedStockOnHandRepository;
  private final CalculatedStockOnHandByLocationRepository calculatedStocksOnHandLocationsRepository;
  private final OrderableRepository orderableRepository;
  private final StockCardLineItemRepository stockCardLineItemRepository;
  private final StockCardExtensionRepository stockCardExtensionRepository;
  private final SiglusAuthenticationHelper authenticationHelper;
  private final LocationDraftRepository locationDraftRepository;

  private static final String LOCATION_MANAGEMENT_TAB = "locationManagement";
  private static final String CSV_SUFFIX = ".csv";
  private static final String CONTENT_TYPE = "application/force-download";
  private static final String DISPOSITION_BASE = "attachment; filename=";
  private static final String FILE_NAME = "Informações de localização.csv";

  public Page<FacilitySearchResultDto> searchForFacilities(FacilitySearchParamDto facilitySearchParamDto,
      Pageable pageable) {
    Page<FacilityDto> facilityDtos = siglusFacilityReferenceDataService.searchAllFacilities(facilitySearchParamDto,
        pageable);

    List<FacilityDto> facilityDtoList = facilityDtos.getContent();
    List<FacilitySearchResultDto> facilitySearchResultDtoList = FacilitySearchResultDto.from(facilityDtoList);

    facilitySearchResultDtoList.forEach(eachFacility -> {
      FacilityExtension byFacilityId = facilityExtensionRepository.findByFacilityId(eachFacility.getId());
      eachFacility.setIsAndroidDevice(null != byFacilityId && BooleanUtils.isTrue(byFacilityId.getIsAndroid()));
    });

    return Pagination.getPage(facilitySearchResultDtoList, pageable, facilityDtos.getTotalElements());
  }

  public FacilitySearchResultDto createFacility(SiglusFacilityDto siglusFacilityDto) {
    FacilityDto createdNewFacilityDto = siglusFacilityReferenceDataService.createFacility(siglusFacilityDto);
    FacilityExtension facilityExtension = FacilityExtension
        .builder()
        .facilityId(createdNewFacilityDto.getId())
        .facilityCode(createdNewFacilityDto.getCode())
        .isAndroid(siglusFacilityDto.getIsAndroidDevice())
        .build();
    facilityExtensionRepository.save(facilityExtension);
    return getFacilityInfo(createdNewFacilityDto.getId());
  }

  @Transactional
  public void eraseDeviceInfoByFacilityId(String facilityCode) {
    AppInfo androidInfoByFacilityId = appInfoRepository.findByFacilityCode(facilityCode);
    if (null == androidInfoByFacilityId) {
      return;
    }
    log.info("The Android device info has been removed with facilityCode: {}", facilityCode);
    appInfoRepository.deleteByFacilityCode(facilityCode);
  }

  public FacilitySearchResultDto getFacility(UUID facilityId) {
    return getFacilityInfo(facilityId);
  }

  @Transactional
  public FacilitySearchResultDto updateFacility(UUID facilityId, SiglusFacilityDto siglusFacilityDto) {
    FacilityDto facilityDto = SiglusFacilityDto.from(siglusFacilityDto);
    saveReportTypes(siglusFacilityDto);
    siglusFacilityReferenceDataService.saveFacility(facilityDto);
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    deleteDraftsWhenToggleLocationManagement(siglusFacilityDto, facilityExtension);
    if (null == facilityExtension) {
      facilityExtension = FacilityExtension
          .builder()
          .facilityId(facilityId)
          .facilityCode(siglusFacilityDto.getCode())
          .enableLocationManagement(siglusFacilityDto.getEnableLocationManagement())
          .isAndroid(siglusFacilityDto.getIsAndroidDevice())
          .build();
    } else {
      facilityExtension.setIsAndroid(siglusFacilityDto.getIsAndroidDevice());
      facilityExtension.setEnableLocationManagement(siglusFacilityDto.getEnableLocationManagement());
    }
    log.info("The facility extension info has changed; facilityId: {}", facilityId);
    facilityExtensionRepository.save(facilityExtension);
    if (StringUtils.equals(siglusFacilityDto.getTab(), LOCATION_MANAGEMENT_TAB)) {
      assignToVirtualLocation(siglusFacilityDto);
    }
    return getFacilityInfo(siglusFacilityDto.getId());
  }

  public void exportLocationInfo(UUID facilityId, HttpServletResponse response) {
    response.setContentType(CONTENT_TYPE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_BASE + "\"" + FILE_NAME + "\"");
    List<FacilityLocations> locationList = facilityLocationsRepository.findByFacilityId(facilityId);
    try {
      ServletOutputStream outputStream = response.getOutputStream();
      outputStream.write(0xef);
      outputStream.write(0xbb);
      outputStream.write(0xbf);
      outputStream.flush();
      EasyExcelFactory
          .write(response.getOutputStream())
          .head(getHeadRow())
          .autoCloseStream(true)
          .excelType(ExcelTypeEnum.CSV)
          .sheet(0)
          .doWrite(CollectionUtils.isEmpty(locationList) ? Lists.newArrayList() : getDataRows(locationList));
    } catch (IOException ioException) {
      log.error("Error: {} occurred while exporting to csv", ioException.getMessage());
      throw new ValidationMessageException(ioException, new Message(CsvUploadMessageKeys.ERROR_IO));
    }
  }

  @Transactional
  public void uploadLocationInfo(UUID facilityId, MultipartFile locationManagementFile) throws IOException {
    validateCsvFile(locationManagementFile);
    List<FacilityLocations> locationManagementList = Lists.newArrayList();
    BufferedReader locationInfoReader = new BufferedReader(new InputStreamReader(
        new BOMInputStream(locationManagementFile.getInputStream()), StandardCharsets.UTF_8));
    CSVParser fileParser = new CSVParser(locationInfoReader, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateCsvHeaders(fileParser);
    List<CSVRecord> csvRecordList = csvValidator.validateDuplicateLocationCode(fileParser);
    for (CSVRecord eachRow : csvRecordList) {
      csvValidator.validateNullRow(eachRow);
      String locationCode = eachRow.get(LocationConstants.PORTUGUESE_LOCATION_CODE);
      String area = eachRow.get(LocationConstants.PORTUGUESE_AREA);
      String zone = eachRow.get(LocationConstants.PORTUGUESE_ZONE);
      String rack = eachRow.get(LocationConstants.PORTUGUESE_RACK);
      String barcode = eachRow.get(LocationConstants.PORTUGUESE_BARCODE);
      String bin = eachRow.get(LocationConstants.PORTUGUESE_BIN);
      String level = eachRow.get(LocationConstants.PORTUGUESE_LEVEL);
      FacilityLocations locationManagement = new FacilityLocations(facilityId, locationCode, area, zone, rack,
          barcode, bin, level);
      locationManagementList.add(locationManagement);
    }
    log.info("delete location management info with facilityId: {}", facilityId);
    facilityLocationsRepository.deleteByFacilityId(facilityId);
    log.info("Save location management info with facilityId: {}", facilityId);
    facilityLocationsRepository.save(locationManagementList);
  }

  @Transactional
  public void upgradeAndroidFacilityToWeb(UUID facilityId) {
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    if (null == facilityExtension || !facilityExtension.getIsAndroid()) {
      throw new PermissionMessageException(
          new org.openlmis.stockmanagement.util.Message("siglusapi.error.notAndroidFacility"));
    }
    facilityExtension.setIsAndroid(false);
    facilityExtensionRepository.save(facilityExtension);
    if (emptyStockCardCount(facilityId)) {
      return;
    }
    List<StockCard> stockCardList = stockCardRepository.findByFacilityIdIn(facilityId);
    List<StockCard> stockCardsWithEmptyLotId = stockCardList.stream().filter(stockCard -> null == stockCard.getLotId())
        .collect(Collectors.toList());
    List<UUID> orderableIdsWithEmptyLotId = stockCardsWithEmptyLotId.stream().map(StockCard::getOrderableId)
        .collect(Collectors.toList());

    List<Orderable> orderables = orderableRepository.findLatestByIds(orderableIdsWithEmptyLotId);
    List<UUID> orderableIdsNoKit = orderables.stream()
        .filter(orderable -> !KitConstants.isKit(orderable.getProductCode().toString()))
        .map(Orderable::getId).collect(Collectors.toList());

    List<StockCard> stockCards = stockCardsWithEmptyLotId.stream()
        .filter(stockCard -> orderableIdsNoKit.contains(stockCard.getOrderableId())).collect(Collectors.toList());
    List<UUID> stockCardIds = stockCards.stream().map(StockCard::getId).collect(Collectors.toList());
    log.info("delete on stockCardLineItem when upgrade to web, stockCardId: {}", stockCardIds);
    stockCardLineItemRepository.deleteByStockCardIdIn(stockCardIds);
    log.info("delete on calculatedStockOnHand when upgrade to web, facilityId: {}", facilityId);
    Set<UUID> orderableIds = stockCards.stream().map(StockCard::getOrderableId).collect(Collectors.toSet());
    calculatedStockOnHandRepository.deleteByFacilityIdAndOrderableIds(facilityId, orderableIds);
    log.info("delete on stockCardExtension when upgrade to web, stockCardId: {}", stockCardIds);
    stockCardExtensionRepository.deleteByStockCardIdIn(stockCardIds);
    log.info("delete on stockCard when upgrade to web, stockCardId: {}", stockCardIds);
    stockCardRepository.deleteByIdIn(stockCardIds);
  }

  private void validateReportTypes(SiglusFacilityDto siglusFacilityDto) {
    UUID facilityId = siglusFacilityDto.getId();
    Map<String, SiglusReportType> programCodeToReportType = siglusReportTypeRepository
        .findByFacilityId(facilityId)
        .stream()
        .collect(Collectors.toMap(SiglusReportType::getProgramCode, Function.identity()));

    siglusFacilityDto.getReportTypes().forEach(dto -> {
      SiglusReportType original = programCodeToReportType.get(dto.getProgramCode());
      if (null != original && dto.getStartDate().isBefore(original.getStartDate())) {
        LocalDate previousDate = siglusProcessingPeriodService
            .getPreviousPeriodStartDateSinceInitiate(dto.getProgramCode(), facilityId);
        if (previousDate != null && dto.getStartDate().isBefore(previousDate)) {
          throw new IllegalArgumentException("Invalid start date");
        }
      }
    });
  }

  private void saveReportTypes(SiglusFacilityDto siglusFacilityDto) {
    if (CollectionUtils.isNotEmpty(siglusFacilityDto.getReportTypes())) {
      validateReportTypes(siglusFacilityDto);
      List<SiglusReportType> toSave = siglusFacilityDto.getReportTypes()
          .stream().map(SiglusReportType::from).collect(Collectors.toList());
      siglusReportTypeRepository.save(toSave);
    }
  }

  private List<List<String>> getHeadRow() {
    List<List<String>> header = Lists.newArrayList();
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_LOCATION_CODE));
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_AREA));
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_ZONE));
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_RACK));
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_BARCODE));
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_BIN));
    header.add(Collections.singletonList(LocationConstants.PORTUGUESE_LEVEL));
    return header;
  }

  private List<List<String>> getDataRows(List<FacilityLocations> locationList) {
    List<List<String>> dataRows = Lists.newArrayList();
    for (FacilityLocations locationManagement : locationList) {
      List<String> eachRow = Lists.newArrayList();
      eachRow.add(locationManagement.getLocationCode());
      eachRow.add(locationManagement.getArea());
      eachRow.add(locationManagement.getZone());
      eachRow.add(locationManagement.getRack());
      eachRow.add(locationManagement.getBarcode());
      eachRow.add(locationManagement.getBin());
      eachRow.add(locationManagement.getLevel());
      dataRows.add(eachRow);
    }
    return dataRows;
  }

  private void validateCsvFile(MultipartFile csvFile) {
    if (null == csvFile || csvFile.isEmpty()) {
      throw new ValidationMessageException(CsvUploadMessageKeys.ERROR_FILE_IS_EMPTY);
    } else if (!csvFile.getOriginalFilename().endsWith(CSV_SUFFIX)) {
      throw new ValidationMessageException(CsvUploadMessageKeys.ERROR_INCORRECT_FILE_FORMAT);
    }
  }

  public SiglusReportTypeDto from(SiglusReportType reportType) {
    SiglusReportTypeDto dto = new SiglusReportTypeDto();
    BeanUtils.copyProperties(reportType, dto);
    dto.setPreviousPeriodStartDateSinceRecentSubmit(siglusProcessingPeriodService
        .getPreviousPeriodStartDateSinceInitiate(reportType.getProgramCode(), reportType.getFacilityId()));
    return dto;
  }

  private FacilitySearchResultDto getFacilityInfo(UUID facilityId) {
    FacilityDto facilityInfo = siglusFacilityReferenceDataService.findOneWithoutCache(facilityId);
    if (null == facilityInfo) {
      log.info("Facility not found; Facility id: {}", facilityId);
      throw new NotFoundException("Resources not found");
    }
    FacilitySearchResultDto searchResultDto = FacilitySearchResultDto.from(facilityInfo);
    List<SiglusReportTypeDto> reportTypeDtos = siglusReportTypeRepository.findByFacilityId(facilityId)
        .stream().map(this::from).collect(Collectors.toList());
    searchResultDto.setReportTypes(reportTypeDtos);
    searchResultDto.setIsNewFacility(emptyStockCardCount(facilityId));
    searchResultDto.setHasSuccessUploadLocations(
        !CollectionUtils.isEmpty(facilityLocationsRepository.findByFacilityId(facilityId)));
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    if (null == facilityExtension) {
      searchResultDto.setEnableLocationManagement(false);
      searchResultDto.setIsAndroidDevice(false);
      return searchResultDto;
    }
    searchResultDto.setIsAndroidDevice(facilityExtension.getIsAndroid());
    searchResultDto.setEnableLocationManagement(BooleanUtils.isTrue(facilityExtension.getEnableLocationManagement()));
    return searchResultDto;
  }

  private boolean emptyStockCardCount(UUID facilityId) {
    return stockCardRepository.countByFacilityId(facilityId) == 0;
  }

  private void assignToVirtualLocation(SiglusFacilityDto siglusFacilityDto) {
    if (!emptyStockCardCount(siglusFacilityDto.getId())) {
      UUID userId = authenticationHelper.getCurrentUser().getId();
      List<StockCard> stockCards = stockCardRepository.findByFacilityIdIn(siglusFacilityDto.getId());
      List<UUID> stockCardIds = stockCards.stream().map(StockCard::getId).collect(Collectors.toList());
      if (BooleanUtils.isTrue(siglusFacilityDto.getEnableLocationManagement())) {
        List<CalculatedStockOnHand> calculatedStockOnHandList = findStockCardIdsHasStockOnHandOnLot(stockCardIds);
        if (CollectionUtils.isNotEmpty(calculatedStockOnHandList)) {
          assignNewVirtualLocations(calculatedStockOnHandList, userId);
        }
      } else {
        List<CalculatedStockOnHandByLocation> stockCardIdsHasStockOnHandOnLocation =
            findStockCardIdsHasStockOnHandOnLocation(stockCardIds);
        deletePreviousSohWithLocation(stockCardIds);
        assignExistLotToVirtualLocations(stockCardIdsHasStockOnHandOnLocation, userId);
      }
    }
  }

  private List<CalculatedStockOnHandByLocation> findStockCardIdsHasStockOnHandOnLocation(List<UUID> stockCardIds) {
    return calculatedStocksOnHandLocationsRepository.findLatestLocationSohByStockCardIds(stockCardIds);
  }

  private List<CalculatedStockOnHand> findStockCardIdsHasStockOnHandOnLot(List<UUID> stockCardIds) {
    return calculatedStockOnHandRepository.findLatestStockOnHands(stockCardIds, ZonedDateTime.now());
  }

  private void assignNewVirtualLocations(List<CalculatedStockOnHand> calculatedStockOnHandList, UUID userId) {
    List<StockCardLocationMovementLineItem> lineItemsWithVirtualLocation = Lists.newArrayList();
    List<CalculatedStockOnHandByLocation> calculatedStockOnHandByLocationList = Lists.newArrayList();
    updateStockOnHandByLocation(calculatedStockOnHandList);
    calculatedStockOnHandList = calculatedStockOnHandList.stream()
        .filter(calculatedStockOnHand -> calculatedStockOnHand.getStockOnHand() > 0).collect(Collectors.toList());
    calculatedStockOnHandList.forEach(calculatedStockOnHand -> {
      StockCardLocationMovementLineItem stockCardLocationMovementLineItem = StockCardLocationMovementLineItem
          .builder()
          .stockCardId(calculatedStockOnHand.getStockCardId())
          .occurredDate(LocalDate.now())
          .userId(userId)
          .quantity(calculatedStockOnHand.getStockOnHand())
          .srcLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
          .srcArea(LocationConstants.VIRTUAL_LOCATION_AREA)
          .destLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
          .destArea(LocationConstants.VIRTUAL_LOCATION_AREA)
          .build();
      lineItemsWithVirtualLocation.add(stockCardLocationMovementLineItem);
      CalculatedStockOnHandByLocation calculatedStockOnHandByLocation = CalculatedStockOnHandByLocation
          .builder()
          .stockCardId(calculatedStockOnHand.getStockCardId())
          .occurredDate(new Date())
          .stockOnHand(calculatedStockOnHand.getStockOnHand())
          .calculatedStocksOnHandId(calculatedStockOnHand.getId())
          .locationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
          .area(LocationConstants.VIRTUAL_LOCATION_AREA)
          .build();
      calculatedStockOnHandByLocationList.add(calculatedStockOnHandByLocation);
    });
    log.info("assign virtual location when enable location; stockCardLocationMovementLineItemRepository size: {}",
        lineItemsWithVirtualLocation.size());
    stockCardLocationMovementLineItemRepository.save(lineItemsWithVirtualLocation);
    log.info("assign virtual location when enable location; calculatedStocksOnHandLocationsRepository size: {}",
        calculatedStockOnHandByLocationList.size());
    calculatedStocksOnHandLocationsRepository.save(calculatedStockOnHandByLocationList);
  }

  private void assignExistLotToVirtualLocations(List<CalculatedStockOnHandByLocation> calculatedStocksOnHandLocations,
      UUID userId) {
    List<StockCardLocationMovementLineItem> lineItemsWithVirtualLocation = Lists.newArrayList();
    List<CalculatedStockOnHandByLocation> calculatedStockOnHandByLocationList = Lists.newArrayList();

    Set<UUID> stockCardIds = calculatedStocksOnHandLocations.stream()
        .map(CalculatedStockOnHandByLocation::getStockCardId).collect(Collectors.toSet());
    List<StockCardLocationMovementLineItem> lineItems;
    lineItems = stockCardIds.isEmpty() ? Collections.emptyList() : stockCardLocationMovementLineItemRepository
        .findLatestByStockCardId(stockCardIds);
    List<UUID> virtualLocationStockcardIds = lineItems.stream()
        .filter(m -> LocationConstants.VIRTUAL_LOCATION_CODE.equals(m.getSrcLocationCode())
            && LocationConstants.VIRTUAL_LOCATION_AREA.equals(m.getSrcArea())
            && LocationConstants.VIRTUAL_LOCATION_CODE.equals(m.getDestLocationCode())
            && LocationConstants.VIRTUAL_LOCATION_AREA.equals(m.getDestArea()))
        .map(StockCardLocationMovementLineItem::getStockCardId).collect(Collectors.toList());

    calculatedStocksOnHandLocations.forEach(calculatedStocksOnHandLocation -> {
      if (!virtualLocationStockcardIds.contains(calculatedStocksOnHandLocation.getStockCardId())) {
        StockCardLocationMovementLineItem productLocationMovementLineItem = StockCardLocationMovementLineItem
            .builder()
            .stockCardId(calculatedStocksOnHandLocation.getStockCardId())
            .occurredDate(LocalDate.now())
            .userId(userId)
            .quantity(calculatedStocksOnHandLocation.getStockOnHand())
            .srcLocationCode(calculatedStocksOnHandLocation.getLocationCode())
            .srcArea(calculatedStocksOnHandLocation.getArea())
            .destLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
            .destArea(LocationConstants.VIRTUAL_LOCATION_AREA)
            .build();
        lineItemsWithVirtualLocation.add(productLocationMovementLineItem);
      }
      CalculatedStockOnHandByLocation calculatedStockOnHandByLocation = CalculatedStockOnHandByLocation
          .builder()
          .stockCardId(calculatedStocksOnHandLocation.getStockCardId())
          .occurredDate(new Date())
          .stockOnHand(calculatedStocksOnHandLocation.getStockOnHand())
          .calculatedStocksOnHandId(calculatedStocksOnHandLocation.getCalculatedStocksOnHandId())
          .locationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
          .area(LocationConstants.VIRTUAL_LOCATION_AREA)
          .build();
      calculatedStockOnHandByLocationList.add(calculatedStockOnHandByLocation);
    });
    log.info("assign virtual location when disable location; stockCardLocationMovementLineItem size: {}",
        lineItemsWithVirtualLocation.size());
    stockCardLocationMovementLineItemRepository.save(lineItemsWithVirtualLocation);
    log.info("assign virtual location when disable location; calculatedStocksOnHandLocations size: {}",
        calculatedStockOnHandByLocationList.size());
    calculatedStocksOnHandLocationsRepository.save(calculatedStockOnHandByLocationList);
  }

  private void updateStockOnHandByLocation(List<CalculatedStockOnHand> calculatedStockOnHandList) {
    Set<UUID> stockCardIds = calculatedStockOnHandList.stream()
        .map(CalculatedStockOnHand::getStockCardId).collect(Collectors.toSet());
    List<CalculatedStockOnHandByLocation> latestSohByLocation = calculatedStocksOnHandLocationsRepository
        .findLatestLocationSohByStockCardIds(stockCardIds);
    if (CollectionUtils.isNotEmpty(latestSohByLocation)) {
      List<CalculatedStockOnHandByLocation> updateCalculateSohList = Lists.newArrayList();
      List<CalculatedStockOnHand> removeDuplicateList = Lists.newArrayList();
      for (CalculatedStockOnHand calculatedStockOnHand : calculatedStockOnHandList) {
        for (CalculatedStockOnHandByLocation calculatedStockOnHandByLocation : latestSohByLocation) {
          if (calculatedStockOnHand.getStockCardId().equals(calculatedStockOnHandByLocation.getStockCardId())
              && !calculatedStockOnHand.getStockOnHand().equals(calculatedStockOnHandByLocation.getStockOnHand())) {
            calculatedStockOnHandByLocation.setStockOnHand(calculatedStockOnHand.getStockOnHand());
            updateCalculateSohList.add(calculatedStockOnHandByLocation);
            removeDuplicateList.add(calculatedStockOnHand);
          }
          if (calculatedStockOnHand.getStockCardId().equals(calculatedStockOnHandByLocation.getStockCardId())) {
            removeDuplicateList.add(calculatedStockOnHand);
          }
        }
      }
      calculatedStockOnHandList.removeAll(removeDuplicateList);
      log.info("assign virtual location when enable location; soh update on calculatedStocksOnHandLocations size: {}",
          updateCalculateSohList.size());
      calculatedStocksOnHandLocationsRepository.save(updateCalculateSohList);

      Map<UUID, Integer> stockCardIdToSohMap = updateCalculateSohList.stream()
          .collect(Collectors.toMap(CalculatedStockOnHandByLocation::getStockCardId,
              CalculatedStockOnHandByLocation::getStockOnHand));
      if (MapUtils.isNotEmpty(stockCardIdToSohMap)) {
        List<StockCardLocationMovementLineItem> updateLineItems = Lists.newArrayList();
        List<StockCardLocationMovementLineItem> lineItemByLocation = stockCardLocationMovementLineItemRepository
            .findLatestByStockCardId(stockCardIdToSohMap.keySet());
        List<StockCardLocationMovementLineItem> previousVirtualLocationLineItems = lineItemByLocation.stream()
            .filter(lineItem ->
                LocationConstants.VIRTUAL_LOCATION_CODE.equals(lineItem.getDestLocationCode()))
            .collect(Collectors.toList());
        previousVirtualLocationLineItems.forEach(lineItem -> {
          StockCardLocationMovementLineItem updateLineItem = StockCardLocationMovementLineItem
              .builder()
              .stockCardId(lineItem.getStockCardId())
              .occurredDate(LocalDate.now())
              .userId(lineItem.getUserId())
              .quantity(stockCardIdToSohMap.get(lineItem.getStockCardId()))
              .srcLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
              .srcArea(LocationConstants.VIRTUAL_LOCATION_AREA)
              .destLocationCode(LocationConstants.VIRTUAL_LOCATION_CODE)
              .destArea(LocationConstants.VIRTUAL_LOCATION_AREA)
              .build();
          updateLineItems.add(updateLineItem);
        });
        log.info(
            "assign virtual location when enable location; soh update on stockCardLocationMovementLineItem size: {}",
            updateCalculateSohList.size());
        stockCardLocationMovementLineItemRepository.save(updateLineItems);
      }
    }
  }

  private void deletePreviousSohWithLocation(List<UUID> stockCardIds) {
    log.info("assignExistLotToVirtualLocations, delete previous calculated soh records, ids: {}", stockCardIds);
    calculatedStocksOnHandLocationsRepository.deleteByStockCardIdIn(stockCardIds);
  }

  private void deleteDraftsWhenToggleLocationManagement(SiglusFacilityDto siglusFacilityDto,
      FacilityExtension facilityExtension) {
    if (facilityExtension == null || facilityExtension.getEnableLocationManagement() == null) {
      if (siglusFacilityDto.getEnableLocationManagement()) {
        log.info("delete location related drafts, facilityId: {}", siglusFacilityDto.getId());
        locationDraftRepository.deleteLocationRelatedDrafts(siglusFacilityDto.getId());
      }
    } else if (facilityExtension.getEnableLocationManagement() != siglusFacilityDto.getEnableLocationManagement()) {
      log.info("delete location related drafts, facilityId: {}", siglusFacilityDto.getId());
      locationDraftRepository.deleteLocationRelatedDrafts(siglusFacilityDto.getId());
    }
  }
}
