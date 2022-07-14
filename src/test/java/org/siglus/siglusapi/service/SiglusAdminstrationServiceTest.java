package org.siglus.siglusapi.service;

import org.apache.commons.lang.BooleanUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.utils.Pagination;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import static org.junit.Assert.assertEquals;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusAdminstrationServiceTest {

    @InjectMocks
    private SiglusAdministrationsService siglusAdministrationsService;
    @Mock
    private AppInfoRepository appInfoRepository;
    @Mock
    private SiglusFacilityReferenceDataService siglusFacilityReferenceDataService;
    @Mock
    private FacilityExtensionRepository facilityExtensionRepository;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static final Integer page = 0;

    private static final Integer size = 3;

    private static final String sort = "name,asc";

    private static final UUID facilityId = UUID.randomUUID();

    private static final UUID device1 = UUID.randomUUID();

    private static final UUID device2 = UUID.randomUUID();

    private static final UUID device3 = UUID.randomUUID();

    @Test
    public void searchForFacilitiesWithIsAndroid(){
        // given
        when(siglusFacilityReferenceDataService.searchAllFacilities(page, size, sort))
                .thenReturn(mockFacilityDto());
        when(facilityExtensionRepository.findByFacilityId(device1)).thenReturn(mockFacilityExtension(device1, true));
        when(facilityExtensionRepository.findByFacilityId(device2)).thenReturn(mockFacilityExtension(device2, false));
        when(facilityExtensionRepository.findByFacilityId(device3)).thenReturn(null);

        // when
        Page<FacilityDto> facilityDtos = siglusAdministrationsService.searchForFacilities(page, size, sort);
        int isAndroid = 0;

        for (FacilityDto m : facilityDtos.getContent()){
            if(BooleanUtils.isTrue(m.getIsAndroid()))
                isAndroid++;
        }

        //then
        assertEquals(1, isAndroid);
    }

    @Test
    public void deleteAndroidInfoByFacilityId(){
        // given
        AppInfo appInfo = mockAppInfo();
        when(appInfoRepository.findOne(facilityId)).thenReturn(appInfo);

        // when
        siglusAdministrationsService.eraseAndroidByFacilityId(appInfo.getId());

        //then
        verify(appInfoRepository, times(1)).delete(appInfo.getId());
    }

    @Test
    public void deleteAndroidInfoWithWrongFacilityId(){
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The facilityId is not acceptable");

        siglusAdministrationsService.eraseAndroidByFacilityId(facilityId);
    }

    private AppInfo mockAppInfo(){
        AppInfo appInfo = new AppInfo();
        appInfo.setId(facilityId);
        return appInfo;
    }

    private Page<FacilityDto> mockFacilityDto(){
        Pageable pageable = new PageRequest(0, 3);
        List<FacilityDto> content = new ArrayList<>();
        FacilityDto facilityInfo1 = new FacilityDto();
        facilityInfo1.setId(device1);
        facilityInfo1.setActive(true);

        FacilityDto facilityInfo2 = new FacilityDto();
        facilityInfo2.setId(device2);
        facilityInfo2.setActive(false);

        FacilityDto facilityInfo3 = new FacilityDto();
        facilityInfo3.setId(device3);
        facilityInfo3.setActive(true);

        content.add(facilityInfo1);
        content.add(facilityInfo2);
        content.add(facilityInfo3);
        return Pagination.getPage(content, pageable);
    }

    private FacilityExtension mockFacilityExtension(UUID facilityId, Boolean isAndroid){
        FacilityExtension facilityExtension = new FacilityExtension();
        facilityExtension.setFacilityId(facilityId);
        facilityExtension.setIsAndroid(isAndroid);
        return facilityExtension;
    }
}
