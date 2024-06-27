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
import static org.siglus.siglusapi.util.SiglusDateHelper.DATE_MONTH_YEAR;
import static org.siglus.siglusapi.util.SiglusDateHelper.getFormatDate;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.StatusLogEntry;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.ConsultationNumberColumnDto;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.KitUsageLineItemDto;
import org.siglus.siglusapi.dto.KitUsageServiceLineItemDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@SuppressWarnings("PMD.CyclomaticComplexity")
public class ViaRequisitionReportService implements IRequisitionReportService {

  private final Set<String> supportedProgramSet = new HashSet<>(
      newArrayList(ProgramConstants.VIA_PROGRAM_CODE, ProgramConstants.MMC_PROGRAM_CODE));

  @Autowired
  private SiglusAdministrationsService administrationsService;

  @Autowired
  private SiglusProcessingPeriodService periodService;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Override
  public Set<String> supportedProgramCodes() {
    return supportedProgramSet;
  }

  @Override
  public InputStream getTemplateFile() {
    return getClass().getResourceAsStream("/static/requisition/VIA_pt.xlsx");
  }

  @Override
  public void generateReport(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    WriteSheet writeSheet = EasyExcelFactory.writerSheet(0).build();
    Map<String, Object> summary = buildSummary(requisition);
    excelWriter.fill(summary, writeSheet);
    FillConfig fillConfig = FillConfig.builder().forceNewRow(true).build();
    excelWriter.fill(new FillWrapper("product", buildProducts(requisition)), fillConfig, writeSheet);
  }

  private Map<String, Object> buildSummary(SiglusRequisitionDto requisition) {
    Map<String, Object> summary = new HashMap<>();
    summary.put("requisitionNumber", requisition.getRequisitionNumber());
    summary.put("regular", getCheckedStr(!requisition.getEmergency()));
    summary.put("emergency", getCheckedStr(requisition.getEmergency()));
    summary.put("viaVia", getCheckedStr(false));
    summary.put("submitDate", getSubmitDate(requisition));
    if (requisition.getKitUsageLineItems().size() >= 1) {
      KitUsageLineItemDto kitUsageLineItemDto = requisition.getKitUsageLineItems().get(0);
      KitUsageServiceLineItemDto hf = kitUsageLineItemDto.getServices().get("HF");
      if (!ObjectUtils.isEmpty(hf)) {
        summary.put("kitsReceived", hf.getValue());
      }
      KitUsageServiceLineItemDto chw = kitUsageLineItemDto.getServices().get("CHW");
      if (!ObjectUtils.isEmpty(chw)) {
        summary.put("kitsReceivedChw", chw.getValue());
      }
    }
    if (requisition.getKitUsageLineItems().size() >= 2) {
      KitUsageLineItemDto kitUsageLineItemDto = requisition.getKitUsageLineItems().get(1);
      KitUsageServiceLineItemDto hf = kitUsageLineItemDto.getServices().get("HF");
      if (!ObjectUtils.isEmpty(hf)) {
        summary.put("kitsOpened", hf.getValue());
      }
      KitUsageServiceLineItemDto chw = kitUsageLineItemDto.getServices().get("CHW");
      if (!ObjectUtils.isEmpty(chw)) {
        summary.put("kitsOpenedChw", chw.getValue());
      }
    }
    if (requisition.getConsultationNumberLineItems().size() >= 1) {
      Map<String, ConsultationNumberColumnDto> columnDtoMap =
          requisition.getConsultationNumberLineItems().get(0).getColumns();
      ConsultationNumberColumnDto consultationNumber = columnDtoMap.get("consultationNumber");
      if (!ObjectUtils.isEmpty(consultationNumber)) {
        summary.put("consultationNumber", consultationNumber.getValue());
      }
    }

    FacilitySearchResultDto facility = administrationsService.getFacility(requisition.getFacilityId());
    summary.put("facilityName", facility.getName());
    summary.put("district", facility.getGeographicZone().getName());
    summary.put("province", facility.getGeographicZone().getParent().getName());

    ProcessingPeriodDto periodDto = periodService.getProcessingPeriodDto(requisition.getProcessingPeriodId());
    summary.put("periodStartDate", getFormatDate(periodDto.getStartDate(), DATE_MONTH_YEAR));
    summary.put("periodEndDate", getFormatDate(periodDto.getEndDate(), DATE_MONTH_YEAR));

    Object signaure = requisition.getExtraData().get("signaure");
    if (signaure instanceof Map) {
      Object submit = ((Map<?, ?>) signaure).get("submit");
      if (!ObjectUtils.isEmpty(submit)) {
        summary.put("createdBy", submit.toString());
      }
      Object approve = ((Map<?, ?>) signaure).get("approve");
      if (!ObjectUtils.isEmpty(approve) && approve instanceof List) {
        summary.put("approvedBy", ((List<?>) approve).get(0).toString());
      }
    }
    return summary;
  }

