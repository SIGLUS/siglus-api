package org.siglus.siglusapi.util;

import static org.junit.Assert.*;
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