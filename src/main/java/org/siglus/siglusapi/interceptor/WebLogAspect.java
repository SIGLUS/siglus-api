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

package org.siglus.siglusapi.interceptor;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class WebLogAspect {

  private static final String USER_NAME = "UserName";
  private static final String FACILITY_CODE = "FacilityCode";
  private static final String FACILITY_NAME = "FacilityName";
  private static final String UNIQUE_ID = "UniqueId";
  private static final String DEVICE_INFO = "DeviceInfo";
  private static final String VERSION_CODE = "VersionCode";
  private static final String ANDROID_SDK_VERSION = "AndroidSDKVersion";

  @Pointcut("within(org.siglus.siglusapi.web..*)")
  public void webLog() {
    // do nothing
  }

  @Around("webLog()")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();
    ServletRequestAttributes attributes = (ServletRequestAttributes)
        RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = attributes.getRequest();
    String method = request.getMethod();
    String url = request.getRequestURL().toString();
    String args = Arrays.toString(joinPoint.getArgs());
    if (isAndroid(request)) {
      AndroidHeader androidHeader = getAndroidHeader(request);
      log.info("[Android-API][START] {} {}, header: {}, args: {}", method, url, androidHeader, args);
    } else {
      log.info("[Web-API][START] {} {}, args: {}", method, url, args);
    }
    Object result = joinPoint.proceed();
    long costTime = System.currentTimeMillis() - startTime;
    if (isAndroid(request)) {
      log.info("[Android-API][END] {} {}, cost-time: {}ms", method, url, costTime);
    } else {
      log.info("[Web-API][END] {} {}, cost-time: {}ms", method, url, costTime);
    }
    return result;
  }

  private AndroidHeader getAndroidHeader(HttpServletRequest request) {
    return AndroidHeader.builder()
        .username(request.getHeader(USER_NAME))
        .facilityCode(request.getHeader(FACILITY_CODE))
        .facilityName(request.getHeader(FACILITY_NAME))
        .uniqueId(request.getHeader(UNIQUE_ID))
        .deviceInfo(request.getHeader(DEVICE_INFO))
        .versionCode(request.getHeader(VERSION_CODE))
        .androidSdkVersion(request.getHeader(ANDROID_SDK_VERSION))
        .build();
  }

  private boolean isAndroid(HttpServletRequest request) {
    return StringUtils.isNotEmpty(request.getHeader(FACILITY_CODE));
  }

}
