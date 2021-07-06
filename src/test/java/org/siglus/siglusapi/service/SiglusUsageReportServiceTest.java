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

package org.siglus.siglusapi.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.common.constant.FieldConstants.ACTUAL_END_DATE;
import static org.siglus.common.constant.FieldConstants.ACTUAL_START_DATE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.joda.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.common.domain.referencedata.Dispensable;
import org.siglus.common.domain.referencedata.Orderable;
import org.siglus.common.domain.referencedata.OrderableDisplayCategory;
import org.siglus.common.domain.referencedata.Program;
import org.siglus.common.domain.referencedata.ProgramOrderable;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.siglusapi.domain.AvailableUsageColumn;
import org.siglus.siglusapi.domain.AvailableUsageColumnSection;
import org.siglus.siglusapi.domain.KitUsageLineItem;
import org.siglus.siglusapi.domain.UsageCategory;
import org.siglus.siglusapi.domain.UsageTemplateColumn;
import org.siglus.siglusapi.domain.UsageTemplateColumnSection;
import org.siglus.siglusapi.dto.KitUsageLineItemDto;
import org.siglus.siglusapi.dto.KitUsageServiceLineItemDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.sequence.RequisitionActionSequence;
import org.siglus.siglusapi.repository.KitUsageLineItemRepository;
import org.siglus.siglusapi.repository.UsageTemplateColumnSectionRepository;
import org.springframework.beans.BeanUtils;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.TooManyMethods"})
@RunWith(MockitoJUnitRunner.class)
public class SiglusUsageReportServiceTest {

  static final String USER_INPUT = "USER_INPUT";
  static final String COLLECTION = "collection";
  static final String CALCULATE_FROM_STOCK_CARD = "STOCK_CARDS";

  @Mock
  UsageTemplateColumnSectionRepository columnSectionRepository;

  @Mock
  OrderableKitRepository orderableKitRepository;

  @Mock
  StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Mock
  KitUsageLineItemRepository kitUsageRepository;

  @Mock
  private List<UsageReportDataProcessor> usageReportDataProcessors;

  @Mock
  private ValidatorFactory validatorFactory;

  @Mock
  private ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;

  @Mock
  private PeriodService periodService;

  @Mock
  private ProgramOrderableRepository programOrderableRepository;

  @InjectMocks
  SiglusUsageReportService siglusUsageReportService;

  private RequisitionV2Dto requisitionV2Dto;

  private UUID templateId;

  private UUID programId;

  private final UUID requisitionId = UUID.randomUUID();

  private final UUID kitId = UUID.randomUUID();

  private final UUID kitId2 = UUID.randomUUID();

  private RequisitionTemplateExtensionDto extensionDto;

  private BasicRequisitionTemplateDto requisitionTemplateDto;

  private KitUsageLineItem usageLineItem;

  private SiglusRequisitionDto siglusRequisitionDto;

  private Orderable kitProduct;

  private Orderable kitProduct2;

  @Before
  public void prepare() {
    templateId = UUID.randomUUID();
    requisitionV2Dto = new RequisitionV2Dto();
    programId = UUID.randomUUID();

    extensionDto = new RequisitionTemplateExtensionDto();
    extensionDto.setEnableKitUsage(false);
    requisitionTemplateDto = new BasicRequisitionTemplateDto();
    requisitionTemplateDto.setId(templateId);
    requisitionTemplateDto.setExtension(extensionDto);
    requisitionV2Dto.setTemplate(requisitionTemplateDto);
    requisitionV2Dto.setProgram(new ObjectReferenceDto(programId, ""));
    requisitionV2Dto.setId(requisitionId);
    siglusRequisitionDto = new SiglusRequisitionDto();
    BeanUtils.copyProperties(requisitionV2Dto, siglusRequisitionDto);
    when(columnSectionRepository.findByRequisitionTemplateId(templateId))
        .thenReturn(getMockKitSection());
    kitProduct = new Orderable(Code.code("kitProduct"), Dispensable.createNew("each"), 10,
        7, true, kitId, 1L);
    kitProduct2 = new Orderable(Code.code("kitProduct"), Dispensable.createNew("each"), 10,
        7, true, kitId2, 1L);

    Program program = new Program(programId);
    OrderableDisplayCategory category = OrderableDisplayCategory.createNew(Code.code("category"));
    ProgramOrderable programOrderable = ProgramOrderable
        .createNew(program, category, kitProduct, CurrencyUnit.USD);
    kitProduct.setProgramOrderables(Arrays.asList(programOrderable));
    kitProduct2.setProgramOrderables(Arrays.asList(programOrderable));
    when(orderableKitRepository.findAllKitProduct()).thenReturn(Arrays.asList(kitProduct));
    usageLineItem = KitUsageLineItem.builder()
        .requisitionId(requisitionId)
        .collection(COLLECTION)
        .service("HF")
        .value(10)
        .build();
    when(stockCardRangeSummaryStockManagementService.findAll()).thenReturn(new ArrayList<>());
    when(programAdditionalOrderableRepository.findAllByProgramId(programId))
        .thenReturn(mockProgramAdditionalOrderableList(kitId2));
    when((programOrderableRepository.findByProgramId(programId)))
        .thenReturn(mockProgramOrderableList());
    when(periodService.getPeriod(any())).thenReturn(mockProcessingPeriodDto(false));
  }

