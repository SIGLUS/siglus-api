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

package org.openlmis.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_SEND_MAIL_FAILURE;

import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

public class EmailSenderTest {

  private static final String FROM = "noreply@test.org";
  private static final String TO = "user@send.to.me.com";
  private static final String SUBJECT = "Very important message";
  private static final String BODY = "I was kidding";

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private JavaMailSender mailSender;

  @InjectMocks
  private EmailSender sender;

  @Captor
  private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(sender, "from", FROM);

    given(mailSender.createMimeMessage()).willReturn(new MimeMessage((Session) null));
  }

  @Test
  public void shouldSendMessage() throws MessagingException, IOException {
    // when
    sender.sendMail(TO, SUBJECT, BODY);

    // then
    verify(mailSender).send(mimeMessageCaptor.capture());

    MimeMessage value = mimeMessageCaptor.getValue();
    assertThat(value.getFrom()[0].toString()).isEqualTo(FROM);
    assertThat(value.getAllRecipients()[0].toString()).isEqualTo(TO);
    assertThat(value.getSubject()).isEqualTo(SUBJECT);
    assertThat(value.getContent().toString()).isEqualTo(BODY);
  }

  @Test
  public void shouldThrowExceptionIfMailCanNotBeSend() {
    // given
    exception.expect(ServerException.class);
    exception.expectMessage(ERROR_SEND_MAIL_FAILURE);

    willThrow(new MailSendException("test-exception"))
        .given(mailSender)
        .send(any(MimeMessage.class));

    // when
    sender.sendMail(TO, SUBJECT, BODY);

    // then
    // the exception should be thrown
  }

}