  private List<ViaProduct> buildProducts(SiglusRequisitionDto requisition) {
    List<UUID> orderableIds = requisition.getLineItems().stream()
        .filter(item -> item instanceof RequisitionLineItemV2Dto)
        .map(item -> ((RequisitionLineItemV2Dto) item).getOrderable().getId())
        .collect(Collectors.toList());
    Map<UUID, OrderableDto> orderableDtoMap = orderableReferenceDataService.findByIds(orderableIds)
        .stream().collect(Collectors.toMap(BaseDto::getId, dto -> dto));
    return requisition.getLineItems().stream()
        .map(lineItem -> {
          ViaProduct product = new ViaProduct();
          OrderableDto orderable = orderableDtoMap.get(((RequisitionLineItemV2Dto) lineItem).getOrderable().getId());
          if (!ObjectUtils.isEmpty(orderable)) {
            product.setCode(orderable.getProductCode());
            product.setName(orderable.getFullProductName());
          }
          product.setInitialAmount(lineItem.getBeginningBalance());
          product.setSumEntries(lineItem.getTotalReceivedQuantity());
          product.setSumIssues(lineItem.getTotalConsumedQuantity());
          Integer theoreticalSum = lineItem.getBeginningBalance() + lineItem.getTotalReceivedQuantity()
              - lineItem.getTotalConsumedQuantity();
          product.setTheoreticalSum(theoreticalSum);
          product.setInventoryStock(lineItem.getStockOnHand());
          product.setDifference(lineItem.getStockOnHand() - theoreticalSum);
          int theoreticalRequest = 2 * lineItem.getTotalConsumedQuantity() - lineItem.getStockOnHand();
          product.setTheoreticalRequest(Math.min(theoreticalRequest, 0));
          product.setQuantityRequested(getQuantity(lineItem.getRequestedQuantity()));
          product.setQuantityApproved(getQuantity(lineItem.getAuthorizedQuantity()));
          return product;
        })
        .sorted((productA, productB) -> String.CASE_INSENSITIVE_ORDER.compare(productA.name, productB.name))
        .collect(Collectors.toList());
  }

  private String getSubmitDate(SiglusRequisitionDto requisition) {
    StatusLogEntry statusLogEntry = requisition.getStatusChanges().get(RequisitionStatus.SUBMITTED.name());
    if (ObjectUtils.isEmpty(statusLogEntry)) {
      return "";
    }
    return getFormatDate(LocalDate.from(statusLogEntry.getChangeDate()), DATE_MONTH_YEAR);
  }

  private String getCheckedStr(Boolean checked) {
    return checked ? "☑" : "☐";
  }

  private int getQuantity(Integer value) {
    return ObjectUtils.isEmpty(value) ? 0 : value;
  }

  @Data
  public static class ViaProduct {
    private String code;
    private String name;
    private Integer initialAmount;
    private Integer sumEntries;
    private Integer sumIssues;
    private Integer theoreticalSum;
    private String totalRequested = "-";
    private Integer inventoryStock;
    private Integer difference;
    private Integer theoreticalRequest;
    private Integer quantityRequested;
    private Integer quantityApproved;
  }
}
