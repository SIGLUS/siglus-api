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

package org.openlmis.requisition.service.referencedata;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.IdealStockAmountDtoDataBuilder;

public class IdealStockAmountReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<IdealStockAmountDto> {

  private IdealStockAmountReferenceDataService service;

  @Override
  protected IdealStockAmountDto generateInstance() {
    return new IdealStockAmountDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<IdealStockAmountDto> getService() {
    return new IdealStockAmountReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (IdealStockAmountReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindIdealStockAmountBasedOnParameters() {
    // given
    UUID facilityId = UUID.randomUUID();
    UUID processingPeriodId = UUID.randomUUID();
    IdealStockAmountDto isa = new IdealStockAmountDtoDataBuilder().buildAsDto();

    // when
    mockPageResponseEntity(isa);
    List<IdealStockAmountDto> result = service.search(facilityId, processingPeriodId);

    // then
    assertThat(result, hasSize(1));
    assertThat(result, hasItem(isa));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl())
        .hasQueryParameter("facilityId", facilityId)
        .hasQueryParameter("processingPeriodId", processingPeriodId);
  }
}
