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

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.security.KeyPair;
import java.util.UUID;
import org.junit.Test;

public class MachineTokenTest {
  private static final KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
  private static final KeyPair invalidKeyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);

  @Test
  public void canParseTokenGivenSignedToken() {
    // given
    MachineToken token = getToken();
    // when
    MachineToken parsedToken = MachineToken.parse(token.getPayload());
    // then
    assertThat(parsedToken.getAgentId()).isEqualTo(token.getAgentId());
    assertThat(parsedToken.getFacilityId()).isEqualTo(token.getFacilityId());
  }

  @Test
  public void canVerifySuccessfullyGivenSignedToken() {
    // given
    MachineToken token = getToken();
    MachineToken parsedToken = MachineToken.parse(token.getPayload());
    // when
    boolean passed = parsedToken.verify(keyPair.getPublic().getEncoded());
    // then
    assertThat(passed).isTrue();
  }

  @Test(expected = SignatureException.class)
  public void shouldThrowWhenVerifyGivenNotMatchedPubkey() {
    // given
    MachineToken token = getToken();
    MachineToken parsedToken = MachineToken.parse(token.getPayload());
    // when
    parsedToken.verify(invalidKeyPair.getPublic().getEncoded());
  }

  private MachineToken getToken() {
    UUID agentId = UUID.randomUUID();
    UUID facilityId = UUID.randomUUID();
    return MachineToken.sign(agentId, facilityId, keyPair.getPrivate().getEncoded());
  }
}
