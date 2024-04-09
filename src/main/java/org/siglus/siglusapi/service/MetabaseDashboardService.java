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
import static org.siglus.siglusapi.constant.FieldConstants.JWT_TOKEN_HEADER_PARAM_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.JWT_TOKEN_HEADER_PARAM_VALUE;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_EXTENSION_URL;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_PARAM_TEMPLATE;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_PART_URL;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_PAYLOAD_TEMPLATE;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_FOUND;

import ca.uhn.fhir.util.ObjectUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.siglusapi.domain.FacilityLevel;
import org.siglus.siglusapi.domain.UserReportView;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.FacilityGeographicInfoDto;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.MetabaseUrlDto;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.repository.MetabaseDashboardRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusUserReportViewRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetabaseDashboardService {

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Autowired
  private MetabaseDashboardRepository metabaseDashboardRepository;
  @Autowired
  private SiglusFacilityRepository siglusFacilityRepository;
  @Autowired
  private SiglusUserReportViewRepository siglusUserReportViewRepository;

  @Value("${metabase.secret.key}")
  private String metabaseSecretKey;
  @Value("${metabase.site.url}")
  private String masterSiteUrl;
  @Value("${metabase.token.expired.time}")
  private Integer metabaseTokenExpiredTime;

  public MetabaseUrlDto getMetabaseDashboardAddressByDashboardName(String dashboardName) {

    String payload = getPayloadByDashboardName(dashboardName);
    return getMetabaseUrlDtoBypayload(payload);
  }

  private MetabaseUrlDto getMetabaseUrlDtoBypayload(String payload) {
    Map<String, Object> payloadMap = null;
    try {
      payloadMap = new ObjectMapper().readValue(payload, Map.class);
    } catch (IOException e) {
      log.error("there is IO Wrong occured in read metabase payload");
    }
    String jwtToken = Jwts.builder()
        .setHeaderParam(JWT_TOKEN_HEADER_PARAM_NAME, JWT_TOKEN_HEADER_PARAM_VALUE)
        .setClaims(payloadMap)
        .signWith(Keys.hmacShaKeyFor(metabaseSecretKey.getBytes()), SignatureAlgorithm.HS256)
        .setExpiration(
            new Date(LocalDateTime.now().plusHours(metabaseTokenExpiredTime).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()))
        .compact();
    return new MetabaseUrlDto(
        masterSiteUrl + METABASE_PART_URL + jwtToken + METABASE_EXTENSION_URL);
  }

  public String getPayloadByDashboardName(String dashboardName) {

    Integer dashboardId = getDashboardIdByDashboardName(dashboardName);
    if (authenticationHelper.isTheCurrentUserAdmin()) {
      return String.format(METABASE_PAYLOAD_TEMPLATE, dashboardId, "");
    }
    if (authenticationHelper.isTheCurrentUserReportViewer()) {
      Map<String, Object> dashboradMap = new HashMap<>();
      dashboradMap.put("dashboard", dashboardId);
      Map<String, Object> payloadMap = new HashMap<>();
      payloadMap.put("resource", dashboradMap);
      payloadMap.put("params", getRequestParamForReportViewerUser());
      try {
        return new ObjectMapper().writeValueAsString(payloadMap);
      } catch (IOException e) {
        log.error("there is IO Wrong occured in write metabase payload");
      }
    }
    FacilityDto facility = siglusFacilityReferenceDataService
        .findOne(authenticationHelper.getCurrentUser().getHomeFacilityId());

    String requestParam = getRequestParamByFacility(facility);

    return String.format(METABASE_PAYLOAD_TEMPLATE, dashboardId, requestParam);
  }

  private Map<String, Object> getRequestParamForReportViewerUser() {
    UUID userId = authenticationHelper.getCurrentUserId()
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_USER_NOT_FOUND)));
    Map<String, Object> paramMap = new HashMap<>();
    List<UserReportView> userReportViews = siglusUserReportViewRepository.findAllByUserId(userId);
    if (CollectionUtils.isEmpty(userReportViews)) {
      return paramMap;
    }
    List<UserReportView> reportList = userReportViews.stream()
        .filter(userReportView ->
            ObjectUtil.equals(userReportView.getProvinceId(), ALL_GEOGRAPHIC_UUID)
                && ObjectUtil.equals(userReportView.getDistrictId(), ALL_GEOGRAPHIC_UUID))
        .collect(Collectors.toList());
    if (reportList.size() >= 1) {
      return paramMap;
    }
    Set<UUID> provinceIds = userReportViews.stream()
        .filter(userReportView -> ObjectUtil.equals(userReportView.getDistrictId(), ALL_GEOGRAPHIC_UUID))
        .map(UserReportView::getProvinceId)
        .collect(Collectors.toSet());
    Set<UUID> districtIds = userReportViews.stream()
        .map(UserReportView::getDistrictId)
        .collect(Collectors.toSet());

    List<FacilityGeographicInfoDto> allFacilityGeographicInfos =
        siglusFacilityRepository.getAllFacilityGeographicInfo();
    Set<String> facilityCodes = allFacilityGeographicInfos.stream()
        .filter(infoDto ->
            provinceIds.contains(infoDto.getProvinceId()) || districtIds.contains(infoDto.getDistrictId()))
        .map(FacilityGeographicInfoDto::getFacilityCode)
        .collect(Collectors.toSet());

    paramMap.put(FacilityLevel.SITE.getMetabaseRequestParamKey(), facilityCodes);
    return paramMap;
  }

  private Integer getDashboardIdByDashboardName(String dashboardName) {
    return metabaseDashboardRepository.findByDashboardName(dashboardName)
        .orElseThrow(() -> new IllegalArgumentException(
            "there is no mapping dashboard to dashaboard name : " + dashboardName)).getDashboardId();
  }

  private String getRequestParamByFacility(FacilityDto facility) {
    String level = authenticationHelper.getFacilityGeographicZoneLevel();
    String paramKey = FacilityLevel.findMetabaseRequestParamKeyByLevel(level);
    return String.format(METABASE_PARAM_TEMPLATE, paramKey, facility.getCode());
  }


}
