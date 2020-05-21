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

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.web.FacilitySearchParams;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SuppressWarnings("PMD.TooManyMethods")
public class FacilityServiceTest {

  private static final String ID = "id";
  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String FACILITY_TYPE_CODE = "type";
  private static final String ZONE_ID = "zoneId";
  private static final String RECURSE = "recurse";
  private static final String FACILITY_NAME = "Facility";
  private static final String FACILITY_TYPE = "facility-type";
  private static final String FACILITY_CODE = "FAC1";


  @Mock
  private GeographicZoneRepository geographicZoneRepository;

  @Mock
  private GeographicZoneService geographicZoneService;

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private FacilityTypeRepository facilityTypeRepository;

  @Mock
  private Facility facility;

  @Mock
  private Facility facility2;

  private UUID facility1Id = UUID.randomUUID();
  private UUID facility2Id = UUID.randomUUID();
  private UUID parentId = UUID.randomUUID();
  private UUID childId = UUID.randomUUID();
  private UUID childOfChildId = UUID.randomUUID();
  private List<Facility> facilityList;

  @InjectMocks
  private FacilityService facilityService = new FacilityService();

  private PageRequest pageable = new PageRequest(0, Integer.MAX_VALUE);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    facilityList = Lists.newArrayList(facility, facility2);
    when(facility.getId()).thenReturn(facility1Id);
    when(facility2.getId()).thenReturn(facility2Id);

