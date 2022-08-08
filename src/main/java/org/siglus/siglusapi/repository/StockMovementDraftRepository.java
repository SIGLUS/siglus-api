package org.siglus.siglusapi.repository;

import java.util.UUID;
import org.siglus.siglusapi.domain.StockMovementDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementDraftRepository extends JpaRepository<StockMovementDraft, UUID> {

}
