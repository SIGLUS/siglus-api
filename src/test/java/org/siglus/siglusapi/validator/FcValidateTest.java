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

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.fc.ProductDto;
import org.siglus.siglusapi.service.fc.FcDataException;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class FcValidateTest {

  @InjectMocks
  private FcValidate validate;

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityCodeNull() {
    // then
    validate.validateEmptyFacilityCode(null);
  }

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityCodeEmpty() {
    // then
    validate.validateEmptyFacilityCode("");
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenFacilityCodeExist() {
    // then
    validate.validateEmptyFacilityCode("04030101");
  }

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityNotExist() {
    // then
    validate.validateExistFacility(Arrays.asList());
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenFacilityExist() {
    // given
    FacilityDto facilityDto = new FacilityDto();

    // then
    validate.validateExistFacility(Arrays.asList(facilityDto));
  }

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenUserNotExist() {
    // then
    validate.validateExistUser(Arrays.asList());
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenUserExist() {
    // given
    UserDto userDto = new UserDto();

    // then
    validate.validateExistUser(Arrays.asList(userDto));
  }

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenSourceNull() {
    // then
    validate.validateFcSource(null);
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenSourceExist() {
    // then
    validate.validateFcSource(new ValidSourceDestinationDto());
  }

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenProductNull() {
    // then
    validate.validateFcProduct(null);
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenProductExist() {
    // given
    ProductDto productDto = new ProductDto();

    // then
    validate.validateFcProduct(Arrays.asList(productDto));
  }

  @Test(expected = FcDataException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityNull() {
    // then
    validate.validateFacility(null);
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenFacilityNotNull() {
    // then
    validate.validateFacility(new Facility());
  }
}
