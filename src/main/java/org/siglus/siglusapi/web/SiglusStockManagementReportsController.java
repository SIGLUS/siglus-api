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

package org.siglus.siglusapi.web;

import java.util.UUID;
import org.siglus.siglusapi.service.SiglusJasperReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/api/siglusapi")
public class SiglusStockManagementReportsController {

  @Autowired
  private SiglusJasperReportService reportService;

  /**
   * Get stock card report in PDF format.
   *
   * @param stockCardId stock card id.
   * @return generated PDF report
   */
  @GetMapping("/stockCards/{id}/print")
  public ModelAndView getStockCardByLot(
      @PathVariable("id") UUID stockCardId) {
    return reportService.getStockCardByLotReportView(stockCardId);
  }

  /**
   * Get stock card report in PDF format.
   *
   * @param orderableId stock card id.
   * @return generated PDF report
   */
  @GetMapping("/orderable/{id}/print")
  public ModelAndView getStockCardByOrderable(
      @PathVariable("id") UUID orderableId) {
    return reportService.getStockCardByOrderableReportView(orderableId);
  }

  /**
   * Get stock card summaries report by program and facility.
   *
   * @return generated PDF report
   */
  @GetMapping("/stockCardSummaries/print")
  public ModelAndView getStockCardSummaries(
      @RequestParam("program") UUID programId,
      @RequestParam("facility") UUID facilityId) {
    return reportService.getStockCardSummariesReportView(programId, facilityId);
  }
}

