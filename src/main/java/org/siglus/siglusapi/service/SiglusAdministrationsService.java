package org.siglus.siglusapi.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@Slf4j
public class SiglusAdministrationsService {
  @Autowired
  private FacilityExtensionRepository facilityExtensionRepository;

  @Autowired
  private AppInfoRepository appInfoRepository;

  @Autowired
  private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;

  public Page<FacilityDto> searchForFacilities(Integer page, Integer size, String sort) {
      // get results from OpenLMIS interface
      Page<FacilityDto> facilityDtos = siglusFacilityReferenceDataService.searchAllFacilities(page, size, sort);
      // justify isAndroid by facilityId
      facilityDtos.getContent().forEach(m -> {
          FacilityExtension byFacilityId = facilityExtensionRepository.findByFacilityId(m.getId());
          m.setIsAndroid(null != byFacilityId && BooleanUtils.isTrue(byFacilityId.getIsAndroid()));
      });
      return facilityDtos;
  }

  public void eraseAndroidByFacilityId(UUID facilityId) {
      AppInfo one = appInfoRepository.findOne(facilityId);
      if (null == one) {
          log.info("The facilityId: {} is not exist", facilityId);
          throw new IllegalArgumentException("The facilityId is not acceptable");
      }
      appInfoRepository.delete(facilityId);
  }
}
