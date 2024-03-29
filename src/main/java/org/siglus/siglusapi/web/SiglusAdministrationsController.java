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

package org.siglus.siglusapi.web;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.dto.FacilityDeviceDto;
import org.siglus.siglusapi.dto.FacilitySearchParamDto;
import org.siglus.siglusapi.dto.FacilitySearchResultDto;
import org.siglus.siglusapi.dto.SiglusFacilityDto;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.siglus.siglusapi.web.request.DeviceTypeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/siglusapi/facilities")
public class SiglusAdministrationsController {

  private final SiglusAdministrationsService administrationsService;

  @PostMapping
  public Page<FacilitySearchResultDto> showFacilitiesInfos(@RequestBody FacilitySearchParamDto facilitySearchParamDto,
      Pageable pageable) {
    return administrationsService.searchForFacilities(facilitySearchParamDto, pageable);
  }

  @PostMapping("/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  public FacilitySearchResultDto createFacility(@RequestBody SiglusFacilityDto siglusFacilityDto) {
    return administrationsService.createFacility(siglusFacilityDto);
  }

  @GetMapping("/{facilityId}")
  public FacilitySearchResultDto getFacility(@PathVariable UUID facilityId) {
    return administrationsService.getFacility(facilityId);
  }

  @PutMapping("/{facilityId}")
  public FacilitySearchResultDto updateFacility(@PathVariable UUID facilityId,
      @RequestBody SiglusFacilityDto siglusFacilityDto) {
    return administrationsService.updateFacility(facilityId, siglusFacilityDto);
  }

  @GetMapping("/{facilityId}/upgradeToWeb")
  public void upgradeAndroidFacilityToWeb(@PathVariable UUID facilityId) {
    administrationsService.upgradeAndroidFacilityToWeb(facilityId);
  }


  @GetMapping("/{facilityId}/locations")
  public void exportLocationManagementTemplate(@PathVariable("facilityId") UUID facilityId,
      HttpServletResponse response) {
    administrationsService.exportLocationInfo(facilityId, response);
  }

  @PostMapping("{facilityId}/locations")
  public void uploadLocationInfo(@PathVariable("facilityId") UUID facilityId,
      @RequestParam("file") MultipartFile locationManagementFile, @RequestParam("format") String format)
      throws IOException {
    administrationsService.uploadLocationInfo(facilityId, locationManagementFile);
  }

  @DeleteMapping("/{facilityCode}/deviceInfo")
  public void eraseAndroidDeviceInfo(@PathVariable String facilityCode) {
    administrationsService.eraseDeviceInfoByFacilityId(facilityCode);
  }

  @GetMapping("/{facilityId}/facilityDevice")
  public FacilityDeviceDto getFacilityDevice(@PathVariable UUID facilityId) {
    return administrationsService.getFacilityDevice(facilityId);
  }

  @DeleteMapping("/{facilityId}/facilityDevice")
  public void eraseFacilityDevice(@PathVariable UUID facilityId, @RequestBody DeviceTypeRequest deviceTypeRequest) {
    administrationsService.eraseDeviceInfo(deviceTypeRequest.getFacilityDeviceType(), facilityId);
  }

  @PutMapping("/{facilityId}/to")
  public void changeFacility(@PathVariable UUID facilityId, @RequestBody DeviceTypeRequest deviceTypeRequest) {
    switch (deviceTypeRequest.getFacilityDeviceType()) {
      case WEB:
        administrationsService.changeToWeb(facilityId);
        break;
      case ANDROID:
        administrationsService.changeToAndroid(facilityId);
        break;
      case LOCAL_MACHINE:
        administrationsService.changeToLocalMachine((facilityId));
        break;
      default:
        break;
    }
  }
}