  @Test
  public void shouldDeleteKitLineItemWhenRequisitionDelete() {
    // given
    when(kitUsageRepository.findByRequisitionId(requisitionId))
        .thenReturn(Arrays.asList(usageLineItem));

    // when
    siglusUsageReportService.deleteUsageReport(requisitionId);

    // then
    verify(kitUsageRepository).delete(Arrays.asList(usageLineItem));
  }

  @Test
  public void shouldNotDeleteKitLineItemIfListIsEmptyWhenRequisitionDelete() {
    // given
    List<KitUsageLineItem> kitUsageLineItems = Collections.emptyList();
    when(kitUsageRepository.findByRequisitionId(requisitionId))
        .thenReturn(kitUsageLineItems);

    // when
    siglusUsageReportService.deleteUsageReport(requisitionId);

    // then
    verify(kitUsageRepository, never()).delete(kitUsageLineItems);
  }

  @Test
  public void shouldGetKitValueWhenSearchUsageReport() {
    // given
    when(kitUsageRepository.findByRequisitionId(requisitionId))
        .thenReturn(Arrays.asList(usageLineItem));

    // when
    SiglusRequisitionDto dto = siglusUsageReportService.searchUsageReport(requisitionV2Dto);

    // then
    assertEquals(COLLECTION, dto.getKitUsageLineItems().get(0).getCollection());
    assertEquals(Integer.valueOf(10),
        dto.getKitUsageLineItems().get(0).getServices().get("HF").getValue());
  }

  @Test
  public void shouldCallSaveWhenSaveUsageReport() {
    // given
    when(kitUsageRepository.findByRequisitionId(requisitionId))
        .thenReturn(Arrays.asList(usageLineItem));
    KitUsageLineItemDto kitUsageLineItemDto = new KitUsageLineItemDto();
    kitUsageLineItemDto.setCollection(COLLECTION);
    KitUsageServiceLineItemDto serviceLineItemDto = KitUsageServiceLineItemDto.builder()
        .id(UUID.randomUUID()).value(10).build();
    HashMap<String, KitUsageServiceLineItemDto> serviceLineItems = new HashMap<>();
    serviceLineItems.put("HF", serviceLineItemDto);
    kitUsageLineItemDto.setServices(serviceLineItems);
    siglusRequisitionDto.setKitUsageLineItems(Arrays.asList(kitUsageLineItemDto));
    usageLineItem.setId(serviceLineItemDto.getId());
    KitUsageLineItem lineItem = getMockKitUsageLineItem();
    lineItem.setId(serviceLineItemDto.getId());
    when(kitUsageRepository.save(Arrays.asList(lineItem))).thenReturn(Arrays.asList(lineItem));

    // when
    SiglusRequisitionDto resultDto = siglusUsageReportService
        .saveUsageReport(siglusRequisitionDto, requisitionV2Dto);

    // then
    verify(kitUsageRepository).save(Arrays.asList(usageLineItem));
    assertEquals(Integer.valueOf(10),
        resultDto.getKitUsageLineItems().get(0).getServices().get("HF").getValue());
  }

  @Test
  public void shouldIsEmptyIfTemplateEnableKitFalse() {
    // when
    SiglusRequisitionDto requisitionDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);

