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
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_0;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_COLUMN_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_0;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_1;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_2;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_3;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_4;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_5;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_6;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_8;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.NEW_SECTION_9;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.PATIENT_TYPE;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.STLINHAS;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.TOTAL_COLUMN;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_COMMUNITY;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.RegimenLineItems.COLUMN_NAME_PATIENT;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;

import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;
import com.alibaba.excel.write.metadata.fill.FillWrapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.StatusChangeDto;
import org.openlmis.requisition.dto.StatusMessageDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.service.referencedata.OrderableReferenceDataService;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.dto.GeographicProvinceDistrictDto;
import org.siglus.siglusapi.dto.PatientColumnDto;
import org.siglus.siglusapi.dto.PatientGroupDto;
import org.siglus.siglusapi.dto.RegimenColumnDto;
import org.siglus.siglusapi.dto.RegimenLineDto;
import org.siglus.siglusapi.dto.RegimenSummaryLineDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@SuppressWarnings("PMD.CyclomaticComplexity")
public class TarvRequisitionReportService implements IRequisitionReportService {

  private final Set<String> supportedProgramSet = new HashSet<>(newArrayList(ProgramConstants.TARV_PROGRAM_CODE));

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
  @Autowired
  private OrderableReferenceDataService orderableReferenceDataService;

  @Override
  public Set<String> supportedProgramCodes() {
    return supportedProgramSet;
  }

  @Override
  public InputStream getTemplateFile() {
    return getClass().getResourceAsStream("/static/requisition/TARV_pt.xlsx");
  }

  @Override
  public void generateReport(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    WriteSheet writeSheet = EasyExcel.writerSheet().build();
    fillTopContent(requisition, excelWriter, writeSheet);
    fillProductListContent(requisition, excelWriter, writeSheet);
    fillRegimenListContent(requisition, excelWriter, writeSheet);
    fillRegimenAndSummaryContent(requisition, excelWriter, writeSheet);
    fillPatientContent(requisition, excelWriter, writeSheet);
    reWritePatientContent(requisition, excelWriter);
    fillBottomContent(requisition, excelWriter, writeSheet);
    mergeCells(requisition, excelWriter);
  }

