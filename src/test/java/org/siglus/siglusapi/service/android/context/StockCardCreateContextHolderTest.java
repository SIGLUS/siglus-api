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

package org.siglus.siglusapi.service.android.context;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.siglusapi.migration.AdditionalOrderable;
import org.siglus.siglusapi.migration.MasterDataRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class StockCardCreateContextHolderTest {

  private ProgramDto program;
  private UUID homeFacilityId;
  private AdditionalOrderable additionalOrderable;

  @InjectMocks
  private StockCardCreateContextHolder stockCardCreateContextHolder;
  @Mock
  private SiglusAuthenticationHelper siglusAuthenticationHelper;
  @Mock
  private MasterDataRepository masterDataRepository;
  @Mock
  private RequisitionService requisitionService;

  private ApprovedProductDto actualApprovedOrderable;

  @Before
  public void setup() {
    program = new ProgramDto();
    program.setId(UUID.randomUUID());
    homeFacilityId = UUID.randomUUID();
    additionalOrderable = AdditionalOrderable.builder().orderableId(UUID.randomUUID())
        .tradeItemId(UUID.randomUUID()).build();
    given(masterDataRepository.findAdditionalOrderableByProgram(any(UUID.class)))
        .willReturn(singletonList(
            additionalOrderable));
    actualApprovedOrderable = prepareMockedApprovedProduct(homeFacilityId, program);
  }

  @Test
  public void shouldReturnApprovedOrderablesPlusAdditionalOderablesWhenGetProgramProductsGivenMigrationAuthContext() {
    // given
    given(siglusAuthenticationHelper.isTheDataMigrationUser())
        .willReturn(true);

    // when
    List<OrderableDto> gotOrderables = stockCardCreateContextHolder.getProgramProducts(
        homeFacilityId, program);

    // then
    assertThat(gotOrderables).extracting(OrderableDto::getId)
        .containsExactlyInAnyOrder(actualApprovedOrderable.getId(), additionalOrderable.getOrderableId());
  }

  @Test
  public void shouldReturnActualApprovedOrderablesOnlyWhenGetProgramProductsGivenNonMigrationAuthContext() {
    // given
    given(siglusAuthenticationHelper.isTheDataMigrationUser())
        .willReturn(false);

    // when
    List<OrderableDto> gotOrderables = stockCardCreateContextHolder.getProgramProducts(
        homeFacilityId, program);

    // then
    assertThat(gotOrderables).extracting(OrderableDto::getId)
        .containsExactlyInAnyOrder(actualApprovedOrderable.getId());
  }

  private ApprovedProductDto prepareMockedApprovedProduct(UUID homeFacilityId, ProgramDto program) {
    ApprovedProductDto actualApprovedOrderable = new ApprovedProductDto();
    actualApprovedOrderable.setId(UUID.randomUUID());
    OrderableDto orderable = new OrderableDto();
    orderable.setId(actualApprovedOrderable.getId());
    actualApprovedOrderable.setOrderable(orderable);
    given(requisitionService.getApprovedProductsWithoutAdditional(homeFacilityId, program.getId()))
        .willReturn(singletonList(actualApprovedOrderable));
    return actualApprovedOrderable;
  }
}