package org.siglus.siglusapi.domain;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.siglus.common.domain.BaseEntity;
import org.siglus.siglusapi.dto.StockManagementInitialDraftDto;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.CreatedDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "stock_management_initial_drafts", schema = "siglusintegration")
public class StockManagementInitialDraft extends BaseEntity {

  @Column(nullable = false)
  private UUID facilityId;

  @Column(nullable = false)
  private UUID programId;

  private UUID destinationId;

  private UUID sourceId;

  private String documentNumber;

  private String locationFreeText;

  private String draftType;

  @CreatedDate
  private LocalDateTime createdTime;

  public static StockManagementInitialDraft createInitialDraft(
      StockManagementInitialDraftDto initialDraftDto) {
    StockManagementInitialDraft initialDraft = new StockManagementInitialDraft();
    BeanUtils.copyProperties(initialDraftDto, initialDraft);
    return initialDraft;
  }
}
