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

import com.alibaba.fastjson.JSON;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = attributes.getRequest();
    String method = request.getMethod();
    String url = request.getRequestURL().toString();
    String traceId = "trace-" + UUID.randomUUID();
    Object requestParam = getRequestParams(joinPoint, request);
    Object requestBody = joinPoint.getArgs() == null ? null : JSON.toJSON(joinPoint.getArgs()[0]);
    if (isAndroid(request)) {
      AndroidHeader androidHeader = getAndroidHeader(request);
      log.info("[Android-API][START] {} {}, {}, header: {}, param: {}, body: {}", method, url, traceId, androidHeader,
          requestParam, requestBody);
    } else {
      log.info("[Web-API][START] {} {}, {}, param: {}, body: {}", method, url, traceId, requestParam, requestBody);
    }
    Object result = joinPoint.proceed();
    long costTime = System.currentTimeMillis() - startTime;
    if (isAndroid(request)) {
      log.info("[Android-API][END] {} {}, {}, response: {}, cost-time: {}ms",
          method, url, traceId, JSON.toJSON(result), costTime);
    } else {
      log.info("[Web-API][END] {} {}, {}, response: {}, cost-time: {}ms",
          method, url, traceId, JSON.toJSON(result), costTime);
    }
    return result;
  }

  private Object getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
    Object requestParam = null;
    Method targetMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
    if (targetMethod != null) {
      Annotation[] annotations = targetMethod.getAnnotations();
      for (Annotation annotation : annotations) {
        if (getRequestMappingClasses().contains(annotation.annotationType())) {
          requestParam = JSON.toJSON(request.getParameterMap());
          break;
        }
      }
    }
    return requestParam;
  }

  private List<Object> getRequestMappingClasses() {
    return Arrays.asList(RequestMapping.class, PostMapping.class, GetMapping.class, PutMapping.class,
        DeleteMapping.class, PatchMapping.class);
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
