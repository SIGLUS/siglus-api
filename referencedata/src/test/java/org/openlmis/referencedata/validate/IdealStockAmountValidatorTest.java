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

import static org.openlmis.referencedata.util.messagekeys.IdealStockAmountMessageKeys.ERROR_FROM_FIELD_REQUIRED;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.BasicFacilityDto;
import org.openlmis.referencedata.dto.CommodityTypeDto;
import org.openlmis.referencedata.dto.IdealStockAmountCsvModel;
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.openlmis.referencedata.dto.ProcessingScheduleDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods"})
public class IdealStockAmountValidatorTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @InjectMocks
  private IdealStockAmountValidator isaValidator;

  private IdealStockAmountCsvModel isa;
  private BasicFacilityDto facility;
  private ProcessingScheduleDto processingSchedule;
  private ProcessingPeriodDto processingPeriod;
  private CommodityTypeDto commodityType;

  @Before
  public void setUp() {
    facility = new BasicFacilityDto();
    facility.setCode("facility-code");
    processingSchedule = new ProcessingScheduleDto();
    processingSchedule.setCode("schedule-code");
    processingPeriod = new ProcessingPeriodDto();
    processingPeriod.setName("period");
    processingPeriod.setProcessingSchedule(processingSchedule);
    commodityType = new CommodityTypeDto();
    commodityType.setClassificationId("classification-id");
    commodityType.setClassificationSystem("classification-system");
    isa = new IdealStockAmountCsvModel(facility, commodityType, processingPeriod, 11);
  }

  @Test
  public void shouldNotThrowExceptionIfRequiredFieldsAreNotNull() {
    isaValidator.validate(isa);
  }

  @Test
  public void shouldThrowExceptionIfFacilityIsNull() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_FROM_FIELD_REQUIRED, "facility").toString());

    isa.setFacility((BasicFacilityDto) null);
    isaValidator.validate(isa);
  }

  @Test
  public void shouldThrowExceptionIfProcessingPeriodIsNull() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_FROM_FIELD_REQUIRED, "processingPeriod").toString());

    isa.setProcessingPeriod((ProcessingPeriodDto) null);
    isaValidator.validate(isa);
  }

  @Test
  public void shouldThrowExceptionIfProcessingScheduleIsNull() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_FROM_FIELD_REQUIRED, "processingPeriod.processingSchedule").toString());

    isa.getProcessingPeriod().setProcessingSchedule(null);
    isaValidator.validate(isa);
  }

  @Test
  public void shouldThrowExceptionIfCommodityTypeIsNull() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_FROM_FIELD_REQUIRED, "commodityType").toString());

    isa.setCommodityType((CommodityTypeDto) null);
    isaValidator.validate(isa);
  }

  @Test
  public void shouldThrowExceptionIfAmountIsNull() {
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(
        new Message(ERROR_FROM_FIELD_REQUIRED, "amount").toString());

    isa.setAmount(null);
    isaValidator.validate(isa);
  }
}
