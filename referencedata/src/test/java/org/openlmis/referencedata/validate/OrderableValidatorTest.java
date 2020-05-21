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
import static org.openlmis.referencedata.validate.OrderableValidator.IN_BOX_CUBE_DIMENSION;
import static org.openlmis.referencedata.validate.OrderableValidator.MAXIMUM_TEMPERATURE;
import static org.openlmis.referencedata.validate.OrderableValidator.MINIMUM_TEMPERATURE;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.testbuilder.OrderableDataBuilder;
import org.openlmis.referencedata.testbuilder.TemperatureMeasurementDataBuilder;
import org.openlmis.referencedata.testbuilder.VolumeMeasurementDataBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class OrderableValidatorTest {

  @InjectMocks
  private Validator validator = new OrderableValidator();

  private Orderable orderable;
  private OrderableDto orderableDto;
  private Errors errors;

  @Before
  public void setUp() {
    orderableDto = new OrderableDto();

    orderable = new OrderableDataBuilder()
            .withMinimumTemperature("CEL", 2.0)
            .withMaximumTemperature("CEL", 8.0)
            .withInBoxCubeDimension("MLT", 200.0)
            .build();
    orderable.export(orderableDto);

    errors = new BeanPropertyBindingResult(orderableDto, "orderableDto");
  }

  @Test
  public void shouldNotFindErrorsWhenOrderableIsValid() throws Exception {
    validator.validate(orderableDto, errors);

    assertThat(errors.getErrorCount()).isEqualTo(0);
  }

  @Test
  public void shouldNotRejectIfTemperatureUnitCodeIsSupported() {
    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(MINIMUM_TEMPERATURE)).isFalse();
    assertThat(errors.hasFieldErrors(MAXIMUM_TEMPERATURE)).isFalse();
  }

  @Test
  public void shouldRejectIfTemperatureUnitCodeIsNotSupported() {
    orderableDto.setMinimumTemperature(new TemperatureMeasurementDataBuilder()
            .withTemperatureMeasurementUnitCode("F").build());
    orderableDto.setMaximumTemperature(new TemperatureMeasurementDataBuilder()
            .withTemperatureMeasurementUnitCode("F").build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(MINIMUM_TEMPERATURE)).isTrue();
    assertThat(errors.hasFieldErrors(MAXIMUM_TEMPERATURE)).isTrue();
  }

  @Test
  public void shouldRejectIfMinTemperatureValueIsGreaterThanMaxTemperatureValue() {
    orderableDto.setMinimumTemperature(new TemperatureMeasurementDataBuilder()
            .withValue(12.0).build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(MINIMUM_TEMPERATURE)).isTrue();
  }

  @Test
  public void shouldRejectIfInBoxCubeDimensionCodeIsNotSupported() {
    orderableDto.setInBoxCubeDimension(new VolumeMeasurementDataBuilder()
            .withMeasurementUnitCode("ML").build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(IN_BOX_CUBE_DIMENSION)).isTrue();
  }

  @Test
  public void shouldRejectIfInBoxCubeDimensionValueIsNotPositiveNumber() {
    orderableDto.setInBoxCubeDimension(new VolumeMeasurementDataBuilder()
            .withValue(-0.2).build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(IN_BOX_CUBE_DIMENSION)).isTrue();
  }

  @Test
  public void shouldRejectIfInBoxCubeDimensionCodeIsNotGiven() {
    orderableDto.setInBoxCubeDimension(new VolumeMeasurementDataBuilder()
            .withMeasurementUnitCode(null).build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(IN_BOX_CUBE_DIMENSION)).isTrue();
  }

  @Test
  public void shouldRejectIfInBoxCubeDimensionValueIsNotGiven() {
    orderableDto.setInBoxCubeDimension(new VolumeMeasurementDataBuilder()
            .withValue(null).build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(IN_BOX_CUBE_DIMENSION)).isTrue();
  }

  @Test
  public void shouldRejectIfTemperatureUnitCodeIsNotGiven() {
    orderableDto.setMinimumTemperature(new TemperatureMeasurementDataBuilder()
            .withTemperatureMeasurementUnitCode(null).build());
    orderableDto.setMaximumTemperature(new TemperatureMeasurementDataBuilder()
            .withTemperatureMeasurementUnitCode(null).build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(MINIMUM_TEMPERATURE)).isTrue();
    assertThat(errors.hasFieldErrors(MAXIMUM_TEMPERATURE)).isTrue();
  }

  @Test
  public void shouldRejectIfOnlyTemperatureUnitCodeIsNotGiven() {
    orderableDto.setMinimumTemperature(new TemperatureMeasurementDataBuilder()
            .withValue(null).build());
    orderableDto.setMaximumTemperature(new TemperatureMeasurementDataBuilder()
            .withValue(null).build());

    validator.validate(orderableDto, errors);
    assertThat(errors.hasFieldErrors(MINIMUM_TEMPERATURE)).isTrue();
    assertThat(errors.hasFieldErrors(MAXIMUM_TEMPERATURE)).isTrue();
  }
}
