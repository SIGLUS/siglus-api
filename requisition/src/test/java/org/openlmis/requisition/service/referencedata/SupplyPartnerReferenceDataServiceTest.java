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

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.SupplyPartnerDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.SupplyPartnerDtoDataBuilder;

public class SupplyPartnerReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<SupplyPartnerDto> {

  private SupplyPartnerReferenceDataService service;

  @Override
  protected SupplyPartnerDto generateInstance() {
    return new SupplyPartnerDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<SupplyPartnerDto> getService() {
    return new SupplyPartnerReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (SupplyPartnerReferenceDataService) prepareService();
  }

  @Test
  public void shouldFindSupplyPartnerBySupervisoryNodeId() {
    // given
    UUID supervisoryNodeId1 = UUID.randomUUID();
    UUID supervisoryNodeId2 = UUID.randomUUID();
    UUID supervisoryNodeId3 = UUID.randomUUID();

    Set<UUID> supervisoryNodeIds = Sets
        .newHashSet(supervisoryNodeId1, supervisoryNodeId2, supervisoryNodeId3);

    // when
    SupplyPartnerDto dto = mockPageResponseEntityAndGetDto();
    List<SupplyPartnerDto> result = service.search(supervisoryNodeIds);

    // then
    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("supervisoryNodeId", supervisoryNodeId1)
        .hasQueryParameter("supervisoryNodeId", supervisoryNodeId2)
        .hasQueryParameter("supervisoryNodeId", supervisoryNodeId3);
  }

}
