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

package org.siglus.siglusapi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.dto.Message.createFromMessageKeyStr;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.siglus.siglusapi.dto.Message;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Message.class)
@SuppressWarnings("PMD.TooManyMethods")
public class MessageTest {

  @Test(expected = NullPointerException.class)
  public void messageShouldRequireNonNullKey() {
    new Message(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void messageShouldRequireNonEmptyKey() {
    new Message(" ");
  }

  @Test
  public void toStringShouldHandleObjects() {
    String key = "key.something";
    Date today = new Date();
    Message message = new Message(key, "a", today);

    // expected is:  "key.something: a, <date>"
    assertEquals(key + ": " + "a" + ", " + today, message.toString());
  }

  @Test
  public void equalsAndHashCodeShouldUseKey() {
    Message foo1 = new Message("foo");
    Message foo2 = new Message("foo");

    assertEquals(foo1, foo1);
    assertNotEquals(foo1, new Object());
    assertEquals(foo1, foo2);
    assertEquals(foo1.hashCode(), foo2.hashCode());
  }

  @Test
  public void equalsAndHashCodeShouldIgnoreSpace() {
    Message foo1 = new Message("Foo");
    Message foo2 = new Message(" Foo ");

    assertEquals(foo1, foo2);
    assertEquals(foo1.hashCode(), foo2.hashCode());
  }

  @Test
  public void shouldReturnNullWhenMessageKeyStrIsNull() {
    Message message = createFromMessageKeyStr(null);
    assertNull(message);
  }

  @Test
  public void shouldReturnNullWhenMessageKeyStrIsNullString() {
    Message message = createFromMessageKeyStr("null");
    assertNull(message);
  }

  @Test
  public void shouldReturnMessageKeyWhenMessageKeyStrIsNotNull() {
    Message message = createFromMessageKeyStr("{\"messageKey\": \"hello\"}");

    assert message != null;
    assertEquals("hello", message.getKey());
  }

  @Test
  public void shouldReturnMessageKeyStrWhenIoExceptionHappen() throws Exception {
    ObjectMapper objectMapper = mock(ObjectMapper.class);

    when(objectMapper.readValue(anyString(), eq(HashMap.class)))
        .thenThrow(IOException.class).getMock();
    PowerMockito.whenNew(ObjectMapper.class).withNoArguments().thenReturn(objectMapper);

    Message message = createFromMessageKeyStr("{\"messageKey\": \"hello\"}");

    assert message != null;
    assertEquals("{\"messageKey\": \"hello\"}", message.getKey());
  }

}