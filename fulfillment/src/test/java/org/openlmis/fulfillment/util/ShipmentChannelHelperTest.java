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

package org.openlmis.fulfillment.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.domain.FtpProtocol;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.domain.TransferType;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ShipmentChannelHelperTest {

  private static final String INCOMING = "/incoming";
  private static final String ERROR = "/error";
  private static final String ARCHIVE = "/archive";

  private static final String SHIPMENT_POLLING_RATE = "1000";
  private static final String SHIPPED_BY_ID = UUID.randomUUID().toString();

  private static final String SFTP_CONTEXT_XML = "/META-INF/shipment-sftp-context.xml";
  private static final String FTP_CONTEXT_XML = "/META-INF/shipment-ftp-context.xml";
  private static final String FILE_CONTEXT_XML = "/META-INF/shipment-file-context.xml";

  @Mock
  ApplicationContext context;

  @InjectMocks
  private ShipmentChannelHelper channelHelper;


  @Before
  public void setup() {
    ReflectionTestUtils.setField(channelHelper, "pollingRate", SHIPMENT_POLLING_RATE);
    ReflectionTestUtils.setField(channelHelper, "shippedById", SHIPPED_BY_ID);
  }

  @Test
  public void getContextForLocalPath() {
    TransferProperties local = createLocalTransferProperty(TransferType.SHIPMENT);

    String contextPath = channelHelper.getContextPath(local);

    assertThat(contextPath, is(FILE_CONTEXT_XML));
  }

  @Test
  public void getContextForFtpTransferProperties() {

    TransferProperties ftp = createFtpTransferProperty(TransferType.SHIPMENT);

    String contextPath = channelHelper.getContextPath(ftp);

    assertThat(contextPath, is(FTP_CONTEXT_XML));
  }

  @Test
  public void getContextForSftpTransferProperties() {

    TransferProperties ftp = createFtpTransferProperty(TransferType.SHIPMENT);
    ((FtpTransferProperties) ftp).setProtocol(FtpProtocol.SFTP);

    String contextPath = channelHelper.getContextPath(ftp);

    assertThat(contextPath, is(SFTP_CONTEXT_XML));
  }


  @Test
  public void shouldBuildLocalProperties() throws Exception {
    LocalTransferProperties localProps = createLocalTransferProperty(TransferType.SHIPMENT);
    EnvPropertyBuilder builder = mock(EnvPropertyBuilder.class);
    when(builder.with(anyString(), anyString())).thenReturn(builder);

    channelHelper.buildProperties(builder, localProps);

    verify(builder).with("shipment.polling.rate", SHIPMENT_POLLING_RATE);
    verify(builder).with("shipment.shippedById", SHIPPED_BY_ID);
    verify(builder).with("remote.incoming.directory", localProps.getPath() + INCOMING);
    verify(builder).with("remote.error.directory", localProps.getPath() + ERROR);
    verify(builder).with("remote.archive.directory", localProps.getPath() + ARCHIVE);
  }

  @Test
  public void buildFtpProperties() throws Exception {
    FtpTransferProperties ftpProps = createFtpTransferProperty(TransferType.SHIPMENT);
    EnvPropertyBuilder builder = mock(EnvPropertyBuilder.class);
    when(builder.with(anyString(), anyString())).thenReturn(builder);

    channelHelper.buildProperties(builder, ftpProps);

    verify(builder).with("host", ftpProps.getServerHost());
    verify(builder).with("user", ftpProps.getUsername());
    verify(builder).with("password", ftpProps.getPassword());
    verify(builder).with("port", ftpProps.getServerPort().toString());
    verify(builder)
        .with("remote.incoming.directory", ftpProps.getRemoteDirectory() + INCOMING);
    verify(builder).with("remote.error.directory", ftpProps.getRemoteDirectory() + ERROR);
    verify(builder).with("remote.archive.directory", ftpProps.getRemoteDirectory() + ARCHIVE);
    verify(builder).with("local.directory", ftpProps.getLocalDirectory() + INCOMING);
  }

  private FtpTransferProperties createFtpTransferProperty(TransferType transferType) {
    FtpTransferProperties ftpTransferProperties = new FtpTransferProperties();
    ftpTransferProperties.setId(UUID.randomUUID());
    ftpTransferProperties.setFacilityId(UUID.randomUUID());
    ftpTransferProperties.setTransferType(transferType);
    ftpTransferProperties.setLocalDirectory("/var/lib/openlmis/shipments/");
    ftpTransferProperties.setRemoteDirectory("/shipment/files/csv");
    ftpTransferProperties.setPassiveMode(true);
    ftpTransferProperties.setProtocol(FtpProtocol.FTP);
    ftpTransferProperties.setServerHost("localhost");
    ftpTransferProperties.setUsername("random-user");
    ftpTransferProperties.setPassword("random-password");
    ftpTransferProperties.setServerPort(1000);
    return ftpTransferProperties;
  }

  private LocalTransferProperties createLocalTransferProperty(TransferType transferType) {
    LocalTransferProperties localTransferProperties = new LocalTransferProperties();
    localTransferProperties.setId(UUID.randomUUID());
    localTransferProperties.setFacilityId(UUID.randomUUID());
    localTransferProperties.setTransferType(transferType);
    localTransferProperties.setPath("/var/lib/openlmis/shipments");
    return localTransferProperties;
  }
}