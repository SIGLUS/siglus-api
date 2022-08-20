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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.FacilityLocations;
import org.siglus.siglusapi.dto.FacilityLocationsDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.repository.FacilityLocationsRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;

@RunWith(MockitoJUnitRunner.class)
public class SiglusLotLocationServiceTest extends TestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @InjectMocks
  private SiglusLotLocationService service;

  @Mock
  private FacilityLocationsRepository facilityLocationsRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  private final UUID facilityId = UUID.randomUUID();

  private final FacilityLocations facilityLocations = FacilityLocations.builder()
      .facilityId(facilityId)
      .area("A")
      .locationCode("AA25D")
      .build();

  @Before
  public void setup() {
    UserDto userDto = new UserDto();
    userDto.setHomeFacilityId(facilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }

  @Test
  public void shouldSearchLocationsByFacility() {
    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(newArrayList(facilityLocations));

    List<FacilityLocationsDto> facilityLocationsDtos = service.searchLocationsByFacility();

    assertEquals(facilityId, facilityLocationsDtos.get(0).getFacilityId());
    assertEquals("A", facilityLocationsDtos.get(0).getArea());
    assertEquals("AA25D", facilityLocationsDtos.get(0).getLocationCode());
  }

  @Test
  public void shouldThrowExceptionWhenFacilityLocationsNotFound() {
    exception.expect(NotFoundException.class);
    exception.expectMessage(ERROR_LOCATIONS_BY_FACILITY_NOT_FOUND);

    when(facilityLocationsRepository.findByFacilityId(facilityId)).thenReturn(Collections.emptyList());

    service.searchLocationsByFacility();
  }
}