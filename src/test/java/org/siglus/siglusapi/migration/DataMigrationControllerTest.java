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

package org.siglus.siglusapi.migration;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.migration.DataMigrationController;
import org.siglus.siglusapi.migration.DataMigrationService;

@RunWith(MockitoJUnitRunner.class)
public class DataMigrationControllerTest {

  @Mock
  private DataMigrationService dataMigrationService;
  @InjectMocks
  private DataMigrationController dataMigrationController;

  @Test
  public void shouldCallMigrationServiceWhenCreateStockCards() {
    // when
    dataMigrationController.createStockCards("facility-id", emptyList());

    // then
    verify(dataMigrationService)
        .createStockCards(anyString(), anyListOf(StockCardCreateRequest.class));
  }

  @Test
  public void shouldCallMigrationServiceWhenCreateCmms() {
    // when
    dataMigrationController.createOrUpdateCmms("facility-id", emptyList());

    // then
    verify(dataMigrationService)
        .createOrUpdateCmms(anyString(), anyListOf(HfCmmDto.class));
  }
}
