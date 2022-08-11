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

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.BooleanUtils;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.domain.SiglusReportType;
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
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.validator.CsvValidator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class SiglusAdministrationsService {
  @Autowired
  private AppInfoRepository appInfoRepository;
  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Autowired
  private FacilityExtensionRepository facilityExtensionRepository;
  @Autowired
  private FacilityLocationsRepository facilityLocationsRepository;
  @Autowired
  private StockCardRepository stockCardRepository;
  @Autowired
  private CsvValidator csvValidator;

  @Autowired
  private SiglusReportTypeRepository siglusReportTypeRepository;

  @Autowired
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  private static final String CSV_SUFFIX = ".csv";
  private static final String MEDIA_TYPE = "text/csv";
  private static final String DISPOSITION_BASE = "attachment; filename=";
  private static final String FILE_NAME = "Location Information.csv";
  private static final String LOCATION_CODE = "Location Code";
  private static final String AREA = "Area";
  private static final String ZONE = "Zone";
  private static final String RACK = "Rack";
  private static final String BARCODE = "Barcode";
  private static final String BIN = "Bin";
  private static final String LEVEL = "Level";

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

  public FacilitySearchResultDto updateFacility(UUID facilityId, SiglusFacilityDto siglusFacilityDto) {
    FacilityDto facilityDto = SiglusFacilityDto.from(siglusFacilityDto);
    saveReportTypes(siglusFacilityDto);
    siglusFacilityReferenceDataService.saveFacility(facilityDto);
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    if (null == facilityExtension) {
      facilityExtension = FacilityExtension
          .builder()
          .facilityId(facilityId)
          .facilityCode(siglusFacilityDto.getCode())
          .enableLocationManagement(siglusFacilityDto.getEnableLocationManagement())
          .isAndroid(siglusFacilityDto.getIsAndroidDevice())
          .build();
      log.info("The facility extension: {} info has changed", facilityExtension);
    } else {
      facilityExtension.setIsAndroid(siglusFacilityDto.getIsAndroidDevice());
      facilityExtension.setEnableLocationManagement(siglusFacilityDto.getEnableLocationManagement());
      log.info("The facility extension: {} info has changed", facilityExtension);
    }
    facilityExtensionRepository.save(facilityExtension);
    return getFacilityInfo(siglusFacilityDto.getId());
  }

  public void exportLocationInfo(UUID facilityId, HttpServletResponse response) {
    response.setContentType(MEDIA_TYPE);
    response.addHeader(HttpHeaders.CONTENT_DISPOSITION, DISPOSITION_BASE + "\"" + FILE_NAME + "\"");

    List<FacilityLocations> locationList = facilityLocationsRepository.findByFacilityId(facilityId);
    try {
      writeLocationInfoOnCsv(locationList, response.getWriter());
    } catch (IOException ioException) {
      log.error("Error: {} occurred while exporting to csv", ioException.getMessage());
      throw new ValidationMessageException(ioException, new Message(CsvUploadMessageKeys.ERROR_IO));
    }
  }

  @Transactional
  public void uploadLocationInfo(UUID facilityId, MultipartFile locationManagementFile) throws IOException {
    validateCsvFile(locationManagementFile);
    List<FacilityLocations> locationManagementList = Lists.newArrayList();
    BufferedReader locationInfoReader = new BufferedReader(new InputStreamReader(locationManagementFile
        .getInputStream()));
    CSVParser fileParser = new CSVParser(locationInfoReader, CSVFormat.EXCEL.withFirstRecordAsHeader());
    csvValidator.validateCsvHeaders(fileParser);
    List<CSVRecord> csvRecordList = csvValidator.validateDuplicateLocationCode(fileParser);
    for (CSVRecord eachRow : csvRecordList) {
      csvValidator.validateNullRow(eachRow);
      String locationCode = eachRow.get(LOCATION_CODE);
      String area = eachRow.get(AREA);
      String zone = eachRow.get(ZONE);
      String rack = eachRow.get(RACK);
      String barcode = eachRow.get(BARCODE);
      int bin = Integer.parseInt(eachRow.get(BIN));
      String level = eachRow.get(LEVEL);
      FacilityLocations locationManagement = new FacilityLocations(facilityId, locationCode, area, zone, rack,
          barcode, bin, level);
      locationManagementList.add(locationManagement);
    }
    log.info("delete location management info with facilityId: {}", facilityId);
    facilityLocationsRepository.deleteByFacilityId(facilityId);
    log.info("Save location management info with facilityId: {}", facilityId);
    facilityLocationsRepository.save(locationManagementList);
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

  private void writeLocationInfoOnCsv(List<FacilityLocations> locationList, Writer writer)
      throws IOException {
    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
    csvPrinter.printRecord(LOCATION_CODE, AREA, ZONE, RACK, BARCODE, BIN, LEVEL);
    if (CollectionUtils.isEmpty(locationList)) {
      return;
    }
    for (FacilityLocations locationManagement : locationList) {
      csvPrinter.printRecord(
          locationManagement.getLocationCode(),
          locationManagement.getArea(),
          locationManagement.getZone(),
          locationManagement.getRack(),
          locationManagement.getBarcode(),
          locationManagement.getBin(),
          locationManagement.getLevel());
    }
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
    FacilityDto facilityInfo = siglusFacilityReferenceDataService.findOneFacility(facilityId);
    if (null == facilityInfo) {
      log.info("Facility not found; Facility id: {}", facilityId);
      throw new NotFoundException("Resources not found");
    }
    FacilitySearchResultDto searchResultDto = FacilitySearchResultDto.from(facilityInfo);
    List<SiglusReportTypeDto> reportTypeDtos = siglusReportTypeRepository.findByFacilityId(facilityId)
            .stream().map(this::from).collect(Collectors.toList());
    searchResultDto.setReportTypes(reportTypeDtos);
    searchResultDto.setIsNewFacility(emptyStockCardCount(facilityId));
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    if (null == facilityExtension) {
      searchResultDto.setEnableLocationManagement(false);
      searchResultDto.setIsAndroidDevice(false);
      return searchResultDto;
    }
    searchResultDto.setIsAndroidDevice(facilityExtension.getIsAndroid());
    searchResultDto.setEnableLocationManagement(BooleanUtils.isTrue(facilityExtension.getEnableLocationManagement()));
    searchResultDto.setHasSuccessUploadLocations(
        !CollectionUtils.isEmpty(facilityLocationsRepository.findByFacilityId(facilityId)));
    return searchResultDto;
  }

  private boolean emptyStockCardCount(UUID facilityId) {
    return stockCardRepository.countByFacilityId(facilityId) == 0;
  }

}
