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

package org.siglus.siglusapi.localmachine.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;

public class PayloadSerializerTest {
  @Test
  public void canLoadPayloadSuccessfullyGivenDumpedBytes() {
    TestPayload payload = new TestPayload("id");
    PayloadSerializer payloadSerializer = new PayloadSerializer();
    byte[] payloadBytes = payloadSerializer.dump(payload);

    Object payloadLoaded = payloadSerializer.load(payloadBytes);

    assertThat(payloadLoaded).isEqualTo(payload);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class TestPayload {
    private String id;
  }
}
