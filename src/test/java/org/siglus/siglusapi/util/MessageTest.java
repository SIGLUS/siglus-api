package org.siglus.siglusapi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.util.Message.createFromMessageKeyStr;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.siglus.siglusapi.util.Message.LocalizedMessage;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

@RunWith(PowerMockRunner.class)
@PrepareForTest( Message.class )
public class MessageTest {

  @Test(expected = NullPointerException.class)
  public void messageShouldRequireNonNullKey() {
    new Message(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void messageShouldRequireNonEmptyKey() {
    new Message(" ");
  }

  @Test(expected = NullPointerException.class)
  public void localMessageShouldRequireNonEmptyMessage() {
    MessageSource messageSource = mock(MessageSource.class);
    Locale locale = Locale.getDefault();
    Message message = new Message("hi");

    when(messageSource.getMessage("hi",  new Object[]{"arg1"}, locale)).thenReturn("");

    message.localMessage(messageSource, locale);
  }

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionWhenNotGetMessage() {
    MessageSource messageSource = mock(MessageSource.class);
    Locale locale = Locale.getDefault();
    Message message = new Message("hi");

    when(messageSource.getMessage("hi",  new Object[]{ }, locale)).thenReturn(null);

    message.localMessage(messageSource, locale);
  }

  @Test
  public void shouldReturnMessageWhenGetLocalMessageShouldReturnMessage() {
    MessageSource messageSource = mock(MessageSource.class);
    Locale locale = Locale.getDefault();
    Message message = new Message("hi", "arg1");

    when(messageSource.getMessage("hi",  new Object[]{"arg1"}, locale)).thenReturn("hello");

    LocalizedMessage localizedMessage = message.localMessage(messageSource, locale);
    assertEquals("hello", localizedMessage.getMessage());
  }

  @Test(expected = NoSuchMessageException.class)
  public void humanStringShouldThrowExceptionIfKeyNotFound() {
    MessageSource messageSource = mock(MessageSource.class);
    Locale locale = Locale.getDefault();

    String key = "foo.bar";
    String p1 = "some";
    String p2 = "stuff";
    Message message = new Message("foo.bar", "some", "stuff");

    when(messageSource.getMessage(key, new Object[]{p1, p2}, locale))
        .thenThrow(NoSuchMessageException.class);

    message.localMessage(messageSource, locale);
  }

  @Test
  public void toStringShouldHandleObjects() {
    String key = "key.something";
    Date today = new Date();
    Message message = new Message(key, "a", today);

    // expected is:  "key.something: a, <date>"
    assertEquals(key + ": " + "a" + ", " + today.toString(), message.toString());
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
  public void shouldREturnMessageKeyStrWhenIOExceptionHappen() throws Exception {
    ObjectMapper objectMapper = mock(ObjectMapper.class);

    when(objectMapper.readValue(anyString(), eq(HashMap.class)))
        .thenThrow(IOException.class).getMock();
    PowerMockito.whenNew(ObjectMapper.class).withNoArguments().thenReturn(objectMapper);

    Message message = createFromMessageKeyStr("{\"messageKey\": \"hello\"}");

    assert message != null;
    assertEquals("{\"messageKey\": \"hello\"}", message.getKey());
  }
  
}