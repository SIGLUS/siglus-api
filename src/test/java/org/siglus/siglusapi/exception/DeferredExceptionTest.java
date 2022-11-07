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

package org.siglus.siglusapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DeferredExceptionTest {
  @Test
  public void shouldNotThrowWhenEmitGivenExceptionIsEmpty() {
    // given
    DeferredException deferredException = new DeferredException();
    // when
    deferredException.emit();
  }

  @Test(expected = DeferredException.class)
  public void shouldThrowWhenEmitGivenExceptionIsNotEmpty() {
    // given
    DeferredException deferredException = new DeferredException();
    deferredException.add("", new RuntimeException());
    // when
    deferredException.emit();
  }

  @Test
  public void errorMessageShouldContainAllExceptionWhenGetMessage() {
    // given
    DeferredException deferredException = new DeferredException();
    deferredException.add("ctx1", new RuntimeException("err1"));
    deferredException.add("ctx1", new RuntimeException("err2"));
    deferredException.add("ctx2", new RuntimeException("err3"));
    // when
    String message = deferredException.getMessage();
    // then
    assertThat(message).contains("ctx1", "ctx2", "err1", "err2", "err3");
  }
}
