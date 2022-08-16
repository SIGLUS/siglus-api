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

package org.siglus.siglusapi.service.task.report;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.springframework.data.domain.Example;

@RunWith(MockitoJUnitRunner.class)
public class CalculateWebCmmServiceTest {

  @InjectMocks
  private CalculateWebCmmService calculateWebCmmService;

  @Mock
  private FacilityExtensionRepository facilityExtensionRepository;
  @Mock
  private StockCardRepository stockCardRepository;
  @Mock
  private ProcessingPeriodRepository processingPeriodRepository;
  @Mock
  private FacilityCmmsRepository facilityCmmsRepository;
  @Mock
  private OrderableRepository orderableRepository;
  @Mock
  private SiglusStockCardService siglusStockCardService;

  private final UUID orderableId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final UUID stockCardId = UUID.randomUUID();
  private final String orderableCode = "orderable code";

  @Test
  public void shouldSuccessWhenCalculateAllPeriod() {
    mockFacilityExtension();
    when(stockCardRepository.findAll()).thenReturn(buildMockStockCards());
    when(orderableRepository.findLatestByIds(Sets.newHashSet(orderableId))).thenReturn(buildMockOrderables());
    when(processingPeriodRepository.findAll()).thenReturn(buildMockPeriods());
    when(siglusStockCardService.getProductMovements(any(), any(), any(), any())).thenReturn(
        buildStockMovementResDtos());

    calculateWebCmmService.calculateAllPeriod();

    verify(facilityCmmsRepository).save(anyList());
  }

  @Test
  public void shouldNotSaveWhenCalculateAllPeriodWithNoProductMovement() {
    mockFacilityExtension();
    when(stockCardRepository.findAll()).thenReturn(buildMockStockCards());
    when(orderableRepository.findLatestByIds(Sets.newHashSet(orderableId))).thenReturn(buildMockOrderables());
    when(processingPeriodRepository.findAll()).thenReturn(buildMockPeriods());
    when(siglusStockCardService.getProductMovements(any(), any(), any(), any())).thenReturn(null);

    calculateWebCmmService.calculateAllPeriod();

    verify(facilityCmmsRepository, times(0)).save(anyList());
  }

  @Test
  public void shouldNotSaveWhenCalculateAllPeriodWithNoPeriod() {
    mockFacilityExtension();
    when(stockCardRepository.findAll()).thenReturn(buildMockStockCards());
    when(orderableRepository.findLatestByIds(Sets.newHashSet(orderableId))).thenReturn(buildMockOrderables());
    when(processingPeriodRepository.findAll()).thenReturn(Lists.newArrayList());
    when(siglusStockCardService.getProductMovements(any(), any(), any(), any())).thenReturn(null);

    calculateWebCmmService.calculateAllPeriod();

    verify(facilityCmmsRepository, times(0)).save(anyList());
  }

  @Test
  public void shouldNotSaveWhenCalculateAllPeriodWithNoWebFacility() {
    mockNoneFacilityExtension();
    when(stockCardRepository.findAll()).thenReturn(buildMockStockCards());
    when(orderableRepository.findLatestByIds(Sets.newHashSet(orderableId))).thenReturn(buildMockOrderables());
    when(processingPeriodRepository.findAll()).thenReturn(buildMockPeriods());
    when(siglusStockCardService.getProductMovements(any(), any(), any(), any())).thenReturn(null);

    calculateWebCmmService.calculateAllPeriod();

    verify(facilityCmmsRepository, times(0)).save(anyList());
  }

  @Test
  public void shouldSuccessWhenCalculateCurrentPeriod() {
    mockFacilityExtension();
    when(stockCardRepository.findAll()).thenReturn(buildMockStockCards());
    when(orderableRepository.findLatestByIds(Sets.newHashSet(orderableId))).thenReturn(buildMockOrderables());
    when(processingPeriodRepository.findAll()).thenReturn(buildMockPeriods());
    when(siglusStockCardService.getProductMovements(any(), any(), any(), any())).thenReturn(
        buildStockMovementResDtos());

    calculateWebCmmService.calculateCurrentPeriod();

    verify(facilityCmmsRepository).save(anyList());
  }

  private void mockFacilityExtension() {
    Example<FacilityExtension> example = Example.of(FacilityExtension.builder().isAndroid(Boolean.FALSE).build());
    when(facilityExtensionRepository.findAll(example)).thenReturn(buildMockFacilityExtensions());
  }

  private void mockNoneFacilityExtension() {
    Example<FacilityExtension> example = Example.of(FacilityExtension.builder().isAndroid(Boolean.FALSE).build());
    when(facilityExtensionRepository.findAll(example)).thenReturn(Lists.newArrayList());
  }

