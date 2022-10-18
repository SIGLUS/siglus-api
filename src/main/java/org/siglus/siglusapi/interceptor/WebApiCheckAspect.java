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

import static org.siglus.siglusapi.i18n.MessageKeys.CURRENT_IS_NOT_LOCATION_MANAGEMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.CURRENT_IS_NOT_STOCK_MANAGEMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CURRENT_IS_NOT_LOCATION_MANAGEMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_CURRENT_IS_NOT_STOCK_MANAGEMENT;
import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_WEB_USER;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.openlmis.stockmanagement.exception.PermissionMessageException;
import org.openlmis.stockmanagement.util.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.FacilityConfigHelper;
import org.siglus.siglusapi.util.LocalMachineHelper;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class WebApiCheckAspect {

  private final AndroidHelper androidHelper;
  private final LocalMachineHelper localMachineHelper;
  private final FacilityConfigHelper facilityConfigHelper;
  private final Machine machine;

  @Pointcut("within(org.siglus.siglusapi.web..*) && !within(org.siglus.siglusapi.web.android.*) &&"
      + "(@annotation(org.springframework.web.bind.annotation.PostMapping) "
      + "|| @annotation(org.springframework.web.bind.annotation.PutMapping) "
      + "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) "
      + "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
  public void webWriteApi() {
    // do nothing
  }

  @Before("webWriteApi()")
  public void checkWebWriteApi() {
    if (machine.isOnlineWeb() && (localMachineHelper.isLocalMachine() || androidHelper.isAndroid())) {
      throw new PermissionMessageException(new Message(ERROR_NOT_WEB_USER));
    }
  }

  @Pointcut("within(org.siglus.siglusapi.web.withlocation.*)")
  public void withLocationApi() {
    // do nothing
  }

  @Before("withLocationApi()")
  public void checkWithLocationApi() {
    if (facilityConfigHelper.isStockManagement()) {
      throw new BusinessDataException(new org.siglus.siglusapi.dto.Message(ERROR_CURRENT_IS_NOT_LOCATION_MANAGEMENT),
          CURRENT_IS_NOT_LOCATION_MANAGEMENT);
    }
  }

  @Pointcut("within(org.siglus.siglusapi.web.withoutlocation.*)")
  public void withoutLocationApi() {
    // do nothing
  }

  @Before("withoutLocationApi()")
  public void checkWithoutLocationApi() {
    if (facilityConfigHelper.isLocationManagement()) {
      throw new BusinessDataException(new org.siglus.siglusapi.dto.Message(ERROR_CURRENT_IS_NOT_STOCK_MANAGEMENT),
          CURRENT_IS_NOT_STOCK_MANAGEMENT);
    }
  }
}
