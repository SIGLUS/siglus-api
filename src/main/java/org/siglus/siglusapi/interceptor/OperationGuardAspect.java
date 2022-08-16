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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Aspect
public class OperationGuardAspect {
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Value("${guard.secret}")
  private String encodedSecret;

  @Around("@annotation(OperationGuardAspect.Guarded)")
  public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
    authorized();
    return joinPoint.proceed();
  }

  public void authorized() {
    HttpServletRequest request =
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    String secret = request.getHeader("x-guard-secret");
    this.assertAuthorized(secret);
  }

  void assertAuthorized(String secret) {
    secret = Optional.ofNullable(secret).orElse("");
    if (encoder.matches(secret, encodedSecret)) {
      return;
    }
    throw new AuthenticationException(new Message(ERROR_PERMISSION_NOT_SUPPORTED));
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface Guarded {}
}
