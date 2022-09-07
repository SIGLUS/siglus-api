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

package org.siglus.siglusapi.localmachine.webapi;

import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.interceptor.OperationGuardAspect.Guarded;
import org.siglus.siglusapi.localmachine.agent.LocalActivationService;
import org.siglus.siglusapi.localmachine.eventstore.EventRecordRepository;
import org.siglus.siglusapi.localmachine.repository.AgentInfoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(value = "machine.debug", havingValue = "on")
@Profile("localmachine")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/siglusapi/localmachine/agent")
public class DebugController {
  private final LocalActivationService localActivationService;
  private final AgentInfoRepository agentInfoRepository;
  private final EventRecordRepository eventRecordRepository;

  @Guarded
  @PutMapping("/activationInfo")
  @Transactional
  public void resetActivationInfo(@RequestBody @Validated LocalActivationRequest request) {
    agentInfoRepository.deleteAll();
    eventRecordRepository.deleteAll();
    localActivationService.activate(request);
  }
}
