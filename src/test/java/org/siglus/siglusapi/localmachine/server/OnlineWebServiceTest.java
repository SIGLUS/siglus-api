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

package org.siglus.siglusapi.localmachine.server;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.repository.MasterDataSql;
import org.siglus.siglusapi.localmachine.repository.MovementSql;
import org.siglus.siglusapi.localmachine.repository.RequisitionOrderSql;
import org.siglus.siglusapi.localmachine.repository.TableCopyRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OnlineWebServiceTest {

  @Mock
  private TableCopyRepository tableCopyRepository;

  @InjectMocks
  private OnlineWebService onlineWebService;

  private static final UUID facilityId = UUID.randomUUID();

  @Test
  public void shouldGenerateZipWhenLocalMachineResync() throws IOException {
    // given
    ReflectionTestUtils.setField(onlineWebService, "zipExportPath", "/tmp/");
    List<File> tableFiles = new ArrayList<>();
    tableFiles.add(new File("/tmp/masterdata.txt"));
    tableFiles.add(new File("/tmp/movement.txt"));
    tableFiles.add(new File("/tmp/requisitionOrder.txt"));

    when(tableCopyRepository.copyDateToFile(any(), eq(MasterDataSql.getMasterDataSqlMap()), eq(facilityId)))
        .thenReturn(Collections.singletonList(tableFiles.get(0)));
    when(tableCopyRepository.copyDateToFile(any(), eq(MovementSql.getMovementSql()), eq(facilityId)))
        .thenReturn(Collections.singletonList(tableFiles.get(1)));
    when(tableCopyRepository.copyDateToFile(any(), eq(RequisitionOrderSql.getRequisitionOrderSql()), eq(facilityId)))
        .thenReturn(Collections.singletonList(tableFiles.get(2)));

    writeDataToFile(tableFiles.get(0));
    writeDataToFile(tableFiles.get(1));
    writeDataToFile(tableFiles.get(2));

    HttpServletResponse httpServletResponse = new MockHttpServletResponse();

    // when
    onlineWebService.reSyncData(facilityId, httpServletResponse);

    // then
    verify(tableCopyRepository, times(1))
        .copyDateToFile(any(), eq(MasterDataSql.getMasterDataSqlMap()), eq(facilityId));
    verify(tableCopyRepository, times(1)).copyDateToFile(any(), eq(MovementSql.getMovementSql()), eq(facilityId));
    verify(tableCopyRepository, times(1))
        .copyDateToFile(any(), eq(RequisitionOrderSql.getRequisitionOrderSql()), eq(facilityId));
  }

  private void writeDataToFile(File file) throws IOException {
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
    out.write("test");
    out.close();
  }
}
