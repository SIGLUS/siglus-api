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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.FtpProtocol;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;

@RunWith(MockitoJUnitRunner.class)
public class OrderSenderTest {

  @Mock
  private ProducerTemplate producerTemplate;

  @Mock
  private OrderStorage orderStorage;

  @Mock
  private TransferPropertiesRepository transferPropertiesRepository;

  @InjectMocks
  private OrderFtpSender orderFtpSender;

  @Mock
  private Order order;

  @Mock
  private Path path;

  @Mock
  private File file;

  @Before
  public void setUp() throws Exception {
    FtpTransferProperties setting = new FtpTransferProperties();
    setting.setId(UUID.randomUUID());
    setting.setFacilityId(UUID.randomUUID());
    setting.setProtocol(FtpProtocol.FTP);
    setting.setServerHost("host");
    setting.setServerPort(21);
    setting.setRemoteDirectory("remote/dir");
    setting.setLocalDirectory("local/dir");
    setting.setUsername("username");
    setting.setPassword("password");
    setting.setPassiveMode(true);
    setting.setTransferType(TransferType.ORDER);

    when(orderStorage.getOrderAsPath(order)).thenReturn(path);
    when(transferPropertiesRepository.findFirstByFacilityIdAndTransferType(any(), any()))
        .thenReturn(setting);

    when(path.toFile()).thenReturn(file);
  }

  @Test
  public void shouldReturnTrueIfMessageHasBeenSentSuccessfully() throws Exception {
    assertThat(orderFtpSender.send(order), is(true));
  }

  @Test
  public void shouldReturnFalseIfMessageHasNotBeenSentSuccessfully() throws Exception {
    doThrow(new RuntimeException("test purpose"))
    .when(producerTemplate).sendBodyAndHeader(anyString(), any(), eq(Exchange.FILE_NAME), any());

    assertThat(orderFtpSender.send(order), is(false));
  }
}
