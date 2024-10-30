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

package org.siglus.siglusapi.service.export;

import static com.google.common.collect.Lists.newArrayList;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.openlmis.requisition.dto.StatusMessageDto;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.AgeGroupLineItemDto;
import org.siglus.siglusapi.dto.AgeGroupServiceDto;
import org.siglus.siglusapi.dto.GeographicProvinceDistrictDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
public class MtbRequisitionReportServiceService implements IRequisitionReportService {

  private final Set<String> supportedProgramSet = new HashSet<>(newArrayList(ProgramConstants.MTB_PROGRAM_CODE));
  @Autowired
  private SiglusGeographicInfoRepository siglusGeographicInfoRepository;
  @Autowired
  private SiglusFacilityRepository siglusFacilityRepository;
  @Autowired
  private SiglusProcessingPeriodService siglusProcessingPeriodService;
  @Autowired
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;
  @Autowired
  private OrderableRepository orderableRepository;


  @Override
  public Set<String> supportedProgramCodes() {
    return supportedProgramSet;
  }

  @Override
  public InputStream getTemplateFile() {
    return getClass().getResourceAsStream("/static/requisition/MTB_pt.xlsx");
  }

  @Override
  public void generateReport(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    WriteSheet writeSheet = EasyExcel.writerSheet().build();
    fillTopContent(requisition, excelWriter, writeSheet);
    fillProductListContent(requisition, excelWriter, writeSheet);
    fillPatientContent(requisition, excelWriter, writeSheet);
    fillBottomContent(requisition, excelWriter, writeSheet);
  }

  private void fillTopContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter, WriteSheet writeSheet) {
    Map<String, Object> topContent = new HashMap<>();
    UUID facilityId = requisition.getFacilityId();
    Facility facility = siglusFacilityRepository.findOne(facilityId);
    GeographicProvinceDistrictDto geographic = siglusGeographicInfoRepository
        .getGeographicProvinceDistrictInfo(facility.getCode());
    topContent.put("provinceName", geographic.getProvinceName());
    topContent.put("districtName", geographic.getDistrictName());
    topContent.put("facilityName", facility.getName());
    UUID processingPeriodId = requisition.getProcessingPeriodId();
    ProcessingPeriodDto processingPeriodDto = siglusProcessingPeriodService.getProcessingPeriodDto(processingPeriodId);
    topContent.put("year", processingPeriodDto.getEndDate().getYear());
    topContent.put("month", processingPeriodDto.getEndDate().format(DateTimeFormatter.ofPattern("MMMM",
        new Locale("pt", "MZ"))));
    excelWriter.fill(topContent, writeSheet);
  }

  private void fillProductListContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter,
      WriteSheet writeSheet) {
    List<BaseRequisitionLineItemDto> lineItems = requisition.getLineItems();
    Set<UUID> orderableIds = lineItems.stream().map(lineItem -> lineItem.getOrderableIdentity().getId())
        .collect(Collectors.toSet());
    Map<UUID, String> orderableIdToNameMap = orderableRepository.findLatestByIds(orderableIds).stream()
        .collect(Collectors.toMap(Orderable::getId, Orderable::getFullProductName));
    Map<UUID, String> orderableIdToUnitMap = programOrderablesExtensionRepository
        .findAllByProgramCode(ProgramConstants.MTB_PROGRAM_CODE).stream()
        .collect(Collectors.toMap(ProgramOrderablesExtension::getOrderableId,
            extension -> extension.getUnit() == null ? "" : extension.getUnit()));
    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
    List<Map<String, Object>> productContents = new ArrayList<>();
    lineItems.forEach(lineItem -> {
      Map<String, Object> productContent = new HashMap<>();
      UUID orderableId = lineItem.getOrderableIdentity().getId();
      productContent.put("productName", orderableIdToNameMap.getOrDefault(orderableId, ""));
      productContent.put("productUnit", orderableIdToUnitMap.getOrDefault(orderableId, ""));
      productContent.put("productInitialStock", lineItem.getBeginningBalance());
      productContent.put("productEntries", lineItem.getTotalReceivedQuantity());
      productContent.put("productIssues", lineItem.getTotalConsumedQuantity());
      productContent.put("productAdjustments", lineItem.getTotalLossesAndAdjustments());
      productContent.put("productInventory", lineItem.getStockOnHand());
      LocalDate expirationDate = lineItem.getExpirationDate();
      productContent.put("productExpireDate", expirationDate == null ? "" : expirationDate.toString());
      productContent.put("requestedQuantity", getQuantity(lineItem.getRequestedQuantity()));
      productContent.put("authorizedQuantity", getQuantity(lineItem.getAuthorizedQuantity()));
      productContent.put("approvedQuantity", getQuantity(lineItem.getApprovedQuantity()));
      productContents.add(productContent);
    });
    excelWriter.fill(new FillWrapper("productLineItem", productContents), fillConfig, writeSheet);
  }

  private void fillBottomContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter, WriteSheet writeSheet) {
    Map<String, Object> bottomContent = new HashMap<>();
    bottomContent.put("createdDate",
        requisition.getCreatedDate().toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("pt", "MZ"))));
    Map<String, Object> extraDataMap = requisition.getExtraData();
    Map<String, Object> signature = (Map<String, Object>) extraDataMap.get("signaure");
    bottomContent.put("authorize", signature.get("authorize"));
    List<String> approves = (List<String>) signature.get("approve");
    bottomContent.put("approve", String.join(",", approves));
    List<StatusChangeDto> statusHistory = requisition.getStatusHistory();
    StringBuilder stringBuilder = new StringBuilder();
    statusHistory.forEach(statusChangeDto -> {
      String comment = Optional.ofNullable(statusChangeDto.getStatusMessageDto())
          .orElse(new StatusMessageDto())
          .getBody();
      stringBuilder.append(comment == null ? "" : comment);
    });
    bottomContent.put("comment", stringBuilder.toString());
    excelWriter.fill(bottomContent, writeSheet);
  }

  private void fillPatientContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter, WriteSheet writeSheet) {
    Map<String, Object> patientContent = new HashMap<>();
    List<PatientGroupDto> patientLineItems = requisition.getPatientLineItems();
    patientLineItems.forEach(patientGroupDto -> {
      Map<String, PatientColumnDto> columnMap = patientGroupDto.getColumns();
      columnMap.keySet().forEach(columnKey ->
          patientContent.put(patientGroupDto.getName() + columnKey, columnMap.get(columnKey).getValue()));
    });
    excelWriter.fill(patientContent, writeSheet);

    Map<String, Object> ageContent = new HashMap<>();
    List<AgeGroupServiceDto> ageGroupLineItems = requisition.getAgeGroupLineItems();
    ageGroupLineItems.forEach(ageGroupServiceDto -> {
      Map<String, AgeGroupLineItemDto> columnMap = ageGroupServiceDto.getColumns();
      columnMap.keySet().forEach(columnKey ->
          ageContent.put(ageGroupServiceDto.getService() + columnKey, columnMap.get(columnKey).getValue()));
    });
    excelWriter.fill(ageContent, writeSheet);
  }

  private static int getQuantity(Integer value) {
    return ObjectUtils.isEmpty(value) ? 0 : value;
  }

}
