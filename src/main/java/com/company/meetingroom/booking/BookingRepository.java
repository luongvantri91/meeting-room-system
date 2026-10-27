package com.company.meetingroom.booking;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Half-open interval overlap: [start, end) intersects [bStart, bEnd). */
    @Query("""
            select count(b) > 0 from Booking b
            where b.room.id = :roomId
              and b.status = com.company.meetingroom.booking.BookingStatus.CONFIRMED
              and b.startTime < :end
              and b.endTime > :start
            """)
    boolean existsOverlap(@Param("roomId") Long roomId, @Param("start") Instant start, @Param("end") Instant end);
}