    when(facilityTypeRepository.existsByCode(FACILITY_TYPE)).thenReturn(true);
    when(geographicZoneRepository.exists(parentId)).thenReturn(true);
  }

  @Test
  public void shouldReturnFacilitiesIfZoneCodeAndNameNotProvidedForSearch() {
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(CODE, FACILITY_CODE);
    params.add(FACILITY_TYPE_CODE, FACILITY_TYPE);
    params.add(ID, Sets.newHashSet(facility1Id, facility2Id));
    params.add(RECURSE, false);

    FacilitySearchParams searchParams = new FacilitySearchParams(params);

    when(facilityRepository
        .search(searchParams, emptySet(), null, pageable))
        .thenReturn(Pagination.getPage(facilityList, pageable, 2));

    List<Facility> actual = facilityService.searchFacilities(searchParams, pageable).getContent();

    assertEquals(facilityList, actual);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldOnlyThrowValidationExceptionIfQueryMapCantBeParsed() {
    when(facilityRepository.findAll(pageable)).thenReturn(
            Pagination.getPage(facilityList, pageable, 2));

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(CODE, "-1");
    params.add(NAME, "-1");
    params.add(ZONE_ID, "a");
    params.add(RECURSE, "a");
    facilityService.searchFacilities(new FacilitySearchParams(params), pageable);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfGeographicZoneDoesNotExist() {
    when(geographicZoneRepository.exists(parentId)).thenReturn(false);

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(ZONE_ID, UUID.randomUUID().toString());
    facilityService.searchFacilities(new FacilitySearchParams(params), pageable);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfFacilityTypeDoesNotExist() {
    when(facilityTypeRepository.existsByCode(any(String.class))).thenReturn(false);
    PageRequest pageable = new PageRequest(0, 10, new Sort("name"));

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(ZONE_ID, UUID.randomUUID().toString());
    facilityService.searchFacilities(new FacilitySearchParams(params), pageable);
  }

  @Test
  public void shouldReturnAllElementsIfNoSearchCriteriaProvided() {
    FacilitySearchParams searchParams = new FacilitySearchParams(new LinkedMultiValueMap<>());

    when(facilityRepository
        .search(searchParams, emptySet(), null, pageable))
        .thenReturn(Pagination.getPage(facilityList, pageable, 2));

    List<Facility> actual = facilityService
        .searchFacilities(new FacilitySearchParams(new LinkedMultiValueMap<>()), pageable)
            .getContent();
    assertEquals(facilityList, actual);
  }

  @Test
  public void shouldReturnAllElementsIfOnlyIdsAreProvided() {
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(ID, facility1Id.toString());
    params.add(ID, facility2Id.toString());

    FacilitySearchParams searchParams = new FacilitySearchParams(params);

    when(facilityRepository
        .search(searchParams, emptySet(), null, pageable))
        .thenReturn(Pagination.getPage(facilityList, pageable, 2));

    List<Facility> actual = facilityService
        .searchFacilities(searchParams, pageable)
        .getContent();

    assertEquals(2, actual.size());
    assertEquals(facilityList, actual);
  }

  @Test
  public void shouldSearchForFacilitiesInChildZonesIfRecurseOptionProvided() {
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(RECURSE, true);
    params.add(CODE, FACILITY_CODE);
    params.add(NAME, FACILITY_NAME);
    params.add(FACILITY_TYPE_CODE, FACILITY_TYPE);
    params.add(ZONE_ID, parentId.toString());
    params.add(ID, facility1Id.toString());
    params.add(ID, facility2Id.toString());

    FacilitySearchParams searchParams = new FacilitySearchParams(params);

    prepareForSearchWithRecurse(searchParams);

    final List<Facility> actual = facilityService
        .searchFacilities(searchParams, pageable).getContent();

    verifyAfterSearchWithRecurse(actual, searchParams);
  }

  @Test
  public void shouldSearchForFacilitiesInParentZoneOnlyIfRecurseOptionIsOff() {
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(RECURSE, false);
    params.add(CODE, FACILITY_CODE);
    params.add(NAME, FACILITY_NAME);
    params.add(FACILITY_TYPE_CODE, FACILITY_TYPE);
    params.add(ZONE_ID, parentId.toString());
    params.add(ID, facility1Id.toString());
    params.add(ID, facility2Id.toString());

    prepareForSearchWithoutRecurse(params);

    List<Facility> actual = facilityService.searchFacilities(
        new FacilitySearchParams(params), pageable).getContent();

    verifyAfterSearchWithoutRecurse(actual, params);
  }

  @Test
  public void shouldSearchForFacilitiesWithExtraData() {
    final String code = "FAC1";
    final String name = "Facility";

    when(geographicZoneService.getAllZonesInHierarchy(parentId))
        .thenReturn(Sets.newHashSet(childId, childOfChildId));

    Map<String, String> extraData = new HashMap<>();
    extraData.put("type", "rural");

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add(RECURSE, true);
    params.add(CODE, code);
    params.add(NAME, name);
    params.add(FACILITY_TYPE_CODE, FACILITY_TYPE);
    params.add(ZONE_ID, parentId.toString());
    params.add("extraData", extraData);

    when(facilityRepository
        .search(new FacilitySearchParams(params), of(parentId, childId, childOfChildId),
            "{\"type\":\"rural\"}", pageable))
        .thenReturn(Pagination.getPage(Lists.newArrayList(facility2), pageable, 1));

    List<Facility> actual = facilityService.searchFacilities(
        new FacilitySearchParams(params), pageable).getContent();

    assertEquals(1, actual.size());
    assertThat(actual, hasItem(facility2));
  }

  private void prepareForSearchWithRecurse(FacilitySearchParams params) {
    when(geographicZoneService.getAllZonesInHierarchy(parentId))
        .thenReturn(Sets.newHashSet(childId, childOfChildId));

    when(facilityRepository
        .search(params, of(parentId, childId, childOfChildId), null, pageable))
        .thenReturn(Pagination.getPage(Lists.newArrayList(facility, facility2), pageable, 2));
  }

  private void verifyAfterSearchWithRecurse(List<Facility> actual, FacilitySearchParams params) {
    verify(facilityRepository)
        .search(params, of(parentId, childId, childOfChildId), null, pageable);

    assertEquals(2, actual.size());
    assertThat(actual, hasItem(facility));
    assertThat(actual, hasItem(facility2));
  }

  private void prepareForSearchWithoutRecurse(MultiValueMap<String, Object> params) {
    when(facilityRepository
        .search(new FacilitySearchParams(params), of(parentId), null, pageable))
        .thenReturn(Pagination.getPage(Lists.newArrayList(facility), pageable, 1));
  }

  private void verifyAfterSearchWithoutRecurse(List<Facility> actual,
      MultiValueMap<String, Object> params) {
    verify(facilityRepository)
        .search(new FacilitySearchParams(params), of(parentId), null, pageable);
    verifyNoMoreInteractions(facilityRepository);

    assertEquals(1, actual.size());
    assertThat(actual, hasItem(facility));
  }
}
