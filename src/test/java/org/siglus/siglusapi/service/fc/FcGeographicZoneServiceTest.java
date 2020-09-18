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

package org.siglus.siglusapi.service.fc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.dto.GeographicLevelDto;
import org.siglus.siglusapi.dto.OpenLmisGeographicZoneDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneDistrictDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneNationalDto;
import org.siglus.siglusapi.dto.fc.FcGeographicZoneProvinceDto;
import org.siglus.siglusapi.service.client.SiglusGeographicLevelService;
import org.siglus.siglusapi.service.client.SiglusGeographicZoneService;

@RunWith(MockitoJUnitRunner.class)
public class FcGeographicZoneServiceTest {

  public static final String ACTIVE = "Activo";

  @Mock
  private SiglusGeographicZoneService geographicZoneService;

  @Mock
  private SiglusGeographicLevelService geographicLevelService;

  @InjectMocks
  private FcGeographicZoneService fcGeographicZoneService;

  List<FcGeographicZoneNationalDto> dtos;

  @Before
  public void prepare() {
    dtos = Arrays.asList(
        FcGeographicZoneNationalDto.builder()
            .code("1")
            .description("description 1")
            .status(ACTIVE)
            .provinces(Arrays.asList(
                FcGeographicZoneProvinceDto.builder()
                    .code("11")
                    .description("description 11")
                    .status(ACTIVE)
                    .districts(Arrays.asList(
                        FcGeographicZoneDistrictDto.builder()
                            .code("111")
                            .description("description 111")
                            .status(ACTIVE)
                            .build(),
                        FcGeographicZoneDistrictDto.builder()
                            .code("112")
                            .description("description 112")
                            .status(ACTIVE)
                            .build()
                    ))
                    .build(),
                FcGeographicZoneProvinceDto.builder()
                    .code("12")
                    .description("description 12")
                    .status(ACTIVE)
                    .districts(Arrays.asList(
                        FcGeographicZoneDistrictDto.builder()
                            .code("121")
                            .description("description 121")
                            .status(ACTIVE)
                            .build(),
                        FcGeographicZoneDistrictDto.builder()
                            .code("122")
                            .description("description 122")
                            .status(ACTIVE)
                            .build()
                    ))
                    .build()
            ))
            .build(),
        FcGeographicZoneNationalDto.builder()
            .code("2")
            .description("description 2")
            .status(ACTIVE)
            .provinces(Arrays.asList(
                FcGeographicZoneProvinceDto.builder()
                    .code("21")
                    .description("description 21")
                    .status(ACTIVE)
                    .districts(Arrays.asList(
                        FcGeographicZoneDistrictDto.builder()
                            .code("211")
                            .description("description 211")
                            .status(ACTIVE)
                            .build(),
                        FcGeographicZoneDistrictDto.builder()
                            .code("212")
                            .description("description 212")
                            .status(ACTIVE)
                            .build()
                    ))
                    .build(),
                FcGeographicZoneProvinceDto.builder()
                    .code("22")
                    .description("description 22")
                    .status(ACTIVE)
                    .districts(Arrays.asList(
                        FcGeographicZoneDistrictDto.builder()
                            .code("221")
                            .description("description 221")
                            .status(ACTIVE)
                            .build(),
                        FcGeographicZoneDistrictDto.builder()
                            .code("222")
                            .description("description 222")
                            .status(ACTIVE)
                            .build()
                    ))
                    .build()
            ))
            .build()
    );

    GeographicLevelDto level1 = GeographicLevelDto.builder()
        .code("national")
        .name("National")
        .levelNumber(1)
        .build();
    GeographicLevelDto level2 = GeographicLevelDto.builder()
        .code("province")
        .name("Province")
        .levelNumber(2)
        .build();
    GeographicLevelDto level3 = GeographicLevelDto.builder()
        .code("district")
        .name("District")
        .levelNumber(3)
        .build();
    List<GeographicLevelDto> levelDtos = Arrays.asList(level1, level2, level3);
    when(geographicLevelService.searchAllGeographicLevel()).thenReturn(levelDtos);

    OpenLmisGeographicZoneDto national1 = OpenLmisGeographicZoneDto.builder()
        .code("1")
        .name("description 1")
        .level(level1)
        .parent(null)
        .build();
    OpenLmisGeographicZoneDto province11 = OpenLmisGeographicZoneDto.builder()
        .code("11")
        .name("description 11")
        .level(level2)
        .parent(national1)
        .build();
    OpenLmisGeographicZoneDto district111 = OpenLmisGeographicZoneDto.builder()
        .code("111")
        .name("description 111")
        .level(level3)
        .parent(province11)
        .build();
    OpenLmisGeographicZoneDto district112 = OpenLmisGeographicZoneDto.builder()
        .code("112")
        .name("district 112")
        .level(level3)
        .parent(province11)
        .build();
    OpenLmisGeographicZoneDto province12 = OpenLmisGeographicZoneDto.builder()
        .code("12")
        .name("province 12")
        .level(level2)
        .parent(national1)
        .build();
    OpenLmisGeographicZoneDto district121 = OpenLmisGeographicZoneDto.builder()
        .code("121")
        .name("description 121")
        .level(level3)
        .parent(province12)
        .build();
    OpenLmisGeographicZoneDto national2 = OpenLmisGeographicZoneDto.builder()
        .code("2")
        .name("national 2")
        .level(level1)
        .parent(null)
        .build();
    List<OpenLmisGeographicZoneDto> zoneDtos = Arrays.asList(
        national1, province11, district111, district112, province12, district121, national2
    );
    when(geographicZoneService.searchAllGeographicZones()).thenReturn(zoneDtos);
  }

