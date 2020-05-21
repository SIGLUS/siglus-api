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
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.StockCardRangeSummaryDtoDataBuilder;

public class StockCardRangeSummaryStockManagementSerivceTest
    extends BaseStockmanagementServiceTest<StockCardRangeSummaryDto> {

  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID orderableId = UUID.randomUUID();
  private String tag = "tag";
  private LocalDate startDate = LocalDate.of(2017, 1, 1);
  private LocalDate endDate = LocalDate.now();

  private StockCardRangeSummaryStockManagementService service;

  @Override
  protected StockCardRangeSummaryDto generateInstance() {
    return new StockCardRangeSummaryDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<StockCardRangeSummaryDto> getService() {
    return new StockCardRangeSummaryStockManagementService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (StockCardRangeSummaryStockManagementService) prepareService();
  }

  @Test
  public void shouldFindStockCardSummaries() {
    // when
    StockCardRangeSummaryDto stockCardRangeSummaryDto = mockPageResponseEntityAndGetDto();
    List<StockCardRangeSummaryDto> actual = service.search(
        programId, facilityId, Collections.singleton(new VersionIdentityDto(orderableId, 1L)),
        tag, startDate, endDate);

    // then
    assertThat(actual, hasSize(1));
    assertThat(actual, contains(stockCardRangeSummaryDto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl())
        .hasQueryParameter("programId", programId)
        .hasQueryParameter("facilityId", facilityId)
        .hasQueryParameter("orderableId", orderableId)
        .hasQueryParameter("tag", tag)
        .hasQueryParameter("startDate", startDate)
        .hasQueryParameter("endDate", endDate);
  }
}
