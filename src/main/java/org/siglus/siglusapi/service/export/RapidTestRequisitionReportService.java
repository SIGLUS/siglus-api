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
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_5;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_6;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.NEW_COLUMN_7;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_CONSUMO;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_HIVDETERMINE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_POSITIVE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_POSITIVE_HIV;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_POSITIVE_SYPHILIS;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.PROJECT_UNJUSTIFIED;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.SERVICE_APES;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.SERVICE_HF;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.TestConsumptionLineItems.TOTAL;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.openlmis.requisition.dto.StatusMessageDto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.GeographicProvinceDistrictDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.TestConsumptionOutcomeDto;
import org.siglus.siglusapi.dto.TestConsumptionProjectDto;
import org.siglus.siglusapi.dto.TestConsumptionServiceDto;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RapidTestRequisitionReportService implements IRequisitionReportService {

  private final Set<String> supportedProgramSet = new HashSet<>(newArrayList(ProgramConstants.RAPIDTEST_PROGRAM_CODE));

  @Autowired
  private SiglusGeographicInfoRepository siglusGeographicInfoRepository;
  @Autowired
  private SiglusFacilityRepository siglusFacilityRepository;
  @Autowired
  private SiglusProcessingPeriodService siglusProcessingPeriodService;
  @Autowired
  private OrderableRepository orderableRepository;

  @Override
  public Set<String> supportedProgramCodes() {
    return supportedProgramSet;
  }

  @Override
  public InputStream getTemplateFile() {
    return getClass().getResourceAsStream("/static/requisition/TR_pt.xlsx");
  }

  @Override
  public void generateReport(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    WriteSheet writeSheet = EasyExcel.writerSheet().build();
    fillTopContent(requisition, excelWriter, writeSheet);
    fillProductListContent(requisition, excelWriter, writeSheet);
    fillConsumptionListContent(requisition, excelWriter, writeSheet);
    reWriteConsumptionListContent(requisition, excelWriter);
    fillBottomContent(requisition, excelWriter, writeSheet);
  }

  private void reWriteConsumptionListContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    int apesSourceRow = 6 + requisition.getLineItems().size() + 15;
    int apesTargetRow = apesSourceRow + 1;
    Sheet sheet = excelWriter.writeContext().writeSheetHolder().getCachedSheet();
    Row sourceRow = sheet.getRow(apesSourceRow);
    Row targetRow = sheet.getRow(apesTargetRow);
    for (int colIndex = 2; colIndex <= 26; colIndex++) {
      Cell sourceCell = sourceRow.getCell(colIndex);
      Cell targetCell = targetRow.getCell(colIndex);
      if (sourceCell != null) {
        targetCell.setCellStyle(sourceCell.getCellStyle());
        switch (sourceCell.getCellType()) {
          case BLANK:
            targetCell.setBlank();
            break;
          case BOOLEAN:
            targetCell.setCellValue(sourceCell.getBooleanCellValue());
            break;
          case ERROR:
            targetCell.setCellErrorValue(sourceCell.getErrorCellValue());
            break;
          case FORMULA:
            targetCell.setCellFormula(sourceCell.getCellFormula());
            break;
          case NUMERIC:
            targetCell.setCellValue(sourceCell.getNumericCellValue());
            break;
          case STRING:
            targetCell.setCellValue(sourceCell.getStringCellValue());
            break;
          default:
            break;
        }
        sourceCell.setCellValue("");
        sourceCell.setCellStyle(null);
      }
    }
  }

  private void fillConsumptionListContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter,
      WriteSheet writeSheet) {
    List<TestConsumptionServiceDto> lineItems = requisition.getTestConsumptionLineItems();
    ArrayList<String> sortedServices = newArrayList(SERVICE_HF, NEW_COLUMN_0, NEW_COLUMN_3, NEW_COLUMN_4, NEW_COLUMN_5,
        NEW_COLUMN_1, NEW_COLUMN_6, NEW_COLUMN_2, NEW_COLUMN_7, TOTAL, SERVICE_APES);
    ArrayList<String> sortedProjects = newArrayList(PROJECT_HIVDETERMINE, NEW_COLUMN_0, NEW_COLUMN_1, NEW_COLUMN_2,
        NEW_COLUMN_3, NEW_COLUMN_4, NEW_COLUMN_5, NEW_COLUMN_6);
    ArrayList<String> sortedOutcomes = newArrayList(PROJECT_CONSUMO, PROJECT_POSITIVE, PROJECT_POSITIVE_HIV,
        PROJECT_POSITIVE_SYPHILIS, PROJECT_UNJUSTIFIED);
    List<TestesConsumption> testesConsumptions = newArrayList();
    for (String service : sortedServices) {
      Optional<TestConsumptionServiceDto> serviceDto = lineItems.stream()
          .filter(lineItem -> service.equals(lineItem.getService())).findFirst();
      if (serviceDto.isPresent()) {
        List<Integer> consumptions = new ArrayList<>();
        Map<String, TestConsumptionProjectDto> projectDtoMap = serviceDto.get().getProjects();
        for (String project : sortedProjects) {
          TestConsumptionProjectDto projectDto = projectDtoMap.get(project);
          if (projectDto != null) {
            Map<String, TestConsumptionOutcomeDto> outcomeDtoMap = projectDto.getOutcomes();
            for (String outcome : sortedOutcomes) {
              TestConsumptionOutcomeDto outcomeDto = outcomeDtoMap.get(outcome);
              if (outcomeDto != null) {
                consumptions.add(outcomeDto.getValue());
              }
            }
          }
        }
        testesConsumptions.add(TestesConsumption.from(consumptions));
      }
    }
    excelWriter.fill(new FillWrapper("consumptions", testesConsumptions), writeSheet);
  }

  private void fillProductListContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter,
      WriteSheet writeSheet) {
    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
    excelWriter.fill(new FillWrapper("product", buildProductList(requisition)), fillConfig, writeSheet);
  }

  private List<TestesProduct> buildProductList(SiglusRequisitionDto requisition) {
    List<BaseRequisitionLineItemDto> lineItems = requisition.getLineItems();
    Set<UUID> orderableIds = lineItems.stream().map(lineItem -> lineItem.getOrderableIdentity().getId())
        .collect(Collectors.toSet());
    Map<UUID, Orderable> orderableIdToOrderableMap = orderableRepository.findLatestByIds(orderableIds).stream()
        .collect(Collectors.toMap(Orderable::getId, Function.identity()));

    return requisition.getLineItems().stream()
        .map(lineItem -> TestesProduct.from(orderableIdToOrderableMap, lineItem))
        .collect(Collectors.toList());
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

  private void fillBottomContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter, WriteSheet writeSheet) {
    Map<String, Object> bottomContent = new HashMap<>();
    bottomContent.put("createdDate",
        requisition.getCreatedDate().toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("pt", "MZ"))));
    Map<String, Object> extraDataMap = requisition.getExtraData();
    Map<String, Object> signature = (Map<String, Object>) extraDataMap.get("signaure");
    bottomContent.put("observer", signature.get("submit"));
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

  @Data
  public static class TestesProduct {

    private String empty;
    private String code;
    private String name;
    private Integer initiate;
    private Integer receive;
    private Integer issue;
    private Integer adjust;
    private Integer inventory;
    private String expiredDate;

    public static TestesProduct from(Map<UUID, Orderable> orderableIdToOrderableMap,
        BaseRequisitionLineItemDto lineItem) {
      TestesProduct product = new TestesProduct();
      Orderable orderable = orderableIdToOrderableMap.get(lineItem.getOrderableIdentity().getId());
      product.setCode(orderable.getProductCode().toString());
      product.setName(orderable.getFullProductName());
      product.setInitiate(lineItem.getBeginningBalance());
      product.setIssue(lineItem.getTotalConsumedQuantity());
      product.setReceive(lineItem.getTotalReceivedQuantity());
      product.setAdjust(lineItem.getTotalLossesAndAdjustments());
      product.setInventory(lineItem.getStockOnHand());
      LocalDate expirationDate = lineItem.getExpirationDate();
      product.setExpiredDate(
          expirationDate == null ? "" : expirationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      return product;
    }
  }

  @Data
  public static class TestesConsumption {

    private Integer determineConsumption;
    private Integer determinePositive;
    private Integer determineUnjustified;

    private Integer unigoidConsumption;
    private Integer unigoidPositive;
    private Integer unigoidUnjustified;

    private Integer syphilisConsumption;
    private Integer syphilisPositive;
    private Integer syphilisUnjustified;

    private Integer malariaConsumption;
    private Integer malariaPositive;
    private Integer malariaUnjustified;

    private Integer duoTestesConsumption;
    private Integer duoTestesPositiveHiv;
    private Integer duoTestesPositiveSyphilis;
    private Integer duoTestesUnjustified;

    private Integer btestesConsumption;
    private Integer btestesPositive;
    private Integer btestesUnjustified;

    private Integer tdrConsumption;
    private Integer tdrPositive;
    private Integer tdrUnjustified;

    private Integer novoConsumption;
    private Integer novoPositive;
    private Integer novoUnjustified;

    public static TestesConsumption from(List<Integer> consumptions) {
      TestesConsumption testesConsumption = new TestesConsumption();
      if (consumptions.size() == 12) {
        setFirst12Value(testesConsumption, consumptions);
      } else if (consumptions.size() == 25) {
        setFirst12Value(testesConsumption, consumptions);
        setLast13Value(testesConsumption, consumptions);
      }
      return testesConsumption;
    }

    private static void setFirst12Value(TestesConsumption testesConsumption, List<Integer> consumptions) {
      testesConsumption.setDetermineConsumption(consumptions.get(0));
      testesConsumption.setDeterminePositive(consumptions.get(1));
      testesConsumption.setDetermineUnjustified(consumptions.get(2));

      testesConsumption.setUnigoidConsumption(consumptions.get(3));
      testesConsumption.setUnigoidPositive(consumptions.get(4));
      testesConsumption.setUnigoidUnjustified(consumptions.get(5));

      testesConsumption.setSyphilisConsumption(consumptions.get(6));
      testesConsumption.setSyphilisPositive(consumptions.get(7));
      testesConsumption.setSyphilisUnjustified(consumptions.get(8));

      testesConsumption.setMalariaConsumption(consumptions.get(9));
      testesConsumption.setMalariaPositive(consumptions.get(10));
      testesConsumption.setMalariaUnjustified(consumptions.get(11));
    }

    private static void setLast13Value(TestesConsumption testesConsumption, List<Integer> consumptions) {
      testesConsumption.setDuoTestesConsumption(consumptions.get(12));
      testesConsumption.setDuoTestesPositiveHiv(consumptions.get(13));
      testesConsumption.setDuoTestesPositiveSyphilis(consumptions.get(14));
      testesConsumption.setDuoTestesUnjustified(consumptions.get(15));

      testesConsumption.setBtestesConsumption(consumptions.get(16));
      testesConsumption.setBtestesPositive(consumptions.get(17));
      testesConsumption.setBtestesUnjustified(consumptions.get(18));

      testesConsumption.setTdrConsumption(consumptions.get(19));
      testesConsumption.setTdrPositive(consumptions.get(20));
      testesConsumption.setTdrUnjustified(consumptions.get(21));

      testesConsumption.setNovoConsumption(consumptions.get(22));
      testesConsumption.setNovoPositive(consumptions.get(23));
      testesConsumption.setNovoUnjustified(consumptions.get(24));
    }
  }
}
