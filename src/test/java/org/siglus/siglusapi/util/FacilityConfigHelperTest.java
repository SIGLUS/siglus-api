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

package org.siglus.siglusapi.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;

@RunWith(MockitoJUnitRunner.class)
public class FacilityConfigHelperTest {

  @InjectMocks
  private FacilityConfigHelper facilityConfigHelper;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private FacilityExtensionRepository facilityExtensionRepository;

  private final UUID facilityId = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto user = mock(UserDto.class);
    when(authHelper.getCurrentUser()).thenReturn(user);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
  }

  @Test
  public void shouldReturnFalseIfFacilityExtensionNotConfigured() {
    // given
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);

    // when
    boolean isLocationManagement = facilityConfigHelper.isLocationManagement();

    // then
    assertFalse(isLocationManagement);
  }

  @Test
  public void shouldReturnFalseIfEnableLocationManagementIsNullWhenCheckIsLocationManagement() {
    // given
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .enableLocationManagement(null)
        .build();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);

    // when
    boolean isLocationManagement = facilityConfigHelper.isLocationManagement();

    // then
    assertFalse(isLocationManagement);
  }

  @Test
  public void shouldReturnFalseIfEnableLocationManagementIsFalseWhenCheckIsLocationManagement() {
    // given
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .enableLocationManagement(false)
        .build();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);

    // when
    boolean isLocationManagement = facilityConfigHelper.isLocationManagement();

    // then
    assertFalse(isLocationManagement);
  }

  @Test
  public void shouldReturnTrueIfEnableLocationManagementIsFalseWhenCheckIsLocationManagement() {
    // given
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .enableLocationManagement(true)
        .build();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);

    // when
    boolean isLocationManagement = facilityConfigHelper.isLocationManagement();

    // then
    assertTrue(isLocationManagement);
  }

  @Test
  public void shouldReturnTrueIfEnableLocationManagementIsFalseWhenCheckIsStockManagement() {
    // given
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .enableLocationManagement(false)
        .build();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);

    // when
    boolean isStockManagement = facilityConfigHelper.isStockManagement();

    // then
    assertTrue(isStockManagement);
  }

  @Test
  public void shouldReturnFalseIfIsTrustedClientWhenCheckIsStockManagement() {
    // given
    when(authHelper.getCurrentUser()).thenReturn(null);
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .enableLocationManagement(false)
        .build();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);

    // when
    boolean isStockManagement = facilityConfigHelper.isStockManagement();

    // then
    assertFalse(isStockManagement);
  }

  @Test
  public void shouldReturnFalseIfIsTrustedClientWhenCheckIsLocationManagement() {
    // given
    when(authHelper.getCurrentUser()).thenReturn(null);
    FacilityExtension facilityExtension = FacilityExtension.builder()
        .facilityId(facilityId)
        .enableLocationManagement(true)
        .build();
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(facilityExtension);

    // when
    boolean isLocationManagement = facilityConfigHelper.isLocationManagement();

    // then
    assertFalse(isLocationManagement);
  }
}