  private void mergeCells(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    Set<String> orderableCategories = new HashSet<>(
        buildOrderableIdToCategoryMap(requisition.getLineItems()).values());
    int adultFirstRow =
        4 + requisition.getLineItems().size() + 2 * (orderableCategories.size() - 1) + 2;
    Set<RegimenLineDto> adultoRegimens = requisition.getRegimenLineItems().stream()
        .filter(lineItem -> lineItem.getRegimen() != null
            && Objects.equals("Adulto", lineItem.getRegimen().getRegimenCategory().getName()))
        .collect(Collectors.toSet());
    int adultLastRow = adultFirstRow + adultoRegimens.size() + 1;
    int childrenFirstRow = adultLastRow + 1;
    Set<RegimenLineDto> childrenRegimens = requisition.getRegimenLineItems().stream()
        .filter(lineItem -> lineItem.getRegimen() != null
            && Objects.equals("Criança", lineItem.getRegimen().getRegimenCategory().getName()))
        .collect(Collectors.toSet());
    int childrenLastRow = childrenFirstRow + childrenRegimens.size() + 1;

    Sheet sheet = excelWriter.writeContext().writeSheetHolder().getCachedSheet();
    // regimenList
    mergeCell(adultFirstRow, adultLastRow, 1, 1, sheet);
    mergeCell(childrenFirstRow, childrenLastRow, 1, 1, sheet);
    // Tipo de doentes em TARV
    mergeCell(adultFirstRow, adultFirstRow, 6, 8, sheet);
    mergeCell(adultFirstRow + 1, adultFirstRow + 1, 6, 8, sheet);
    mergeCell(adultFirstRow + 2, adultFirstRow + 2, 6, 8, sheet);
    mergeCell(adultFirstRow + 3, adultFirstRow + 3, 6, 8, sheet);
    mergeCell(adultFirstRow + 4, adultFirstRow + 4, 6, 8, sheet);
    // Faixa Etária dos Pacientes TARV
    mergeCell(adultFirstRow + 5, adultFirstRow + 5, 6, 9, sheet);
    mergeCell(adultFirstRow + 6, adultFirstRow + 6, 6, 8, sheet);
    mergeCell(adultFirstRow + 7, adultFirstRow + 7, 6, 8, sheet);
    mergeCell(adultFirstRow + 8, adultFirstRow + 8, 6, 8, sheet);
    mergeCell(adultFirstRow + 9, adultFirstRow + 9, 6, 8, sheet);
    // Profilaxia
    mergeCell(adultFirstRow + 10, adultFirstRow + 10, 6, 9, sheet);
    mergeCell(adultFirstRow + 11, adultFirstRow + 11, 6, 8, sheet);
    mergeCell(adultFirstRow + 12, adultFirstRow + 12, 6, 8, sheet);
    // Total global
    mergeCell(adultFirstRow + 13, adultFirstRow + 13, 6, 9, sheet);
    mergeCell(adultFirstRow + 14, adultFirstRow + 14, 6, 8, sheet);
    mergeCell(adultFirstRow + 15, adultFirstRow + 15, 6, 8, sheet);
    // Tipo de Dispensa - Levantaram no mês
    mergeCell(adultFirstRow + 16, adultFirstRow + 16, 6, 9, sheet);
    mergeCell(adultFirstRow + 17, adultFirstRow + 17, 6, 8, sheet);
    mergeCell(adultFirstRow + 18, adultFirstRow + 18, 6, 8, sheet);
    mergeCell(adultFirstRow + 19, adultFirstRow + 19, 6, 8, sheet);
    mergeCell(adultFirstRow + 20, adultFirstRow + 20, 6, 8, sheet);
    mergeCell(adultFirstRow + 21, adultFirstRow + 21, 6, 8, sheet);
    // Tipo de Dispensa - Total de pacientes com tratamento
    mergeCell(adultFirstRow + 22, adultFirstRow + 22, 6, 9, sheet);
    mergeCell(adultFirstRow + 23, adultFirstRow + 23, 6, 8, sheet);
    mergeCell(adultFirstRow + 24, adultFirstRow + 24, 6, 8, sheet);
    mergeCell(adultFirstRow + 25, adultFirstRow + 25, 6, 8, sheet);
    mergeCell(adultFirstRow + 26, adultFirstRow + 26, 6, 8, sheet);
    mergeCell(adultFirstRow + 27, adultFirstRow + 27, 6, 8, sheet);
    // Tipo de Dispensa
    mergeCell(adultFirstRow + 29, adultFirstRow + 29, 6, 11, sheet);
  }

  private void mergeCell(int firstRow, int lastRow, int firstCol, int lastCol, Sheet sheet) {
    CellRangeAddress rangeAddress = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
    sheet.addMergedRegion(rangeAddress);
  }

