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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.android.UsageSectionConstants.MmiaPatientLineItems.TABLE_DISPENSED_DB_KEY;
import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.ANDROID_REQUISITION_INTERNAL_APPROVED;
import static org.siglus.siglusapi.dto.enums.EventCategoryEnum.REQUISITION_FINAL_APPROVED;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.dto.android.request.PatientLineItemsRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ShedLockFactory;
import org.siglus.siglusapi.localmachine.ShedLockFactory.AutoClosableLock;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEvent;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecord;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataEventRecordRepository;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataOffset;
import org.siglus.siglusapi.localmachine.eventstore.MasterDataOffsetRepository;
import org.siglus.siglusapi.localmachine.repository.MasterDataSql;
import org.siglus.siglusapi.localmachine.repository.MovementSql;
import org.siglus.siglusapi.localmachine.repository.RequisitionOrderSql;
import org.siglus.siglusapi.localmachine.repository.TableCopyRepository;
import org.siglus.siglusapi.localmachine.webapi.ResyncMasterDataResponse;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.siglus.siglusapi.util.S3FileHandler;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OnlineWebServiceTest {

  @Mock
  private TableCopyRepository tableCopyRepository;

  @Mock
  private MasterDataEventRecordRepository masterDataEventRecordRepository;

  @Mock
  private MasterDataOffsetRepository masterDataOffsetRepository;

  @Mock
  private S3FileHandler s3FileHandler;

  @Mock
  private ShedLockFactory lockFactory;

  @Mock
  private SiglusAdministrationsService administrationsService;

  @Mock
  private AppInfoRepository appInfoRepository;

  @Mock
  private FacilityRepository facilityRepository;

  @InjectMocks
  private OnlineWebService onlineWebService;

  private static final UUID facilityId = UUID.randomUUID();
  private static final String facilityCode = "01100206";

  @Test
  public void shouldReturnMinOneOfSnapshotIdOrMinimumOffsetWhenGetMinimumOffset() {
    // given
    given(masterDataOffsetRepository.getMinimumOffset()).willReturn(33L);
    long minOffset = 9L;
    given(masterDataEventRecordRepository.findBySnapshotVersionIsNotNull())
        .willReturn(Collections.singletonList(MasterDataEventRecord.builder().id(minOffset).build()));
    // then
    assertThat(onlineWebService.getMinimumOffsetForDeletion()).isEqualTo(minOffset);
  }

  @Test
  public void shouldReturn0WhenGetMinimumOffsetForDeletionGivenNoOffsetFound() {
    // given
    given(masterDataOffsetRepository.getMinimumOffset()).willReturn(null);
    given(masterDataEventRecordRepository.findBySnapshotVersionIsNotNull())
        .willReturn(Collections.singletonList(MasterDataEventRecord.builder().id(99L).build()));
    // then
    assertThat(onlineWebService.getMinimumOffsetForDeletion()).isZero();
  }

  @Test
  public void shouldReturn0WhenGetMinimumOffsetForDeletionGivenSnapshotNotExists() {
    // given
    given(masterDataEventRecordRepository.findBySnapshotVersionIsNotNull()).willReturn(Collections.emptyList());
    // then
    assertThat(onlineWebService.getMinimumOffsetForDeletion()).isZero();
  }

  @Test
  public void shouldSkipLastSnapshotWhenCleanMasterDataSnapshot() {
    // given
    MasterDataEventRecord snap1 = MasterDataEventRecord.builder().id(11L).snapshotVersion("11.zip").build();
    MasterDataEventRecord snap2 = MasterDataEventRecord.builder().id(22L).snapshotVersion("22.zip").build();
    MasterDataEventRecord snap3 = MasterDataEventRecord.builder().id(33L).snapshotVersion("33.zip").build();
    given(masterDataEventRecordRepository.findBySnapshotVersionIsNotNull())
        .willReturn(Arrays.asList(snap1, snap3, snap2));
    // when
    onlineWebService.cleanMasterDataSnapshot();
    // then
    ArgumentCaptor<String> deletedFileNames = ArgumentCaptor.forClass(String.class);
    verify(s3FileHandler, VerificationModeFactory.atLeastOnce())
        .deleteFileFromS3(deletedFileNames.capture());
    assertThat(deletedFileNames.getAllValues())
        .containsExactlyInAnyOrder("masterdata/11.zip", "masterdata/22.zip");
    ArgumentCaptor<LinkedList> deletedRecordCaptor = ArgumentCaptor.forClass(LinkedList.class);
    verify(masterDataEventRecordRepository, VerificationModeFactory.atLeastOnce())
        .delete(deletedRecordCaptor.capture());
    assertThat(deletedRecordCaptor.getValue()).containsExactlyInAnyOrder(snap1, snap2);
  }

  @Test
  public void shouldGenerateZipWhenLocalMachineResync() throws IOException, InterruptedException {
    // given
    mockWaitLock();
    ReflectionTestUtils.setField(onlineWebService, "zipExportPath", "/tmp/simam/resync/");
    List<File> tableFiles = new ArrayList<>();
    tableFiles.add(new File("/tmp/movement.txt"));
    tableFiles.add(new File("/tmp/requisitionOrder.txt"));

    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(tableCopyRepository.copyDateToFile(any(), eq(MovementSql.getMovementSql()), eq(facilityId)))
        .thenReturn(Collections.singletonList(tableFiles.get(0)));
    when(tableCopyRepository.copyDateToFile(any(), eq(RequisitionOrderSql.getRequisitionOrderSql()), eq(facilityId)))
        .thenReturn(Collections.singletonList(tableFiles.get(1)));

    writeDataToFile(tableFiles.get(0));
    writeDataToFile(tableFiles.get(1));

    HttpServletResponse httpServletResponse = new MockHttpServletResponse();

    // when
    onlineWebService.resyncData(facilityId, httpServletResponse);

    // then
    verify(tableCopyRepository, times(1)).copyDateToFile(any(), eq(MovementSql.getMovementSql()), eq(facilityId));
    verify(tableCopyRepository, times(1))
        .copyDateToFile(any(), eq(RequisitionOrderSql.getRequisitionOrderSql()), eq(facilityId));
    verify(administrationsService, times(1)).deleteDrafts(facilityId);
  }

  @Test
  public void shouldReturnS3UrlWhenLocalMachineResyncMasterData() throws IOException {
    // given
    ReflectionTestUtils.setField(onlineWebService, "zipExportPath", "/tmp/simam/resync/");
    List<File> tableFiles = Collections.singletonList(new File("/tmp/masterdata.txt"));
    when(tableCopyRepository.copyDateToFile(any(), eq(MasterDataSql.getMasterDataSqlMap()), eq(facilityId)))
        .thenReturn(Collections.singletonList(tableFiles.get(0)));
    writeDataToFile(tableFiles.get(0));
    String snapshotVersion = "test.zip";
    when(masterDataEventRecordRepository.save(any(MasterDataEventRecord.class)))
        .thenReturn(MasterDataEventRecord.builder().id(10L).snapshotVersion(snapshotVersion).build());
    when(masterDataEventRecordRepository.findLatestRecordId()).thenReturn(15L);
    when(s3FileHandler.getUrlFromS3("masterdata/" + snapshotVersion))
        .thenReturn("https://test.zip");

    // when
    ResyncMasterDataResponse resp = onlineWebService.resyncMasterData(facilityId);

    // then
    assertEquals("https://test.zip", resp.getDownloadUrl());
    verify(tableCopyRepository, times(1))
        .copyMasterDateToFile(any(), eq(MasterDataSql.getMasterDataSqlMap()), eq(null));
    verify(masterDataEventRecordRepository, times(1)).save(any(MasterDataEventRecord.class));
    verify(masterDataOffsetRepository, times(1)).save(any(MasterDataOffset.class));
  }

  @Test
  public void shouldReturnFinalApproveEventWhenVersionIsEqual2011() {
    // given
    Event e = Event.builder().category(REQUISITION_FINAL_APPROVED.name()).receiverId(facilityId).build();
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.0.11"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(e));

    // then
    assertEquals(results.size(), 1);
  }

  @Test
  public void shouldReturnFinalApproveEventWhenVersionIsAfter2011() {
    // given
    Event e = Event.builder().category(REQUISITION_FINAL_APPROVED.name()).receiverId(facilityId).build();
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.0.12"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(e));

    // then
    assertEquals(results.size(), 1);
  }

  @Test
  public void shouldReturnFinalApproveEventWhenVersionIs210() {
    // given
    Event e = Event.builder().category(REQUISITION_FINAL_APPROVED.name()).receiverId(facilityId).build();
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.1.0"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(e));

    // then
    assertEquals(results.size(), 1);
  }

  @Test
  public void shouldNotReturnFinalApproveEventWhenVersionIsBefore2011() {
    // given
    Event e = Event.builder().category(REQUISITION_FINAL_APPROVED.name()).receiverId(facilityId).build();
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.0.6"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(e));

    // then
    assertEquals(results.size(), 0);
  }

  @Test
  public void shouldReturnOriginalListForBetaVersion() {
    // given
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.1.0-beta-03"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(mock210MiaRequisition()));

    // then
    assertEquals(results.size(), 1);
    AndroidRequisitionSyncedEvent payload = (AndroidRequisitionSyncedEvent) results.get(0).getPayload();
    List<PatientLineItemsRequest> dbLineItems = payload.getRequest().getPatientLineItems().stream()
        .filter(patientLineItem -> TABLE_DISPENSED_DB_KEY.equals(patientLineItem.getName()))
        .collect(Collectors.toList());
    assertEquals(dbLineItems.size(), 1);
  }

  @Test
  public void shouldIgnoreDbWhenVersionIs2019() {
    // given
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.0.19"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(mock210MiaRequisition()));

    // then
    assertEquals(results.size(), 1);
    AndroidRequisitionSyncedEvent payload = (AndroidRequisitionSyncedEvent) results.get(0).getPayload();
    List<PatientLineItemsRequest> dbLineItems = payload.getRequest().getPatientLineItems().stream()
        .filter(patientLineItem -> TABLE_DISPENSED_DB_KEY.equals(patientLineItem.getName()))
        .collect(Collectors.toList());
    assertEquals(dbLineItems.size(), 0);
  }

  @Test
  public void shouldKeepDbWhenVersionIs210() {
    // given
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.1.0"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(mock210MiaRequisition()));

    // then
    assertEquals(results.size(), 1);
    AndroidRequisitionSyncedEvent payload = (AndroidRequisitionSyncedEvent) results.get(0).getPayload();
    List<PatientLineItemsRequest> dbLineItems = payload.getRequest().getPatientLineItems().stream()
        .filter(patientLineItem -> TABLE_DISPENSED_DB_KEY.equals(patientLineItem.getName()))
        .collect(Collectors.toList());
    assertEquals(dbLineItems.size(), 1);
  }

  @Test
  public void shouldKeepDbWhenVersionIs211() {
    // given
    when(facilityRepository.findOne(facilityId)).thenReturn(mockFacility());
    when(appInfoRepository.findByFacilityCode(facilityCode)).thenReturn(mockAppInfo("2.1.1"));
    // when
    List<Event> results = onlineWebService.filterFinalApproveEventForOldVersion(Arrays.asList(mock210MiaRequisition()));

    // then
    assertEquals(results.size(), 1);
    AndroidRequisitionSyncedEvent payload = (AndroidRequisitionSyncedEvent) results.get(0).getPayload();
    List<PatientLineItemsRequest> dbLineItems = payload.getRequest().getPatientLineItems().stream()
        .filter(patientLineItem -> TABLE_DISPENSED_DB_KEY.equals(patientLineItem.getName()))
        .collect(Collectors.toList());
    assertEquals(dbLineItems.size(), 1);
  }

  private Facility mockFacility() {
    Facility facility = new Facility();
    facility.setCode(facilityCode);
    return facility;
  }

  private AppInfo mockAppInfo(String version) {
    return AppInfo.builder()
        .facilityCode(facilityCode)
        .versionCode(version)
        .build();
  }

  private void writeDataToFile(File file) throws IOException {
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
    out.write("test");
    out.close();
  }

  private void mockWaitLock() throws InterruptedException {
    AutoClosableLock lock = new AutoClosableLock(Optional.ofNullable(mock(SimpleLock.class)));
    given(lockFactory.waitLock(anyString(), anyLong())).willReturn(lock);
  }

  private Event mock210MiaRequisition() {
    PatientLineItemsRequest lineItemsRequest = PatientLineItemsRequest
        .builder()
        .name(TABLE_DISPENSED_DB_KEY)
        .build();
    List<PatientLineItemsRequest> patientLineItemsRequests = Arrays.asList(lineItemsRequest);
    RequisitionCreateRequest createRequest = RequisitionCreateRequest.builder()
        .patientLineItems(patientLineItemsRequests).build();
    AndroidRequisitionSyncedEvent payload = AndroidRequisitionSyncedEvent.builder()
        .request(createRequest).build();
    return Event.builder()
        .category(ANDROID_REQUISITION_INTERNAL_APPROVED.name()).receiverId(facilityId).payload(payload).build();
  }
}
