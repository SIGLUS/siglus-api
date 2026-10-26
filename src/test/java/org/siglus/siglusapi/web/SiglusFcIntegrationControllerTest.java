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

import java.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.service.SiglusFcIntegrationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusFcIntegrationControllerTest {

  @InjectMocks
  private SiglusFcIntegrationController siglusFcIntegrationController;

  @Mock
  private SiglusFcIntegrationService siglusFcIntegrationService;

  private Pageable pageable = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
      PaginationConstants.NO_PAGINATION);

  private final LocalDate date = LocalDate.of(2020, 2, 21);

  @Test
  public void shouldSearchRequisitionsWithDefaultPageable() {
    // when
    siglusFcIntegrationController.searchRequisitions(date, pageable);

    // then
    verify(siglusFcIntegrationService).searchRequisitions(date,
        new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER, 20));
  }

  @Test
  public void shouldSearchRequisitionsWithInputPageable() {
    // given
    pageable = new PageRequest(1, 10);

    // when
    siglusFcIntegrationController.searchRequisitions(date, pageable);

    // then
    verify(siglusFcIntegrationService).searchRequisitions(date, pageable);
  }

  @Test
  public void shouldOnlySearchNeedApprovalRequisition() {
    // given
    pageable = new PageRequest(1, 20);

    // when
    siglusFcIntegrationController.searchNeedApprovalRequisitions(date, pageable);

    // then
    verify(siglusFcIntegrationService).searchNeedApprovalRequisitions(date, pageable);

  }
}
