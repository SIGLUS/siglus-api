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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class WebLogAspect {

  @Pointcut("within(org.siglus.siglusapi.web.*)")
  public void webLog() {
    // do nothing
  }

  @Around("webLog()")
  public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();
    ServletRequestAttributes attributes = (ServletRequestAttributes)
        RequestContextHolder.getRequestAttributes();
    HttpServletRequest request = attributes.getRequest();
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String method = request.getMethod();
    String url = request.getRequestURL().toString();
    String body = Arrays.toString(joinPoint.getArgs());
    String className = joinPoint.getTarget().getClass().getName();
    String methodName = signature.getName();

    log.info("[API] {} {}, body:{}, method: {}.{}", method, url, body, className, methodName);

    Object result = joinPoint.proceed();

    log.info("[API] {} {}, cost: {}ms", method, url, System.currentTimeMillis() - startTime);
    return result;
  }

}
