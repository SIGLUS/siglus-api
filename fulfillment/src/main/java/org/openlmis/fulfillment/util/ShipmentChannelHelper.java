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

import java.util.Properties;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.FtpProtocol;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.service.FulfillmentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class ShipmentChannelHelper {

  private static final String INCOMING = "/incoming";
  private static final String ERROR = "/error";
  private static final String ARCHIVE = "/archive";

  private static final String SFTP_CONTEXT_XML = "/META-INF/shipment-sftp-context.xml";
  private static final String FTP_CONTEXT_XML = "/META-INF/shipment-ftp-context.xml";
  private static final String FILE_CONTEXT_XML = "/META-INF/shipment-file-context.xml";

  @Value("${shipment.polling.rate}")
  private String pollingRate;

  @Value("${shipment.shippedById}")
  private String shippedById;

  /**
   * Returns the appropriate spring context xml for provided transfer property.
   *
   * @param transferProperties TransferProperties
   */
  public String getContextPath(TransferProperties transferProperties) {
    if (transferProperties instanceof FtpTransferProperties) {
      return (FtpProtocol.SFTP.equals(((FtpTransferProperties) transferProperties).getProtocol()))
          ? SFTP_CONTEXT_XML
          : FTP_CONTEXT_XML;
    } else if (transferProperties instanceof LocalTransferProperties) {
      return FILE_CONTEXT_XML;
    }
    throw new FulfillmentException("Context path could not be resolved.");
  }

  /**
   * builds environment properties for a local transfer properties.
   *
   * @param local LocalTransferProperties
   * @return Properties
   */
  private Properties buildLocalProperties(EnvPropertyBuilder builder,
      LocalTransferProperties local) {

    return builder.with("shipment.polling.rate", pollingRate)
        .with("shipment.shippedById", shippedById)
        .with("remote.incoming.directory", local.getPath() + INCOMING)
        .with("remote.archive.directory", local.getPath() + ARCHIVE)
        .with("remote.error.directory", local.getPath() + ERROR)
        .build();
  }

  /**
   * builds a properties object for FTP transfer type.
   *
   * @param ftp FTPTransferType
   * @return Properties
   */
  private Properties buildFtpProperties(EnvPropertyBuilder builder,
      FtpTransferProperties ftp) {
    return builder.with("host", ftp.getServerHost())
        .with("user", ftp.getUsername())
        .with("password", ftp.getPassword())
        .with("port", ftp.getServerPort().toString())

        .with("shipment.polling.rate", pollingRate)
        .with("shipment.shippedById", shippedById)

        .with("remote.incoming.directory", ftp.getRemoteDirectory() + INCOMING)
        .with("remote.archive.directory", ftp.getRemoteDirectory() + ARCHIVE)
        .with("remote.error.directory", ftp.getRemoteDirectory() + ERROR)

        .with("local.directory", ftp.getLocalDirectory() + INCOMING)
        .build();
  }

  /**
   * returns the appropriate environment properties object.
   *
   * @param properties Properties
   * @return Properties
   */
  public Properties buildProperties(EnvPropertyBuilder builder, TransferProperties properties) {
    if (properties instanceof FtpTransferProperties) {
      return buildFtpProperties(builder, (FtpTransferProperties) properties);
    } else {
      return buildLocalProperties(builder, (LocalTransferProperties) properties);
    }
  }

  /**
   * Creates and returns Configurable Application Context.
   * @param transferProperties transfer properties.
   * @param applicationContext parent application context.
   * @return ConfigurableApplicationContext.
   */
  public synchronized ConfigurableApplicationContext createChannel(
      TransferProperties transferProperties,
      ApplicationContext applicationContext) {
    ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
        new String[]{this.getContextPath(transferProperties)}, false, applicationContext);
    EnvPropertyBuilder builder = new EnvPropertyBuilder();
    ctx.getEnvironment().getPropertySources()
        .addLast(
            new PropertiesPropertySource("shipment", buildProperties(builder, transferProperties)));
    ctx.refresh();
    return ctx;
  }

}
