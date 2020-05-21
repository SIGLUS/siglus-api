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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.GeographicZoneDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.GeographicZoneDtoDataBuilder;

public class GeographicZoneReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<GeographicZoneDto> {

  private GeographicZoneReferenceDataService service;

  @Override
  protected GeographicZoneDto generateInstance() {
    return new GeographicZoneDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<GeographicZoneDto> getService() {
    return new GeographicZoneReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (GeographicZoneReferenceDataService) prepareService();
  }

  @Test
  public void shouldSearch() {
    // given
    Integer levelNumber = new Random().nextInt(100);
    UUID parent = UUID.randomUUID();

    Map<String, Object> expectedBody = new HashMap<>();
    expectedBody.put("levelNumber", levelNumber);
    expectedBody.put("parent", parent);

    // when
    GeographicZoneDto geographicZoneDto = mockPageResponseEntityAndGetDto();
    Collection<GeographicZoneDto> result = service.search(levelNumber, parent);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(geographicZoneDto));

    verifyPageRequest()
        .isPostRequest()
        .hasAuthHeader()
        .hasBody(expectedBody);
  }
}
