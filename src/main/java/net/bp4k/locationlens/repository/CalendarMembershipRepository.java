package net.bp4k.locationlens.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import net.bp4k.locationlens.entity.CalendarMembershipEntity;

public interface CalendarMembershipRepository
        extends JpaRepository<CalendarMembershipEntity, String> {

    @EntityGraph(attributePaths = "calendar")
    List<CalendarMembershipEntity>
            findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                    String userId);
}
