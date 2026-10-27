package com.company.meetingroom.room;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            select r from Room r
            where (:active is null or r.active = :active)
              and r.capacity >= :minCapacity
            """)
    Page<Room> search(@Param("active") Boolean active, @Param("minCapacity") int minCapacity, Pageable pageable);

    @Query("""
            select r from Room r
            where r.active = true
              and r.capacity >= :minCapacity
              and not exists (
                  select 1 from Booking b
                  where b.room = r
                    and b.status = com.company.meetingroom.booking.BookingStatus.CONFIRMED
                    and b.startTime < :end
                    and b.endTime > :start)
            order by r.name
            """)
    List<Room> findAvailable(@Param("start") Instant start, @Param("end") Instant end,
                             @Param("minCapacity") int minCapacity);
}
