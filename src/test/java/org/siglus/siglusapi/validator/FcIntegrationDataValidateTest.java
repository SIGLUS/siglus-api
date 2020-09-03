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
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.ValidationMessageException;
import org.siglus.siglusapi.domain.RequisitionExtension;

@RunWith(MockitoJUnitRunner.class)
public class FcIntegrationDataValidateTest {

  @InjectMocks
  private FcIntegrationDataValidate validate;

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenRequisitionNumberNull() {
    validate.validateEmptyRequisitionNumber(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenRequisitionNumberEmpty() {
    validate.validateEmptyRequisitionNumber("");
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenRequisitionNumberExist() {
    validate.validateEmptyRequisitionNumber("RNR-NO010906120000192");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenRequisitionNumberNotExist() {
    validate.validateExistRequisitionNumber(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenRequisitionExtensionExistButNumberNull() {
    RequisitionExtension extension = new RequisitionExtension();
    validate.validateExistRequisitionNumber(extension);
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenRequisitionExtensionExist() {
    RequisitionExtension extension = new RequisitionExtension();
    extension.setRequisitionNumber(Integer.valueOf(13));
    validate.validateExistRequisitionNumber(extension);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityCodeNull() {
    validate.validateEmptyFacilityCode(null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityCodeEmpty() {
    validate.validateEmptyFacilityCode("");
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenFacilityCodeExist() {
    validate.validateEmptyFacilityCode("04030101");
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenFacilityNotExist() {
    validate.validateExistFacility(Arrays.asList());
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenFacilityExist() {
    FacilityDto facilityDto = new FacilityDto();
    validate.validateExistFacility(Arrays.asList(facilityDto));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenUserNotExist() {
    validate.validateExistUser(Arrays.asList());
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenUserExist() {
    UserDto userDto = new UserDto();
    validate.validateExistUser(Arrays.asList(userDto));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowValidationMessageExceptionWhenSourceNull() {
    validate.validateFcSource(null);
  }

  @Test
  public void shouldThrowValidationMessageExceptionWhenSourceExist() {
    validate.validateFcSource(new ValidSourceDestinationDto());
  }
}
