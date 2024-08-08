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

package org.siglus.siglusapi.localmachine.event.fc.receiptplan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.service.fc.FcReceiptPlanService;
import org.siglus.siglusapi.util.SiglusSimulateUserAuthHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("localmachine")
public class FcReceiptPlanReplayer {

  private final FcReceiptPlanService fcReceiptPlanService;

  private final SiglusSimulateUserAuthHelper siglusSimulateUserAuthHelper;

  @Transactional
  @EventListener(classes = {FcReceiptPlanEvent.class})
  public void replay(FcReceiptPlanEvent event) {
    try {
      log.info("start replay event of fc receipt plan, requisitionNumber = " + event.getReceiptPlanDto()
          .getRequisitionNumber());
      doReplay(event);
      log.info("end replay receipt plan, requisitionNumber = " + event.getReceiptPlanDto().getRequisitionNumber());
    } catch (Exception e) {
      log.error("fail to replay event of fc receipt plan, msg = " + e.getMessage(), e);
      throw e;
    }
  }

  private void doReplay(FcReceiptPlanEvent event) {
    OAuth2Authentication originAuth = siglusSimulateUserAuthHelper.simulateNewUserBefore();
    try {
      UserDto userDto = fcReceiptPlanService.getFcUserInfo();
      siglusSimulateUserAuthHelper.simulateNewUserAuth(userDto.getId());
      fcReceiptPlanService.updateRequisition(event.getReceiptPlanDto(), userDto);
    } finally {
      siglusSimulateUserAuthHelper.simulateNewUserAfter(originAuth);
    }
  }
}
