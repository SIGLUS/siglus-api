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

import static org.openlmis.referencedata.util.messagekeys.IdealStockAmountMessageKeys.ERROR_COMMODITY_TYPE_NOT_FOUND;
import static org.openlmis.referencedata.util.messagekeys.IdealStockAmountMessageKeys.ERROR_FACILITY_NOT_FOUND;
import static org.openlmis.referencedata.util.messagekeys.IdealStockAmountMessageKeys.ERROR_PROCESSING_PERIOD_NOT_FOUND;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.referencedata.dto.IdealStockAmountCsvModel;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.CommodityTypeRepository;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.ProcessingPeriodRepository;
import org.openlmis.referencedata.repository.ProcessingScheduleRepository;
import org.openlmis.referencedata.service.IdealStockAmountService;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.ProcessingScheduleMessageKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IdealStockAmountProcessor is used for uploads of Ideal Stock Amount.
 * It uploads each ideal stock amount by record.
 */
@Component
public class IdealStockAmountProcessor
    implements RecordProcessor<IdealStockAmountCsvModel, IdealStockAmount> {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdealStockAmountProcessor.class);

  @Autowired
  private IdealStockAmountService idealStockAmountService;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private ProcessingPeriodRepository processingPeriodRepository;

  @Autowired
  private ProcessingScheduleRepository processingScheduleRepository;

  @Autowired
  private CommodityTypeRepository commodityTypeRepository;

  @Override
  public List<IdealStockAmount> process(List<IdealStockAmountCsvModel> records) {
    Profiler profiler = new Profiler("PROCESS_DTO_CHUNK");
    profiler.setLogger(LOGGER);

    profiler.start("SEARCH_EXISTING_ISA");
    List<IdealStockAmount> idealStockAmounts = convert(records);
    Map<Integer, IdealStockAmount> isaMap = new HashMap<>();
    for (IdealStockAmount isa : idealStockAmountService.search(idealStockAmounts)) {
      isaMap.put(hash(isa), isa);
    }

    List<IdealStockAmount> resultList = new ArrayList<>();

    profiler.start("PROCESS_RECORDS");
    for (IdealStockAmount isa : idealStockAmounts) {
      IdealStockAmount result = isaMap.getOrDefault(hash(isa), null);
      if (null == result) {
        resultList.add(prepareNewIdealStockAmountObject(isa));
      } else {
        result.setAmount(isa.getAmount());
        resultList.add(result);
      }
    }

    profiler.stop().log();

    return resultList;
  }

  private IdealStockAmount prepareNewIdealStockAmountObject(IdealStockAmount isa) {
    Facility facility = facilityRepository.findByCode(isa.getFacility().getCode())
        .orElseThrow(() -> new ValidationMessageException(new Message(ERROR_FACILITY_NOT_FOUND,
            isa.getFacility().getCode())));

    ProcessingSchedule schedule = processingScheduleRepository
        .findOneByCode(isa.getProcessingPeriod().getProcessingSchedule().getCode())
        .orElseThrow(() -> new ValidationMessageException(new Message(
            ProcessingScheduleMessageKeys.ERROR_NOT_FOUND_WITH_CODE,
            isa.getProcessingPeriod().getProcessingSchedule().getCode()
        )));
    ProcessingPeriod period = processingPeriodRepository.findOneByNameAndProcessingSchedule(
        isa.getProcessingPeriod().getName(),
        schedule)
        .orElseThrow(() -> new ValidationMessageException(
            new Message(ERROR_PROCESSING_PERIOD_NOT_FOUND,
            isa.getProcessingPeriod().getName(),
            isa.getProcessingPeriod().getProcessingSchedule().getCode())));

    CommodityType commodityType = commodityTypeRepository
        .findByClassificationIdAndClassificationSystem(isa.getCommodityType().getClassificationId(),
            isa.getCommodityType().getClassificationSystem())
        .orElseThrow(() -> new ValidationMessageException(new Message(
            ERROR_COMMODITY_TYPE_NOT_FOUND,
            isa.getCommodityType().getClassificationId(),
            isa.getCommodityType().getClassificationSystem())));

    return new IdealStockAmount(facility, commodityType, period, isa.getAmount());
  }

  private List<IdealStockAmount> convert(List<IdealStockAmountCsvModel> list) {
    List<IdealStockAmount> result = new ArrayList<>();

    for (IdealStockAmountCsvModel isa : list) {
      final Facility facility = new Facility(isa.getFacility().getCode());

      ProcessingSchedule schedule = new ProcessingSchedule();
      schedule.setCode(Code.code(isa.getProcessingPeriod().getProcessingSchedule().getCode()));
      ProcessingPeriod period = new ProcessingPeriod();
      period.setName(isa.getProcessingPeriod().getName());
      period.setProcessingSchedule(schedule);

      CommodityType commodityType = new CommodityType();
      commodityType.setClassificationId(isa.getCommodityType().getClassificationId());
      commodityType.setClassificationSystem(isa.getCommodityType().getClassificationSystem());

      result.add(new IdealStockAmount(facility, commodityType, period, isa.getAmount()));
    }

    return result;
  }

  private int hash(IdealStockAmount isa) {
    return Objects.hash(isa.getFacility().getCode(),
        isa.getCommodityType().getClassificationId(),
        isa.getCommodityType().getClassificationSystem(),
        isa.getProcessingPeriod().getName(),
        isa.getProcessingPeriod().getProcessingSchedule().getCode());
  }
}
