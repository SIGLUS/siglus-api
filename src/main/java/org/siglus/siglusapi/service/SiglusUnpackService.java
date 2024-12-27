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

import static com.google.common.collect.Sets.newHashSet;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_ADJUST;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_CARDS_VIEW;
import static org.openlmis.stockmanagement.service.PermissionService.STOCK_INVENTORIES_EDIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.OrderableChild;
import org.openlmis.referencedata.dto.OrderableChildDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.referencedata.PermissionStringDto;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.siglus.common.repository.OrderableKitRepository;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotSearchParams;
import org.siglus.siglusapi.dto.OrderableInKitDto;
import org.siglus.siglusapi.dto.SiglusOrdeableKitDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.NotFoundException;
import org.siglus.siglusapi.exception.ValidationMessageException;
import org.siglus.siglusapi.i18n.OrderableMessageKeys;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public class SiglusUnpackService {

  @Autowired
  private OrderableKitRepository orderableKitRepository;

  @Autowired
  private SiglusLotReferenceDataService lotReferenceDataService;

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  PermissionService permissionService;

  @Autowired
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Autowired
  private SiglusDateHelper dateHelper;

  public SiglusOrdeableKitDto getKitByFacilityIdAndOrderableId(UUID facilityId, UUID orderableId) {
    List<SiglusOrdeableKitDto> siglusOrdeableKitDtos = getKitsByFacilityId(facilityId);
    return siglusOrdeableKitDtos.stream()
        .filter(siglusOrdeableKitDto -> orderableId.equals(siglusOrdeableKitDto.getId()))
        .findFirst()
        .orElse(null);
  }

  public List<SiglusOrdeableKitDto> getKitsByFacilityId(UUID facilityId) {
    List<OrderableDto> kitOrderables = searchKitOrderables();
    UserDto user = authenticationHelper.getCurrentUser();
    Set<PermissionStringDto> permissionStrings = permissionService
        .getPermissionStrings(user.getId()).get();
    return kitOrderables.stream()
        .filter(orderableDto -> isUnpackPermission(permissionStrings, orderableDto, facilityId))
        .map(kitOrderable -> getKitDto(kitOrderable, facilityId))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public List<OrderableInKitDto> searchOrderablesInKit(UUID kitProductId) {
    if (kitProductId == null) {
      throw new ValidationMessageException(OrderableMessageKeys.ERROR_INVALID_PARAMS);
    }
    List<OrderableChild> children = getOrderableChildren(kitProductId);
    Map<UUID, List<LotDto>> lotMap = getLots(children);
    return getOrderableInKit(children, lotMap);
  }

  public Set<UUID> orderablesInKit() {
    List<OrderableDto> kitOrderables = searchKitOrderables();
    Set<UUID> orderablesInKit = newHashSet();
    kitOrderables.forEach(kitOrderable -> {
      Set<OrderableChildDto> children = kitOrderable.getChildren();
      orderablesInKit.addAll(children.stream()
          .map(orderableChild -> orderableChild.getOrderable().getId())
          .collect(Collectors.toSet()));
    });
    return orderablesInKit;
  }

  private List<OrderableDto> searchKitOrderables() {
    return OrderableDto.newInstance(orderableKitRepository.findAllKitProduct());
  }

  private Boolean isUnpackPermission(Set<PermissionStringDto> permissionDtos,
      OrderableDto orderableDto, UUID facilityId) {
    UUID programId = orderableDto.getPrograms().stream().findFirst().orElseThrow(() ->
        new NotFoundException("Orderable's program Not Found")).getProgramId();
    List<String> rights = getPermissionRightBy(permissionDtos, programId, facilityId);
    return rights.containsAll(Arrays.asList(STOCK_INVENTORIES_EDIT,
        STOCK_ADJUST, STOCK_CARDS_VIEW));
  }

  private List<String> getPermissionRightBy(Set<PermissionStringDto> permissionDtos,
      UUID programId, UUID facilityId) {
    return permissionDtos
        .stream()
        .filter(permissionStringDto -> facilityId.equals(permissionStringDto.getFacilityId()))
        .filter(permissionStringDto -> programId.equals(permissionStringDto.getProgramId()))
        .map(PermissionStringDto::getRightName)
        .collect(Collectors.toList());
  }

  private SiglusOrdeableKitDto getKitDto(OrderableDto kitOrderable, UUID facilityId) {
    UUID programId = kitOrderable.getPrograms().stream().findFirst().orElseThrow(() ->
        new NotFoundException("Kit's program Not Found")).getProgramId();
    List<StockCard> stockCards = calculatedStockOnHandService
        .getStockCardsWithStockOnHandByOrderableIds(programId, facilityId,
            Collections.singletonList(kitOrderable.getId()));
    if (CollectionUtils.isEmpty(stockCards)) {
      return null;
    }
    SiglusOrdeableKitDto orderableKitDto = new SiglusOrdeableKitDto();
    BeanUtils.copyProperties(kitOrderable, orderableKitDto);
    orderableKitDto.setStockOnHand(stockCards.get(0).getStockOnHand());
    orderableKitDto.setProductCode(kitOrderable.getProductCode());
    orderableKitDto.setProgramId(programId);
    return orderableKitDto;
  }

  private List<OrderableChild> getOrderableChildren(@RequestParam UUID kitProductId) {
    Set<OrderableChild> childSet = orderableRepository
        .findAllLatestByIds(newHashSet(kitProductId), null)
        .getContent()
        .get(0)
        .getChildren();
    return new ArrayList<>(childSet);
  }

  private Map<UUID, List<LotDto>> getLots(List<OrderableChild> children) {
    List<UUID> uuids = children
        .stream()
        .map(this::getTradeItemId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    LotSearchParams lotSearchParams = new LotSearchParams();
    lotSearchParams.setTradeItemId(uuids);
    List<LotDto> lots = lotReferenceDataService.getLots(lotSearchParams);
    return lots.stream()
        .filter(lot -> !lot.getExpirationDate().isBefore(dateHelper.getCurrentDate()))
        .collect(Collectors.groupingBy(LotDto::getTradeItemId));
  }

  private UUID getTradeItemId(OrderableChild child) {
    String tradeItem = child.getOrderable().getTradeItemIdentifier();
    return tradeItem == null ? null : UUID.fromString(tradeItem);
  }

  private List<OrderableInKitDto> getOrderableInKit(List<OrderableChild> children,
      Map<UUID, List<LotDto>> lotMap) {
    return children.stream()
        .map(orderableChild -> {
          OrderableInKitDto dto = new OrderableInKitDto();
          Orderable orderable = orderableChild.getOrderable();
          BeanUtils.copyProperties(orderable, dto);
          dto.setProductCode(orderable.getProductCode().toString());
          dto.setQuantity(orderableChild.getQuantity());
          dto.setLots(getLotList(lotMap, orderableChild.getOrderable().getTradeItemIdentifier()));
          OrderableDto orderableDto = new OrderableDto();
          orderable.export(orderableDto);
          if (!CollectionUtils.isEmpty(orderableDto.getPrograms())) {
            dto.setProgramId(orderableDto.getPrograms().stream().findFirst().get().getProgramId());
          }
          return dto;
        })
        .collect(Collectors.toList());
  }

  private Set<LotDto> getLotList(Map<UUID, List<LotDto>> lotMap, String tradeItem) {
    if (tradeItem == null) {
      return Collections.emptySet();
    }
    List<LotDto> lots = lotMap.get(UUID.fromString(tradeItem));
    if (lots == null || lots.isEmpty()) {
      return Collections.emptySet();
    }
    return new HashSet<>(lots);
  }
}
