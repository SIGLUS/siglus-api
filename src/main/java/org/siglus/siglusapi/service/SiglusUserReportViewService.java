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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CURRENT_USER_NOT_ADMIN_USER;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_REPORT_VIEWER_USER;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.siglus.siglusapi.domain.UserReportView;
import org.siglus.siglusapi.dto.GeographicInfoDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.repository.SiglusUserReportViewRepository;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
public class SiglusUserReportViewService {

  @Autowired
  private SiglusUserReportViewRepository siglusUserReportViewRepository;
  @Autowired
  private SiglusGeographicInfoRepository siglusGeographicInfoRepository;
  @Autowired
  private SiglusAuthenticationHelper siglusAuthenticationHelper;
  @Autowired
  private SiglusUserReferenceDataService userService;
  @Value("${role.report.viewer.id}")
  private String roleReportViewerId;

  public List<GeographicInfoDto> getReportViewGeographicInfo(UUID userId) {
    checkUser(userId);
    List<UserReportView> userReportViews = siglusUserReportViewRepository.findAllByUserId(userId);
    Set<UUID> provinceIds = userReportViews.stream()
        .map(UserReportView::getProvinceId)
        .filter(provinceId -> !ObjectUtils.isEmpty(provinceId))
        .collect(Collectors.toSet());
    Set<UUID> districtIds = userReportViews.stream()
        .map(UserReportView::getDistrictId)
        .filter(districtId -> !ObjectUtils.isEmpty(districtId))
        .collect(Collectors.toSet());
    Map<UUID, String> provinceIdToNameMap = siglusGeographicInfoRepository.findAllByIdIn(provinceIds).stream()
        .collect(Collectors.toMap(GeographicZone::getId, GeographicZone::getName));
    Map<UUID, String> districtToNameMap = siglusGeographicInfoRepository.findAllByIdIn(districtIds).stream()
        .collect(Collectors.toMap(GeographicZone::getId, GeographicZone::getName));
    return userReportViews.stream().map(userReportView ->
        GeographicInfoDto.builder()
            .provinceId(userReportView.getProvinceId())
            .provinceName(provinceIdToNameMap.get(userReportView.getProvinceId()))
            .districtId(userReportView.getDistrictId())
            .districtName(districtToNameMap.get(userReportView.getDistrictId()))
            .build()
    ).collect(Collectors.toList());
  }

  private void checkUser(UUID userId) {
    Optional<UUID> userIdOptional = siglusAuthenticationHelper.getCurrentUserId();
    if (!userIdOptional.isPresent() || !siglusAuthenticationHelper.isTheCurrentUserAdmin()) {
      throw new BusinessDataException(new Message(ERROR_CURRENT_USER_NOT_ADMIN_USER));
    }
    if (ObjectUtils.isEmpty(userId)) {
      throw new BusinessDataException(new Message(ERROR_USER_NOT_REPORT_VIEWER_USER));
    }
    UserDto userDto = userService.findOne(userId);
    if (ObjectUtils.isEmpty(userDto)) {
      throw new BusinessDataException(new Message(ERROR_USER_NOT_FOUND));
    }
    Set<UUID> roleIds = userDto.getRoleAssignments().stream().map(RoleAssignmentDto::getRoleId)
        .collect(Collectors.toSet());
    if (!roleIds.contains(UUID.fromString(roleReportViewerId))) {
      throw new BusinessDataException(new Message(ERROR_USER_NOT_REPORT_VIEWER_USER));
    }
  }

  @Transactional
  public void saveReportViewGeographicInfo(UUID userId, List<GeographicInfoDto> geographicInfoDtos) {
    checkUser(userId);
    List<GeographicInfoDto> geographicInfos = siglusGeographicInfoRepository.getGeographicInfo();
    Set<UUID> provinceIds = geographicInfos.stream().map(GeographicInfoDto::getProvinceId).collect(Collectors.toSet());
    Set<UUID> districtIds = geographicInfos.stream().map(GeographicInfoDto::getDistrictId).collect(Collectors.toSet());
    checkGeographicInfoDto(geographicInfoDtos, provinceIds, districtIds);
    List<UserReportView> userReportViews = geographicInfoDtos.stream().map(geographicInfoDto -> UserReportView.builder()
        .userId(userId)
        .provinceId(geographicInfoDto.getProvinceId())
        .districtId(geographicInfoDto.getDistrictId())
        .build()).collect(Collectors.toList());
    siglusUserReportViewRepository.deleteAllByUserId(userId);
    siglusUserReportViewRepository.save(userReportViews);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private void checkGeographicInfoDto(List<GeographicInfoDto> geographicInfoDtos, Set<UUID> provinceIds,
      Set<UUID> districtIds) {
    for (GeographicInfoDto geographicInfoDto : geographicInfoDtos) {
      if (!ObjectUtils.isEmpty(geographicInfoDto.getDistrictId())
          && !ObjectUtils.isEmpty(geographicInfoDto.getProvinceId())) {
        throw new BusinessDataException(new Message(ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID));
      }
      if (ObjectUtils.isEmpty(geographicInfoDto.getDistrictId())
          && ObjectUtils.isEmpty(geographicInfoDto.getProvinceId())
          && geographicInfoDtos.size() > 1) {
        throw new BusinessDataException(new Message(ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID));
      }
      if (!ObjectUtils.isEmpty(geographicInfoDto.getProvinceId())
          && !provinceIds.contains(geographicInfoDto.getProvinceId())) {
        throw new BusinessDataException(new Message(ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID));
      }
      if (!ObjectUtils.isEmpty(geographicInfoDto.getDistrictId())
          && !districtIds.contains(geographicInfoDto.getDistrictId())) {
        throw new BusinessDataException(new Message(ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID));
      }
    }
  }
}