  @Test
  public void shouldReturnFalseWhenNotGetAnyGeographicZonesFromFc() {
    // given
    dtos = new ArrayList<>();

    // when
    boolean result = fcGeographicZoneService.processGeographicZones(dtos);

    // then
    assertFalse(result);
  }

  @Test
  public void shouldCreate7GeographicZonesWhenGet7NewGeographicZonesFromFc() {
    // given

    // when
    boolean result = fcGeographicZoneService.processGeographicZones(dtos);

    // then
    verify(geographicZoneService, times(7)).createGeographicZone(
        any(OpenLmisGeographicZoneDto.class));
    assertTrue(result);
  }

  @Test
  public void shouldCreate1GeographicZonesWhenGet7NewGeographicZonesBut1NationalIsInactivoFromFc() {
    // given
    dtos.get(1).setStatus("Inactivo");

    // when
    boolean result = fcGeographicZoneService.processGeographicZones(dtos);

    // then
    verify(geographicZoneService, times(1)).createGeographicZone(
        any(OpenLmisGeographicZoneDto.class));
    assertTrue(result);
  }

  @Test
  public void shouldCreate4GeographicZonesWhenGet7NewGeographicZonesBut1ProvinceIsInactivoFromFc() {
    // given
    dtos.get(1).getProvinces().get(1).setStatus("Inactivo");

    // when
    boolean result = fcGeographicZoneService.processGeographicZones(dtos);

    // then
    verify(geographicZoneService, times(4)).createGeographicZone(
        any(OpenLmisGeographicZoneDto.class));
    assertTrue(result);
  }

  @Test
  public void shouldCreate6GeographicZonesWhenGet7NewGeographicZonesBut1DistrictIsInactivoFromFc() {
    // given
    dtos.get(1).getProvinces().get(1).getDistricts().get(1).setStatus("Inactivo");

    // when
    boolean result = fcGeographicZoneService.processGeographicZones(dtos);

    // then
    verify(geographicZoneService, times(6)).createGeographicZone(
        any(OpenLmisGeographicZoneDto.class));
    assertTrue(result);
  }

  @Test
  public void shouldUpdate3GeographicZonesWhenGet3OldGeographicZonesFromFc() {
    // given

    // when
    boolean result = fcGeographicZoneService.processGeographicZones(dtos);

    // then
    verify(geographicZoneService, times(3)).updateGeographicZone(
        any(OpenLmisGeographicZoneDto.class));
    assertTrue(result);
  }

}