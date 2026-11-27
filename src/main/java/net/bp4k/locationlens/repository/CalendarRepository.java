package net.bp4k.locationlens.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import net.bp4k.locationlens.entity.CalendarEntity;

public interface CalendarRepository
        extends JpaRepository<CalendarEntity, String> {

    List<CalendarEntity> findAllByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            String ownerId);
}
