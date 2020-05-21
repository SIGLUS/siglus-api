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

package org.openlmis.stockmanagement.service;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.stockmanagement.dto.referencedata.FacilityDto;
import org.openlmis.stockmanagement.dto.referencedata.FacilityTypeDto;
import org.openlmis.stockmanagement.dto.referencedata.SupportedProgramDto;
import org.openlmis.stockmanagement.dto.referencedata.UserDto;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.service.referencedata.FacilityReferenceDataService;
import org.openlmis.stockmanagement.util.AuthenticationHelper;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SecurityContextHolder.class)
public class HomeFacilityPermissionServiceTest {

  @InjectMocks
  private HomeFacilityPermissionService homeFacilityPermissionService;

  @Mock
  private AuthenticationHelper authenticationHelper;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private OAuth2Authentication authentication;
  
  @Mock
  private FacilityReferenceDataService facilityService;
  
  private UUID homeFacilityId;
  private UUID programId;
  private UUID facilityTypeId;
  private UserDto userDto;
  private FacilityDto homeFacilityDto;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(SecurityContextHolder.class);
    PowerMockito.when(SecurityContextHolder.getContext()).thenReturn(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isClientOnly()).thenReturn(false);
    homeFacilityId = randomUUID();
    programId = randomUUID();
    facilityTypeId = randomUUID();
    mockUserDto();
    mockFacilityDto();
  }

  @Test(expected = PermissionMessageException.class)
  public void shouldThrowExceptionWhenProgramIsNotSupportedByTheFacility() throws Exception {

    homeFacilityPermissionService.checkProgramSupported(randomUUID());
  }

  @Test
  public void shouldPassValidationIfProgramIsSupported() throws Exception {

    homeFacilityPermissionService.checkProgramSupported(programId);
  }
  
  private void mockUserDto() {
    userDto = mock(UserDto.class);
    when(userDto.getHomeFacilityId()).thenReturn(homeFacilityId);
    when(authenticationHelper.getCurrentUser()).thenReturn(userDto);
  }
  
  private void mockFacilityDto() {
    homeFacilityDto = mock(FacilityDto.class);

    SupportedProgramDto supportedProgramDto = new SupportedProgramDto();
    supportedProgramDto.setId(programId);
    when(homeFacilityDto.getSupportedPrograms())
        .thenReturn(Collections.singletonList(supportedProgramDto));

    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(facilityTypeId);
    when(homeFacilityDto.getType()).thenReturn(facilityTypeDto);

    when(facilityService.findOne(homeFacilityId)).thenReturn(homeFacilityDto);
  }
}