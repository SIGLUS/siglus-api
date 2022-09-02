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
import static org.mockito.Mockito.mock;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.security.KeyPair;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.server.AgentInfo;
import org.siglus.siglusapi.localmachine.server.AgentInfoRepository;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

@RunWith(MockitoJUnitRunner.class)
public class LocalTokenInterceptorTest {
  private static final KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
  @InjectMocks private LocalTokenInterceptor interceptor;
  @Mock private AgentInfoRepository agentInfoRepository;

  @Test
  public void shouldAddHeadersWhenIntercept() throws IOException {
    // given
    given(agentInfoRepository.getFirstAgentInfo())
        .willReturn(
            AgentInfo.builder()
                .facilityId(UUID.randomUUID())
                .machineId(UUID.randomUUID())
                .privateKey(keyPair.getPrivate().getEncoded())
                .build());
    MockClientHttpRequest request = new MockClientHttpRequest();
    // when
    interceptor.intercept(request, new byte[0], mock(ClientHttpRequestExecution.class));
    // then
    assertThat(request.getHeaders()).containsKey(CommonConstants.VERSION);
    assertThat(request.getHeaders()).containsKey(CommonConstants.ACCESS_TOKEN);
  }
}
