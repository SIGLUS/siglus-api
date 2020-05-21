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

import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.IdealStockAmount;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.repository.IdealStockAmountRepository;

public class IdealStockAmountWriterTest {

  @Mock
  private IdealStockAmountRepository idealStockAmountRepository;

  @InjectMocks
  private IdealStockAmountWriter idealStockAmountWriter;

  private IdealStockAmount idealStockAmount;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    Facility facility = new Facility("facility-code");
    CommodityType commodityType = new CommodityType();
    ProcessingPeriod processingPeriod = new ProcessingPeriod();
    processingPeriod.setStartDate(LocalDate.of(2017, 10, 1));
    processingPeriod.setEndDate(LocalDate.of(2017, 10, 30));

    idealStockAmount = new IdealStockAmount(facility, commodityType, processingPeriod, 123);
  }

  @Test
  public void shouldSetIdIfExistingItemFoundByCode() {
    IdealStockAmount existingCatalogItem = new IdealStockAmount();
    existingCatalogItem.setId(UUID.randomUUID());

    idealStockAmountWriter.write(Arrays.asList(idealStockAmount));

    verify(idealStockAmountRepository).save(Arrays.asList(idealStockAmount));
  }
}
