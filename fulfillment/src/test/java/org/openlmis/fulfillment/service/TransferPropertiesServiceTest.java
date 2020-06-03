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

package org.openlmis.fulfillment.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;
import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.ShipmentContextRunner;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.openlmis.fulfillment.service.referencedata.FacilityDto;
import org.openlmis.fulfillment.service.referencedata.FacilityReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class TransferPropertiesServiceTest {

  @Mock
  private TransferPropertiesRepository transferPropertiesRepository;

  @Mock
  private FacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private ShipmentContextRunner shipmentContextRunner;

  @InjectMocks
  private TransferPropertiesService transferPropertiesService;

  @Test
  public void shouldSaveSetting() {
    // given
    final TransferProperties properties = randomSetting(TransferType.ORDER);
    final FacilityDto facility = mock(FacilityDto.class);

    doNothing().when(shipmentContextRunner).reCreateShipmentChannel(any());
    when(facility.getId()).thenReturn(UUID.randomUUID());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facility);
    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(UUID.class), any()))
        .thenReturn(null);
    when(transferPropertiesRepository.save(properties)).thenReturn(properties);
    // when
    transferPropertiesService.save(properties);

    // then
    verify(transferPropertiesRepository, atLeastOnce()).save(properties);
    verify(shipmentContextRunner, never()).reCreateShipmentChannel(any());
  }

  @Test
  public void saveShouldReCreateShipmentChannelForShipmentTransferProperties() {
    // given
    final TransferProperties properties = randomSetting(TransferType.SHIPMENT);
    final FacilityDto facility = mock(FacilityDto.class);

    doNothing().when(shipmentContextRunner).reCreateShipmentChannel(any());
    when(facility.getId()).thenReturn(UUID.randomUUID());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facility);
    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(UUID.class), any()))
        .thenReturn(null);
    when(transferPropertiesRepository.save(properties)).thenReturn(properties);

    // when
    transferPropertiesService.save(properties);

    // then
    verify(shipmentContextRunner, atLeastOnce()).reCreateShipmentChannel(any());
  }

  @Test(expected = DuplicateTransferPropertiesException.class)
  public void shouldNotSaveSettingIfFacilityIdDuplicated() {
    // given
    final TransferProperties properties = randomSetting(TransferType.ORDER);
    final TransferProperties duplicate = randomSetting(TransferType.ORDER);
    FacilityDto facility = mock(FacilityDto.class);

    when(facility.getId()).thenReturn(UUID.randomUUID());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(facility);
    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(UUID.class), any()))
        .thenReturn(duplicate);

    // when
    transferPropertiesService.save(properties);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotSaveSettingIfFacilityDoesNotExist() {
    // given
    final TransferProperties properties = randomSetting(TransferType.ORDER);

    doNothing().when(shipmentContextRunner).reCreateShipmentChannel(any());
    when(facilityReferenceDataService.findOne(any(UUID.class))).thenReturn(null);
    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(UUID.class), any()))
        .thenReturn(null);

    // when
    transferPropertiesService.save(properties);
  }

  @Test
  public void shouldGetByFacility() {
    // given
    TransferProperties properties = randomSetting(TransferType.ORDER);

    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(UUID.class), any()))
        .thenReturn(properties);

    // when
    TransferProperties result = transferPropertiesService
        .getByFacility(properties.getFacilityId(), TransferType.ORDER);

    // then
    assertEquals(result.getId(), properties.getId());
  }

  @Test
  public void shouldNotGetByFacilityIfFacilityDoesNotExist() {
    // given
    TransferProperties properties = randomSetting(TransferType.ORDER);

    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(UUID.class), any()))
        .thenReturn(null);

    // when
    TransferProperties result = transferPropertiesService
        .getByFacility(properties.getFacilityId(), TransferType.ORDER);

    // then
    assertNull(result);
  }

  private FtpTransferProperties randomSetting(TransferType transferType) {
    FtpTransferProperties properties = new FtpTransferProperties();
    properties.setId(UUID.randomUUID());
    properties.setFacilityId(UUID.randomUUID());
    properties.setServerHost(RandomStringUtils.random(10));
    properties.setServerPort(new Random().nextInt(1000));
    properties.setRemoteDirectory(RandomStringUtils.random(10));
    properties.setUsername(RandomStringUtils.random(10));
    properties.setPassword(RandomStringUtils.random(10));
    properties.setTransferType(transferType);
    return properties;
  }
}
