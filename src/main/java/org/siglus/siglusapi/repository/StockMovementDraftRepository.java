package org.siglus.siglusapi.repository;

import java.util.List;
import java.util.UUID;
import org.siglus.siglusapi.domain.ProductLocationMovementDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementDraftRepository extends JpaRepository<ProductLocationMovementDraft, UUID> {

  List<ProductLocationMovementDraft> findByProgramIdAndFacilityId(UUID programId, UUID facilityId);

}
