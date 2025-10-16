package Model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class Reservation {
    private final String id = UUID.randomUUID().toString();
    private String username;
    private String room;            // S01, L02 ฯลฯ
    private LocalDate date;         // วันที่จอง
    private LocalTime start;        // เวลาเริ่ม
    private LocalTime end;          // เวลาสิ้นสุด
    private String status;          // "Reserved", "Cancelled", "CheckedIn" ฯลฯ

    public Reservation(String username, String room, LocalDate date, LocalTime start, LocalTime end, String status) {
        this.username = username;
        this.room = room;
        this.date = date;
        this.start = start;
        this.end = end;
        this.status = status;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getRoom() { return room; }
    public LocalDate getDate() { return date; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
