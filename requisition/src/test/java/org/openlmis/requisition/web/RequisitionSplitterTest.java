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

package org.openlmis.requisition.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.openlmis.requisition.i18n.MessageKeys.LINE_ITEM_SUPPLIED_BY_OTHER_PARTNER;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.StockAdjustment;
import org.openlmis.requisition.domain.requisition.StockAdjustmentDataBuilder;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyPartnerAssociationDto;
import org.openlmis.requisition.dto.SupplyPartnerDto;
import org.openlmis.requisition.dto.TogglzFeatureDto;
import org.openlmis.requisition.i18n.MessageService;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupplyPartnerReferenceDataService;
import org.openlmis.requisition.service.referencedata.TogglzReferenceDataService;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.StockAdjustmentReasonDataBuilder;
import org.openlmis.requisition.testutils.SupplyPartnerAssociationDtoDataBuilder;
import org.openlmis.requisition.testutils.SupplyPartnerDtoDataBuilder;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.Message.LocalizedMessage;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods"})
public class RequisitionSplitterTest {

  private static final UUID PARTNER_NODE_ID = UUID.randomUUID();

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  private SupervisoryNodeReferenceDataService supervisoryNodeReferenceDataService;

  @Mock
  private SupplyPartnerReferenceDataService supplyPartnerReferenceDataService;

  @Mock
  private TogglzReferenceDataService togglzReferenceDataService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private RequisitionSplitter splitter;

  private Set<UUID> partnerNodeIds = Sets.newHashSet(PARTNER_NODE_ID);
  private UUID supervisoryNodeId;

  private Requisition requisition;
  private SupplyPartnerDto supplyPartner;

  private UUID orderableId1 = UUID.randomUUID();
  private UUID orderableId2 = UUID.randomUUID();
  private UUID orderableId3 = UUID.randomUUID();

  private StockAdjustment stockAdjustment;

  private SupplyPartnerAssociationDto association;
  private SupplyPartnerAssociationDto associationWithDifferentProgram;
  private SupplyPartnerAssociationDto associationWithDifferentNode;
  private SupplyPartnerAssociationDto associationWithDifferentFacility;
  private SupplyPartnerAssociationDto associationWithDifferentOrderable;

  private TogglzFeatureDto featureFlag;

