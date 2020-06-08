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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.openlmis.stockmanagement.util.PageImplRepresentation;
import org.siglus.common.domain.ProcessingPeriodExtension;
import org.siglus.common.repository.ProcessingPeriodExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusProcessingPeriodReferenceDataService;
import org.siglus.siglusapi.testutils.ProcessingPeriodDtoDataBuilder;
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
  private RequisitionAuthenticationHelper requisitionAuthenticationHelper;

  @Mock
  private PermissionService permissionService;

  @Mock
  private PeriodService periodService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private RequisitionService requisitionService;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusProcessingPeriodService  siglusProcessingPeriodService;

  private ProcessingPeriodDtoDataBuilder builder = new ProcessingPeriodDtoDataBuilder();
  private ProcessingPeriodDto fullDto = builder.buildFullDto();
  private ProcessingPeriodDto dto = builder.buildDto();
  private ProcessingPeriodExtension extension = builder.buildExtenstion();

  private UUID processingScheduleId = UUID.fromString("727bef28-de1c-11e9-8785-0242ac130007");
  private int page = 0;
  private int size = 10;
  private Sort sort = new Sort("startDate");

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
    processingPeriodDtos.add(dto);

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
    UUID periodId = dto.getId();

    when(siglusProcessingPeriodReferenceDataService.findOne(periodId)).thenReturn(dto);
    when(processingPeriodExtensionRepository
        .findByProcessingPeriodId(periodId)).thenReturn(extension);

    ProcessingPeriodDto response = siglusProcessingPeriodService.getProcessingPeriod(periodId);

    verify(siglusProcessingPeriodReferenceDataService).findOne(periodId);
    verify(processingPeriodExtensionRepository).findByProcessingPeriodId(periodId);
    assertEquals(response, fullDto);
  }

}


