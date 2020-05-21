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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.SupplyLineDtoDataBuilder;

public class SupplyLineReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<SupplyLineDto> {

  private SupplyLineReferenceDataService service;

  @Override
  protected SupplyLineDto generateInstance() {
    return new SupplyLineDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<SupplyLineDto> getService() {
    return new SupplyLineReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (SupplyLineReferenceDataService) prepareService();
  }

  @Test
  public void shouldSearchSupplyLinesByProgramIdAndSupervisoryNodeId() {
    UUID programId = UUID.randomUUID();
    UUID supervisoryNodeId = UUID.randomUUID();

    SupplyLineDto dto = new SupplyLineDtoDataBuilder().buildAsDto();
    mockPageResponseEntity(dto);
    List<SupplyLineDto> result = service.search(programId, supervisoryNodeId);

    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("programId", programId)
        .hasQueryParameter("supervisoryNodeId", supervisoryNodeId)
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl());
  }

  @Test
  public void shouldSearchSupplyLinesBySupplyingFacilityIdAndProgramId() {
    UUID programId = UUID.randomUUID();
    UUID supplyingFacilityId = UUID.randomUUID();
    Set<UUID> supplyingFacilitiesIds = new HashSet<>();
    supplyingFacilitiesIds.add(supplyingFacilityId);

    SupplyLineDto dto = new SupplyLineDtoDataBuilder().buildAsDto();
    mockPageResponseEntity(dto);

    List<SupplyLineDto> result = service.search(supplyingFacilitiesIds, programId);

    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("supplyingFacilityId", supplyingFacilityId)
        .hasQueryParameter("programId", programId)
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl());
  }

  @Test
  public void shouldSearchSupplyLinesBySupplyingFacilityId() {
    UUID supplyingFacilityId = UUID.randomUUID();
    Set<UUID> supplyingFacilitiesIds = new HashSet<>();
    supplyingFacilitiesIds.add(supplyingFacilityId);

    SupplyLineDto dto = new SupplyLineDtoDataBuilder().buildAsDto();
    mockPageResponseEntity(dto);

    List<SupplyLineDto> result = service.search(supplyingFacilitiesIds, null);

    assertThat(result, hasSize(1));
    assertTrue(result.contains(dto));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .hasQueryParameter("supplyingFacilityId", supplyingFacilityId)
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl());
  }
}
