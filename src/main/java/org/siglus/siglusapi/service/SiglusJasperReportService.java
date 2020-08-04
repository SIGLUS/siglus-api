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

import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_REPORT_ID_NOT_FOUND;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_CARDS_VIEW;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_CODE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.openlmis.stockmanagement.dto.referencedata.ProgramDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.service.JasperReportService;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.service.StockmanagementPermissionService;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

@Service
public class SiglusJasperReportService {

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

  @Autowired
  private JasperReportService jasperReportService;

  /**
   * Generate stock card report in PDF format.
   *
   * @param stockCardId stock card id
   * @param isProduct is by product
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
    return jasperReportService.getStockCardReportView(stockCardDto);
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

    return jasperReportService.getStockCardSummariesReportView(params);
  }

}
