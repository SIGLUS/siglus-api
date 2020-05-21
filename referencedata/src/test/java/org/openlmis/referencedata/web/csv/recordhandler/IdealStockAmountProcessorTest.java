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

package org.openlmis.referencedata.web.csv.recordhandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.dto.BasicFacilityDto;
import org.openlmis.referencedata.dto.CommodityTypeDto;
import org.openlmis.referencedata.dto.IdealStockAmountCsvModel;
import org.openlmis.referencedata.dto.ProcessingPeriodDto;
import org.openlmis.referencedata.dto.ProcessingScheduleDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.CommodityTypeRepository;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.repository.ProcessingScheduleRepository;
import org.openlmis.referencedata.service.IdealStockAmountService;
import org.openlmis.referencedata.validate.IdealStockAmountValidator;

public class IdealStockAmountProcessorTest {

  private static final String FACILITY_CODE = "facility-code";
  private static final String SYSTEM = "system";
  private static final String ID = "id";
  private static final String SCHEDULE = "schedule";
  private static final String PERIOD = "period";

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private IdealStockAmountService service;

  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;

  @Mock
  private ProcessingScheduleRepository processingScheduleRepository;

  @Mock
  private CommodityTypeRepository commodityTypeRepository;

  @Mock
  private IdealStockAmountValidator idealStockAmountsValidator;

  @InjectMocks
  private IdealStockAmountProcessor idealStockAmountProcessor;

  private IdealStockAmount idealStockAmount;
  private Facility facility;
  private CommodityType commodityType;
  private ProcessingPeriod processingPeriod;
  private ProcessingSchedule schedule;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    facility = new Facility(FACILITY_CODE);
    commodityType = new CommodityType();
    commodityType.setClassificationSystem(SYSTEM);
    commodityType.setClassificationId(ID);
    schedule = new ProcessingSchedule();
    schedule.setCode(Code.code(SCHEDULE));
    processingPeriod = new ProcessingPeriod();
    processingPeriod.setName(PERIOD);
    processingPeriod.setProcessingSchedule(schedule);

    idealStockAmount = new IdealStockAmount(facility, commodityType, processingPeriod, 1);

    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.emptyList());

    when(facilityRepository.findByCode(FACILITY_CODE)).thenReturn(Optional.of(facility));
    when(processingScheduleRepository.findOneByCode(schedule.getCode()))
        .thenReturn(Optional.of(schedule));
    when(processingPeriodRepository.findOneByNameAndProcessingSchedule(PERIOD, schedule))
        .thenReturn(Optional.of(processingPeriod));
    when(commodityTypeRepository.findByClassificationIdAndClassificationSystem(ID, SYSTEM))
        .thenReturn(Optional.of(commodityType));
  }

  @Test
  public void shouldUseExistingObject() {
    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.singletonList(idealStockAmount));

    processAndCheckIsa();
  }

  @Test
  public void shouldCreateNewObject() {
    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.emptyList());

    processAndCheckIsa();
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfFacilityNotFound() {
    IdealStockAmountCsvModel isa = createIsaDto();

    idealStockAmountsValidator.validate(isa);
    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.emptyList());
    when(facilityRepository.findByCode(FACILITY_CODE)).thenReturn(Optional.empty());

    idealStockAmountProcessor.process(Collections.singletonList(isa));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfSCheduleNotFound() {
    IdealStockAmountCsvModel isa = createIsaDto();

    idealStockAmountsValidator.validate(isa);
    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.emptyList());
    when(processingScheduleRepository.findOneByCode(schedule.getCode()))
        .thenReturn(Optional.empty());

    idealStockAmountProcessor.process(Collections.singletonList(isa));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfPeriodNotFound() {
    IdealStockAmountCsvModel isa = createIsaDto();

    idealStockAmountsValidator.validate(isa);
    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.emptyList());
    when(processingPeriodRepository.findOneByNameAndProcessingSchedule(PERIOD, schedule))
        .thenReturn(Optional.empty());

    idealStockAmountProcessor.process(Collections.singletonList(isa));
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfCommodityTypeNotFound() {
    IdealStockAmountCsvModel isa = createIsaDto();

    idealStockAmountsValidator.validate(isa);
    when(service.search(anyListOf(IdealStockAmount.class)))
        .thenReturn(Collections.emptyList());
    when(commodityTypeRepository.findByClassificationIdAndClassificationSystem(ID, SYSTEM))
        .thenReturn(Optional.empty());

    idealStockAmountProcessor.process(Collections.singletonList(isa));
  }

  private IdealStockAmountCsvModel createIsaDto() {
    BasicFacilityDto facilityDto = new BasicFacilityDto();
    facilityDto.setCode(FACILITY_CODE);
    CommodityTypeDto commodityTypeDto = new CommodityTypeDto();
    commodityTypeDto.setClassificationSystem(SYSTEM);
    commodityTypeDto.setClassificationId(ID);
    ProcessingScheduleDto schedule = new ProcessingScheduleDto();
    schedule.setCode(SCHEDULE);
    ProcessingPeriodDto processingPeriodDto = new ProcessingPeriodDto();
    processingPeriodDto.setName(PERIOD);
    processingPeriodDto.setProcessingSchedule(schedule);

    return new IdealStockAmountCsvModel(facilityDto, commodityTypeDto, processingPeriodDto, 1212);
  }

  private void processAndCheckIsa() {
    IdealStockAmountCsvModel isa = createIsaDto();
    idealStockAmountsValidator.validate(isa);

    List<IdealStockAmount> result = idealStockAmountProcessor
        .process(Collections.singletonList(isa));

    verify(idealStockAmountsValidator).validate(isa);
    verify(service).search(anyListOf(IdealStockAmount.class));

    assertEquals(result.get(0).getFacility(), facility);
    assertEquals(result.get(0).getAmount(), new Integer(1212));
    assertEquals(result.get(0).getCommodityType(), commodityType);
    assertEquals(result.get(0).getProcessingPeriod(), processingPeriod);
  }
}
