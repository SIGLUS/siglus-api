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

package org.openlmis.requisition.service.stockmanagement;

import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.stockmanagement.service.StockCardSummariesService;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummariesV2DtoBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StockOnHandRetrieverBuilderFactory {

  // [SIGLUS change start]
  // [change reason]: call our modify stock card.
  // @Autowired
  // private StockCardSummariesStockManagementService stockCardSummariesStockManagementService;

  @Autowired
  private StockCardSummariesService stockCardSummariesService;

  @Autowired
  private StockCardSummariesV2DtoBuilder stockCardSummariesV2DtoBuilder;
  // [SIGLUS change end]

  /**
   * Creates new instance of {@link StockOnHandRetrieverBuilder} based on settings from the
   * {@link RequisitionTemplate}.
   */
  public final StockOnHandRetrieverBuilder getInstance(RequisitionTemplate template,
      String columnName) {
    StockOnHandRetrieverBuilder builder;

    if (template.isPopulateStockOnHandFromStockCards()) {
      if (template.isColumnInTemplate(columnName) && template.isColumnDisplayed(columnName)) {
        builder = new StandardStockOnHandRetrieverBuilder();
      } else {
        builder = new EmptyStockOnHandRetrieverBuilder();
      }

    } else {
      builder = new EmptyStockOnHandRetrieverBuilder();
    }

    // [SIGLUS change start]
    // [change reason]: call our modify stock card.
    // builder.setStockCardSummariesService(stockCardSummariesStockManagementService);
    builder.setStockCardSummariesService(stockCardSummariesService);
    builder.setStockCardSummariesV2DtoBuilder(stockCardSummariesV2DtoBuilder);
    // [SIGLUS change end]

    return builder;
  }

}
