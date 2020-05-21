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

package org.openlmis.requisition.settings.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationSettingServiceTest {

  private static final String RESONS_SUFFIX = "reasons.";
  private static final String CONSUMED = RESONS_SUFFIX + "consumed";
  private static final String RECEIPTS = RESONS_SUFFIX + "receipts";
  private static final String BEGINNING_BALANCE_EXCESS = RESONS_SUFFIX + "beginningBalanceExcess";
  private static final String BEGINNING_BALANCE_INSUFFICIENCY =
      RESONS_SUFFIX + "beginningBalanceInsufficiency";

  @Mock
  private Environment env;

  @InjectMocks
  private ConfigurationSettingService configurationSettingService;

  @Test
  public void shouldProperlyParseUuidsFromEnv() {
    final String consumed = "e3e512c9-5a40-4fbb-a598-d405bbed8bf0";
    final String receipts = "33de864f-56d8-4310-a288-3d1fbb33f18c";
    final String beginningBalanceExcess = "f448aabf-879c-411e-92b0-cdfa1096e67f";
    final String beginningBalanceInsufficiency = "1d4bc2f9-6454-4bda-b028-aac98b29d87d";

    doReturn(consumed).when(env).getProperty(CONSUMED);
    doReturn(receipts).when(env).getProperty(RECEIPTS);
    doReturn(beginningBalanceExcess).when(env).getProperty(BEGINNING_BALANCE_EXCESS);
    doReturn(beginningBalanceInsufficiency).when(env).getProperty(BEGINNING_BALANCE_INSUFFICIENCY);

    assertEquals(UUID.fromString(consumed), configurationSettingService.getReasonIdForConsumed());
    assertEquals(UUID.fromString(receipts), configurationSettingService.getReasonIdForReceipts());
    assertEquals(UUID.fromString(beginningBalanceExcess),
        configurationSettingService.getReasonIdForBeginningBalanceExcess());
    assertEquals(UUID.fromString(beginningBalanceInsufficiency),
        configurationSettingService.getReasonIdForBeginningBalanceInsufficiency());
  }


}
