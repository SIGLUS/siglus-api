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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_IO;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.TransferType;
import org.openlmis.fulfillment.repository.TransferPropertiesRepository;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OrderFileStorage.class, FileTemplate.class})
public class OrderStorageTest {
  private static final String FILE_PREFIX = "prefix-";
  private static final String ORDER_CODE = "order-code-123";
  private static final String FILE_NAME = FILE_PREFIX + ORDER_CODE + ".csv";
  private static final String LOCAL_DIR = "/var/lib/openlmis/fulfillment/orders/";
  private static final String FULL_PATH = LOCAL_DIR + FILE_NAME;

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Mock
  private OrderCsvHelper csvHelper;

  @Mock
  private FileTemplateService fileTemplateService;

  @Mock
  private TransferPropertiesRepository transferPropertiesRepository;

  @InjectMocks
  private OrderFileStorage orderFileStorage;

  @Mock
  private Order order;

  @Mock
  private FileTemplate template;

  @Mock
  private BufferedWriter writer;

  @Mock
  private FtpTransferProperties properties;

  private IOException exception = new IOException("test purpose");

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(Files.class);

    when(Files.newBufferedWriter(any(Path.class))).thenReturn(writer);
    when(fileTemplateService.getOrderFileTemplate()).thenReturn(template);
    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(any(), any()))
        .thenReturn(properties);

    when(order.getOrderCode()).thenReturn(ORDER_CODE);
    when(template.getFilePrefix()).thenReturn(FILE_PREFIX);
    when(properties.getPath()).thenReturn(LOCAL_DIR);
  }

  @Test
  public void shouldStoreAnOrder() throws Exception {
    orderFileStorage.store(order);

    verify(fileTemplateService).getOrderFileTemplate();
    verify(csvHelper).writeCsvFile(order, template, writer);

    ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);

    verifyStatic();
    Files.newBufferedWriter(captor.capture());

    Path value = captor.getValue();
    assertThat(value.toString(), is(FULL_PATH));
  }

  @Test
  public void shouldThrowExceptionIfThereIsProblemWithStoringAnOrder() throws Exception {
    doThrow(exception).when(csvHelper).writeCsvFile(order, template, writer);

    expected.expect(OrderStorageException.class);
    expected.expectMessage(ERROR_IO);
    expected.expectCause(is(exception));

    orderFileStorage.store(order);
  }

  @Test
  public void shouldHandleSituationWhenPropertiesDoesNotExist() throws OrderStorageException {
    when(transferPropertiesRepository
        .findFirstByFacilityIdAndTransferType(order.getFacilityId(), TransferType.ORDER))
        .thenReturn(null);

    orderFileStorage.store(order);

    verify(transferPropertiesRepository)
        .findFirstByFacilityIdAndTransferType(order.getFacilityId(), TransferType.ORDER);
    verifyZeroInteractions(fileTemplateService, csvHelper);
  }

  @Test
  public void shouldDeleteAnOrder() throws Exception {
    orderFileStorage.delete(order);

    ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);

    verifyStatic();
    Files.deleteIfExists(captor.capture());

    Path value = captor.getValue();
    assertThat(value.toString(), is(FULL_PATH));
  }

  @Test
  public void shouldThrowExceptionIfThereIsProblemWithDeletingAnOrder() throws Exception {
    when(Files.deleteIfExists(any(Path.class))).thenThrow(exception);

    expected.expect(OrderStorageException.class);
    expected.expectMessage(ERROR_IO);
    expected.expectCause(is(exception));

    orderFileStorage.delete(order);
  }

  @Test
  public void shouldReturnOrderAsPath() throws Exception {
    Path path = orderFileStorage.getOrderAsPath(order);
    assertThat(path.toString(), is(FULL_PATH));
  }

}
