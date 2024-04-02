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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.GeographicZone;
import org.siglus.siglusapi.domain.UserReportView;
import org.siglus.siglusapi.dto.GeographicInfoDto;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.repository.SiglusUserReportViewRepository;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

  public List<GeographicInfoDto> getReportViewGeographicInfo() {
    Optional<UUID> userIdOptional = siglusAuthenticationHelper.getCurrentUserId();
    if (!userIdOptional.isPresent() || !siglusAuthenticationHelper.isTheCurrentUserReportViewer()) {
      return new ArrayList<>();
    }
    UUID userId = userIdOptional.get();
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
    List<GeographicInfoDto> geographicInfoDtos = new ArrayList<>();
    for (UserReportView userReportView : userReportViews) {
      GeographicInfoDto geographicInfoDto = GeographicInfoDto.builder()
          .provinceId(userReportView.getProvinceId())
          .provinceName(provinceIdToNameMap.get(userReportView.getProvinceId()))
          .districtId(userReportView.getDistrictId())
          .districtName(districtToNameMap.get(userReportView.getDistrictId()))
          .build();
      geographicInfoDtos.add(geographicInfoDto);
    }
    return geographicInfoDtos;
  }
}
