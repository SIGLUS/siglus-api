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

package org.openlmis.referencedata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.RequisitionGroup;
import org.openlmis.referencedata.domain.SupervisoryNode;
import org.openlmis.referencedata.dto.ObjectReferenceDto;
import org.openlmis.referencedata.dto.SupervisoryNodeDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.RequisitionGroupRepository;
import org.openlmis.referencedata.repository.SupervisoryNodeRepository;
import org.openlmis.referencedata.testbuilder.FacilityDataBuilder;
import org.openlmis.referencedata.testbuilder.RequisitionGroupDataBuilder;
import org.openlmis.referencedata.testbuilder.SupervisoryNodeDataBuilder;
import org.openlmis.referencedata.util.messagekeys.FacilityMessageKeys;
import org.openlmis.referencedata.util.messagekeys.RequisitionGroupMessageKeys;
import org.openlmis.referencedata.util.messagekeys.SupervisoryNodeMessageKeys;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class SupervisoryNodeBuilderTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private FacilityRepository facilityRepository;

  @Mock
  private SupervisoryNodeRepository supervisoryNodeRepository;

  @Mock
  private RequisitionGroupRepository requisitionGroupRepository;

  @InjectMocks
  private SupervisoryNodeBuilder builder;

  private Facility facility = new FacilityDataBuilder().build();
  private RequisitionGroup requisitionGroup = new RequisitionGroupDataBuilder().build();
  private SupervisoryNode parent = new SupervisoryNodeDataBuilder().build();
  private SupervisoryNode child = new SupervisoryNodeDataBuilder().build();
  private SupervisoryNode partner = new SupervisoryNodeDataBuilder().build();
  private SupervisoryNode partnerOf = new SupervisoryNodeDataBuilder().build();

  private SupervisoryNode supervisoryNode = new SupervisoryNodeDataBuilder()
      .withFacility(facility)
      .withRequisitionGroup(requisitionGroup)
      .withParentNode(parent)
      .withChildNode(child)
      .withPartnerNode(partner)
      .withPartnerNodeOf(partnerOf)
      .build();

  private SupervisoryNodeDto importer = new SupervisoryNodeDto();

  @Before
  public void setUp() {
    supervisoryNode.export(importer);
    importer.setId(null);

    when(facilityRepository.findOne(facility.getId()))
        .thenReturn(facility);
    when(requisitionGroupRepository.findOne(requisitionGroup.getId()))
        .thenReturn(requisitionGroup);
    when(supervisoryNodeRepository.findOne(parent.getId()))
        .thenReturn(parent);
    when(supervisoryNodeRepository.findOne(partnerOf.getId()))
        .thenReturn(partnerOf);
    when(supervisoryNodeRepository.findAll(Sets.newHashSet(child.getId())))
        .thenReturn(Lists.newArrayList(child));
    when(supervisoryNodeRepository.findAll(Sets.newHashSet(partner.getId())))
        .thenReturn(Lists.newArrayList(partner));
  }

  @Test
  public void shouldBuildDomainObjectBasedOnDataFromImporter() {
    SupervisoryNode built = builder.build(importer);

    assertBuiltResource(built, null);
  }

  @Test
  public void shouldUpdateExistingDomainObjectBasedOnDataFromImporter() {
    SupervisoryNode existing = new SupervisoryNode();
    existing.setId(supervisoryNode.getId());

    when(supervisoryNodeRepository.findOne(supervisoryNode.getId())).thenReturn(existing);

    importer.setId(supervisoryNode.getId());

    SupervisoryNode built = builder.build(importer);

    assertBuiltResource(built, importer.getId());
  }

  @Test
  public void shouldBuildDomainObjectWithGivenIdBasedOnDataFromImporter() {
    importer.setId(supervisoryNode.getId());
    when(supervisoryNodeRepository.findOne(supervisoryNode.getId())).thenReturn(null);

    SupervisoryNode built = builder.build(importer);

    assertBuiltResource(built, importer.getId());
  }

  @Test
  public void shouldThrowExceptionIfFacilityCouldNotBeFound() {
    when(facilityRepository.findOne(facility.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfRequisitionGroupCouldNotBeFound() {
    when(requisitionGroupRepository.findOne(requisitionGroup.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(RequisitionGroupMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfParentNodeCouldNotBeFound() {
    when(supervisoryNodeRepository.findOne(parent.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfPartnerNodeOfCouldNotBeFound() {
    when(supervisoryNodeRepository.findOne(partnerOf.getId()))
        .thenReturn(null);

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfChildNodeCouldNotBeFound() {
    when(supervisoryNodeRepository.findAll(Sets.newHashSet(child.getId())))
        .thenReturn(Lists.newArrayList());

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfPartnerNodeCouldNotBeFound() {
    when(supervisoryNodeRepository.findAll(Sets.newHashSet(partner.getId())))
        .thenReturn(Lists.newArrayList());

    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfFacilityIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityMessageKeys.ERROR_NOT_FOUND);

    importer.getFacility().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfRequisitionGroupIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(RequisitionGroupMessageKeys.ERROR_NOT_FOUND);

    importer.getRequisitionGroup().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfParentNodeIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    importer.getParentNode().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfPartnerNodeOfIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    importer.getPartnerNodeOf().setId(null);
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfChildNodeIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    importer.getChildNodes().forEach(child -> child.setId(null));
    builder.build(importer);
  }

  @Test
  public void shouldThrowExceptionIfPartnerNodeIdDoesNotExist() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(SupervisoryNodeMessageKeys.ERROR_NOT_FOUND);

    importer.getPartnerNodes().forEach(partner -> partner.setId(null));
    builder.build(importer);
  }

  @Test
  public void shouldNotThrowExceptionIfFacilityImporterDoesNotExist() {
    importer.setFacility((ObjectReferenceDto) null);

    SupervisoryNode built = builder.build(importer);

    assertThat(built.getFacility()).isNull();
  }

  @Test
  public void shouldNotThrowExceptionIfRequisitionGroupImporterDoesNotExist() {
    importer.setRequisitionGroup((ObjectReferenceDto) null);

    SupervisoryNode built = builder.build(importer);

    assertThat(built.getRequisitionGroup()).isNull();
  }

  @Test
  public void shouldNotThrowExceptionIfParentNodeImporterDoesNotExist() {
    importer.setParentNode((ObjectReferenceDto) null);

    SupervisoryNode built = builder.build(importer);

    assertThat(built.getParentNode()).isNull();
  }

  @Test
  public void shouldNotThrowExceptionIfPartnerNodeOfImporterDoesNotExist() {
    importer.setPartnerNodeOf((ObjectReferenceDto) null);

    SupervisoryNode built = builder.build(importer);

    assertThat(built.getPartnerNodeOf()).isNull();
  }

  @Test
  public void shouldNotThrowExceptionIfChildNoteImporterDoesNotExist() {
    importer.setChildNodes(null);

    SupervisoryNode built = builder.build(importer);

    assertThat(built.getChildNodes()).isNotNull().isEmpty();
  }

  @Test
  public void shouldNotThrowExceptionIfPartnerNoteImporterDoesNotExist() {
    importer.setPartnerNodes(null);

    SupervisoryNode built = builder.build(importer);

    assertThat(built.getPartnerNodes()).isNotNull().isEmpty();
  }

  private void assertBuiltResource(SupervisoryNode node, UUID id) {
    assertThat(node)
        .isEqualToIgnoringGivenFields(importer,
            "id", "facility", "requisitionGroup", "parentNode", "childNodes",
            "partnerNodeOf", "partnerNodes")
        .hasFieldOrPropertyWithValue("id", id)
        .hasFieldOrPropertyWithValue("facility", facility)
        .hasFieldOrPropertyWithValue("requisitionGroup", requisitionGroup)
        .hasFieldOrPropertyWithValue("parentNode", parent)
        .hasFieldOrPropertyWithValue("childNodes", Sets.newHashSet(child))
        .hasFieldOrPropertyWithValue("partnerNodeOf", partnerOf)
        .hasFieldOrPropertyWithValue("partnerNodes", Sets.newHashSet(partner));
  }

}
