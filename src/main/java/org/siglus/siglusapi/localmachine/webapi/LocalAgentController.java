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

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NOT_ACTIVATED_YET;

import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.agent.LocalActivationService;
import org.siglus.siglusapi.localmachine.agent.LocalSyncResultsService;
import org.siglus.siglusapi.localmachine.domain.AgentInfo;
import org.siglus.siglusapi.localmachine.server.LocalExportImportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Profile("localmachine")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/siglusapi/localmachine/agent")
public class LocalAgentController {

  private final LocalActivationService localActivationService;
  private final LocalExportImportService localExportImportService;
  private final Machine machine;
  private final LocalSyncResultsService syncErrorService;

  @Value("${machine.version}")
  private String localMachineVersion;

  @PutMapping
  public void activate(@RequestBody @Validated LocalActivationRequest request) {
    localActivationService.activate(request);
  }

  @GetMapping
  public AgentInfoResponse getCurrentAgentInfo() {
    AgentInfo agentInfo = localActivationService.getCurrentAgentInfo()
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_NOT_ACTIVATED_YET)));
    return AgentInfoResponse.from(agentInfo);
  }

  @GetMapping("/events/export")
  public void exportEvents(HttpServletResponse response) {
    localExportImportService.exportEvents(response);
  }

  @PostMapping("/events/import")
  public void importEvents(@RequestParam("files") MultipartFile[] files) {
    localExportImportService.importEvents(files);
  }

  @GetMapping("/basicInfo")
  public LocalMachineBasicInfo getInternetStatus() {
    return LocalMachineBasicInfo.builder()
        .isConnectedOnlineWeb(machine.isConnectedOnlineWeb())
        .localMachineVersion(localMachineVersion)
        .build();
  }

  @GetMapping("/syncResults")
  public LocalSyncResultsResponse getSyncResults() {
    return syncErrorService.getSyncResults();
  }
}
