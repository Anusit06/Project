package Util.Action;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * ปุ่มยกเลิกการจอง
 * ใส่ปุ่ม "ยกเลิก" ให้คอลัมน์ใน JTable แบบสำเร็จรูป (renderer + editor ในไฟล์เดียว)
 * ใช้เงื่อนไข enable ปุ่มผ่าน canCancel.applyAsBoolean(row) และ
 * เรียก onCancel.accept(row) เมื่อกดปุ่ม
 */
public class CancelButtonColumn {

    public CancelButtonColumn(JTable table, int columnIndex, IntPredicate canCancel, IntConsumer onCancel) {

        TableColumn col = table.getColumnModel().getColumn(columnIndex);
        col.setCellRenderer(new ButtonRenderer(canCancel));
        col.setCellEditor(new ButtonEditor(canCancel, onCancel));
    }

    /**
     * ตัว renderer ของคอลัมน์ปุ่ม “ยกเลิก”
     * ทำหน้าที่วาดปุ่มในแต่ละแถว และเปิด/ปิดปุ่มตามสถานะ
     */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        private final IntPredicate canCancel;
        ButtonRenderer(IntPredicate canCancel) {
            this.canCancel = canCancel;
            setOpaque(true);
            setText("ยกเลิก");
            setBackground(new Color(230, 230, 255));
            setFont(new Font("Tahoma", Font.BOLD, 12));
        }

        /**
         * วาดปุ่มในเซลล์
         * @param table ตาราง
         * @param value ค่าในเซลล์ (ไม่ใช้)
         * @param isSelected ถูกเลือกหรือไม่
         * @param hasFocus โฟกัสหรือไม่
         * @param row แถว
         * @param column คอลัมน์
         * @return คอมโพเนนต์ปุ่มสำหรับแสดงผลในเซลล์
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setEnabled(canCancel.test(row));
            return this;
        }
    }

    /**
     * ตัว editor ของคอลัมน์ปุ่ม “ยกเลิก”
     * รับคลิกและเรียกเมธอด performCancel(row)
     */
    private static class ButtonEditor extends DefaultCellEditor implements TableCellEditor {
        private final JButton button = new JButton("ยกเลิก");
        private final IntPredicate canCancel;
        private final IntConsumer onCancel;
        private int editingRow = -1;

        /**
         * สร้าง editor ด้วย JCheckBox (ตามข้อกำหนดของ DefaultCellEditor)
         * @param checkBox ตัวเช็คบ็อกซ์ที่ส่งต่อให้ซุปเปอร์คลาส
         */
        ButtonEditor(IntPredicate canCancel, IntConsumer onCancel) {
            super(new JCheckBox());
            this.canCancel = canCancel;
            this.onCancel = onCancel;
            button.setOpaque(true);
            button.setBackground(new Color(230, 230, 255));
            button.setFont(new Font("Tahoma", Font.BOLD, 12));
            button.addActionListener(e -> {
                if (editingRow >= 0 && canCancel.test(editingRow)) onCancel.accept(editingRow);
                fireEditingStopped();
            });
        }

        /**
         * สร้างคอมโพเนนต์สำหรับแก้ไข (ปุ่ม) ในเซลล์
         * @param table ตาราง
         * @param value ค่าในเซลล์ (ไม่ใช้)
         * @param isSelected ถูกเลือกหรือไม่
         * @param row แถวที่กำลังแก้ไข
         * @param column คอลัมน์ที่กำลังแก้ไข
         * @return คอมโพเนนต์ปุ่มสำหรับรับคลิก
         */
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            editingRow = row;
            button.setEnabled(canCancel.test(row));
            return button;
        }
    }
}

