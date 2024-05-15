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

import static org.siglus.siglusapi.constant.FieldConstants.ALL_GEOGRAPHIC_UUID;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CURRENT_USER_NOT_ADMIN_USER;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_FOUND;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_REPORT_VIEWER_USER;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID;

import ca.uhn.fhir.util.ObjectUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.openlmis.referencedata.domain.GeographicZone;
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
    if (CollectionUtils.isEmpty(userReportViews)) {
      return Collections.singletonList(
          GeographicInfoDto.builder()
              .provinceId(ALL_GEOGRAPHIC_UUID)
              .districtId(ALL_GEOGRAPHIC_UUID)
              .build());
    }
    Set<UUID> provinceIds = userReportViews.stream()
        .map(UserReportView::getProvinceId)
        .collect(Collectors.toSet());
    Set<UUID> districtIds = userReportViews.stream()
        .map(UserReportView::getDistrictId)
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
    if (userIdOptional.isPresent() && userIdOptional.get().equals(userId)) {
      return;
    }
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
  }

  @Transactional
  public void saveReportViewGeographicInfo(UUID userId, List<GeographicInfoDto> geographicInfoDtos) {
    checkUser(userId);
    if (CollectionUtils.isEmpty(geographicInfoDtos)) {
      siglusUserReportViewRepository.deleteAllByUserId(userId);
      return;
    }
    List<GeographicInfoDto> allGeographicInfos = siglusGeographicInfoRepository.getGeographicInfo();
    Map<UUID, Set<UUID>> allProvinceIdToDistrictIds = allGeographicInfos.stream()
        .collect(Collectors.groupingBy(GeographicInfoDto::getProvinceId,
            Collectors.mapping(GeographicInfoDto::getDistrictId, Collectors.toSet())));
    checkGeographicInfoDto(geographicInfoDtos, allProvinceIdToDistrictIds);

    Map<UUID, Set<UUID>> newProvinceIdToDistrictIds = geographicInfoDtos.stream()
        .collect(Collectors.groupingBy(GeographicInfoDto::getProvinceId,
            Collectors.mapping(GeographicInfoDto::getDistrictId, Collectors.toSet())));
    List<UserReportView> userReportViews = new ArrayList<>();
    newProvinceIdToDistrictIds.keySet()
        .forEach(provinceId -> {
          Set<UUID> districtIds = newProvinceIdToDistrictIds.get(provinceId);
          List<UserReportView> viewList = districtIds.stream()
              .map(districtId -> UserReportView.builder()
                  .userId(userId)
                  .provinceId(provinceId)
                  .districtId(districtId)
                  .build()
              ).collect(Collectors.toList());
          userReportViews.addAll(viewList);
        });
    siglusUserReportViewRepository.deleteAllByUserId(userId);
    siglusUserReportViewRepository.flush();
    siglusUserReportViewRepository.save(userReportViews);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private void checkGeographicInfoDto(List<GeographicInfoDto> geographicInfoDtos,
      Map<UUID, Set<UUID>> allProvinceIdToDistrictIds) {
    for (GeographicInfoDto geographicInfoDto : geographicInfoDtos) {
      if (ObjectUtil.equals(geographicInfoDto.getProvinceId(), ALL_GEOGRAPHIC_UUID)
          && ObjectUtil.equals(geographicInfoDto.getDistrictId(), ALL_GEOGRAPHIC_UUID)) {
        continue;
      }
      if (!ObjectUtil.equals(geographicInfoDto.getProvinceId(), ALL_GEOGRAPHIC_UUID)
          && ObjectUtil.equals(geographicInfoDto.getDistrictId(), ALL_GEOGRAPHIC_UUID)) {
        continue;
      }
      if (ObjectUtil.equals(geographicInfoDto.getProvinceId(), ALL_GEOGRAPHIC_UUID)
          && !ObjectUtil.equals(geographicInfoDto.getDistrictId(), ALL_GEOGRAPHIC_UUID)) {
        throw new BusinessDataException(new Message(ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID));
      }
      Set<UUID> provinceDistrictIds = allProvinceIdToDistrictIds.get(geographicInfoDto.getProvinceId());
      if (!allProvinceIdToDistrictIds.containsKey(geographicInfoDto.getProvinceId())
          || !provinceDistrictIds.contains(geographicInfoDto.getDistrictId())) {
        throw new BusinessDataException(new Message(ERROR_USER_REPORT_VIEW_GEOGRAPHIC_INFO_INVALID));
      }
    }
  }
}
