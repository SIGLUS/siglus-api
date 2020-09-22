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
    String method = request.getMethod();
    String url = request.getRequestURL().toString();
    String body = Arrays.toString(joinPoint.getArgs());

    log.info("[API_START] {} {}, body: {}", method, url, body);

    Object result = joinPoint.proceed();

    long costTime = System.currentTimeMillis() - startTime;
    if (costTime >= 5000) {
      log.info("[API_FINISH] {} {}, cost: {}ms, slow!", method, url, costTime);
    } else {
      log.info("[API_FINISH] {} {}, cost: {}ms", method, url, costTime);
    }
    return result;
  }

}
