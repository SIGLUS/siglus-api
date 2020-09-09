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

import java.text.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.service.SiglusFcIntegrationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusFcIntegrationControllerTest {

  @InjectMocks
  private SiglusFcIntegrationController siglusFcIntegrationController;

  @Mock
  private SiglusFcIntegrationService siglusFcIntegrationService;

  private Pageable pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
      Pagination.NO_PAGINATION);

  private String date = "20200221";

  @Test
  public void shouldSearchRequisitionsWithDefaultPageable() throws ParseException {
    // when
    siglusFcIntegrationController.searchRequisitions(date, pageable);

    // then
    verify(siglusFcIntegrationService).searchRequisitions(date,
        new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, 20));
  }

  @Test
  public void shouldSearchRequisitionsWithInputPageable() throws ParseException {
    // given
    pageable = new PageRequest(1, 10);

    // when
    siglusFcIntegrationController.searchRequisitions(date, pageable);

    // then
    verify(siglusFcIntegrationService).searchRequisitions(date, pageable);
  }
}
