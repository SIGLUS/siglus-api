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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.stockmanagement.StockCardDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.StockCardDtoDataBuilder;

public class StockCardStockManagementServiceTest
    extends BaseStockmanagementServiceTest<StockCardDto> {

  private StockCardStockManagementService service;

  @Override
  protected StockCardDto generateInstance() {
    return new StockCardDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<StockCardDto> getService() {
    return new StockCardStockManagementService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (StockCardStockManagementService) prepareService();
  }

  @Test
  public void shouldGetStockCardsForFacilityAndProgram() {
    // given
    UUID facility = UUID.randomUUID();
    UUID program = UUID.randomUUID();

    // when
    StockCardDto card = mockPageResponseEntityAndGetDto();
    List<StockCardDto> result = service.getStockCards(facility, program);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(card));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("facility", facility)
        .hasQueryParameter("program", program)
        .hasQueryParameter("size", Integer.MAX_VALUE);
  }
}
