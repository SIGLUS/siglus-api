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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.siglus.siglusapi.domain.MetaBaseConfig;
import org.siglus.siglusapi.domain.UserReportView;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityGeographicInfoDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.MetabaseDashboardRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusUserReportViewRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;


@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class MetabaseDashboardServiceTest {

  @InjectMocks
  private MetabaseDashboardService siglusMetabaseDashboardService;

  @Mock
  private MetabaseDashboardRepository metabaseDashboardRepository;

  @Mock
  private SiglusAuthenticationHelper authenticationHelper;

  @Mock
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  private final UserDto uesrDto = new UserDto();

  private final Set<RoleAssignmentDto> roleAssignmentDtos = new HashSet<>();

  private final RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();

  private final FacilityTypeDto facilityTypeDto = new FacilityTypeDto();

  private final String payloadTemplate = "{\"resource\": {\"dashboard\": %d},\"params\": {%s}}";
  private final String facilityCode = "10000";
  private static final UUID ALL_GEOGRAPHIC_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
  @Mock
  private SiglusUserReportViewRepository siglusUserReportViewRepository;
  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;

  @Before
  public void setUp() {
    roleAssignmentDto.setRoleId(UUID.randomUUID());
    roleAssignmentDtos.add(roleAssignmentDto);
    uesrDto.setRoleAssignments(roleAssignmentDtos);
  }

  @Test
  public void shouldReturnEmptyParamsInPayloadWhenAccountRoleIsAdmin() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    when(metabaseDashboardRepository.findByDashboardName(any())).thenReturn(
        Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format(payloadTemplate, 1, ""), payload);
  }

  @Test
  public void shouldReturnProvinceParamsInPayloadWhenAccountRoleIsDpm() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    when(metabaseDashboardRepository.findByDashboardName(any())).thenReturn(
        Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    facilityTypeDto.setCode("DPM");
    when(siglusFacilityReferenceDataService.findOne((UUID) any())).thenReturn(FacilityDto.builder()
        .type(facilityTypeDto).code(facilityCode).build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("PROVINCE");
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format(payloadTemplate, 1, "\"province_facility_code\": \"10000\""), payload);
  }

  @Test
  public void shouldReturnDistrictParamsInPayloadWhenAccountRoleIsDdm() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    when(metabaseDashboardRepository.findByDashboardName(any())).thenReturn(
        Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    facilityTypeDto.setCode("DDM");
    when(siglusFacilityReferenceDataService.findOne((UUID) any())).thenReturn(FacilityDto.builder()
        .type(facilityTypeDto).code(facilityCode).build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("DISTRICT");
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format(payloadTemplate, 1, "\"district_facility_code\": \"10000\""), payload);
  }


  @Test
  public void shouldReturnFacilityParamsInPayloadWhenAccountRoleIsOthers() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    when(metabaseDashboardRepository.findByDashboardName(any())).thenReturn(
        Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    facilityTypeDto.setCode("CS");
    when(siglusFacilityReferenceDataService.findOne((UUID) any())).thenReturn(FacilityDto.builder()
        .type(facilityTypeDto).code(facilityCode).build());
    when(authenticationHelper.getFacilityGeographicZoneLevel()).thenReturn("SITE");
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format(payloadTemplate, 1, "\"facility_code\": \"10000\""), payload);
  }

  @Test
  public void shouldReturnEmptyParamMapWhenGeographicInfoIsEmpty() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    when(authenticationHelper.isTheCurrentUserReportViewer()).thenReturn(true);
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));
    when(siglusUserReportViewRepository.findAllByUserId(any())).thenReturn(new ArrayList<>());
    when(metabaseDashboardRepository.findByDashboardName(any()))
        .thenReturn(Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    facilityTypeDto.setCode("CS");
    when(siglusFacilityReferenceDataService.findOne((UUID) any())).thenReturn(FacilityDto.builder()
        .type(facilityTypeDto).code(facilityCode).build());
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format("{\"resource\":{\"dashboard\":%d},\"params\":{%s}}", 1, ""),
        payload);
  }

  @Test
  public void shouldReturnEmptyParamMapWhenHasAllReportItem() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    when(authenticationHelper.isTheCurrentUserReportViewer()).thenReturn(true);
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));
    UserReportView dto = UserReportView.builder()
        .provinceId(ALL_GEOGRAPHIC_UUID)
        .districtId(ALL_GEOGRAPHIC_UUID)
        .build();
    when(siglusUserReportViewRepository.findAllByUserId(any())).thenReturn(
        Collections.singletonList(dto));

    when(metabaseDashboardRepository.findByDashboardName(any()))
        .thenReturn(Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    facilityTypeDto.setCode("CS");
    when(siglusFacilityReferenceDataService.findOne((UUID) any())).thenReturn(FacilityDto.builder()
        .type(facilityTypeDto).code(facilityCode).build());
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format("{\"resource\":{\"dashboard\":%d},\"params\":{%s}}", 1, ""),
        payload);
  }

  @Test
  public void shouldReturnParamMapWhenHasPartialReportItem() {
    // given
    when(authenticationHelper.getCurrentUser()).thenReturn(uesrDto);
    when(authenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);
    when(authenticationHelper.isTheCurrentUserReportViewer()).thenReturn(true);
    when(authenticationHelper.getCurrentUserId()).thenReturn(Optional.of(UUID.randomUUID()));
    UUID provinceId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    UserReportView dto = UserReportView.builder()
        .provinceId(provinceId)
        .districtId(districtId)
        .build();
    when(siglusUserReportViewRepository.findAllByUserId(any())).thenReturn(
        Collections.singletonList(dto));
    FacilityGeographicInfoDto facilityGeographicInfoDto = FacilityGeographicInfoDto.builder()
        .provinceId(provinceId)
        .districtId(districtId)
        .facilityCode(facilityCode)
        .build();
    when(siglusFacilityRepository.getAllFacilityGeographicInfo()).thenReturn(
        Collections.singletonList(facilityGeographicInfoDto));

    when(metabaseDashboardRepository.findByDashboardName(any()))
        .thenReturn(Optional.of(MetaBaseConfig.builder().dashboardId(1).build()));
    facilityTypeDto.setCode("CS");
    when(siglusFacilityReferenceDataService.findOne((UUID) any())).thenReturn(FacilityDto.builder()
        .type(facilityTypeDto).code(facilityCode).build());
    // when
    String payload = siglusMetabaseDashboardService.getPayloadByDashboardName(anyString());
    // then
    assertEquals(String.format("{\"resource\":{\"dashboard\":%d},\"params\":{%s}}", 1, "\"facility_code\":[\"10000\"]"),
        payload);
  }

}
