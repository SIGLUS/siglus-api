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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.siglus.siglusapi.domain.UserReportView;
import org.siglus.siglusapi.dto.GeographicInfoDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.repository.SiglusUserReportViewRepository;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SiglusUserReportViewServiceTest {

  @InjectMocks
  private SiglusUserReportViewService siglusUserReportViewService;

  @Mock
  private SiglusUserReportViewRepository siglusUserReportViewRepository;

  @Mock
  private SiglusGeographicInfoRepository siglusGeographicInfoRepository;

  @Mock
  private SiglusAuthenticationHelper siglusAuthenticationHelper;

  @Mock
  private SiglusUserReferenceDataService userService;

  private final String roleReportViewerId = "a598b9b4-1dd8-11ed-84e1-acde48001122";

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    ReflectionTestUtils.setField(siglusUserReportViewService, "roleReportViewerId",
        "a598b9b4-1dd8-11ed-84e1-acde48001122");
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowErrorWhenCurrentUserIsNotPresent() {
    // given
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.empty());

    // when
    siglusUserReportViewService.getReportViewGeographicInfo(UUID.randomUUID());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowErrorWhenCurrentUserIsNotAdmin() {
    // given
    UUID currentUserId = UUID.randomUUID();
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
    when(siglusAuthenticationHelper.isTheCurrentUserAdmin()).thenReturn(false);

    // when
    siglusUserReportViewService.getReportViewGeographicInfo(UUID.randomUUID());
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowErrorWhenUserIdIsNull() {
    // given
    UUID currentUserId = UUID.randomUUID();
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
    when(siglusAuthenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);

    // when
    siglusUserReportViewService.getReportViewGeographicInfo(null);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowErrorWhenUserNotFound() {
    // given
    UUID currentUserId = UUID.randomUUID();
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
    when(siglusAuthenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    UUID userId = UUID.randomUUID();
    when(userService.findOne(userId)).thenReturn(null);

    // when
    siglusUserReportViewService.getReportViewGeographicInfo(userId);
  }

  @Test(expected = BusinessDataException.class)
  public void shouldThrowErrorWhenUserNotReportViewer() {
    // given
    UUID currentUserId = UUID.randomUUID();
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
    when(siglusAuthenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
    roleAssignmentDto.setRoleId(UUID.randomUUID());
    Set<RoleAssignmentDto> roleAssignmentDtos = new HashSet<>();
    roleAssignmentDtos.add(roleAssignmentDto);
    userDto.setRoleAssignments(roleAssignmentDtos);
    when(userService.findOne(userId)).thenReturn(userDto);
    // when
    siglusUserReportViewService.getReportViewGeographicInfo(userId);
  }

  @Test
  public void shouldReturnGeographicInfoDtoWhenUserCheckIsRight() {
    // given
    UUID currentUserId = UUID.randomUUID();
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
    when(siglusAuthenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
    roleAssignmentDto.setRoleId(UUID.fromString(roleReportViewerId));
    Set<RoleAssignmentDto> roleAssignmentDtos = new HashSet<>();
    roleAssignmentDtos.add(roleAssignmentDto);
    userDto.setRoleAssignments(roleAssignmentDtos);
    when(userService.findOne(userId)).thenReturn(userDto);

    UUID provinceId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    UserReportView build1 = UserReportView.builder()
        .userId(userId)
        .provinceId(provinceId)
        .build();
    UserReportView build2 = UserReportView.builder()
        .userId(userId)
        .districtId(districtId)
        .build();
    List<UserReportView> userReportViews = Arrays.asList(build1, build2);
    when(siglusUserReportViewRepository.findAllByUserId(userId)).thenReturn(userReportViews);
    Set<UUID> provinceIds = new HashSet<>();
    provinceIds.add(provinceId);
    GeographicZone geographicZone = new GeographicZone();
    geographicZone.setId(provinceId);
    geographicZone.setName("province");
    Set<GeographicZone> provinces = new HashSet<>();
    provinces.add(geographicZone);
    when(siglusGeographicInfoRepository.findAllByIdIn(provinceIds)).thenReturn(provinces);
    Set<UUID> districtIds = new HashSet<>();
    districtIds.add(districtId);
    GeographicZone geographicZone2 = new GeographicZone();
    geographicZone2.setId(districtId);
    geographicZone2.setName("district");
    Set<GeographicZone> districts = new HashSet<>();
    districts.add(geographicZone2);
    when(siglusGeographicInfoRepository.findAllByIdIn(districtIds)).thenReturn(districts);

    // when
    List<GeographicInfoDto> geographicInfos = siglusUserReportViewService.getReportViewGeographicInfo(userId);

    // then
    GeographicInfoDto provinceDto = GeographicInfoDto.builder()
        .provinceId(provinceId)
        .provinceName("province")
        .build();
    GeographicInfoDto districtDto = GeographicInfoDto.builder()
        .provinceId(districtId)
        .provinceName("district")
        .build();
    List<GeographicInfoDto> geographicInfoDtos = Arrays.asList(provinceDto, districtDto);
    Assert.assertEquals(geographicInfos.size(), geographicInfoDtos.size());
  }

  @Test
  public void shouldSaveUserReportViewWhenUserAndGeographicInfoRight() {
    // given
    UUID currentUserId = UUID.randomUUID();
    when(siglusAuthenticationHelper.getCurrentUserId()).thenReturn(Optional.of(currentUserId));
    when(siglusAuthenticationHelper.isTheCurrentUserAdmin()).thenReturn(true);
    UUID userId = UUID.randomUUID();
    UserDto userDto = new UserDto();
    userDto.setId(userId);
    RoleAssignmentDto roleAssignmentDto = new RoleAssignmentDto();
    roleAssignmentDto.setRoleId(UUID.fromString(roleReportViewerId));
    Set<RoleAssignmentDto> roleAssignmentDtos = new HashSet<>();
    roleAssignmentDtos.add(roleAssignmentDto);
    userDto.setRoleAssignments(roleAssignmentDtos);
    when(userService.findOne(userId)).thenReturn(userDto);

    UUID provinceId = UUID.randomUUID();
    UUID districtId = UUID.randomUUID();
    GeographicInfoDto geographicInfoDto = GeographicInfoDto.builder()
        .provinceId(provinceId)
        .districtId(districtId)
        .build();
    when(siglusGeographicInfoRepository.getGeographicInfo()).thenReturn(Collections.singletonList(geographicInfoDto));
    List<GeographicInfoDto> geographicInfoDtos = new ArrayList<>();
    GeographicInfoDto geographicInfoDto1 = GeographicInfoDto.builder()
        .provinceId(provinceId)
        .build();
    GeographicInfoDto geographicInfoDto2 = GeographicInfoDto.builder()
        .districtId(districtId)
        .build();
    geographicInfoDtos.add(geographicInfoDto1);
    geographicInfoDtos.add(geographicInfoDto2);

    // when
    siglusUserReportViewService.saveReportViewGeographicInfo(userId, geographicInfoDtos);

    // then
    verify(siglusUserReportViewRepository, times(1)).deleteAllByUserId(userId);
  }
}
