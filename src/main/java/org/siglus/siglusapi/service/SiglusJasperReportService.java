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

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_GENERATE_REPORT_FAILED;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_IO;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_JASPER_FILE_CREATION;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_REPORT_ID_NOT_FOUND;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_CARDS_VIEW;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_NAME;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.exception.JasperReportViewException;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockmanagementPermissionService;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;

@Service
public class SiglusJasperReportService {

  private static final String CARD_REPORT_URL = "/jasperTemplates/stockCard.jrxml";
  private static final String CARD_SUMMARY_REPORT_URL = "/jasperTemplates/stockCardSummary.jrxml";

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  private SiglusStockCardService siglusStockCardService;

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private StockmanagementPermissionService permissionService;

  @Value("${dateFormat}")
  private String dateFormat;

  @Value("${dateTimeFormat}")
  private String dateTimeFormat;

  @Value("${groupingSeparator}")
  private String groupingSeparator;

  @Value("${groupingSize}")
  private String groupingSize;

  /**
   * Generate stock card report in PDF format.
   *
   * @param stockCardId stock card id
   * @return generated stock card report.
   */
  public ModelAndView getStockCardReportView(UUID stockCardId, Boolean isProduct) {
    StockCardDto stockCardDto;
    if (isProduct == null) {
      stockCardDto = siglusStockCardService.findStockCardById(stockCardId);
    } else {
      stockCardDto = siglusStockCardService.findStockCardByOrderable(stockCardId);
    }
    if (stockCardDto == null) {
      throw new ResourceNotFoundException(new Message(ERROR_REPORT_ID_NOT_FOUND));
    }

    Collections.reverse(stockCardDto.getLineItems());
    Map<String, Object> params = new HashMap<>();
    params.put("datasource", singletonList(stockCardDto));
    params.put("hasLot", stockCardDto.hasLot());
    params.put("dateFormat", dateFormat);
    params.put("decimalFormat", createDecimalFormat());

    return generateReport(CARD_REPORT_URL, params);
  }

  /**
   * Generate stock card summary report in PDF format.
   *
   * @param program  program id
   * @param facility facility id
   * @return generated stock card summary report.
   */
  public ModelAndView getStockCardSummariesReportView(
      UUID program, UUID facility) {
    List<StockCardDto> cards;
    if (ALL_PRODUCTS_PROGRAM_ID.equals(program)) {
      UUID userId = authenticationHelper.getCurrentUser().getId();
      Set<UUID> programIds = siglusStockCardSummariesService
          .getProgramIds(program, userId, STOCK_CARDS_VIEW, facility.toString());
      cards = stockCardSummariesService.findStockCards(programIds, facility);
    } else {
      permissionService.canViewStockCard(program, facility);
      cards = stockCardSummariesService.findStockCards(program, facility);
    }
    StockCardDto firstCard = cards.get(0);
    Map<String, Object> params = new HashMap<>();
    params.put("stockCardSummaries", cards);
    ProgramDto programDto = firstCard.getProgram();
    params.put("program", firstCard.getProgram());
    if (ALL_PRODUCTS_PROGRAM_ID.equals(program)) {
      programDto.setName(ALL_PRODUCTS_PROGRAM_NAME);
      programDto.setCode(ALL_PRODUCTS_PROGRAM_CODE);
    }
    params.put("facility", firstCard.getFacility());
    params.put("showLot", cards.stream().anyMatch(card -> card.getLotId() != null));
    params.put("dateFormat", dateFormat);
    params.put("dateTimeFormat", dateTimeFormat);
    params.put("decimalFormat", createDecimalFormat());

    return generateReport(CARD_SUMMARY_REPORT_URL, params);
  }

  private ModelAndView generateReport(String templateUrl, Map<String, Object> params) {
    JasperReportsPdfView view = createJasperReportsPdfView();
    view.setUrl(compileReportAndGetUrl(templateUrl));
    view.setApplicationContext(appContext);
    return new ModelAndView(view, params);
  }

  private String compileReportAndGetUrl(String templateUrl) {
    try (InputStream inputStream = getClass().getResourceAsStream(templateUrl)) {
      JasperReport report = JasperCompileManager.compileReport(inputStream);

      return saveAndGetUrl(report, "report_temp");
    } catch (IOException ex) {
      throw new JasperReportViewException(new Message((ERROR_IO), ex.getMessage()), ex);
    } catch (JRException ex) {
      throw new JasperReportViewException(new Message(ERROR_GENERATE_REPORT_FAILED), ex);
    }
  }

  private String saveAndGetUrl(JasperReport report, String templateName) throws IOException {
    File reportTempFile;
    try {
      reportTempFile = createTempFile(templateName, ".jasper");
    } catch (IOException ex) {
      throw new JasperReportViewException(ERROR_JASPER_FILE_CREATION, ex);
    }

    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(bos)) {

      out.writeObject(report);
      writeByteArrayToFile(reportTempFile, bos.toByteArray());

      return reportTempFile.toURI().toURL().toString();
    }
  }


  private DecimalFormat createDecimalFormat() {
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    decimalFormatSymbols.setGroupingSeparator(groupingSeparator.charAt(0));
    DecimalFormat decimalFormat = new DecimalFormat("", decimalFormatSymbols);
    decimalFormat.setGroupingSize(Integer.valueOf(groupingSize));
    return decimalFormat;
  }

  private JasperReportsPdfView createJasperReportsPdfView() {
    return new JasperReportsPdfView();
  }
}
