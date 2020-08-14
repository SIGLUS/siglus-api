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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
public class SiglusOrderableControllerTest {

  @InjectMocks
  private SiglusOrderableController siglusOrderableController;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private MultiValueMap<String, Object> queryParams;

  @Mock
  private Pageable pageable;

  private UUID programId = UUID.randomUUID();

  @Test
  public void shouldCallSiglusOrderableServiceWhenSearchOrderables() {
    when(authenticationHelper.getCurrentUser()).thenReturn(new UserDto());

    siglusOrderableController.searchOrderables(queryParams, pageable);

    verify(siglusOrderableService).searchOrderables(any(), any(), any());
  }

  @Test
  public void shouldCallAdditionalToAdd() {
    // when
    siglusOrderableController.searchOrderables(programId, queryParams, pageable);

    // then
    verify(siglusOrderableService).additionalToAdd(any(), any(), any());
  }
}
