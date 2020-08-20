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

import java.time.LocalDate;
import java.util.UUID;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;

final class StandardStockOnHandRetrieverBuilder extends StockOnHandRetrieverBuilder {
  private ApproveProductsAggregator products;
  private UUID programId;
  private UUID facilityId;
  private LocalDate asOfDate;

  @Override
  public StandardStockOnHandRetrieverBuilder forProducts(ApproveProductsAggregator products) {
    this.products = products;
    return this;
  }

  @Override
  public StandardStockOnHandRetrieverBuilder forProgram(UUID programId) {
    this.programId = programId;
    return this;
  }

  @Override
  public StandardStockOnHandRetrieverBuilder forFacility(UUID facilityId) {
    this.facilityId = facilityId;
    return this;
  }

  @Override
  public StandardStockOnHandRetrieverBuilder asOfDate(LocalDate date) {
    this.asOfDate = date;
    return this;
  }

  @Override
  public StandardStockOnHandRetriever build() {
    return new StandardStockOnHandRetriever(
        getStockCardSummariesService(), products, programId, facilityId, asOfDate
    );
  }
}
