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

package org.siglus.siglusapi.localmachine.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.server.AgentInfoRepository;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class MachineTokenMatcherTest {
  @InjectMocks private MachineTokenMatcher machineTokenMatcher;
  @Mock private AgentInfoRepository agentInfoRepository;

  @Before
  public void setup() {
    given(agentInfoRepository.findOneByMachineIdAndFacilityId(any(), any())).willReturn(null);
  }

  @Test
  public void shouldReturnFalseGivenTokenHeaderIsEmpty() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(CommonConstants.VERSION, "1.0");
    request.removeHeader(CommonConstants.ACCESS_TOKEN);
    // when
    boolean matched = machineTokenMatcher.matches(request);
    assertThat(matched).isFalse();
  }
}
