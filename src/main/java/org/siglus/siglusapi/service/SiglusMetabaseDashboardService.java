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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.domain.FacilityLevel;
import org.siglus.siglusapi.domain.FacilityType;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.siglus.siglusapi.dto.MetabaseUrlDto;
import org.siglus.siglusapi.repository.MetabaseDashboardRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SiglusMetabaseDashboardService {

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;
  @Autowired
  SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
  @Autowired
  MetabaseDashboardRepository metabaseDashboardRepository;

  @Value("${metabase.secret.key}")
  private String metabaseSecretKey;

  @Value("${metabase.site.url}")
  private String masterSiteUrl;

  public MetabaseUrlDto getMetabaseDashboardAddressByDashboardName(String dashboardName) {

    String payload = getPayloadByDashboardName(dashboardName);

    Map<String, Object> payloadMap = null;
    try {
      payloadMap = new ObjectMapper().readValue(payload, Map.class);
    } catch (IOException e) {
      log.error("there is IO Wrong occured in read metabase payload");
    }
    String jwtToken = Jwts.builder()
        .setHeaderParam("typ", "JWT")
        .setClaims(payloadMap)
        .signWith(SignatureAlgorithm.HS256, Base64.getEncoder()
            .encodeToString(metabaseSecretKey.getBytes()))
        .setExpiration(
            Date.from(LocalDateTime.now().plusHours(12).atZone(ZoneId.systemDefault()).toInstant()))
        .compact();
    return new MetabaseUrlDto(masterSiteUrl + "/embed/dashboard/" + jwtToken + "#bordered=false&titled=true");
  }


  private String getPayloadByDashboardName(String dashboardName) {

    String payloadTemplate = "{\"resource\": {\"dashboard\": %d},\"params\": {%s}}";

    UUID homeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    FacilityDto facility = siglusFacilityReferenceDataService.findOne(homeFacilityId);

    String level = getLevelByTypeCode(facility.getType().getCode());

    Integer dashboardId = getDashboardIdByLevelAndDashboardName(level, dashboardName);

    String requestParam = getRequestParamByLevel(level, facility);

    return String.format(payloadTemplate, dashboardId, requestParam);
  }

  private String getLevelByTypeCode(String typeCode) {
    return FacilityType.findLevelByTypeCode(typeCode)
        .map(e -> e.getFacilityLevel().getFacilityLevelName())
        .orElseThrow(() -> new IllegalArgumentException(
            "there is no mapping Level to the TypeCode: " + typeCode));
  }

  private Integer getDashboardIdByLevelAndDashboardName(String level, String dashboardName) {
    return metabaseDashboardRepository.findByDashboardNameAndLevel(dashboardName,
        level).orElseThrow(() -> new IllegalArgumentException(
            "there is no mapping dashboard to the level : " + level
                + " and dashboard name" + dashboardName)).getDashboardId();
  }

  private String getRequestParamKeyByLevel(String level) {
    return FacilityType.findMetabaseRequestParamKeyByLevel(level).map(
        e -> e.getFacilityLevel().getMetabaseRequestParamKey())
        .orElseThrow(
            () -> new IllegalArgumentException(
            "there is no mapping MetabaseRequestParamKey to the level: " + level));

  }

  private String getCorrespondindGeographicCodeByLevel(String level, FacilityDto facility) {
    if (level.equals(FacilityLevel.SITE.getFacilityLevelName())) {
      return facility.getCode();
    }
    GeographicZoneDto geographicZone = facility.getGeographicZone();
    HashMap<String, String> geographicZoneCodeMap = new HashMap<>();
    geographicZoneCodeMap = getAllMappingRelationShipBetweenLevelAndCodeByRecursion(geographicZone,
        geographicZoneCodeMap);

    return Optional.ofNullable(geographicZoneCodeMap.get(level)).orElseThrow(
        () -> new IllegalArgumentException("there is no mapping levelCode to the level" + level));
  }

  private HashMap<String, String> getAllMappingRelationShipBetweenLevelAndCodeByRecursion(
      GeographicZoneDto geographicZone, HashMap<String, String> geographicZoneCodeMap) {
    if (geographicZone != null) {
      geographicZoneCodeMap.put(geographicZone.getLevel().getCode(), geographicZone.getCode());
      getAllMappingRelationShipBetweenLevelAndCodeByRecursion(geographicZone.getParent(),
          geographicZoneCodeMap);
    }
    return geographicZoneCodeMap;
  }

  private String getRequestParamByLevel(String level, FacilityDto facility) {
    if (level.equals(FacilityLevel.NATIONAL.getFacilityLevelName())) {
      return "";
    }
    String templateParam = "\"%s\": \"%s\"";
    String paramKey = getRequestParamKeyByLevel(level);
    String geographicCode = getCorrespondindGeographicCodeByLevel(level, facility);
    return String.format(templateParam, paramKey, geographicCode);
  }


}
