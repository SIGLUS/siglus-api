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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.service.FulfillmentException;
import org.springframework.messaging.Message;

@RunWith(MockitoJUnitRunner.class)
public class ShipmentMessageErrorHandlerTest {

  private static final String PREFIX = "2019_10_10__10_10_10__";
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @Mock
  ShipmentArchiveFileNameGenerator generator;
  @InjectMocks
  ShipmentMessageErrorHandler errorHandler;

  File tempDirectory;

  File originalFile;

  @Before
  public void setup() throws IOException {
    tempDirectory = folder.newFolder("openlmis", "shipments", "csv");
    originalFile = folder.newFile("openlmis/shipments/csv/test.csv");
    when(generator.generatePrefix()).thenReturn(PREFIX);
  }

  @After
  public void cleanUp() {
    folder.delete();
  }

  @Test
  public void shouldMoveOriginalFileToPrefixedFileName() throws Exception {
    RuntimeException exception = new FulfillmentException("Failed for reason 1");

    List<Message<File>> messages = errorHandler.extractLogMessages(originalFile, exception);

    assertEquals(2, messages.size());
    assertTrue(messages.get(0).getPayload().getName().startsWith(PREFIX));
  }

  @Test
  public void secondMessageShouldContainErrorLog() throws Exception {
    RuntimeException exception = new FulfillmentException("Failed for reason 1");

    List<Message<File>> messages = errorHandler.extractLogMessages(originalFile, exception);

    File errorLogFile = messages.get(1).getPayload();
    assertTrue(errorLogFile.getName().startsWith(PREFIX));
    assertTrue(errorLogFile.getName().endsWith(".log"));
    String log = FileUtils.readFileToString(errorLogFile);
    assertTrue(log.contains("Failed for reason 1"));
  }

}