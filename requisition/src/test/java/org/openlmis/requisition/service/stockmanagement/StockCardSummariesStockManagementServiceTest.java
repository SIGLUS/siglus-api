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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.stockmanagement.StockCardSummaryDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.StockCardSummaryDtoDataBuilder;

public class StockCardSummariesStockManagementServiceTest
    extends BaseStockmanagementServiceTest<StockCardSummaryDto> {

  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID orderableId = UUID.randomUUID();
  private LocalDate asOfDate = LocalDate.now();

  private StockCardSummariesStockManagementService service;

  @Override
  protected StockCardSummaryDto generateInstance() {
    return new StockCardSummaryDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<StockCardSummaryDto> getService() {
    return new StockCardSummariesStockManagementService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (StockCardSummariesStockManagementService) prepareService();
  }

  @Test
  public void shouldFindStockCardSummaries() {
    // when
    StockCardSummaryDto stockCardSummaryDto = mockPageResponseEntityAndGetDto();
    List<StockCardSummaryDto> actual =
        service.search(programId, facilityId, Collections.singleton(orderableId), asOfDate);

    // then
    assertThat(actual, hasSize(1));
    assertThat(actual, contains(stockCardSummaryDto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl())
        .hasQueryParameter("programId", programId)
        .hasQueryParameter("facilityId", facilityId)
        .hasQueryParameter("orderableId", orderableId)
        .hasQueryParameter("asOfDate", asOfDate);
  }

}
