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

package org.openlmis.referencedata.service;

import static java.util.Arrays.asList;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.referencedata.service.FacilityTypeService.ACTIVE;

import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.testbuilder.FacilityTypeDataBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class FacilityTypeServiceTest {

  private static final String ID = "id";

  @Mock
  private FacilityTypeRepository facilityTypeRepository;

  @InjectMocks
  private FacilityTypeService facilityTypeService = new FacilityTypeService();

  private Pageable pageable;
  private FacilityType facilityType1;
  private FacilityType facilityType2;
  private Page<FacilityType> facilityTypePage;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    pageable = new PageRequest(0, 10);

    facilityType1 = new FacilityTypeDataBuilder().build();
    facilityType2 = new FacilityTypeDataBuilder().build();

    facilityTypePage = new PageImpl<>(asList(facilityType1, facilityType2));
  }

  @Test
  public void shouldReturnAllFacilityTypesIfParamsAreEmpty() {
    when(facilityTypeRepository.findAll(pageable)).thenReturn(facilityTypePage);

    Page<FacilityType> result = facilityTypeService.search(new LinkedMultiValueMap<>(), pageable);

    verify(facilityTypeRepository).findAll(pageable);
    assertEquals(facilityTypePage, result);
  }

  @Test
  public void shouldReturnAllFacilityTypesIfParamsAreIncorrect() {
    when(facilityTypeRepository.findAll(pageable)).thenReturn(facilityTypePage);

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add("someParam", "someValue");

    Page<FacilityType> result = facilityTypeService.search(params, pageable);

    verify(facilityTypeRepository).findAll(pageable);
    assertEquals(facilityTypePage, result);
  }

  @Test
  public void shouldReturnActiveFacilityTypes() {
    when(facilityTypeRepository.findByActive(true, pageable)).thenReturn(facilityTypePage);

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(ACTIVE, "true");

    Page<FacilityType> result = facilityTypeService.search(params, pageable);

    verify(facilityTypeRepository).findByActive(true, pageable);
    assertEquals(facilityTypePage, result);
  }

  @Test
  public void shouldReturnFacilityTypesByIds() {
    Set<UUID> facilityTypeIds = asSet(facilityType1.getId(), facilityType2.getId());
    when(facilityTypeRepository.findByIdIn(facilityTypeIds, pageable))
        .thenReturn(facilityTypePage);

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(ID, facilityType1.getId().toString());
    params.add(ID, facilityType2.getId().toString());

    Page<FacilityType> result = facilityTypeService.search(params, pageable);

    verify(facilityTypeRepository).findByIdIn(facilityTypeIds, pageable);
    assertEquals(facilityTypePage, result);
  }

  @Test
  public void shouldFindFacilityTypesByAllParameters() {
    Set<UUID> facilityTypeIds = asSet(facilityType1.getId(), facilityType2.getId());
    when(facilityTypeRepository.findByIdInAndActive(facilityTypeIds, true, pageable))
        .thenReturn(facilityTypePage);

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(ACTIVE, "true");
    params.add(ID, facilityType1.getId().toString());
    params.add(ID, facilityType2.getId().toString());

    Page<FacilityType> result = facilityTypeService.search(params, pageable);

    verify(facilityTypeRepository).findByIdInAndActive(facilityTypeIds, true, pageable);
    assertEquals(facilityTypePage, result);
  }
}
