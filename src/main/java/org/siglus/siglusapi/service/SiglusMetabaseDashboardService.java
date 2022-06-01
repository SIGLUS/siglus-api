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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.RoleAssignmentDto;
import org.siglus.siglusapi.domain.FacilityLevel;
import org.siglus.siglusapi.domain.FacilitySuppierLevel;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.MetabaseUrlDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.repository.FacilitySupplierLevelRepository;
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
  FacilitySupplierLevelRepository facilitySupplierLevelRepository;
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

  private final String roleAdminId = "a439c5de-b8aa-11e6-80f5-76304dec7eb7";

  public MetabaseUrlDto getMetabaseDashboardAddressByDashboardName(String dashboardName) {

    String payload = getPayloadByDashboardName(dashboardName);
    System.out.println("payload ============ " + payload);
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
        .setHeaderParam("typ", "JWT")
        .setClaims(payloadMap)
        .signWith(SignatureAlgorithm.HS256, Base64.getEncoder()
            .encodeToString(metabaseSecretKey.getBytes()))
        .setExpiration(
            new Date(LocalDateTime.now().plusHours(12).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
        .compact();
    return new MetabaseUrlDto(
        masterSiteUrl + "/embed/dashboard/" + jwtToken
            + "#bordered=false&titled=true"
            + "&hide_parameters=facility_code,district_facility_code,province_facility_code");
  }

  public String getPayloadByDashboardName(String dashboardName) {

    String payloadTemplate = "{\"resource\": {\"dashboard\": %d},\"params\": {%s}}";

    Integer dashboardId = getDashboardIdByDashboardName(dashboardName);

    UserDto userDto = authenticationHelper.getCurrentUser();
    Boolean adminAccount = isAdmin(userDto);
    if (adminAccount) {
      return String.format(payloadTemplate, dashboardId, "");
    }
    FacilityDto facility = siglusFacilityReferenceDataService.findOne(userDto.getHomeFacilityId());

    String requestParam = getRequestParamByFacility(facility);

    return String.format(payloadTemplate, dashboardId, requestParam);
  }

  private Boolean isAdmin(UserDto userDto) {
    Set<UUID> roleAssignmentIds = userDto.getRoleAssignments().stream()
        .map(RoleAssignmentDto::getRoleId).collect(Collectors.toSet());
    return roleAssignmentIds.contains(UUID.fromString(roleAdminId));
  }

  private String getLevelByTypeCode(String typeCode) {
    Optional<FacilitySuppierLevel> facilityTypeCodeOptional = facilitySupplierLevelRepository.findByFacilityTypeCode(
        typeCode);
    if (facilityTypeCodeOptional.isPresent()) {
      return facilityTypeCodeOptional.get().getLevel();
    }
    return FacilityLevel.SITE.getFacilityLevelName();
  }

  private Integer getDashboardIdByDashboardName(String dashboardName) {
    return metabaseDashboardRepository.findByDashboardName(dashboardName)
        .orElseThrow(() -> new IllegalArgumentException(
            "there is no mapping dashboard to dashaboard name : " + dashboardName)).getDashboardId();
  }

  private String getRequestParamByFacility(FacilityDto facility) {
    String templateParam = "\"%s\": \"%s\"";
    String level = getLevelByTypeCode(facility.getType().getCode());
    String paramKey = FacilityLevel.findMetabaseRequestParamKeyByLevel(level);
    return String.format(templateParam, paramKey, facility.getCode());
  }


}