  @Before
  public void setUp() {
    stockAdjustment = new StockAdjustmentDataBuilder().build();

    requisition = new RequisitionDataBuilder()
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withOrderable(orderableId1, 1L)
            .build(), false)
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withOrderable(orderableId2, 1L)
            .build(), false)
        .addLineItem(new RequisitionLineItemDataBuilder()
            .withOrderable(orderableId3, 1L)
            .addStockAdjustment(stockAdjustment)
            .build(), false)
        .addStockAdjustmentReason(new StockAdjustmentReasonDataBuilder()
            .withReasonId(stockAdjustment.getReasonId())
            .build())
        .withPermissionStrings()
        .buildAuthorizedRequisition();

    association = new SupplyPartnerAssociationDtoDataBuilder()
        .withProgram(new ObjectReferenceDto(requisition.getProgramId()))
        .withSupervisoryNode(new ObjectReferenceDto(PARTNER_NODE_ID))
        .withFacility(new ObjectReferenceDto(requisition.getFacilityId()))
        .withFacility(new ObjectReferenceDto(UUID.randomUUID()))
        .withOrderable(new ObjectReferenceDto(orderableId3))
        .withOrderable(new ObjectReferenceDto(UUID.randomUUID()))
        .buildAsDto();

    associationWithDifferentProgram = new SupplyPartnerAssociationDtoDataBuilder()
        .withProgram(new ObjectReferenceDto(UUID.randomUUID()))
        .withSupervisoryNode(new ObjectReferenceDto(PARTNER_NODE_ID))
        .withFacility(new ObjectReferenceDto(requisition.getFacilityId()))
        .withOrderable(new ObjectReferenceDto(orderableId2))
        .buildAsDto();

    associationWithDifferentNode = new SupplyPartnerAssociationDtoDataBuilder()
        .withProgram(new ObjectReferenceDto(requisition.getProgramId()))
        .withSupervisoryNode(new ObjectReferenceDto(UUID.randomUUID()))
        .withFacility(new ObjectReferenceDto(requisition.getFacilityId()))
        .withOrderable(new ObjectReferenceDto(orderableId1))
        .buildAsDto();

    associationWithDifferentFacility = new SupplyPartnerAssociationDtoDataBuilder()
        .withProgram(new ObjectReferenceDto(requisition.getProgramId()))
        .withSupervisoryNode(new ObjectReferenceDto(PARTNER_NODE_ID))
        .withFacility(new ObjectReferenceDto(UUID.randomUUID()))
        .withOrderable(new ObjectReferenceDto(orderableId3))
        .buildAsDto();

    associationWithDifferentOrderable = new SupplyPartnerAssociationDtoDataBuilder()
        .withProgram(new ObjectReferenceDto(requisition.getProgramId()))
        .withSupervisoryNode(new ObjectReferenceDto(PARTNER_NODE_ID))
        .withFacility(new ObjectReferenceDto(requisition.getFacilityId()))
        .withOrderable(new ObjectReferenceDto(UUID.randomUUID()))
        .buildAsDto();

    supplyPartner = new SupplyPartnerDtoDataBuilder()
        .withAssociation(association)
        .withAssociation(associationWithDifferentProgram)
        .withAssociation(associationWithDifferentNode)
        .buildAsDto();

    SupervisoryNodeDto supervisoryNode = DtoGenerator.of(SupervisoryNodeDto.class);
    supervisoryNode.setPartnerNodes(
        partnerNodeIds
            .stream()
            .map(ObjectReferenceDto::new)
            .collect(Collectors.toSet()));

    supervisoryNodeId = supervisoryNode.getId();

    featureFlag = new TogglzFeatureDto();
    featureFlag.setName(RequisitionSplitter.MULTIPLE_SUPPLIERS);
    featureFlag.setEnabled(true);

    Message message = new Message(LINE_ITEM_SUPPLIED_BY_OTHER_PARTNER);
    LocalizedMessage localizedMessage = message
        .new LocalizedMessage(LINE_ITEM_SUPPLIED_BY_OTHER_PARTNER);

    given(requisitionRepository.existsByOriginalRequisitionId(requisition.getId()))
        .willReturn(false);
    given(supervisoryNodeReferenceDataService.findOne(supervisoryNode.getId()))
        .willReturn(supervisoryNode);
    given(supplyPartnerReferenceDataService.search(partnerNodeIds))
        .willReturn(Lists.newArrayList(supplyPartner));
    given(togglzReferenceDataService.findAll())
        .willReturn(Lists.newArrayList(featureFlag));
    given(messageService.localize(message))
        .willReturn(localizedMessage);
  }

  @Test
  public void shouldNotSplitIfFeatureIsTurnedOff() {
    // given
    featureFlag.setEnabled(false);

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when feature is turned off")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfSupervisoryNodeIdWasNotProvided() {
    // when
    RequisitionSplitResult result = splitter.split(requisition, null);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when supervisory node id is null")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfRequisitionHasBeenSplitBefore() {
    // given
    given(requisitionRepository.existsByOriginalRequisitionId(requisition.getId()))
        .willReturn(true);

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when it has been split before")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfRequisitionIsPartOfAnotherRequisition() {
    // given
    requisition.setOriginalRequisitionId(UUID.randomUUID());

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when it is part of another requisition")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfThereIsNoSupplyPartnerForPartnerNode() {
    // given
    given(supplyPartnerReferenceDataService.search(partnerNodeIds))
        .willReturn(Lists.newArrayList());

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when there is no supply partner for partner node")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfThereIsNoSupplyPartnerAssociationForProgram() {
    // given
    supplyPartner.setAssociations(Lists.newArrayList(associationWithDifferentProgram));

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when there is no supply partner for program")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfThereIsNoSupplyPartnerAssociationForFacility() {
    // given
    supplyPartner.setAssociations(Lists.newArrayList(associationWithDifferentFacility));

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when there is no supply partner for facility")
        .isEqualTo(false);
  }

  @Test
  public void shouldNotSplitIfThereIsNoSupplyPartnerAssociationForOrderable() {
    // given
    supplyPartner.setAssociations(Lists.newArrayList(associationWithDifferentOrderable));

    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit())
        .as("Requisition shouldn't be splittable when there is no supply partner for orderable")
        .isEqualTo(false);
  }

  @Test
  public void shouldSplitRequisition() {
    // when
    RequisitionSplitResult result = splitter.split(requisition, supervisoryNodeId);

    // then
    assertThat(result.wasSplit()).isTrue();
    assertThat(result.getOriginalRequisition()).isNotNull();
    assertThat(result.getPartnerRequisitions()).hasSize(1);

    Requisition partnerRequisition = result.getPartnerRequisitions().get(0);
    Requisition originalRequisition = result.getOriginalRequisition();

    assertThat(partnerRequisition)
        .isEqualToIgnoringGivenFields(
            originalRequisition,
            "id", "requisitionLineItems", "version", "draftStatusMessage", "statusChanges",
            "supervisoryNodeId", "previousRequisitions", "availableProducts", "permissionStrings",
            "extraData")
        .hasFieldOrPropertyWithValue("id", null)
        .hasFieldOrPropertyWithValue("version", 1L)
        .hasFieldOrPropertyWithValue("draftStatusMessage", "")
        .hasFieldOrPropertyWithValue("statusChanges", Lists.newArrayList())
        .hasFieldOrPropertyWithValue("supervisoryNodeId", association.getSupervisoryNodeId())
        .hasFieldOrPropertyWithValue("previousRequisitions", Lists.newArrayList())
        .hasFieldOrPropertyWithValue("availableProducts", Sets.newHashSet());
    assertThat(partnerRequisition.getOriginalRequisitionId())
        .isEqualTo(originalRequisition.getId());
    assertThat(partnerRequisition.getPermissionStrings())
        .hasSize(1);
    assertThat(partnerRequisition.getPermissionStrings().get(0))
        .hasFieldOrPropertyWithValue("requisition", partnerRequisition)
        .hasFieldOrPropertyWithValue("permissionString",
            String.format("%s|%s|%s", PermissionService.REQUISITION_VIEW,
                partnerRequisition.getFacilityId(),
                partnerRequisition.getProgramId()));
    assertThat(partnerRequisition.getRequisitionLineItems())
        .hasSize(1);

    Map<VersionEntityReference, RequisitionLineItem> originalLineItems = originalRequisition
        .getRequisitionLineItems()
        .stream()
        .collect(Collectors.toMap(RequisitionLineItem::getOrderable, Function.identity()));

    assertThat(originalLineItems).hasSize(3);

    RequisitionLineItem partnerLineItem = partnerRequisition.getRequisitionLineItems().get(0);
    RequisitionLineItem originalLineItem = originalLineItems.get(partnerLineItem.getOrderable());

    assertThat(partnerLineItem)
        .isEqualToIgnoringGivenFields(
            originalLineItem,
            "id", "skipped", "requisition", "requestedQuantity", "requestedQuantityExplanation",
            "remarks", "approvedQuantity", "stockAdjustments")
        .hasFieldOrPropertyWithValue("id", null)
        .hasFieldOrPropertyWithValue("skipped", false)
        .hasFieldOrPropertyWithValue("requisition", partnerRequisition);

    assertThat(partnerLineItem.getStockAdjustments()).hasSize(1);
    assertThat(partnerLineItem.getStockAdjustments().get(0))
        .hasFieldOrPropertyWithValue("reasonId", stockAdjustment.getReasonId())
        .hasFieldOrPropertyWithValue("quantity", stockAdjustment.getQuantity());

    Set<VersionEntityReference> keysWithoutHandledOrderable = Sets
        .newHashSet(originalLineItems.keySet());
    keysWithoutHandledOrderable.remove(partnerLineItem.getOrderable());

    assertThat(keysWithoutHandledOrderable).hasSize(2);

    for (VersionEntityReference orderableIdentity : keysWithoutHandledOrderable) {
      RequisitionLineItem lineItem = originalLineItems.get(orderableIdentity);
      assertThat(lineItem.getRemarks()).isNullOrEmpty();
    }

    assertThat(originalLineItem.getSkipped()).isTrue();
    assertThat(originalLineItem.getRequestedQuantity()).isNull();
    assertThat(originalLineItem.getRequestedQuantityExplanation()).isNull();
    assertThat(originalLineItem.getApprovedQuantity()).isNull();
    assertThat(originalLineItem.getRemarks()).isEqualTo(LINE_ITEM_SUPPLIED_BY_OTHER_PARTNER);
  }

}
