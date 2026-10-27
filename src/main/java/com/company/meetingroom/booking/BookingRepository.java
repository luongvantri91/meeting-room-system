package com.company.meetingroom.booking;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = "room")
    Page<Booking> findAll(org.springframework.data.jpa.domain.Specification<Booking> spec, Pageable pageable);

    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId
              and b.status = com.company.meetingroom.booking.BookingStatus.CONFIRMED
              and b.endTime > :now
            """)
    boolean existsUpcomingConfirmed(@Param("roomId") Long roomId, @Param("now") Instant now);

    /** Half-open interval overlap: [start, end) intersects [bStart, bEnd). */
    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId
              and b.status = com.company.meetingroom.booking.BookingStatus.CONFIRMED
              and b.startTime < :end
              and b.endTime > :start
            """)
    boolean existsOverlap(@Param("roomId") Long roomId, @Param("start") Instant start, @Param("end") Instant end);

    /** Same as {@link #existsOverlap} but ignores the booking being edited. */
    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId
              and b.id <> :excludeId
              and b.status = com.company.meetingroom.booking.BookingStatus.CONFIRMED
              and b.startTime < :end
              and b.endTime > :start
            """)
    boolean existsOverlapExcluding(@Param("roomId") Long roomId, @Param("start") Instant start,
                                   @Param("end") Instant end, @Param("excludeId") Long excludeId);
}
