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
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;

public class FacilityTypeApprovedProductReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<ApprovedProductDto> {

  private FacilityTypeApprovedProductReferenceDataService service;

  @Override
  protected BaseReferenceDataService<ApprovedProductDto> getService() {
    return new FacilityTypeApprovedProductReferenceDataService();
  }

  @Override
  protected ApprovedProductDto generateInstance() {
    return new ApprovedProductDtoDataBuilder().buildAsDto();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();

    service = (FacilityTypeApprovedProductReferenceDataService) prepareService();
  }

  @Test
  public void shouldReturnApprovedProductsByIdentity() {
    // given
    VersionEntityReference reference = new VersionEntityReference(UUID.randomUUID(), 1L);

    FacilityTypeApprovedProductSearchParams searchParams =
        new FacilityTypeApprovedProductSearchParams(null, null, null,
            Lists.newArrayList(new VersionIdentityDto(reference)), 0, 1);

    // when
    ApprovedProductDto product = mockPageResponseEntityAndGetDto();
    List<ApprovedProductDto> response = service.findByIdentities(Collections.singleton(reference));

    // then
    assertThat(response, hasSize(1));
    assertThat(response, hasItem(product));

    verifyPageRequest()
        .isPostRequest()
        .hasAuthHeader()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl())
        .hasBody(searchParams);
  }

  @Test
  public void shouldReturnEmptyListIfEmptyParamProvided() {
    // given
    disableAuthCheck();

    // when
    List<ApprovedProductDto> response = service.findByIdentities(Collections.emptySet());

    // then
    assertTrue(response.isEmpty());
  }
}
