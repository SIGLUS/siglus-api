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

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.fc.FcScheduleService;

@RunWith(MockitoJUnitRunner.class)
public class FcIntegrationTriggerControllerTest {

  @InjectMocks
  private FcIntegrationTriggerController fcIntegrationTriggerController;

  @Mock
  private FcScheduleService fcScheduleService;

  @Test
  public void shouldFetchCmmsFromFc() {
    // given
    String date = "20200907";

    // when
    fcIntegrationTriggerController.syncCmmFromFc(date);

    // then
    verify(fcScheduleService).syncCmmFromFc(date);
  }

  @Test
  public void fetchCpsFromFc() {
    // given
    String date = "20200908";

    // when
    fcIntegrationTriggerController.syncCpFromFc(date);

    // then
    verify(fcScheduleService).syncCpFromFc(date);
  }

  @Test
  public void shouldFetchProductsFromFc() {
    // given
    String date = "20200910";

    // when
    fcIntegrationTriggerController.syncProductFromFc(date);

    // then
    verify(fcScheduleService).syncProductFromFc(date);
  }

  @Test
  public void processingReceiptPlans() {
    // given
    String date = "20200910";

    // when
    fcIntegrationTriggerController.syncReceiptPlanFromFc(date);

    // then
    verify(fcScheduleService).syncReceiptPlanFromFc(date);
  }

  @Test
  public void fetchProgramsFromFc() {
    // given
    String date = "20200101";

    // when
    fcIntegrationTriggerController.syncProgramFromFc(date);

    // then
    verify(fcScheduleService).syncProgramFromFc(date);
  }

  @Test
  public void fetchRegimensFromFc() {
    // given
    String date = "20200101";

    // when
    fcIntegrationTriggerController.syncRegimenFromFc(date);

    // then
    verify(fcScheduleService).syncRegimenFromFc(date);
  }

}
