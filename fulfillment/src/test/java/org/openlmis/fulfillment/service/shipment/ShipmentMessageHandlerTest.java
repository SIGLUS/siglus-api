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

package org.openlmis.fulfillment.service.shipment;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.FileColumnBuilder;
import org.openlmis.fulfillment.FileTemplateBuilder;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.service.FileTemplateService;
import org.openlmis.fulfillment.service.ShipmentService;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.util.FileColumnKeyPath;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CSVRecord.class, Files.class, ShipmentMessageHandler.class})
public class ShipmentMessageHandlerTest {

  private static final String NEW_MESSAGE_CSV = "/tmp/new-message.csv";
  private static final String ORDER_CODE = "O111";

  @Mock
  FileTemplateService templateService;

  @Mock
  ShipmentCsvFileParser shipmentParser;

  @Mock
  ShipmentBuilder shipmentBuilder;

  @Mock
  MessageChannel errorChannel;

  @Mock
  MessageChannel archiveChannel;

  @Mock
  ApplicationContext context;

  @Mock
  ShipmentService shipmentService;

  @Mock
  ShipmentMessageErrorHandler errorHandler;

  @InjectMocks
  ShipmentMessageHandler messageHandler;

  private File file;

  private FileTemplate template;

  @Before
  public void setup() throws Exception {
    FileTemplateBuilder templateBuilder = new FileTemplateBuilder();
    FileColumnBuilder columnBuilder = new FileColumnBuilder();

    FileColumn orderCode = columnBuilder.withPosition(0).withKeyPath("orderCode").build();

    template = templateBuilder
        .withTemplateType(TemplateType.SHIPMENT)
        .withFileColumns(asList(orderCode))
        .build();

    when(templateService.getFileTemplate(TemplateType.SHIPMENT)).thenReturn(template);

    when(context.getBean("errorChannel")).thenReturn(errorChannel);
    when(context.getBean("outboundShipmentFileArchiveChannel")).thenReturn(archiveChannel);
    file = new File(NEW_MESSAGE_CSV);
    if (!file.exists()) {
      // create file if it does not exist.
      file.createNewFile();
    }

    Message<File> mainPayload = MessageBuilder
        .withPayload(file).build();
    Message<File> errorLog = MessageBuilder
        .withPayload(file).build();

    when(errorHandler.extractLogMessages(any(), any())).thenReturn(asList(mainPayload, errorLog));
  }

  @Test
  public void shouldSendFileToErrorChannelWhenThereIsParserError() throws Exception {
    when(shipmentParser.parse(any(), any())).thenThrow(new RuntimeException());

    Message<File> fileMessage = MessageBuilder
        .withPayload(file).build();

    messageHandler.process(fileMessage);
    verify(errorChannel, times(2)).send(any());
  }

  @Test
  public void shouldSendFileToErrorChannelWhenErrorBuildingShipmentFile() throws Exception {
    doThrow(new RuntimeException()).when(shipmentBuilder).build(any(), any());

    Message<File> fileMessage = MessageBuilder
        .withPayload(file).build();

    messageHandler.process(fileMessage);
    verify(errorChannel, times(2)).send(any());
  }

  @Test
  public void shouldSendFileToErrorChannelWhenErrorPersistingShipmentFile() throws Exception {
    List<CSVRecord> records = createParsedData();
    when(shipmentParser.parse(any(), any())).thenReturn(records);
    when(shipmentBuilder.build(any(), any()))
        .thenReturn(new ShipmentDataBuilder().build());
    when(shipmentService.save(any())).thenThrow(new RuntimeException());
    Message<File> fileMessage = MessageBuilder
        .withPayload(file).build();

    messageHandler.process(fileMessage);
    verify(errorChannel, times(2)).send(any());
    verify(archiveChannel, never()).send(any());
  }


  @Test
  public void shouldSaveFileToArchiveChannelWhenThereIsNoError() throws Exception {
    List<CSVRecord> records = createParsedData();
    when(shipmentParser.parse(any(), any())).thenReturn(records);
    when(shipmentBuilder.build(any(), any()))
        .thenReturn(new ShipmentDataBuilder().build());
    Message<File> fileMessage = MessageBuilder.withPayload(file).build();

    messageHandler.process(fileMessage);
    verify(archiveChannel).send(any());
    verify(errorChannel, never()).send(any());
  }

  @Test
  public void shouldSaveShipmentWhenThereIsNoError() throws Exception {
    Shipment shipment = new ShipmentDataBuilder().build();
    when(shipmentBuilder.build(any(), any()))
        .thenReturn(shipment);
    List<CSVRecord> records = createParsedData();
    when(shipmentParser.parse(any(), any())).thenReturn(records);
    when(shipmentService.save(shipment)).thenReturn(shipment);
    Message<File> fileMessage = MessageBuilder
        .withPayload(file).build();

    messageHandler.process(fileMessage);

    verify(shipmentService).save(any());
  }

  private List<CSVRecord> createParsedData() {
    CSVRecord csvRecord = PowerMockito.mock(CSVRecord.class);
    when(csvRecord.get(FileColumnKeyPath.ORDER_CODE.toString()))
        .thenReturn(ORDER_CODE);
    when(csvRecord.get(FileColumnKeyPath.ORDERABLE_ID.toString()))
        .thenReturn(UUID.randomUUID().toString());
    when(csvRecord.get(FileColumnKeyPath.QUANTITY_SHIPPED.toString()))
        .thenReturn("1000");
    return asList(csvRecord);
  }
}