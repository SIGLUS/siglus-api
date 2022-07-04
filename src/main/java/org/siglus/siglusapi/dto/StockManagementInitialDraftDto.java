package org.siglus.siglusapi.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.StockManagementInitialDraft;
import org.springframework.beans.BeanUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StockManagementInitialDraftDto {

  private UUID id;

  private UUID facilityId;

  private UUID programId;

  private UUID destinationId;

  private UUID sourceId;

  private String documentNumber;

  private String destinationName;

  private String locationFreeText;

  private String draftType;

  public static StockManagementInitialDraftDto from(StockManagementInitialDraft initialDraft) {
    StockManagementInitialDraftDto initialDraftDto = new StockManagementInitialDraftDto();
    BeanUtils.copyProperties(initialDraft, initialDraftDto);
    return initialDraftDto;
  }

  public static List<StockManagementInitialDraftDto> from(Collection<StockManagementInitialDraft> initialDrafts) {
    List<StockManagementInitialDraftDto> initialDraftDtos = new ArrayList<>(initialDrafts.size());
    initialDrafts.forEach(i -> initialDraftDtos.add(from(i)));
    return initialDraftDtos;
  }

}
