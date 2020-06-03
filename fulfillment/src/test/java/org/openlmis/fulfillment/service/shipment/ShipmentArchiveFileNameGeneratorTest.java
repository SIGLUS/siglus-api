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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@RunWith(MockitoJUnitRunner.class)
public class ShipmentArchiveFileNameGeneratorTest {

  @InjectMocks
  private ShipmentArchiveFileNameGenerator generator;

  @Test
  public void generateFileName() {
    File file = new File("tst.csv");
    Message<File> shipmentFileMessage = MessageBuilder.withPayload(file).build();

    String response = generator.generateFileName(shipmentFileMessage);

    assertTrue(response.matches("^\\d{4}_\\d{2}_\\d{2}__\\d{2}_\\d{2}_\\d{2}__tst.csv"));
  }

  @Test
  public void generateFileNameShouldNotChangeFileIfFileNameAlreadyHadDateTime() {
    File file = new File("2019_02_05__10_10_10__tst.csv");
    Message<File> shipmentFileMessage = MessageBuilder.withPayload(file).build();

    String response = generator.generateFileName(shipmentFileMessage);

    assertThat(response, is("2019_02_05__10_10_10__tst.csv"));
  }
}