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

package org.siglus.siglusapi.localmachine.auth;

import static org.siglus.siglusapi.localmachine.auth.AuthenticationArgumentResolver.LOCAL_MACHINE_TOKEN;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.localmachine.CommonConstants;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMachineRequestFilter extends OncePerRequestFilter {
  private static final RequestMatcher nonSecuredRequestMatcher =
      new AntPathRequestMatcher("/api/siglusapi/localmachine/server/agents");
  private static final RequestMatcher securedRequestMatcher =
      new AntPathRequestMatcher("/api/siglusapi/localmachine/server/**");
  private final AgentInfoRepository agentInfoRepository;
  private final AppInfoRepository appInfoRepository;
  private final FacilityRepository facilityRepository;

  public static class AuthorizeException extends IllegalStateException {
    private final HttpStatus status;

    public AuthorizeException(String message) {
      this(HttpStatus.UNAUTHORIZED, message);
    }

    public AuthorizeException(HttpStatus status, String message) {
      super(message);
      this.status = status;
    }
  }

  public RequestMatcher getRequestMatcher() {
    return new OrRequestMatcher(nonSecuredRequestMatcher, securedRequestMatcher);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startTime = System.currentTimeMillis();
    try {
      if (nonSecuredRequestMatcher.matches(request)) {
        filterChain.doFilter(request, response);
        return;
      }
      if (securedRequestMatcher.matches(request)) {
        try {
          authenticate(request);
        } catch (AuthorizeException e) {
          log.error("localmachine auth fail", e);
          response.setStatus(e.status.value());
          response.getWriter().write(e.getMessage());
          return;
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      long cost = System.currentTimeMillis() - startTime;
      log.info(
          "[localmachine] {} - {} cost:{}",
          request.getMethod(),
          request.getRequestURL().toString(),
          cost);
    }
  }

  public void authenticate(HttpServletRequest request) throws AuthorizeException {
    String tokenValue = mustGetTokenValue(request);
    MachineToken machineToken = parseToken(tokenValue);
    bindRequestAttribute(request, machineToken);
    if (StringUtils.isNotBlank(request.getHeader(CommonConstants.DEVICE_INFO))
        && (HttpMethod.PUT.name().equals(request.getMethod())
        || HttpMethod.POST.name().equals(request.getMethod()))) {
      updateMachineDeviceInfo(request, machineToken);
    }
  }

  private String mustGetTokenValue(HttpServletRequest request) throws AuthorizeException {
    String tokenValue = request.getHeader(CommonConstants.ACCESS_TOKEN);
    if (isEmpty(tokenValue)) {
      throw new AuthorizeException("machine access token is required");
    }
    return tokenValue;
  }

  private MachineToken parseToken(String tokenValue) throws AuthorizeException {
    MachineToken machineToken = MachineToken.parse(tokenValue);
    AgentInfo agentInfo = mustGetAgentInfo(machineToken);
    if (!machineToken.verify(agentInfo.getPublicKey())) {
      log.error("fail to verify token:{}", tokenValue);
      throw new AuthorizeException("invalid machine token");
    }
    return machineToken;
  }

  private AgentInfo mustGetAgentInfo(MachineToken machineToken) throws AuthorizeException {
    UUID machineId = machineToken.getMachineId();
    UUID facilityId = machineToken.getFacilityId();
    log.info(String.format("auth check, machineId = %s, facilityId=%s", machineId, facilityId));
    return Optional.ofNullable(
            agentInfoRepository.findOneByMachineIdAndFacilityId(machineId, facilityId))
        .orElseThrow(
            () ->
                new AuthorizeException(
                    HttpStatus.PRECONDITION_FAILED,
                    "localmachine not activated yet. machineId = " + machineId));
  }

  private void bindRequestAttribute(HttpServletRequest request, MachineToken machineToken) {
    request.setAttribute(LOCAL_MACHINE_TOKEN, machineToken);
  }

  void updateMachineDeviceInfo(HttpServletRequest request, MachineToken machineToken) {
    String deviceInfo = request.getHeader(CommonConstants.DEVICE_INFO);
    String deviceVersion = request.getHeader(CommonConstants.VERSION);
    UUID facilityId = machineToken.getFacilityId();
    Facility facility = facilityRepository.findOne(facilityId);
    AppInfo appInfo = appInfoRepository.findByFacilityCode(facility.getCode());
    if (null != appInfo && !appInfo.getUniqueId().equals(machineToken.getMachineId().toString())) {
      return;
    }
    if (null != appInfo && deviceInfo.equals(appInfo.getDeviceInfo())
        && deviceVersion.equals(appInfo.getVersionCode())) {
      return;
    }
    if (null == appInfo) {
      appInfo = buildForAppInfo(facility, machineToken.getMachineId());
    }
    appInfo.setVersionCode(request.getHeader(CommonConstants.VERSION));
    appInfo.setDeviceInfo(deviceInfo);
    log.info("deviceInfo updated, facilityId: {}; new deviceInfo: {}", facilityId, deviceInfo);
    appInfoRepository.save(appInfo);
  }

  private AppInfo buildForAppInfo(Facility facility, UUID machineId) {
    return AppInfo
        .builder()
        .facilityCode(facility.getCode())
        .facilityName(facility.getName())
        .uniqueId(machineId.toString())
        .build();
  }
}
