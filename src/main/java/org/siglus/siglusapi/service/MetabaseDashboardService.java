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

import static org.siglus.siglusapi.constant.FieldConstants.JWT_TOKEN_HEADER_PARAM_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.JWT_TOKEN_HEADER_PARAM_VALUE;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_EXTENSION_URL;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_PARAM_TEMPLATE;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_PART_URL;
import static org.siglus.siglusapi.constant.FieldConstants.METABASE_PAYLOAD_TEMPLATE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.FacilityLevel;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.MetabaseUrlDto;
import org.siglus.siglusapi.repository.MetabaseDashboardRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetabaseDashboardService {

  private final SiglusAuthenticationHelper authenticationHelper;
  private final SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  private final MetabaseDashboardRepository metabaseDashboardRepository;

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
    if (authenticationHelper.isTheCurrentUserAdmin()
        || authenticationHelper.isTheCurrentUserReportViewer()) {
      return String.format(METABASE_PAYLOAD_TEMPLATE, dashboardId, "");
    }
    FacilityDto facility = siglusFacilityReferenceDataService
        .findOne(authenticationHelper.getCurrentUser().getHomeFacilityId());

    String requestParam = getRequestParamByFacility(facility);

    return String.format(METABASE_PAYLOAD_TEMPLATE, dashboardId, requestParam);
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
