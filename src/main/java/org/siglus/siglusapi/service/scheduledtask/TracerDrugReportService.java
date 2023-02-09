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

package org.siglus.siglusapi.service.scheduledtask;

import static java.util.Collections.emptyList;
import static org.siglus.siglusapi.constant.FieldConstants.ALL_GEOGRAPHIC_ZONE;
import static org.siglus.siglusapi.constant.FieldConstants.BASIC_COLUMN;
import static org.siglus.siglusapi.constant.FieldConstants.CMM;
import static org.siglus.siglusapi.constant.FieldConstants.DISTRICT;
import static org.siglus.siglusapi.constant.FieldConstants.DISTRICT_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.DRUG_CODE_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.DRUG_NAME_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.EMPTY_VALUE;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.LOW_STOCK_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.OVER_STOCK_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.PROVINCE;
import static org.siglus.siglusapi.constant.FieldConstants.PROVINCE_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.REGULAR_STOCK_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.REPORT_GENERATED_FOR_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.STOCK_OUT_PORTUGUESE;
import static org.siglus.siglusapi.constant.FieldConstants.SUBTITLE;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.siglus.siglusapi.domain.TracerDrugPersistentData;
import org.siglus.siglusapi.dto.AssociatedGeographicZoneDto;
import org.siglus.siglusapi.dto.RequisitionGeographicZonesDto;
import org.siglus.siglusapi.dto.TracerDrugDto;
import org.siglus.siglusapi.dto.TracerDrugExcelDto;
import org.siglus.siglusapi.dto.TracerDrugExportDto;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.TracerDrugRepository;
import org.siglus.siglusapi.repository.dto.ProductCmm;
import org.siglus.siglusapi.repository.dto.ProductLotSohDto;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.CustomCellWriteHandler;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TracerDrugReportService {

  private final TracerDrugRepository tracerDrugRepository;

  private final SiglusAuthenticationHelper authenticationHelper;

  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final SiglusFacilityRepository facilityRepository;

  @Value("${tracer.drug.initialize.date}")
  private String tracerDrugInitializeDate;

  @Value("${dateFormat}")
  private String dateFormat;

  @Transactional
  @SneakyThrows
  @Async
  public void refreshTracerDrugPersistentData(String startDate, String endDate) {
    log.info("tracer drug persistentData refresh. start = " + System.currentTimeMillis());
    List<String> facilityCodes = facilityRepository.findAllFacilityCodes();
    refreshTracerDrugPersistentDataByFacilities(startDate, endDate, facilityCodes);
    log.info("tracer drug persistentData  refresh. end = " + System.currentTimeMillis());
  }

  public void refreshTracerDrugPersistentDataByFacilities(
      String startDate, String endDate, List<String> facilityCodes) {
    if (CollectionUtils.isEmpty(facilityCodes)) {
      log.warn("facility codes is empty, no action");
      return;
    }
    final Map<String, List<ProductLotSohDto>> facilityCodeToTracerDrugSoh =
        getFacilityCodeToTracerDrugSoh(startDate, endDate, facilityCodes);
    final Map<String, List<ProductCmm>> facilityCodeToCmm = getFacilityCodeToCmm(startDate, endDate, facilityCodes);
    final List<TracerDrugPersistentData> tracerDrugPersistentData = new LinkedList<>();
    for (String facilityCode : facilityCodes) {
      List<ProductLotSohDto> productLotSohDtos =
          facilityCodeToTracerDrugSoh.getOrDefault(facilityCode, emptyList());
      List<ProductCmm> cmms = facilityCodeToCmm.getOrDefault(facilityCode, emptyList());
      List<TracerDrugPersistentData> records =
          calcForOneFacility(
              facilityCode,
              productLotSohDtos,
              cmms,
              LocalDate.parse(startDate),
              LocalDate.parse(endDate));
      tracerDrugPersistentData.addAll(records);
    }
    log.info(
        "save tracer drug persistent data, size:{}, start date:{}, end date:{}",
        tracerDrugPersistentData.size(),
        startDate,
        endDate);
    tracerDrugRepository.batchInsertOrUpdate(tracerDrugPersistentData);
  }

  private Map<String, List<ProductCmm>> getFacilityCodeToCmm(
      String startDate, String endDate, List<String> facilityCodes) {
    final List<ProductCmm> lastCmmTillEndDate =
        tracerDrugRepository.getLastTracerDrugCmmTillDate(startDate, facilityCodes);
    List<ProductCmm> tracerDrugCmm =
        tracerDrugRepository.getTracerDrugCmm(startDate, endDate, facilityCodes);
    return Stream.of(lastCmmTillEndDate, tracerDrugCmm)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(ProductCmm::getFacilityCode));
  }

  private Map<String, List<ProductLotSohDto>> getFacilityCodeToTracerDrugSoh(String startDate, String endDate,
      List<String> facilityCodes) {
    List<ProductLotSohDto> lastSohTillEndDate = tracerDrugRepository.getLastTracerDrugSohTillDate(startDate,
        facilityCodes);
    List<ProductLotSohDto> tracerDrugSoh =
        tracerDrugRepository.getTracerDrugSoh(startDate, endDate, facilityCodes);
    return Stream.of(lastSohTillEndDate, tracerDrugSoh)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(ProductLotSohDto::getFacilityCode));
  }

  List<TracerDrugPersistentData> calcForOneFacility(
      String facilityCode,
      List<ProductLotSohDto> productLotSohDtos,
      List<ProductCmm> cmms,
      LocalDate beginDate,
      LocalDate endDate) {
    productLotSohDtos.sort(Comparator.comparing(ProductLotSohDto::getOccurredDate));
    cmms.sort(Comparator.comparing(ProductCmm::getPeriodBegin));
    final List<TracerDrugPersistentData> tracerDrugPersistentData = new LinkedList<>();
    final CalculationTable calculationTable = new CalculationTable(facilityCode);
    final MondayIterator mondayIterator = new MondayIterator(beginDate, endDate);
    int currentSohIdx = 0;
    int currentCmmIdx = 0;
    while (mondayIterator.hasNext()) {
      final LocalDate checkpoint = mondayIterator.next();
      for (; currentSohIdx < productLotSohDtos.size(); currentSohIdx++) {
        ProductLotSohDto currentSoh = productLotSohDtos.get(currentSohIdx);
        if (currentSoh.getOccurredDate().isAfter(checkpoint)) {
          break;
        }
        calculationTable.updateWith(currentSoh);
      }
      for (; currentCmmIdx < cmms.size(); currentCmmIdx++) {
        ProductCmm productCmm = cmms.get(currentCmmIdx);
        if (productCmm.getPeriodBegin().isAfter(checkpoint)) {
          break;
        }
        calculationTable.updateWith(productCmm);
      }
      calculationTable.updateCheckpoint(checkpoint);
      tracerDrugPersistentData.addAll(calculationTable.generateRecords());
    }
    return tracerDrugPersistentData;
  }

  @Transactional
  @Async
  public void refreshTracerDrugPersistentDataByFacility(List<String> facilityCodes, String startDate, String endDate) {
    log.info("tracer drug persistentData refresh. start = " + System.currentTimeMillis());
    refreshTracerDrugPersistentDataByFacilities(startDate, endDate, facilityCodes);
    log.info("tracer drug persistentData  refresh. end = " + System.currentTimeMillis());
  }

  @Transactional
  @Async
  public void initializeTracerDrugPersistentData() {
    log.info("tracer drug persistentData initialize. start = " + System.currentTimeMillis());
    refreshTracerDrugPersistentData(tracerDrugInitializeDate, LocalDate.now().toString());
    log.info("tracer drug persistentData initialize. end = " + System.currentTimeMillis());
  }


  public TracerDrugExportDto getTracerDrugExportDto() {

    List<AssociatedGeographicZoneDto> associatedGeographicZones = getRequisitionGeographicZonesDtos();

    List<TracerDrugDto> tracerDrugInfo = tracerDrugRepository.getTracerDrugInfo();

    return TracerDrugExportDto
        .builder()
        .tracerDrugs(tracerDrugInfo)
        .geographicZones(associatedGeographicZones)
        .build();
  }

  public void getTracerDrugExcel(HttpServletResponse response,
      String productCode,
      String districtCode,
      String provinceCode,
      String startDate,
      String endDate) throws IOException {

    ExcelWriter excelWriter = EasyExcelFactory.write(response.getOutputStream()).build();

    List<String> requisitionFacilityCodes = getRequisitionFacilityCode(districtCode, provinceCode);

    List<TracerDrugExcelDto> tracerDrugExcelInfo = tracerDrugRepository.getTracerDrugExcelInfo(startDate,
        endDate,
        productCode, requisitionFacilityCodes);

    Map<String, List<TracerDrugExcelDto>> tracerDrugMap = tracerDrugExcelInfo.stream()
        .collect(Collectors.groupingBy(o -> getUniqueKey(o.getFacilityCode(), o.getProductCode())));

    int[][] colorArrays = new int[tracerDrugMap.size()][tracerDrugMap.values().stream()
        .findFirst().orElse(emptyList()).size()];

    List<List<Object>> excelRows = getDataRows(startDate, endDate, tracerDrugMap, colorArrays);

    WriteSheet writeSheet = EasyExcelFactory
        .writerSheet(0)
        .registerWriteHandler(new CustomCellWriteHandler(colorArrays, false))
        .head(getHeadRow(tracerDrugMap))
        .build();

    excelWriter.write(excelRows, writeSheet);
    excelWriter.write(getLegendaRows(), getLegendaWriteSheet());

    excelWriter.finish();
  }


  public List<List<Object>> getDataRows(String startDate, String endDate,
      Map<String, List<TracerDrugExcelDto>> tracerDrugMap, int[][] colorArrays) {
    List<List<Object>> excelRows = new LinkedList<>();

    AtomicInteger colorRow = new AtomicInteger();
    AtomicInteger colorColumn = new AtomicInteger();
    tracerDrugMap.forEach((key, tracerDrugList) -> {
      List<Object> excelRow = new LinkedList<>();
      tracerDrugList = tracerDrugList
          .stream()
          .sorted(Comparator.comparing(TracerDrugExcelDto::getComputationTime))
          .collect(Collectors.toList());
      TracerDrugExcelDto firstTracerDrugDto = tracerDrugList.get(0);
      excelRow.add(firstTracerDrugDto.getProductCode());
      excelRow.add(firstTracerDrugDto.getProgramName());
      excelRow.add(firstTracerDrugDto.getProductName());
      excelRow.add(firstTracerDrugDto.getProvinceName());
      excelRow.add(firstTracerDrugDto.getDistrictName());
      excelRow.add(firstTracerDrugDto.getFacilityName());
      excelRow.add(firstTracerDrugDto.getClosedCmm());
      excelRow.add(startDate + "-" + endDate);
      tracerDrugList.forEach(tracerDrug -> {
        excelRow.add(tracerDrug.getStockOnHand() == null ? EMPTY_VALUE : tracerDrug.getStockOnHand());
        colorArrays[colorRow.get()][colorColumn.getAndIncrement()] = tracerDrug.getStockStatusColorCode();
      });
      if (!excelRow.stream().skip(BASIC_COLUMN).allMatch(o -> o.equals(EMPTY_VALUE))) {
        colorRow.getAndIncrement();
        excelRows.add(excelRow);
      }
      colorColumn.set(0);
    });
    return excelRows;
  }

  public List<String> getRequisitionFacilityCode(String districtCode, String provinceCode) {
    List<RequisitionGeographicZonesDto> allAuthorizedFacility = getAllAuthorizedFacility();
    if (Objects.equals(provinceCode, ALL_GEOGRAPHIC_ZONE)) {
      return
          allAuthorizedFacility.stream().map(RequisitionGeographicZonesDto::getFacilityCode)
              .collect(Collectors.toList());
    } else if (Objects.equals(districtCode, ALL_GEOGRAPHIC_ZONE)) {
      return allAuthorizedFacility.stream()
          .filter(o -> Objects.equals(o.getProvinceCode(), provinceCode))
          .map(RequisitionGeographicZonesDto::getFacilityCode)
          .collect(Collectors.toList());
    }
    return allAuthorizedFacility.stream()
        .filter(o -> Objects.equals(o.getDistrictCode(), districtCode))
        .map(RequisitionGeographicZonesDto::getFacilityCode)
        .collect(Collectors.toList());
  }

  public List<List<String>> getHeadRow(Map<String, List<TracerDrugExcelDto>> collect) {
    List<List<String>> excelHead = new LinkedList<>();
    excelHead.add(Collections.singletonList(DRUG_CODE_PORTUGUESE));
    excelHead.add(Collections.singletonList(PROGRAM_PORTUGUESE));
    excelHead.add(Collections.singletonList(DRUG_NAME_PORTUGUESE));
    excelHead.add(Collections.singletonList(PROVINCE_PORTUGUESE));
    excelHead.add(Collections.singletonList(DISTRICT_PORTUGUESE));
    excelHead.add(Collections.singletonList(FACILITY_PORTUGUESE));
    excelHead.add(Collections.singletonList(CMM));
    excelHead.add(Collections.singletonList(REPORT_GENERATED_FOR_PORTUGUESE));
    List<TracerDrugExcelDto> firstTracerDrug = collect
        .values()
        .stream()
        .findFirst()
        .orElse(emptyList())
        .stream()
        .sorted(Comparator.comparing(
            TracerDrugExcelDto::getComputationTime))
        .collect(Collectors.toList());
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
    firstTracerDrug.forEach(firstTracerDrugDto -> excelHead.add(
        Collections.singletonList(sdf.format(firstTracerDrugDto.getComputationTime()))));
    return excelHead;
  }

  private WriteSheet getLegendaWriteSheet() {
    int[][] legendaColorArrays = new int[4][1];
    for (int i = 0; i < 4; i++) {
      legendaColorArrays[i][0] = i + 1;
    }
    return EasyExcelFactory
        .writerSheet(1, SUBTITLE)
        .registerWriteHandler(new CustomCellWriteHandler(legendaColorArrays, true))
        .head(Collections.singletonList(Collections.singletonList(SUBTITLE)))
        .build();
  }

  private List<List<String>> getLegendaRows() {
    List<List<String>> legendaRows = new LinkedList<>();
    legendaRows.add(Collections.singletonList(STOCK_OUT_PORTUGUESE));
    legendaRows.add(Collections.singletonList(LOW_STOCK_PORTUGUESE));
    legendaRows.add(Collections.singletonList(REGULAR_STOCK_PORTUGUESE));
    legendaRows.add(Collections.singletonList(OVER_STOCK_PORTUGUESE));
    return legendaRows;
  }

  private String getUniqueKey(String facilityCode, String productCode) {
    return facilityCode + "&" + productCode;
  }

  private List<RequisitionGeographicZonesDto> getAllAuthorizedFacility() {
    List<RequisitionGeographicZonesDto> requisitionGeographicZones = tracerDrugRepository
        .getAllRequisitionGeographicZones();
    if (authenticationHelper.isTheCurrentUserAdmin() || authenticationHelper.isTheCurrentUserReportViewer()) {
      return requisitionGeographicZones;
    }
    String level = authenticationHelper.getFacilityGeographicZoneLevel();
    String facilityCode = siglusFacilityReferenceDataService
        .findOne(authenticationHelper.getCurrentUser().getHomeFacilityId()).getCode();
    if (Objects.equals(level, DISTRICT)) {
      return getAuthorizedGeographicZonesByDistrictLevel(facilityCode,
          requisitionGeographicZones);
    } else if (Objects.equals(level, PROVINCE)) {
      return getAuthorizedGeographicZonesByProvinceLevel(facilityCode,
          requisitionGeographicZones);
    } else {
      return getAuthorizedGeographicZonesBySiteLevel(facilityCode,
          requisitionGeographicZones);
    }
  }

  private List<AssociatedGeographicZoneDto> getRequisitionGeographicZonesDtos() {
    return getDistinctGeographicZones(getAssociatedGeographicZoneDto(getAllAuthorizedFacility()));
  }

  private List<AssociatedGeographicZoneDto> getAssociatedGeographicZoneDto(
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {

    List<AssociatedGeographicZoneDto> districtGeographicZoneDto = requisitionGeographicZones
        .stream()
        .filter(o -> o.getDistrictCode() != null)
        .map(o -> o.getDistrictGeographicZoneDtoFrom(o))
        .collect(Collectors.toList());

    List<AssociatedGeographicZoneDto> provinceGeographicZoneDto = requisitionGeographicZones
        .stream()
        .filter(o -> o.getProvinceCode() != null)
        .map(o -> o.getProvinceGeographicZoneDtoFrom(o))
        .collect(Collectors.toList());
    return ListUtils.union(districtGeographicZoneDto, provinceGeographicZoneDto);
  }


  private List<AssociatedGeographicZoneDto> getDistinctGeographicZones(
      List<AssociatedGeographicZoneDto> requisitionDistrictGeographicZones) {
    return requisitionDistrictGeographicZones.stream().collect(
        Collectors.collectingAndThen(
            Collectors.toCollection(() ->
                new TreeSet<>(Comparator.comparing(AssociatedGeographicZoneDto::getCode))), ArrayList::new));
  }


  private List<RequisitionGeographicZonesDto> getAuthorizedGeographicZonesByDistrictLevel(String facilityCode,
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {
    return requisitionGeographicZones
        .stream()
        .filter(o -> o.getDistrictFacilityCode() != null)
        .filter(o -> o.getDistrictFacilityCode().equals(facilityCode))
        .collect(Collectors.toList());
  }


  private List<RequisitionGeographicZonesDto> getAuthorizedGeographicZonesByProvinceLevel(String facilityCode,
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {
    return requisitionGeographicZones
        .stream()
        .filter(o -> o.getProvinceFacilityCode() != null)
        .filter(o -> o.getProvinceFacilityCode().equals(facilityCode))
        .collect(Collectors.toList());
  }


  private List<RequisitionGeographicZonesDto> getAuthorizedGeographicZonesBySiteLevel(String facilityCode,
      List<RequisitionGeographicZonesDto> requisitionGeographicZones) {
    return requisitionGeographicZones
        .stream()
        .filter(o -> o.getFacilityCode() != null)
        .filter(o -> o.getFacilityCode().equals(facilityCode))
        .collect(Collectors.toList());
  }

  static class CalculationTable {

    private final HashMap<String, HashMap<String, Integer>> productCodeToLotSoh = new HashMap<>();
    private final HashMap<String, Double> productCodeToCmm = new HashMap<>();
    private LocalDate checkpoint;
    private final String facilityCode;

    CalculationTable(String facilityCode) {
      this.facilityCode = facilityCode;
    }

    public List<TracerDrugPersistentData> generateRecords() {
      return productCodeToLotSoh.entrySet().stream()
          .map(this::buildTracerDrugPersistentData)
          .collect(Collectors.toList());
    }

    public void updateWith(ProductLotSohDto productLotSoh) {
      String productCode = productLotSoh.getProductCode();
      productCodeToLotSoh.computeIfAbsent(productCode, key -> new HashMap<>());
      HashMap<String, Integer> lotIdToSoh = productCodeToLotSoh.get(productCode);
      lotIdToSoh.put(productLotSoh.getLotId(), productLotSoh.getStockOnHand());
    }

    public void updateWith(ProductCmm productCmm) {
      productCodeToCmm.put(productCmm.getProductCode(), productCmm.getCmm());
    }

    public void updateCheckpoint(LocalDate checkpoint) {
      this.checkpoint = checkpoint;
    }

    private TracerDrugPersistentData buildTracerDrugPersistentData(Entry<String, HashMap<String, Integer>> it) {
      String productCode = it.getKey();
      Integer productSoh = it.getValue().values().stream().reduce(Integer::sum).orElse(0);
      Double cmm = productCodeToCmm.getOrDefault(productCode, null);
      return TracerDrugPersistentData.builder()
          .productCode(productCode)
          .facilityCode(facilityCode)
          .stockOnHand(productSoh)
          .computationTime(checkpoint)
          .cmm(cmm)
          .build();
    }
  }

  static class MondayIterator implements Iterator<LocalDate> {

    private final LocalDate inclusiveEndDate;
    private LocalDate next;

    public MondayIterator(LocalDate inclusiveBeginDate, LocalDate inclusiveEndDate) {
      this.inclusiveEndDate = inclusiveEndDate;
      next = SiglusDateHelper.forwardToMonday(inclusiveBeginDate);
      if (next.isAfter(inclusiveEndDate)) {
        throw new IllegalArgumentException("date range is too narrow");
      }
    }

    @Override
    public boolean hasNext() {
      return Objects.nonNull(next);
    }

    @Override
    public LocalDate next() {
      if (Objects.isNull(next)) {
        throw new NoSuchElementException();
      }
      LocalDate ret = next;
      next = next.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
      if (next.isAfter(inclusiveEndDate)) {
        next = null;
      }
      return ret;
    }
  }
}
