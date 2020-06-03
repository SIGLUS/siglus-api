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

package org.openlmis.fulfillment.web.validator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.FtpProtocol;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.i18n.ExposedMessageSource;
import org.openlmis.fulfillment.i18n.MessageService;
import org.openlmis.fulfillment.util.Message;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FtpTransferPropertiesValidatorTest {

  @Mock
  private ExposedMessageSource messageSource;

  @Spy
  private MessageService messageService;

  @InjectMocks
  private FtpTransferPropertiesValidator validator;

  private FtpTransferProperties properties;

  @Before
  public void setUp() throws IllegalAccessException {
    FieldUtils.writeField(messageService, "messageSource", messageSource, true);
    when(messageSource.getMessage(anyString(), any(), any())).thenReturn("message");
    properties = new FtpTransferProperties();
    properties.setProtocol(FtpProtocol.FTP);
    properties.setUsername("name");
    properties.setPassword("password");
    properties.setServerPort(80);
    properties.setLocalDirectory("/home");
    properties.setRemoteDirectory("/remote");
    properties.setServerHost("host");
    properties.setPassiveMode(true);
  }

  @Test
  public void shouldPassWhenPropertiesAreValid() {
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(0));
  }

  @Test
  public void shouldRejectWhenProtocolIsNull() {
    properties.setProtocol(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenUsernameIsBlank() {
    properties.setUsername("");
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenUsernameIsNull() {
    properties.setUsername(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenPasswordIsBlank() {
    properties.setPassword("");
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenPasswordIsNull() {
    properties.setPassword(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenHostIsBlank() {
    properties.setServerHost("");
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenHostIsNull() {
    properties.setServerHost(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenPortIsLessThanZero() {
    properties.setServerPort(-1);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenPortIsNull() {
    properties.setServerPort(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenRemoteDirectoryIsBlank() {
    properties.setRemoteDirectory("");
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenRemoteDirectoryIsNull() {
    properties.setRemoteDirectory(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenLocalDirectoryIsBlank() {
    properties.setLocalDirectory("");
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenLocalDirectoryIsNull() {
    properties.setLocalDirectory(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

  @Test
  public void shouldRejectWhenPassiveModeIsNull() {
    properties.setPassiveMode(null);
    List<Message.LocalizedMessage> validate = validator.validate(properties);

    assertThat(validate.size(), is(1));
  }

}