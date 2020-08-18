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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;

public abstract class StockOnHandRetrieverBuilder {

  @Setter(AccessLevel.PACKAGE)
  @Getter(AccessLevel.PACKAGE)
  private StockCardSummariesStockManagementService stockCardSummariesService;

  public abstract StockOnHandRetrieverBuilder forProducts(ApproveProductsAggregator products);

  public abstract StockOnHandRetrieverBuilder forProgram(UUID programId);

  public abstract StockOnHandRetrieverBuilder forFacility(UUID facilityId);

  public abstract StockOnHandRetrieverBuilder asOfDate(LocalDate date);

  public abstract StockOnHandRetriever build();

}
