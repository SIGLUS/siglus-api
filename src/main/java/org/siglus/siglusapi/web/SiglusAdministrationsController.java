package org.siglus.siglusapi.web;

import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/siglusapi/administration")
public class SiglusAdministrationsController {

    @Autowired
    private SiglusAdministrationsService administrationsService;

    @GetMapping("/facilities")
    public Page<FacilityDto> showFacilitiesInfos(@RequestParam(value = "page", defaultValue = "0")Integer page,
                                                 @RequestParam(value = "size", defaultValue = "10")Integer size,
                                                 @RequestParam(value = "sort", defaultValue = "name,asc")String sort){
        return administrationsService.searchForFacilities(page, size, sort);
    }

    @DeleteMapping("/{facilityId}/android")
    public void eraseAndroidDevice(@PathVariable UUID facilityId){
        administrationsService.eraseAndroidByFacilityId(facilityId);
    }
}