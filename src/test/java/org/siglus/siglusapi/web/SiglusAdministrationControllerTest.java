package org.siglus.siglusapi.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.SiglusAdministrationsService;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SiglusAdministrationControllerTest {

    @InjectMocks
    private SiglusAdministrationsController siglusAdministrationsController;

    @Mock
    private SiglusAdministrationsService siglusAdministrationsService;

    @Test
    public void shouldDisplayFacilitiesWithIsAndroid(){
        //when
        siglusAdministrationsController.showFacilitiesInfos(null, null, null);

        //then
        verify(siglusAdministrationsService).searchForFacilities(null, null, null);
    }

    @Test
    public void eraseAndroidByFacilityId(){
        //given
        UUID uuid = UUID.randomUUID();

        //when
        siglusAdministrationsController.eraseAndroidDevice(uuid);

        //then
        verify(siglusAdministrationsService).eraseAndroidByFacilityId(uuid);
    }
}
