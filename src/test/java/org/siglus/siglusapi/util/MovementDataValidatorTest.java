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
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;


@RunWith(MockitoJUnitRunner.class)
public class MovementDataValidatorTest {

  @InjectMocks
  private MovementDateValidator movementDateValidator;

  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;

  @Mock
  private SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;

  @Test
  public void shouldReturnPeriodStartDateGivenLastMovementDateIsEarlierWhenGetMovementStartDate() {
    //given
    LocalDate currentDate = LocalDate.of(2022, 7, 28);
    UUID facilityId = UUID.randomUUID();
    LocalDate periodStartDate = LocalDate.of(2022, 7, 23);
    LocalDate lastMovementDate = LocalDate.of(2022, 7, 21);
    //when
    when(processingPeriodRepository.getCurrentPeriodStartDate(any())).thenReturn(periodStartDate);
    when(siglusStockCardLineItemRepository.findFacilityLastMovementDate(facilityId)).thenReturn(lastMovementDate);
    //then
    LocalDate movementStartDate = movementDateValidator.getMovementStartDate(currentDate, facilityId)
        .getLastMovementDate();
    assertEquals(movementStartDate, periodStartDate);
  }

  @Test
  public void shouldReturnLastMovementDateGivenPeriodStartDateIsEarlierWhenGetMovementStartDate() {
    //given
    LocalDate currentDate = LocalDate.of(2022, 7, 28);
    UUID facilityId = UUID.randomUUID();
    LocalDate periodStartDate = LocalDate.of(2022, 7, 21);
    LocalDate lastMovementDate = LocalDate.of(2022, 7, 23);
    //when
    when(processingPeriodRepository.getCurrentPeriodStartDate(any())).thenReturn(periodStartDate);
    when(siglusStockCardLineItemRepository.findFacilityLastMovementDate(facilityId)).thenReturn(lastMovementDate);
    //then
    LocalDate movementStartDate = movementDateValidator.getMovementStartDate(currentDate, facilityId)
        .getLastMovementDate();
    assertEquals(movementStartDate, lastMovementDate);
  }

  @Test
  public void shouldReturnPeriodStartDateGivenNoMovementWhenGetMovementStartDate() {
    //given
    LocalDate currentDate = LocalDate.of(2022, 7, 28);
    UUID facilityId = UUID.randomUUID();
    LocalDate periodStartDate = LocalDate.of(2022, 7, 21);
    //when
    when(processingPeriodRepository.getCurrentPeriodStartDate(any())).thenReturn(periodStartDate);
    when(siglusStockCardLineItemRepository.findFacilityLastMovementDate(facilityId)).thenReturn(null);
    //then
    LocalDate movementStartDate = movementDateValidator.getMovementStartDate(currentDate, facilityId)
        .getLastMovementDate();
    assertEquals(movementStartDate, periodStartDate);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldCallGetStartdateWithServiceWhenValidate() {
    //given
    LocalDate currentDate = LocalDate.of(2022, 7, 28);
    UUID facilityId = UUID.randomUUID();
    LocalDate periodStartDate = LocalDate.of(2022, 7, 29);
    //when
    when(processingPeriodRepository.getCurrentPeriodStartDate(currentDate)).thenReturn(periodStartDate);
    when(siglusStockCardLineItemRepository.findFacilityLastMovementDate(facilityId)).thenReturn(null);
    movementDateValidator.validateMovementDate(currentDate, facilityId);
  }
}