  private List<StockMovementResDto> buildStockMovementResDtos() {
    StockMovementResDto firstStockMovementPeriod202109 = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 9, 21))
        .productCode(orderableCode)
        .movementQuantity(-1)
        .type(MovementType.ISSUE.name())
        .productSoh(110)
        .build();

    StockMovementResDto stockMovementPeriod202110One = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 10, 21))
        .productCode(orderableCode)
        .movementQuantity(-10)
        .type(MovementType.ISSUE.name())
        .productSoh(100)
        .build();
    StockMovementResDto stockMovementPeriod202110Two = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 10, 22))
        .productCode(orderableCode)
        .movementQuantity(-20)
        .type(MovementType.ISSUE.name())
        .productSoh(80)
        .build();
    StockMovementResDto stockMovementPeriod202110Three = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 10, 23))
        .productCode(orderableCode)
        .movementQuantity(-30)
        .type(MovementType.ISSUE.name())
        .productSoh(50)
        .build();
    StockMovementResDto stockMovementPeriod202110FourNonIssue = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 10, 23))
        .productCode(orderableCode)
        .movementQuantity(30)
        .type(MovementType.RECEIVE.name())
        .productSoh(50)
        .build();

    StockMovementResDto stockMovementPeriod202111One = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 11, 21))
        .productCode(orderableCode)
        .movementQuantity(-30)
        .type(MovementType.ISSUE.name())
        .productSoh(10)
        .build();
    StockMovementResDto stockMovementPeriod202111Two = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2021, 11, 22))
        .productCode(orderableCode)
        .movementQuantity(-30)
        .type(MovementType.ISSUE.name())
        .productSoh(0)
        .build();

    StockMovementResDto stockMovementPeriod202201One = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2022, 1, 21))
        .productCode(orderableCode)
        .movementQuantity(-30)
        .type(MovementType.ISSUE.name())
        .productSoh(10)
        .build();
    StockMovementResDto stockMovementPeriod202201Two = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.of(2022, 1, 22))
        .productCode(orderableCode)
        .movementQuantity(-30)
        .type(MovementType.ISSUE.name())
        .productSoh(10)
        .build();

    StockMovementResDto stockMovementPeriodToday = StockMovementResDto.builder()
        .dateOfMovement(LocalDate.now().minusMonths(1))
        .productCode(orderableCode)
        .movementQuantity(-30)
        .type(MovementType.ISSUE.name())
        .productSoh(10)
        .build();

    return Lists.newArrayList(firstStockMovementPeriod202109,
        stockMovementPeriod202110One, stockMovementPeriod202110Two, stockMovementPeriod202110Three,
        stockMovementPeriod202110FourNonIssue,
        stockMovementPeriod202111One, stockMovementPeriod202111Two,
        stockMovementPeriod202201One, stockMovementPeriod202201Two, stockMovementPeriodToday);
  }

  private List<ProcessingPeriod> buildMockPeriods() {
    ProcessingSchedule m1Schedule = new ProcessingSchedule();
    m1Schedule.setCode(Code.code("M1"));
    ProcessingSchedule m2Schedule = new ProcessingSchedule();
    m2Schedule.setCode(Code.code("M2"));
    List<ProcessingPeriod> periods = Lists.newArrayList();
    for (int i = 1; i < 12; i++) {
      ProcessingPeriod m1ProcessingPeriod = ProcessingPeriod.newPeriod(i + "-2021", m1Schedule,
          LocalDate.of(2021, i, 21), LocalDate.of(2021, i + 1, 20));
      periods.add(m1ProcessingPeriod);
    }
    for (int i = 1; i < 12; i++) {
      ProcessingPeriod m1ProcessingPeriod = ProcessingPeriod.newPeriod(i + "-2022", m1Schedule,
          LocalDate.of(2022, i, 21), LocalDate.of(2022, i + 1, 20));
      periods.add(m1ProcessingPeriod);

      ProcessingPeriod m2ProcessingPeriod = ProcessingPeriod.newPeriod(i + "-2022", m2Schedule,
          LocalDate.of(2022, i, 21), LocalDate.of(2022, i + 1, 20));
      periods.add(m2ProcessingPeriod);
    }
    return periods;
  }

  private List<Orderable> buildMockOrderables() {
    Orderable orderable = new Orderable(Code.code(orderableCode), null, 0, 0, Boolean.FALSE, orderableId, 0L);
    return Lists.newArrayList(orderable);
  }

  private List<StockCard> buildMockStockCards() {
    StockCard stockCard = StockCard.builder()
        .facilityId(facilityId)
        .orderableId(orderableId)
        .build();
    stockCard.setId(stockCardId);

    StockCard stockCardSameOrderable = StockCard.builder()
        .facilityId(facilityId)
        .orderableId(orderableId)
        .build();
    stockCard.setId(UUID.randomUUID());
    return Lists.newArrayList(stockCard, stockCardSameOrderable);
  }

  private List<FacilityExtension> buildMockFacilityExtensions() {
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .facilityCode("facility Code")
        .build();
    return Lists.newArrayList(facilityExtension);
  }
}