    // then
    assertEquals(0, requisitionDto.getKitUsageLineItems().size());
  }

  @Test
  public void shouldIsEmptyForUsageReportIfTemplateColumnSectionsEmpty() {
    // given
    when(columnSectionRepository
        .findByRequisitionTemplateId(requisitionV2Dto.getTemplate().getId()))
        .thenReturn(Collections.emptyList());

    // when
    SiglusRequisitionDto requisitionDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);

    // then
    assertEquals(0, requisitionDto.getKitUsageLineItems().size());
  }

  @Test
  public void shouldReturnEqualNullIfColumnIsUsageInput() {
    // given
    extensionDto.setEnableKitUsage(true);
    requisitionTemplateDto.setExtension(extensionDto);
    KitUsageLineItem lineItem = getMockKitUsageLineItem();
    lineItem.setCollection("kitColumn");
    lineItem.setValue(null);
    when(kitUsageRepository.save(Arrays.asList(lineItem))).thenReturn(Arrays.asList(lineItem));

    // when
    SiglusRequisitionDto requisitionDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);

    // then
    assertEquals(1, requisitionDto.getKitUsageLineItems().size());
    assertEquals(null,
        requisitionDto.getKitUsageLineItems().get(0).getServices().get("HF").getValue());
  }

  @Test
  public void shouldReturnCalculatedKitValueIfColumnIsStockCardWithIsReportOnlyFalse() {
    // given
    Integer tagValue = Integer.valueOf(20);
    prepareKitTestData(kitProduct, tagValue);

    // when
    SiglusRequisitionDto resultDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);

    // then
    assertEquals(tagValue,
        resultDto.getKitUsageLineItems().get(0).getServices().get("HF").getValue());
  }

  @Test
  public void shouldReturnCalculatedKitValueIfColumnIsStockCardWithIsReportOnlyTrue() {
    // given
    when(periodService.getPeriod(any())).thenReturn(mockProcessingPeriodDto(true));
    when(orderableKitRepository.findAllKitProduct())
        .thenReturn(newArrayList(kitProduct, kitProduct2));

    Integer tagValue = Integer.valueOf(10);
    prepareKitTestData(kitProduct2, tagValue);

    // when
    SiglusRequisitionDto resultDto = siglusUsageReportService
        .initiateUsageReport(requisitionV2Dto);

    // then
    assertEquals(tagValue,
        resultDto.getKitUsageLineItems().get(0).getServices().get("HF").getValue());
  }

  @Test
  public void shouldNotDeleteKitLineItemIfListIsEmptyWhenRequisitionDelete1() {
    // given
    SiglusRequisitionDto siglusRequisitionDto = mock(SiglusRequisitionDto.class);
    RequisitionV2Dto updatedDto = mock(RequisitionV2Dto.class);
    Validator validator = mock(Validator.class);
    when(validatorFactory.getValidator()).thenReturn(validator);

    // when
    siglusUsageReportService.saveUsageReportWithValidation(siglusRequisitionDto, updatedDto);

    // then
    verify(validator).validate(siglusRequisitionDto, RequisitionActionSequence.class);
  }

  private void prepareKitTestData(Orderable kit, Integer tagValue) {
    extensionDto.setEnableKitUsage(true);
    requisitionTemplateDto.setExtension(extensionDto);
    UUID facilityId = UUID.randomUUID();
    requisitionV2Dto.setFacility(new ObjectReferenceDto(facilityId, ""));
    Map<String, Object> extraData = new HashMap<>();
    extraData.put(ACTUAL_START_DATE, "2020-08-01");
    extraData.put(ACTUAL_END_DATE, "2020-08-20");
    requisitionV2Dto.setExtraData(extraData);
    List<UsageTemplateColumnSection> mockSection = getMockKitSection();
    UsageTemplateColumn column = mockSection.get(0).getColumns().get(0);
    column.setSource(CALCULATE_FROM_STOCK_CARD);
    String tag = "received";
    column.setTag(tag);
    when(columnSectionRepository
        .findByRequisitionTemplateId(requisitionV2Dto.getTemplate().getId()))
        .thenReturn(mockSection);
    StockCardRangeSummaryDto summaryDto = new StockCardRangeSummaryDto();
    summaryDto.setOrderable(new ObjectReferenceDto(kit.getId(), ""));
    Map<String, Integer> tagAmount = new HashMap<>();
    tagAmount.put(tag, tagValue);
    summaryDto.setTags(tagAmount);
    VersionIdentityDto versionIdentityDto =
        new VersionIdentityDto(kit.getId(), kit.getVersionNumber());
    HashSet kitProducts = new HashSet();
    kitProducts.add(versionIdentityDto);
    OrderableDto kitProductDto = new OrderableDto();
    kit.export(kitProductDto);
    final UUID programId = kitProductDto.getPrograms().stream().findFirst().get()
        .getProgramId();
    when(stockCardRangeSummaryStockManagementService
        .search(programId, facilityId, kitProducts, null,
            getActualDate(extraData, ACTUAL_START_DATE), getActualDate(extraData, ACTUAL_END_DATE)))
        .thenReturn(Arrays.asList(summaryDto));
    when(kitUsageRepository.save(any(List.class))).thenAnswer(i -> i.getArguments()[0]);
  }

  private LocalDate getActualDate(Map<String, Object> extraData, String field) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return LocalDate.parse((String) extraData.get(field), dateTimeFormatter);
  }

  private List<UsageTemplateColumnSection> getMockKitSection() {
    UsageTemplateColumnSection templateColumnSection = new UsageTemplateColumnSection();
    templateColumnSection.setCategory(UsageCategory.KITUSAGE);
    templateColumnSection.setName(COLLECTION);

    AvailableUsageColumnSection availableSection = new AvailableUsageColumnSection();
    availableSection.setName(COLLECTION);
    availableSection.setId(UUID.randomUUID());
    availableSection.setColumns(getAvailableUsageColumns());
    availableSection.setDisplayOrder(1);
    templateColumnSection.setSection(availableSection);

    UsageTemplateColumn column = new UsageTemplateColumn();
    column.setId(UUID.randomUUID());
    column.setName("kitColumn");
    column.setSource(USER_INPUT);
    column.setIsDisplayed(true);
    column.setDisplayOrder(1);
    column.setAvailableSources(USER_INPUT);
    templateColumnSection.setColumns(Arrays.asList(column));

    UsageTemplateColumnSection templateServiceSection = new UsageTemplateColumnSection();
    templateServiceSection.setCategory(UsageCategory.KITUSAGE);
    templateServiceSection.setName("service");
    templateServiceSection.setSection(availableSection);

    UsageTemplateColumn service = new UsageTemplateColumn();
    service.setId(UUID.randomUUID());
    service.setName("HF");
    service.setSource(USER_INPUT);
    service.setIsDisplayed(true);
    service.setDisplayOrder(1);
    service.setAvailableSources(USER_INPUT);
    templateServiceSection.setColumns(Arrays.asList(service));

    return Arrays.asList(templateColumnSection, templateServiceSection);
  }

  private KitUsageLineItem getMockKitUsageLineItem() {
    KitUsageLineItem lineItem = new KitUsageLineItem();
    lineItem.setRequisitionId(requisitionV2Dto.getId());
    lineItem.setCollection(COLLECTION);
    lineItem.setService("HF");
    lineItem.setValue(10);
    return lineItem;
  }

  private List<AvailableUsageColumn> getAvailableUsageColumns() {
    AvailableUsageColumn availableUsageColumn = new AvailableUsageColumn();
    availableUsageColumn.setId(UUID.randomUUID());
    availableUsageColumn.setName("available");
    availableUsageColumn.setSources("USER_INPUT|STOCK_CARDS");
    availableUsageColumn.setDisplayOrder(1);
    return Arrays.asList(availableUsageColumn);
  }

  private List<ProgramAdditionalOrderable> mockProgramAdditionalOrderableList(UUID kitId) {
    ProgramAdditionalOrderable orderable = new ProgramAdditionalOrderable();
    orderable.setAdditionalOrderableId(kitId);
    orderable.setProgramId(programId);
    return newArrayList(orderable);
  }

  private List<ProgramOrderable> mockProgramOrderableList() {
    return newArrayList(new ProgramOrderable());
  }

  private ProcessingPeriodDto mockProcessingPeriodDto(boolean isReportOnly) {
    ProcessingPeriodDto periodDto = new ProcessingPeriodDto();
    Map<String, String> extra = new HashMap<>();
    extra.put("reportOnly", isReportOnly ? "true" : "false");
    periodDto.setExtraData(extra);
    return periodDto;
  }

}
