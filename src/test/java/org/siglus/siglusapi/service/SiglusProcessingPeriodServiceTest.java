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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.PermissionStrings;
import org.openlmis.requisition.utils.AuthenticationHelper;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.card.StockCardLineItem;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.testutils.ProcessingPeriodDtoDataBuilder;
import org.siglus.siglusapi.testutils.RequisitionLineItemDataBuilder;
import org.siglus.siglusapi.validator.SiglusProcessingPeriodValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class SiglusProcessingPeriodServiceTest {

  @Mock
  private SiglusProcessingPeriodReferenceDataService siglusProcessingPeriodReferenceDataService;

  @Mock
  private ProcessingPeriodExtensionRepository processingPeriodExtensionRepository;

  @Mock
  private SiglusProcessingPeriodValidator siglusProcessingPeriodValidator;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private PermissionService permissionService;

  @Mock
  private PeriodService periodService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionService requisitionService;

  @Mock
  private PermissionStrings permissionStrings;

  @Mock
  private SiglusRequisitionRepository siglusRequisitionRepository;

  @Mock
  private SiglusProgramService siglusProgramService;

  @Mock
  private ReportTypeRepository reportTypeRepository;

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusProcessingPeriodService siglusProcessingPeriodService;

  private final ProcessingPeriodDtoDataBuilder builder = new ProcessingPeriodDtoDataBuilder();
  private final ProcessingPeriodDto fullDto = builder.buildFullDto();
  private final ProcessingPeriodDto periodDto = builder.buildDto();
  private final ProcessingPeriodExtension extension = builder.buildExtenstion();

  private final UUID processingScheduleId = UUID.randomUUID();
  private final int page = 0;
  private final int size = 10;
  private final Sort sort = new Sort("startDate");

  private final UUID facilityId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID orderableId = UUID.randomUUID();
  private final UUID requisitionId = UUID.randomUUID();
  private final UUID requisitionId2 = UUID.randomUUID();

  @Test
  public void shouldCreateProcessingPeriodIfPassValidation() {

    when(siglusProcessingPeriodReferenceDataService.saveProcessingPeriod(fullDto))
        .thenReturn(fullDto);
    when(processingPeriodExtensionRepository.save(extension))
        .thenReturn(extension);

    ProcessingPeriodDto response =
        siglusProcessingPeriodService.createProcessingPeriod(fullDto);

    verify(siglusProcessingPeriodReferenceDataService)
        .saveProcessingPeriod(fullDto);
    verify(processingPeriodExtensionRepository).save(extension);
    assertEquals(response, fullDto);
  }

  @Test
  public void shouldGetProcessingPeriods() {
    MultiValueMap<String, Object> map = new LinkedMultiValueMap();
    List<Object> list = new ArrayList<>();
    list.add(processingScheduleId.toString());
    map.put("processingScheduleId", list);

    List<ProcessingPeriodDto> processingPeriodDtos = new ArrayList<>();
    processingPeriodDtos.add(periodDto);

    PageImplRepresentation<ProcessingPeriodDto> pageImpl = new PageImplRepresentation();
    pageImpl.setContent(processingPeriodDtos);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);

    PageRequest pageRequest = new PageRequest(page, size, sort);
    when(siglusProcessingPeriodReferenceDataService.searchProcessingPeriods(
        processingScheduleId, null, null, null, null,
        Collections.emptySet(), pageRequest)).thenReturn(pageImpl);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(extensions);

    Page<ProcessingPeriodDto> response = siglusProcessingPeriodService
        .getAllProcessingPeriods(map, pageRequest);

    verify(siglusProcessingPeriodReferenceDataService)
        .searchProcessingPeriods(processingScheduleId, null,
            null, null, null, Collections.emptySet(), pageRequest);
    verify(processingPeriodExtensionRepository).findAll();
    assertEquals(response.getContent().get(0), fullDto);
  }

  @Test
  public void shouldGetProcessingPeriodById() {
    UUID periodId = periodDto.getId();

    when(siglusProcessingPeriodReferenceDataService.findOne(periodId)).thenReturn(periodDto);
    when(processingPeriodExtensionRepository
        .findByProcessingPeriodId(periodId)).thenReturn(extension);

    ProcessingPeriodDto response = siglusProcessingPeriodService.getProcessingPeriod(periodId);

    verify(siglusProcessingPeriodReferenceDataService).findOne(periodId);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(periodId);
    assertEquals(response, fullDto);
  }

  @Test
  public void shouldKeepUntouchedIfNoExtensionWhenGetProcessingPeriodById() {
    UUID periodId = periodDto.getId();

    when(siglusProcessingPeriodReferenceDataService.findOne(periodId)).thenReturn(periodDto);
    when(processingPeriodExtensionRepository
        .findByProcessingPeriodId(periodId)).thenReturn(null);

    ProcessingPeriodDto response = siglusProcessingPeriodService.getProcessingPeriod(periodId);

    verify(siglusProcessingPeriodReferenceDataService).findOne(periodId);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(periodId);
    assertEquals(response, periodDto);

  }

  @Test
  public void shouldGetProcessingPeriodsForInitiateOfRegualrRequisition() {
    setupReportType();
    Collection<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(periodDto);
    when(periodService.searchByProgramAndFacility(programId, facilityId)).thenReturn(periods);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(extensions);

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, false));
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId, false))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(fullDto);
    requisitionPeriod.setRequisitionId(requisitionId);
    requisitionPeriod.setRequisitionStatus(RequisitionStatus.INITIATED);

    Collection<RequisitionPeriodDto> response =
        siglusProcessingPeriodService.getPeriods(programId, facilityId, false);

    assertEquals(1, response.size());
    assertTrue(response.contains(requisitionPeriod));
  }

  @Test
  public void shouldGetProcessingPeriodsForEmergencyRequisitionWhenHaveProcessForPrePeriod() {
    //given
    setupReportType();
    ProcessingPeriodDto prePeriodDto = builder.buildPerDto();
    when(periodService.getCurrentPeriods(programId, facilityId))
        .thenReturn(Arrays.asList(periodDto));
    when(periodService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(Arrays.asList(prePeriodDto, periodDto));
    when(processingPeriodExtensionRepository.findAll())
        .thenReturn(Arrays.asList(builder.buildPreExtenstion(), extension));

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, true));
    Requisition preRequisition = createRequisition(UUID.randomUUID(), RequisitionStatus.INITIATED,
        true);
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId,
        true)).thenReturn(Arrays.asList(preRequisition));
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId, true))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    List<Requisition> authorizedRequisitions = new ArrayList<>();
    authorizedRequisitions.add(createRequisition(requisitionId2, RequisitionStatus.AUTHORIZED,
        false));
    Set<String> status = newHashSet("AUTHORIZED", "IN_APPROVAL", "APPROVED",
        "RELEASED", "RELEASED_WITHOUT_ORDER");
    when(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(facilityId, programId,
        periodDto.getId(), false, status)).thenReturn(authorizedRequisitions);
    when(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(facilityId, programId,
        prePeriodDto.getId(), false, status)).thenReturn(authorizedRequisitions);

    //when
    List<RequisitionPeriodDto> response =
        siglusProcessingPeriodService.getPeriods(programId, facilityId, true)
            .stream().collect(Collectors.toList());

    //then
    assertEquals(1, response.size());
    assertEquals("perPeriod", response.get(0).getName());
  }

  @Test
  public void shouldGetProcessingPeriodsForEmergencyRequisitionWhenEmptyForPrePeriod() {
    //given
    setupReportType();
    ProcessingPeriodDto prePeriodDto = builder.buildPerDto();
    prePeriodDto.setId(UUID.randomUUID());
    when(periodService.getCurrentPeriods(programId, facilityId))
        .thenReturn(Arrays.asList(periodDto));
    when(periodService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(Arrays.asList(prePeriodDto, periodDto));
    when(processingPeriodExtensionRepository.findAll())
        .thenReturn(Arrays.asList(builder.buildPreExtenstion(), extension));

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED,
        true));
    when(requisitionRepository.searchRequisitions(prePeriodDto.getId(), facilityId, programId,
        true)).thenReturn(Collections.emptyList());
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId,
        true)).thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    List<Requisition> authorizedRequisitions = new ArrayList<>();
    authorizedRequisitions.add(createRequisition(requisitionId2, RequisitionStatus.AUTHORIZED,
        false));
    Set<String> status = newHashSet("AUTHORIZED", "IN_APPROVAL", "APPROVED",
        "RELEASED", "RELEASED_WITHOUT_ORDER");
    when(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(facilityId, programId,
        periodDto.getId(), false, status)).thenReturn(authorizedRequisitions);

    //when
    List<RequisitionPeriodDto> response =
        siglusProcessingPeriodService.getPeriods(programId, facilityId, true)
            .stream().collect(Collectors.toList());

    //then
    assertEquals(1, response.size());
    assertEquals(periodDto.getName(), response.get(0).getName());
  }

  @Test
  public void shouldGetProcessingPeriodsForEmergencyRequisitionWhenOnlyHaveOnePeriods() {
    //given
    setupReportType();
    List<ProcessingPeriodDto> periods = new ArrayList<>();
    periods.add(periodDto);
    when(periodService.getCurrentPeriods(programId, facilityId)).thenReturn(periods);
    when(periodService.searchByProgramAndFacility(programId, facilityId)).thenReturn(periods);

    List<ProcessingPeriodExtension> extensions = new ArrayList<>();
    extensions.add(extension);
    when(processingPeriodExtensionRepository.findAll()).thenReturn(extensions);

    List<Requisition> requisitions = new ArrayList<>();
    requisitions.add(createRequisition(requisitionId, RequisitionStatus.INITIATED, true));
    when(requisitionRepository.searchRequisitions(periodDto.getId(), facilityId, programId, true))
        .thenReturn(requisitions);

    when(permissionService.canInitRequisition(programId, facilityId))
        .thenReturn(ValidationResult.success());
    when(permissionService.canAuthorizeRequisition(any()))
        .thenReturn(ValidationResult.success());

    List<Requisition> authorizedRequisitions = new ArrayList<>();
    authorizedRequisitions.add(createRequisition(requisitionId2, RequisitionStatus.AUTHORIZED,
        false));
    when(siglusRequisitionRepository.searchAfterAuthorizedRequisitions(facilityId, programId,
        periodDto.getId(), false, newHashSet("AUTHORIZED", "IN_APPROVAL", "APPROVED",
            "RELEASED", "RELEASED_WITHOUT_ORDER")))
        .thenReturn(authorizedRequisitions);

    RequisitionPeriodDto requisitionPeriod = RequisitionPeriodDto.newInstance(fullDto);
    requisitionPeriod.setRequisitionId(requisitionId);
    requisitionPeriod.setRequisitionStatus(RequisitionStatus.INITIATED);
    requisitionPeriod.setCurrentPeriodRegularRequisitionAuthorized(true);

    //when
    Collection<RequisitionPeriodDto> response =
        siglusProcessingPeriodService.getPeriods(programId, facilityId, true);

    //then
    assertEquals(1, response.size());
    assertTrue(response.contains(requisitionPeriod));

  }

  private Requisition createRequisition(UUID requisitionId, RequisitionStatus status,
      boolean isEmergency) {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .build();
    Requisition requisition = new Requisition();
    requisition.setId(requisitionId);
    requisition.setProgramId(programId);
    requisition.setFacilityId(facilityId);
    requisition.setRequisitionLineItems(Lists.newArrayList(lineItem));
    requisition.setStatus(status);
    requisition.setEmergency(isEmergency);
    requisition.setCreatedDate(ZonedDateTime.now());
    return requisition;
  }

  private void setupReportType() {
    String programCode = "TARV";
    ProgramDto programDto = mock(ProgramDto.class);
    ReportType reportType = mock(ReportType.class);
    when(reportType.getStartDate()).thenReturn(LocalDate.of(2020, 1, 1));
    when(programDto.getCode()).thenReturn(programCode);
    when(siglusProgramService.getProgram(programId)).thenReturn(programDto);
    when(reportTypeRepository.findOneByFacilityIdAndProgramCodeAndActiveIsTrue(facilityId, programCode))
            .thenReturn(Optional.of(reportType));

    StockCardLineItem lineItem = new StockCardLineItem();
    lineItem.setOccurredDate(LocalDate.of(2020, 1, 1));
    StockCard stockCard = mock(StockCard.class);
    when(stockCard.getLineItems()).thenReturn(Arrays.asList(lineItem));
    when(stockCardRepository.findByProgramIdAndFacilityId(programId, facilityId))
            .thenReturn(Arrays.asList(stockCard));

    when(siglusStockCardLineItemRepository.findAllByStockCardIn(Arrays.asList(stockCard)))
            .thenReturn(Arrays.asList(lineItem));
  }
}


