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

package org.siglus.siglusapi.web.android;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.android.response.RegimenResponse;
import org.siglus.siglusapi.service.android.SiglusRegimenService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusRegimenControllerTest {

  @InjectMocks
  private SiglusRegimenController controller;

  @Mock
  private SiglusRegimenService service;

  @Mock
  private List<RegimenResponse> regimens;

  @Before
  public void setup() {
    when(service.getRegimens()).thenReturn(regimens);
  }

  @Test
  public void shouldCallServiceWhenGetRegimens() {
    // when
    List<RegimenResponse> result = controller.getRegimens();

    // then
    assertSame(regimens, result);
    verify(service).getRegimens();
  }

}
