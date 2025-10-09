package Util;

import Model.Reservation;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationFileHandler {
    private static final String FILE = "Bookings.txt";

    public static synchronized void save(Reservation r) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, true))) {
            // username,room,yyyy-MM-dd,HH:mm,HH:mm,status
            bw.write(String.join(",",
                r.getUsername(),
                r.getRoom(),
                r.getDate().toString(),
                r.getStart().toString(),
                r.getEnd().toString(),
                r.getStatus()
            ));
            bw.newLine();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static synchronized List<Reservation> loadAll() {
        List<Reservation> list = new ArrayList<>();
        File f = new File(FILE);
        if (!f.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length != 6) continue;
                list.add(new Reservation(
                    p[0], p[1],
                    LocalDate.parse(p[2]),
                    LocalTime.parse(p[3]),
                    LocalTime.parse(p[4]),
                    p[5]
                ));
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }

    public static synchronized void overwriteAll(List<Reservation> items) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE, false))) {
            for (Reservation r : items) {
                bw.write(String.join(",",
                    r.getUsername(),
                    r.getRoom(),
                    r.getDate().toString(),
                    r.getStart().toString(),
                    r.getEnd().toString(),
                    r.getStatus()
                ));
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
