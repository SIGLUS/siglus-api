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

package org.siglus.siglusapi.validator;

import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.enums.PhysicalInventorySubDraftEnum;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.StockManagementInitialDraftsRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class ActiveDraftValidatorTest {

  @InjectMocks
  ActiveDraftValidator activeDraftValidator;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private StockManagementInitialDraftsRepository stockManagementInitialDraftsRepository;

  private final UUID initialDraftId = UUID.randomUUID();

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityIdIsNull() {
    activeDraftValidator.validateFacilityId(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityIdIsEmpty() {
    activeDraftValidator.validateFacilityId(new UUID(0L, 0L));
  }

  @Test
  public void shouldNotThrowExceptionWhenFacilityIdIsValid() {
    activeDraftValidator.validateFacilityId(UUID.randomUUID());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenProgramIdIsNull() {
    activeDraftValidator.validateProgramId(null);
  }

  @Test
  public void shouldNotThrowExceptionWhenProgramIdIsValid() {
    activeDraftValidator.validateProgramId(UUID.randomUUID());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenIsDraftIsNull() {
    activeDraftValidator.validateIsDraft(null);
  }

  @Test
  public void shouldNotThrowExceptionWhenIsDraftIsValid() {
    activeDraftValidator.validateIsDraft(true);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenDraftTypeIsEmpty() {
    activeDraftValidator.validateDraftType("");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenDraftTypeIsEmptyString() {
    activeDraftValidator.validateDraftType(" ");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenDraftTypeIsNotIncluded() {
    activeDraftValidator.validateDraftType("ajs");
  }

  @Test
  public void shouldNotThrowExceptionWhenDraftTypeIsValid() {
    activeDraftValidator.validateDraftType("adjustment");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenDraftUserIsNotCurrentUser() {
    UserDto user = new UserDto();
    user.setId(UUID.randomUUID());
    StockManagementDraft drafts = new StockManagementDraft();
    drafts.setUserId(UUID.randomUUID());

    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    activeDraftValidator.validateDraftUser(drafts);
  }

  @Test
  public void shouldNotThrowValidationMessageExceptionWhenDraftUserIsCurrentUser() {
    UserDto user = new UserDto();
    UUID uuid = UUID.randomUUID();
    user.setId(uuid);
    StockManagementDraft drafts = new StockManagementDraft();
    drafts.setUserId(uuid);

    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    activeDraftValidator.validateDraftUser(drafts);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenUserIdIsNull() {
    activeDraftValidator.validateUserId(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenUserIdIsEmpty() {
    activeDraftValidator.validateUserId(new UUID(0L, 0L));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenUserIdIsNotCurrentUserId() {
    UserDto user = new UserDto();
    user.setId(UUID.randomUUID());

    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    activeDraftValidator.validateUserId(UUID.randomUUID());
  }

  @Test
  public void shouldNotThrowExceptionUserIdIsCurrentUserId() {
    UserDto user = new UserDto();
    UUID uuid = UUID.randomUUID();
    user.setId(uuid);

    when(authenticationHelper.getCurrentUser()).thenReturn(user);

    activeDraftValidator.validateUserId(uuid);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenNoStockManagementDraftFound() {

    when(stockManagementInitialDraftsRepository.findOne(initialDraftId))
        .thenReturn(null);

    activeDraftValidator.validateInitialDraftId(initialDraftId);
  }

  @Test(expected = NotFoundException.class)
  public void shouldThrowExceptionWhenSubDraftIsNull() {
    activeDraftValidator.validateSubDraft(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenSubDraftStatusIsSubmitted() {
    StockManagementDraft drafts = new StockManagementDraft();
    drafts.setDraftType(FieldConstants.ISSUE);
    drafts.setStatus(PhysicalInventorySubDraftEnum.SUBMITTED);
    activeDraftValidator.validateSubDraftStatus(drafts);
  }
}