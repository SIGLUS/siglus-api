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

package org.openlmis.fulfillment;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.openlmis.fulfillment.util.ShipmentChannelHelper;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest()
public class ShipmentContextRunnerTest {

  @Mock
  private TransferPropertiesRepository transferPropertiesRepository;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private ShipmentChannelHelper channelHelper;

  @InjectMocks
  private ShipmentContextRunner shipmentContextRunner;

  @Before
  public void setup() {
  }

  @Test
  public void shouldInitializeCustomContextForLocalTransferType() throws Exception {
    TransferProperties localTransferProperties = createLocalTransferProperty(TransferType.SHIPMENT);
    when(transferPropertiesRepository.findByTransferType(TransferType.SHIPMENT))
        .thenReturn(asList(localTransferProperties));
    ClassPathXmlApplicationContext mockContext = mock(ClassPathXmlApplicationContext.class);
    when(channelHelper.createChannel(localTransferProperties, applicationContext))
        .thenReturn(mockContext);

    shipmentContextRunner.run();

    verify(channelHelper).createChannel(localTransferProperties, applicationContext);
    Map<UUID, ConfigurableApplicationContext> contexts = Whitebox
        .getInternalState(shipmentContextRunner, "contexts");

    ConfigurableApplicationContext context = contexts.get(localTransferProperties.getId());
    assertNotNull(context);
  }

  @Test
  public void shouldInitializeCustomContextForFtpTransferType() throws Exception {
    TransferProperties ftpTransferProperties = createFtpTransferProperty(TransferType.SHIPMENT);
    when(transferPropertiesRepository.findByTransferType(TransferType.SHIPMENT))
        .thenReturn(asList(ftpTransferProperties));

    ClassPathXmlApplicationContext mockContext = mock(ClassPathXmlApplicationContext.class);
    doNothing().when(mockContext).close();
    when(channelHelper.createChannel(ftpTransferProperties, applicationContext))
        .thenReturn(mockContext);

    shipmentContextRunner.run();

    verify(channelHelper).createChannel(ftpTransferProperties, applicationContext);

    Map<UUID, ConfigurableApplicationContext> contexts = Whitebox
        .getInternalState(shipmentContextRunner, "contexts");

    ConfigurableApplicationContext context = contexts.get(ftpTransferProperties.getId());
    assertNotNull(context);
  }


  @Test
  public void reCreateContextShouldCloseExistingContext() throws Exception {
    FtpTransferProperties ftpProps = createFtpTransferProperty(TransferType.SHIPMENT);
    when(transferPropertiesRepository.findByTransferType(TransferType.SHIPMENT))
        .thenReturn(asList(ftpProps));
    // this initializes the context for the first time.
    ClassPathXmlApplicationContext mockContext = mock(ClassPathXmlApplicationContext.class);
    when(channelHelper.createChannel(ftpProps, applicationContext))
        .thenReturn(mockContext);
    shipmentContextRunner.run();

    shipmentContextRunner.reCreateShipmentChannel(ftpProps);
    verify(channelHelper, times(2)).createChannel(ftpProps, applicationContext);
    verify(mockContext).close();
  }

  @Test
  public void reCreateContextShouldNotCloseIfContextIsNew() throws Exception {
    FtpTransferProperties ftpProps = createFtpTransferProperty(TransferType.SHIPMENT);
    when(transferPropertiesRepository.findByTransferType(TransferType.SHIPMENT))
        .thenReturn(asList(ftpProps));
    // this initializes the context for the first time.
    ClassPathXmlApplicationContext mockContext = mock(ClassPathXmlApplicationContext.class);
    when(channelHelper.createChannel(ftpProps, applicationContext))
        .thenReturn(mockContext);
    shipmentContextRunner.reCreateShipmentChannel(ftpProps);
    verify(channelHelper, times(1)).createChannel(ftpProps, applicationContext);
    verify(mockContext, never()).close();
  }

  private FtpTransferProperties createFtpTransferProperty(TransferType transferType) {
    FtpTransferProperties ftpTransferProperties = new FtpTransferProperties();
    ftpTransferProperties.setId(UUID.randomUUID());
    ftpTransferProperties.setTransferType(transferType);
    return ftpTransferProperties;
  }

  private TransferProperties createLocalTransferProperty(TransferType transferType) {
    LocalTransferProperties localTransferProperties = new LocalTransferProperties();
    localTransferProperties.setId(UUID.randomUUID());
    localTransferProperties.setTransferType(transferType);
    return localTransferProperties;
  }

}