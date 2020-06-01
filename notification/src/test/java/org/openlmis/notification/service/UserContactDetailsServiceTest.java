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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.notification.domain.EmailVerificationToken;
import org.openlmis.notification.domain.UserContactDetails;
import org.openlmis.notification.repository.EmailVerificationTokenRepository;
import org.openlmis.notification.repository.UserContactDetailsRepository;
import org.openlmis.notification.testutils.EmailVerificationTokenDataBuilder;
import org.openlmis.notification.util.EmailDetailsDataBuilder;
import org.openlmis.notification.util.UserContactDetailsDataBuilder;
import org.springframework.context.i18n.LocaleContextHolder;

@RunWith(MockitoJUnitRunner.class)
public class UserContactDetailsServiceTest {

  @Mock
  private UserContactDetailsRepository repository;

  @Mock
  private EmailVerificationNotifier notifier;

  @Mock
  private EmailVerificationTokenRepository verificationRepository;

  @InjectMocks
  private UserContactDetailsService service;

  private UserContactDetails contactDetails = new UserContactDetailsDataBuilder()
      .withEmailDetails(new EmailDetailsDataBuilder().withVerified(true).build())
      .withAllowNotify(true)
      .build();

  @Before
  public void setUp() {
    when(repository.save(any(UserContactDetails.class))).thenReturn(contactDetails);
  }

  @Test
  public void shouldAddContactDetails() {
    when(repository.exists(contactDetails.getId())).thenReturn(false);

    UserContactDetails saved = service.addOrUpdate(contactDetails);

    verify(repository).save(contactDetails);
    verify(notifier)
        .sendNotification(contactDetails, contactDetails.getEmailAddress(),
            LocaleContextHolder.getLocale());

    assertThat(saved.isAllowNotify(), is(false));
    assertThat(saved.isEmailAddressVerified(), is(false));
  }

  @Test
  public void shouldNotSendVerificationIfNewContactDetailsDoesNotHaveEmailAddress() {
    when(repository.exists(contactDetails.getId())).thenReturn(false);

    contactDetails.setEmailDetails(null);
    service.addOrUpdate(contactDetails);

    verify(repository).save(contactDetails);
    verifyZeroInteractions(notifier);
  }

  // it also check that verification email will not be sent because
  // email address was not changed
  @Test
  public void shouldUpdateContactDetails() {
    prepareForUpdate();

    contactDetails.setAllowNotify(false);

    UserContactDetails saved = service.addOrUpdate(contactDetails);
    assertThat(saved.isAllowNotify(), is(false));

    verify(repository).save(contactDetails);
    verifyZeroInteractions(notifier, verificationRepository);
  }

  @Test
  public void shouldSendNotificationIfEmailWasChanged() {
    UserContactDetails existing = prepareForUpdate();
    String email = "1" + existing.getEmailAddress();
    contactDetails.getEmailDetails().setEmail(email);

    service.addOrUpdate(contactDetails);
    verify(notifier).sendNotification(contactDetails, email, LocaleContextHolder.getLocale());
  }

  @Test
  public void shouldSendVerificationIfEmailWasChangedAgain() {
    prepareForUpdate();
    EmailVerificationToken token = new EmailVerificationTokenDataBuilder()
        .withContactDetails(contactDetails)
        .build();

    when(verificationRepository.findOneByUserContactDetails(contactDetails))
        .thenReturn(token);

    String email = "1" + token.getEmailAddress();
    contactDetails.getEmailDetails().setEmail(email);

    service.addOrUpdate(contactDetails);
    verify(notifier).sendNotification(contactDetails, email, LocaleContextHolder.getLocale());
  }

  @Test
  public void shouldSendVerificationIfThereIsNoToken() {
    UserContactDetails existing = prepareForUpdate();

    when(verificationRepository.findOneByUserContactDetails(contactDetails))
        .thenReturn(null);

    String email = "1" + existing.getEmailAddress();
    contactDetails.getEmailDetails().setEmail(email);

    service.addOrUpdate(contactDetails);
    verify(notifier).sendNotification(contactDetails, email, LocaleContextHolder.getLocale());
  }

  @Test
  public void shouldNotSendVerificationIfTokenExistsAndEmailsAreSame() {
    prepareForUpdate();
    EmailVerificationToken token = new EmailVerificationTokenDataBuilder()
        .withContactDetails(contactDetails)
        .build();

    when(verificationRepository.findOneByUserContactDetails(contactDetails))
        .thenReturn(token);

    contactDetails.getEmailDetails().setEmail(token.getEmailAddress());

    service.addOrUpdate(contactDetails);
    verifyZeroInteractions(notifier);
  }

  @Test
  public void shouldNotSendVerificationIfEmailWasSetToNull() {
    prepareForUpdate();
    EmailVerificationToken token = new EmailVerificationTokenDataBuilder()
        .withContactDetails(contactDetails)
        .build();

    when(verificationRepository.findOneByUserContactDetails(contactDetails))
        .thenReturn(token);

    contactDetails.setEmailDetails(null);

    service.addOrUpdate(contactDetails);
    verifyZeroInteractions(notifier);
  }

  private UserContactDetails prepareForUpdate() {
    UserContactDetails existing = new UserContactDetailsDataBuilder()
        .withReferenceDataUserId(contactDetails.getId())
        .withEmailDetails(new EmailDetailsDataBuilder()
            .withEmail(contactDetails.getEmailAddress())
            .withVerified(contactDetails.isEmailAddressVerified())
            .build())
        .build();

    when(repository.exists(contactDetails.getId())).thenReturn(true);
    when(repository.findOne(contactDetails.getId())).thenReturn(existing);

    return existing;
  }

}