  private void fillPatientContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter, WriteSheet writeSheet) {
    Map<String, Object> patientContent = new HashMap<>();
    List<PatientGroupDto> patientLineItems = requisition.getPatientLineItems();
    Long total0mes = 0L;
    Long totalTotal = 0L;
    for (PatientGroupDto lineItem : patientLineItems) {
      String name = lineItem.getName();
      Map<String, PatientColumnDto> columns = lineItem.getColumns();
      if (PATIENT_TYPE.equals(name)) {
        patientContent.put("novos", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("Manutenção", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        patientContent.put("Alteração", getPatientGroupDtoValue(columns.get(NEW_COLUMN_3)));
        patientContent.put("Trânsito", getPatientGroupDtoValue(columns.get(NEW_COLUMN_1)));
        patientContent.put("Transferências", getPatientGroupDtoValue(columns.get(NEW_COLUMN_2)));
      } else if (NEW_SECTION_0.equals(name)) {
        patientContent.put("Adultos", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("4anos", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        patientContent.put("9anos", getPatientGroupDtoValue(columns.get(NEW_COLUMN_1)));
        patientContent.put("14anos", getPatientGroupDtoValue(columns.get(NEW_COLUMN_2)));
      } else if (NEW_SECTION_1.equals(name)) {
        patientContent.put("PPE", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("Exposta", getPatientGroupDtoValue(columns.get(NEW_COLUMN_1)));
      } else if (NEW_SECTION_8.equals(name)) {
        patientContent.put("totalPatient", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("totalMeses", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
      } else if (NEW_SECTION_2.equals(name)) {
        patientContent.put("DS5mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("DS4mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        patientContent.put("DS3mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN_1)));
        patientContent.put("DS2mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN_2)));
        patientContent.put("DS1mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN_3)));
        Integer newColumn3 = getPatientGroupDtoValue(columns.get(NEW_COLUMN_4));
        total0mes += newColumn3;
        patientContent.put("DS0mes", newColumn3);
        Integer total = getPatientGroupDtoValue(columns.get(TOTAL_COLUMN));
        totalTotal += total;
        patientContent.put("DSTotal", getPatientGroupDtoValue(columns.get(TOTAL_COLUMN)));
      } else if (NEW_SECTION_3.equals(name)) {
        patientContent.put("DT2mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("DT1mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        Integer newColumn1 = getPatientGroupDtoValue(columns.get(NEW_COLUMN_1));
        total0mes += newColumn1;
        patientContent.put("DT0mes", newColumn1);
        Integer total = getPatientGroupDtoValue(columns.get(TOTAL_COLUMN));
        totalTotal += total;
        patientContent.put("DTTotal", total);
      } else if (NEW_SECTION_4.equals(name)) {
        Integer value = getPatientGroupDtoValue(columns.get(NEW_COLUMN));
        total0mes += value;
        patientContent.put("DM0mes", value);
        Integer total = getPatientGroupDtoValue(columns.get(TOTAL_COLUMN));
        totalTotal += total;
        patientContent.put("DMTotal", total);
      } else if (NEW_SECTION_5.equals(name)) {
        patientContent.put("MesDS", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("MesDT", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        if (columns.containsKey(NEW_COLUMN_2)) {
          patientContent.put("MesDB", getPatientGroupDtoValue(columns.get(NEW_COLUMN_2)));
        }
        patientContent.put("MesDM", getPatientGroupDtoValue(columns.get(NEW_COLUMN_1)));
        patientContent.put("MesTotal", getPatientGroupDtoValue(columns.get(TOTAL_COLUMN)));
      } else if (NEW_SECTION_6.equals(name)) {
        patientContent.put("TrateDS", getPatientGroupDtoValue(columns.get(NEW_COLUMN)));
        patientContent.put("TrateDT", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        if (columns.containsKey(NEW_COLUMN_2)) {
          patientContent.put("TrateDB", getPatientGroupDtoValue(columns.get(NEW_COLUMN_2)));
        }
        patientContent.put("TrateDM", getPatientGroupDtoValue(columns.get(NEW_COLUMN_1)));
        patientContent.put("TrateTotal", getPatientGroupDtoValue(columns.get(TOTAL_COLUMN)));
      } else if (NEW_SECTION_9.equals(name)) {
        patientContent.put("DB1mes", getPatientGroupDtoValue(columns.get(NEW_COLUMN_0)));
        Integer newColumn1 = getPatientGroupDtoValue(columns.get(NEW_COLUMN_1));
        total0mes += newColumn1;
        patientContent.put("DB0mes", newColumn1);
        Integer total = getPatientGroupDtoValue(columns.get(TOTAL_COLUMN));
        totalTotal += total;
        patientContent.put("DBTotal", total);
      }
    }
    patientContent.put("total0mes", total0mes);
    patientContent.put("totalTotal", totalTotal);
    patientContent.put("Ajuste", total0mes == 0 ? "0.00" : String.format("%.2f", (double) totalTotal / total0mes));
    List<StatusChangeDto> statusHistory = requisition.getStatusHistory();
    StringBuilder stringBuilder = new StringBuilder();
    statusHistory.forEach(statusChangeDto -> {
      String comment = Optional.ofNullable(statusChangeDto.getStatusMessageDto())
          .orElse(new StatusMessageDto())
          .getBody();
      stringBuilder.append(comment == null ? "" : comment);
    });
    patientContent.put("comment", stringBuilder.toString());
    excelWriter.fill(patientContent, writeSheet);
  }

  private Integer getPatientGroupDtoValue(PatientColumnDto dto) {
    return dto == null ? 0 : dto.getValue();
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
    excelWriter.fill(bottomContent, writeSheet);
  }

  private void reWritePatientContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter) {
    Set<String> orderableCategories = new HashSet<>(
        buildOrderableIdToCategoryMap(requisition.getLineItems()).values());
    int regimenSize = requisition.getRegimenLineItems().size();
    int sourceStartRow =
        4 + requisition.getLineItems().size() + 2 * (orderableCategories.size() - 1) + 2 + regimenSize + 13;
    int sourceEndRow = sourceStartRow + 38 + 2;
    CellRangeAddress sourceRange = CellRangeAddress.valueOf("B" + sourceStartRow + ":G" + sourceEndRow);

    int targetStartRow = 4 + requisition.getLineItems().size() + 2 * (orderableCategories.size() - 1) + 2;
    int targetEndRow = targetStartRow + 38 + 2;
    CellRangeAddress targetRange = CellRangeAddress.valueOf("G" + targetStartRow + ":L" + targetEndRow);

    Sheet sheet = excelWriter.writeContext().writeSheetHolder().getCachedSheet();
    for (int rowNum = sourceRange.getFirstRow(); rowNum <= sourceRange.getLastRow(); rowNum++) {
      Row sourceRow = sheet.getRow(rowNum);
      Row targetRow = sheet.getRow(rowNum + targetRange.getFirstRow() - sourceRange.getFirstRow());
      if (targetRow == null) {
        targetRow = sheet.createRow(rowNum + targetRange.getFirstRow() - sourceRange.getFirstRow());
      }

      for (int colIndex = sourceRange.getFirstColumn(); colIndex <= sourceRange.getLastColumn(); colIndex++) {
        if (sourceRow == null) {
          continue;
        }
        Cell sourceCell = sourceRow.getCell(colIndex);
        Cell targetCell = targetRow.createCell(colIndex + targetRange.getFirstColumn() - sourceRange.getFirstColumn());

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

  }

  private void fillRegimenListContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter,
      WriteSheet writeSheet) {
    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
    List<TarvRegimen> tarvRegimens = buildRegimenList(requisition);
    List<TarvRegimen> adultRegimens = tarvRegimens.stream()
        .filter(regimen -> "Adulto".equals(regimen.category))
        .collect(Collectors.toList());
    int index = 1;
    for (TarvRegimen adultRegimen : adultRegimens) {
      adultRegimen.setIndex(index);
      index++;
    }
    excelWriter.fill(new FillWrapper("adultRegimen", adultRegimens), fillConfig, writeSheet);
    List<TarvRegimen> childrenRegimens = tarvRegimens.stream()
        .filter(regimen -> "Criança".equals(regimen.category))
        .collect(Collectors.toList());
    for (TarvRegimen childrenRegimen : childrenRegimens) {
      childrenRegimen.setIndex(index);
      index++;
    }
    excelWriter.fill(new FillWrapper("childrenRegimen", childrenRegimens), fillConfig, writeSheet);
  }

  private void fillRegimenAndSummaryContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter,
      WriteSheet writeSheet) {
    Map<String, Object> regimenMap = new HashMap<>();
    RegimenLineDto totalRegimen = requisition.getRegimenLineItems().stream()
        .filter(lineItem -> lineItem.getRegimen() == null && "total".equals(lineItem.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("total regimen Not Found"));
    Map<String, RegimenColumnDto> totalRegimenMap = totalRegimen.getColumns();
    regimenMap.put("totalRegimenCommunity", getRegimenColumnDtoValue(totalRegimenMap.get(COLUMN_NAME_COMMUNITY)));
    regimenMap.put("totalRegimenPatients", getRegimenColumnDtoValue(totalRegimenMap.get(COLUMN_NAME_PATIENT)));

    List<RegimenSummaryLineDto> regimenSummaryLineItems = requisition.getRegimenSummaryLineItems();
    regimenSummaryLineItems.forEach(lineItem -> {
      Map<String, RegimenColumnDto> columns = lineItem.getColumns();
      if (STLINHAS.equals(lineItem.getName())) {
        regimenMap.put("1stLinhasCommunity", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_COMMUNITY)));
        regimenMap.put("1stLinhasPatients", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_PATIENT)));
      } else if (NEW_COLUMN_0.equals(lineItem.getName())) {
        regimenMap.put("2ndLinhasCommunity", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_COMMUNITY)));
        regimenMap.put("2ndLinhasPatients", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_PATIENT)));
      } else if (NEW_COLUMN_1.equals(lineItem.getName())) {
        regimenMap.put("3rdLinhasCommunity", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_COMMUNITY)));
        regimenMap.put("3rdLinhasPatients", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_PATIENT)));
      } else if (TOTAL_COLUMN.equals(lineItem.getName())) {
        regimenMap.put("totalLinhasCommunity", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_COMMUNITY)));
        regimenMap.put("totalLinhasPatients", getRegimenColumnDtoValue(columns.get(COLUMN_NAME_PATIENT)));
      }
    });
    excelWriter.fill(regimenMap, writeSheet);
  }

  private static Integer getRegimenColumnDtoValue(RegimenColumnDto dto) {
    return dto == null ? 0 : dto.getValue();
  }

  private List<TarvRegimen> buildRegimenList(SiglusRequisitionDto requisition) {
    List<RegimenLineDto> regimenLineItems = requisition.getRegimenLineItems();

    return regimenLineItems.stream()
        .filter(lineItem -> lineItem.getRegimen() != null)
        .map(TarvRegimen::from)
        .sorted(Comparator.comparing(TarvRegimen::getCategory))
        .collect(Collectors.toList());
  }

  private void fillProductListContent(SiglusRequisitionDto requisition, ExcelWriter excelWriter,
      WriteSheet writeSheet) {
    FillConfig fillConfig = FillConfig.builder().forceNewRow(Boolean.TRUE).build();
    excelWriter.fill(new FillWrapper("product", buildProductList(requisition)), fillConfig, writeSheet);
  }

  private List<TarvProduct> buildProductList(SiglusRequisitionDto requisition) {
    List<BaseRequisitionLineItemDto> lineItems = requisition.getLineItems();
    Set<UUID> orderableIds = lineItems.stream().map(lineItem -> lineItem.getOrderableIdentity().getId())
        .collect(Collectors.toSet());
    Map<UUID, String> orderableIdToCategoryMap = buildOrderableIdToCategoryMap(lineItems);
    Map<UUID, Orderable> orderableIdToOrderableMap = orderableRepository.findLatestByIds(orderableIds).stream()
        .collect(Collectors.toMap(Orderable::getId, Function.identity()));
    Map<UUID, String> orderableIdToUnitMap = programOrderablesExtensionRepository
        .findAllByProgramCode(ProgramConstants.TARV_PROGRAM_CODE).stream()
        .collect(Collectors.toMap(ProgramOrderablesExtension::getOrderableId,
            extension -> extension.getUnit() == null ? "" : extension.getUnit(), (a, b) -> a));

    List<TarvProduct> tarvProducts = lineItems.stream()
        .map(lineItem ->
            TarvProduct.from(orderableIdToCategoryMap, orderableIdToOrderableMap, orderableIdToUnitMap, lineItem))
        .sorted(Comparator.comparing(TarvProduct::getCategory))
        .collect(Collectors.toList());
    return setProductsLineIndex(tarvProducts);
  }

  private Map<UUID, String> buildOrderableIdToCategoryMap(List<BaseRequisitionLineItemDto> lineItems) {
    Set<VersionEntityReference> versionEntityReferences = lineItems.stream().map(lineItemDto -> {
      VersionIdentityDto orderableIdentity = lineItemDto.getOrderableIdentity();
      return new VersionEntityReference(orderableIdentity.getId(),
          orderableIdentity.getVersionNumber());
    }).collect(Collectors.toSet());
    return orderableReferenceDataService.findByIdentities(versionEntityReferences).stream()
        .collect(Collectors.toMap(BasicOrderableDto::getId,
            orderableDto -> orderableDto.getPrograms().stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Orderable's program Not Found"))
                .getOrderableCategoryDisplayName()));
  }

  private List<TarvProduct> setProductsLineIndex(List<TarvProduct> tarvProducts) {
    List<TarvProduct> finalTarvProducts = new ArrayList<>();
    int lineIndex = 1;
    for (int arrayIndex = 0; arrayIndex < tarvProducts.size(); arrayIndex++) {
      TarvProduct tarvProduct = tarvProducts.get(arrayIndex);
      if (arrayIndex >= 1
          && !Objects.equals(tarvProduct.getCategory(), tarvProducts.get(arrayIndex - 1).getCategory())) {
        finalTarvProducts.add(new TarvProduct(lineIndex));
        lineIndex++;
        finalTarvProducts.add(new TarvProduct(lineIndex));
        lineIndex++;
      }
      tarvProduct.setIndex(lineIndex);
      finalTarvProducts.add(tarvProduct);
      lineIndex++;
    }
    return finalTarvProducts;
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

  @Data
  public static class TarvProduct {

    private Integer index;
    private String code;
    private String name;
    private String unit;
    private Integer initiate;
    private Integer receive;
    private Integer issue;
    private Integer adjust;
    private Integer inventory;
    private String expiredDate;
    private String category;
    private Integer requestedQuantity;
    private Integer authorizedQuantity;
    private Integer approvedQuantity;
    private Integer suggestedQuantity;

    public TarvProduct() {
    }

    public TarvProduct(Integer index) {
      this.index = index;
    }

    public static TarvProduct from(
        Map<UUID, String> orderableIdToCategoryMap,
        Map<UUID, Orderable> orderableIdToOrderableMap,
        Map<UUID, String> orderableIdToUnitMap,
        BaseRequisitionLineItemDto lineItem) {
      TarvProduct product = new TarvProduct();
      Orderable orderable = orderableIdToOrderableMap.get(lineItem.getOrderableIdentity().getId());
      product.setCode(orderable.getProductCode().toString());
      product.setName(orderable.getFullProductName());
      product.setUnit(orderableIdToUnitMap.get(orderable.getId()));
      product.setInitiate(lineItem.getBeginningBalance());
      product.setIssue(lineItem.getTotalConsumedQuantity());
      product.setReceive(lineItem.getTotalReceivedQuantity());
      product.setAdjust(lineItem.getTotalLossesAndAdjustments());
      product.setInventory(lineItem.getStockOnHand());
      LocalDate expirationDate = lineItem.getExpirationDate();
      product.setExpiredDate(
          expirationDate == null ? "" : expirationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      product.setCategory(orderableIdToCategoryMap.get(orderable.getId()));
      product.setRequestedQuantity(getQuantity(lineItem.getRequestedQuantity()));
      product.setAuthorizedQuantity(getQuantity(lineItem.getAuthorizedQuantity()));
      product.setApprovedQuantity(getQuantity(lineItem.getApprovedQuantity()));
      product.setSuggestedQuantity(getQuantity(lineItem.getSuggestedQuantity()));
      return product;
    }

    private static int getQuantity(Integer value) {
      return ObjectUtils.isEmpty(value) ? 0 : value;
    }
  }

  @Data
  public static class TarvRegimen {

    private Integer index;
    private String name;
    private Integer patientsNumber;
    private Integer communityNumber;
    private String category;

    public static TarvRegimen from(RegimenLineDto lineItem) {
      TarvRegimen tarvRegimen = new TarvRegimen();
      tarvRegimen.setName(lineItem.getRegimen().getFullProductName());
      Map<String, RegimenColumnDto> columns = lineItem.getColumns();
      tarvRegimen.setPatientsNumber(getRegimenColumnDtoValue(columns.get("patients")));
      tarvRegimen.setCommunityNumber(getRegimenColumnDtoValue(columns.get("community")));
      tarvRegimen.setCategory(lineItem.getRegimen().getRegimenCategory().getName());
      return tarvRegimen;
    }
  }
}
