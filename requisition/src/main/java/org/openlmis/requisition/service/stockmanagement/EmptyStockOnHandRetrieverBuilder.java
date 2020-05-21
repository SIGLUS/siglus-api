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

final class EmptyStockOnHandRetrieverBuilder extends StockOnHandRetrieverBuilder {
  private static final StockOnHandRetriever EMPTY = new EmptyStockOnHandRetriever();

  @Override
  public EmptyStockOnHandRetrieverBuilder forProducts(ApproveProductsAggregator products) {
    return this;
  }

  @Override
  public EmptyStockOnHandRetrieverBuilder forProgram(UUID programId) {
    return this;
  }

  @Override
  public EmptyStockOnHandRetrieverBuilder forFacility(UUID facilityId) {
    return this;
  }

  @Override
  public EmptyStockOnHandRetrieverBuilder asOfDate(LocalDate date) {
    return this;
  }

  @Override
  public StockOnHandRetriever build() {
    return EMPTY;
  }

}
