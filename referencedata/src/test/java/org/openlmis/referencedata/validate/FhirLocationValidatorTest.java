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

package org.openlmis.referencedata.validate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.validate.FhirLocationValidator.EXTRA_DATA;
import static org.openlmis.referencedata.validate.FhirLocationValidator.IS_MANAGED_EXTERNALLY;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.BaseEntity;
import org.openlmis.referencedata.domain.FhirLocation;
import org.openlmis.referencedata.dto.BaseDto;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@RunWith(MockitoJUnitRunner.class)
public abstract class FhirLocationValidatorTest
    <D extends BaseDto & FhirLocation, E extends BaseEntity & FhirLocation> {

  abstract FhirLocationValidator<D, E> getValidator();

  abstract Class<D> getDtoDefinition();

  abstract D getTarget();

  abstract E getExistingResource();

  @Mock
  private SecurityContext securityContext;

  @Mock
  private OAuth2Authentication authentication;

  private D target;
  private E entity;

  Errors errors;

  @Before
  public void setUp() {
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isClientOnly()).thenReturn(true);

    target = getTarget();
    entity = getExistingResource();

    errors = new BeanPropertyBindingResult(target, "target");
  }

  @Test
  public void shouldSupportDtoClass() {
    assertThat(getValidator().supports(getDtoDefinition())).isTrue();
  }

  @Test
  public void shouldRejectIfFhirFlagIsSetAndUserSentRequest() {
    target.setId(null);
    target.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());

    mockUserRequest();
    getValidator().validate(target, errors);

    assertErrorMessage(errors, EXTRA_DATA, getValidator().getUnallowedKeyErrorMessage());
  }

  @Test
  public void shouldNotRejectIfFhirFlagIsSetAndRequestIsFromService() {
    target.setId(null);
    target.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());

    getValidator().validate(target, errors);

    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldNotRejectIfFhirFlagWasNotSetForNewResource() {
    target.setId(null);
    target.getExtraData().remove(IS_MANAGED_EXTERNALLY);

    getValidator().validate(target, errors);

    verifyZeroInteractions(securityContext, authentication);
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldRejectIfFhirFlagWasChangedAndUserSentRequest() {
    target.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.FALSE.toString());
    entity.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());

    mockUserRequest();
    getValidator().validate(target, errors);

    assertErrorMessage(errors, EXTRA_DATA, getValidator().getModifiedKeyErrorMessage());
  }

  @Test
  public void shouldNotRejectIfFhirFlagWasChangedAndServiceSentRequest() {
    target.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.FALSE.toString());
    entity.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());
    getValidator().validate(target, errors);
    assertEquals(0, errors.getErrorCount());
  }

  @Test
  public void shouldNotRejectIfFhirFlagWasNotChanged() {
    target.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());
    entity.getExtraData().put(IS_MANAGED_EXTERNALLY, Boolean.TRUE.toString());

    getValidator().validate(target, errors);

    assertEquals(0, errors.getErrorCount());
  }

  void mockUserRequest() {
    when(authentication.isClientOnly()).thenReturn(false);
  }
}
