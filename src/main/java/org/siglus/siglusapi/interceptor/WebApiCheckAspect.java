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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.siglusapi.util.AndroidHelper;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class WebApiCheckAspect {

  private final AndroidHelper androidHelper;

  @Pointcut("within(org.siglus.siglusapi.web.*) && "
      + "(@annotation(org.springframework.web.bind.annotation.PostMapping) "
      + "|| @annotation(org.springframework.web.bind.annotation.PutMapping) "
      + "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) "
      + "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
  public void webApi() {
    // do nothing
  }

  @Before("webApi()")
  public void before() {
    if (androidHelper.isAndroid()) {
      throw new PermissionMessageException(new Message("siglusapi.error.notWebUser"));
    }
  }

}
