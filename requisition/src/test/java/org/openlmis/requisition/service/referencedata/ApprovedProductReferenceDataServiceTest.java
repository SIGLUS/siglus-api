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

package org.openlmis.requisition.service.referencedata;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.BaseCommunicationService;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;

public class ApprovedProductReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<ApprovedProductDto> {

  // [SIGLUS change start]
  // [change reason]: support virtual program
  @Mock
  private ProgramExtensionRepository programExtensionRepository;
  // [SIGLUS change end]

  private ApprovedProductReferenceDataService service;

  @Override
  protected ApprovedProductDto generateInstance() {
    return new ApprovedProductDtoDataBuilder().buildAsDto();
  }

  @Override
  protected BaseCommunicationService<ApprovedProductDto> getService() {
    return new ApprovedProductReferenceDataService();
  }

  @Override
  @Before
  public void setUp() {
    super.setUp();
    service = (ApprovedProductReferenceDataService) prepareService();
    // [SIGLUS change start]
    // [change reason]: support virtual program
    service.programExtensionRepository = programExtensionRepository;
    // [SIGLUS change end]
  }

  @Test
  public void shouldReturnApprovedProducts() {
    // given
    ProgramDto program = new ProgramDtoDataBuilder().buildAsDto();
    UUID facilityId = randomUUID();

    ApprovedProductDto product = new ApprovedProductDtoDataBuilder()
        .withOrderable(new OrderableDtoDataBuilder()
            .withProgramOrderable(program.getId(), true)
            .buildAsDto())
        .withProgram(program)
        .buildAsDto();

    // when
    mockPageResponseEntity(product);

    // [SIGLUS change start]
    // [change reason]: support virtual program
    ProgramExtension programExtension = new ProgramExtension();
    programExtension.setIsVirtual(false);
    when(programExtensionRepository.findByProgramId(program.getId()))
        .thenReturn(programExtension);
    // [SIGLUS change end]
    ApproveProductsAggregator response = service.getApprovedProducts(facilityId, program.getId());

    // then
    ApprovedProductReference reference = new ApprovedProductReference(
        product.getId(), product.getVersionNumber(), product.getOrderable().getId(),
        product.getOrderable().getVersionNumber()
    );

    assertThat(response.getApprovedProductReferences(), hasSize(1));
    assertThat(response.getApprovedProductReferences(), hasItem(reference));

    verifyPageRequest()
        .isGetRequest()
        .hasAuthHeader()
        .hasEmptyBody()
        .isUriStartsWith(service.getServiceUrl() + service.getUrl()
            + facilityId + "/approvedProducts")
        .hasQueryParameter("programId", program.getId());
  }
}
