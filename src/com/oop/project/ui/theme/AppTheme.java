
package com.oop.project.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class AppTheme {

    // ==================== COLOR PALETTE ====================

    // Brand colors
    public static final Color PRIMARY =
            new Color(108, 131, 208);

    public static final Color PRIMARY_HOVER =
            new Color(91, 113, 190);

    public static final Color SUCCESS =
            new Color(134, 193, 111);

    public static final Color SUCCESS_HOVER =
            new Color(113, 174, 91);

    public static final Color DANGER =
            new Color(190, 91, 77);

    // Backgrounds
    public static final Color BACKGROUND =
            new Color(243, 243, 243);

    public static final Color SURFACE =
            Color.WHITE;

    public static final Color SOFT_SURFACE =
            new Color(248, 248, 248);

    // Text and borders
    public static final Color TEXT =
            new Color(55, 55, 55);

    public static final Color TEXT_SECONDARY =
            new Color(115, 115, 115);

    public static final Color BORDER =
            new Color(220, 220, 220);

    public static final Color SELECTION =
            new Color(225, 232, 250);

    // ==================== FONTS ====================

    public static final Font BODY =
            new Font("SansSerif", Font.PLAIN, 14);

    public static final Font BODY_BOLD =
            new Font("SansSerif", Font.BOLD, 14);

    public static final Font SECTION_TITLE =
            new Font("SansSerif", Font.BOLD, 19);

    public static final Font TABLE_FONT =
            new Font("SansSerif", Font.PLAIN, 13);

    public static final Font TABLE_HEADER_FONT =
            new Font("SansSerif", Font.BOLD, 12);

    // ==================== BORDERS ====================

    public static final Border PANEL_BORDER =
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
            );

    public static final Border INPUT_BORDER =
            BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
            );

    public static final Border BUTTON_BORDER =
            BorderFactory.createEmptyBorder(8, 14, 8, 14);

    private AppTheme() {
        // Utility class: khong tao object
    }

    // ==================== GLOBAL THEME ====================

    public static void setup() {
        // Chon Nimbus Look and Feel co san trong Java
        try {
            for (UIManager.LookAndFeelInfo info
                    : UIManager.getInstalledLookAndFeels()) {

                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Cannot set Nimbus Look and Feel: "
                            + e.getMessage()
            );
        }

        // General font
        UIManager.put("defaultFont", BODY);
        UIManager.put("Label.font", BODY);
        UIManager.put("Label.foreground", TEXT);

        // Main containers
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("Viewport.background", SURFACE);

        // Buttons: neutral default
        UIManager.put("Button.font", BODY_BOLD);
        UIManager.put("Button.background", SOFT_SURFACE);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.border", BUTTON_BORDER);
        UIManager.put("Button.focusPainted", false);
        UIManager.put("Button.margin",
                new Insets(8, 14, 8, 14));

        // Text fields
        UIManager.put("TextField.font", BODY);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", PRIMARY);
        UIManager.put("TextField.border", INPUT_BORDER);

        UIManager.put("PasswordField.font", BODY);
        UIManager.put("PasswordField.background", SURFACE);
        UIManager.put("PasswordField.foreground", TEXT);
        UIManager.put("PasswordField.border", INPUT_BORDER);

        UIManager.put("TextArea.font", BODY);
        UIManager.put("TextArea.background", SURFACE);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.border", INPUT_BORDER);

        // Combo boxes
        UIManager.put("ComboBox.font", BODY);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);

        // Checkboxes and radio buttons
        UIManager.put("CheckBox.font", BODY);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("RadioButton.font", BODY);
        UIManager.put("RadioButton.foreground", TEXT);

        // Tables
        UIManager.put("Table.font", TABLE_FONT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);

        UIManager.put("TableHeader.font", TABLE_HEADER_FONT);
        UIManager.put("TableHeader.background",
                new Color(247, 247, 247));
        UIManager.put("TableHeader.foreground", TEXT);

        // Scrollbars
        UIManager.put("ScrollBar.width", 12);
    }

    // ==================== CONTAINER STYLES ====================

    // Panel chua noi dung: nen trang, vien xam nhe
    public static void styleContentPanel(JPanel panel) {
        panel.setBackground(SURFACE);
        panel.setBorder(PANEL_BORDER);
    }

    // Panel nen tong the
    public static void styleBackgroundPanel(JPanel panel) {
        panel.setBackground(BACKGROUND);
    }

    // ==================== LABEL STYLES ====================

    // Tieu de section
    public static void styleSectionTitle(JLabel label) {
        label.setFont(SECTION_TITLE);
        label.setForeground(PRIMARY);
    }

    // Noi dung thong thuong
    public static void styleBodyLabel(JLabel label) {
        label.setFont(BODY);
        label.setForeground(TEXT);
    }

    // Chu mo ta phu
    public static void styleSecondaryLabel(JLabel label) {
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
    }

    // ==================== BUTTON STYLES ====================

    // Nut hanh dong chinh: Add, Save, Search
    public static void stylePrimaryButton(JButton button) {
        styleButtonBase(button);
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
    }

    // Nut thao tac thanh cong: Refresh, Import, Confirm
    public static void styleSuccessButton(JButton button) {
        styleButtonBase(button);
        button.setBackground(SUCCESS);
        button.setForeground(Color.WHITE);
    }

    // Nut xoa hoac huy hanh dong nguy hiem
    public static void styleDangerButton(JButton button) {
        styleButtonBase(button);
        button.setBackground(DANGER);
        button.setForeground(Color.WHITE);
    }

    // Nut phu
    public static void styleSecondaryButton(JButton button) {
        styleButtonBase(button);
        button.setBackground(SOFT_SURFACE);
        button.setForeground(TEXT);
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BUTTON_BORDER
                )
        );
    }

    private static void styleButtonBase(JButton button) {
        button.setFont(BODY_BOLD);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setBorder(BUTTON_BORDER);
        button.setCursor(
                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
    }

    // ==================== INPUT STYLES ====================

    public static void styleTextField(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(SURFACE);
        field.setCaretColor(PRIMARY);
        field.setBorder(INPUT_BORDER);
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(BODY);
        comboBox.setForeground(TEXT);
        comboBox.setBackground(SURFACE);
    }

    // ==================== TABLE STYLES ====================

    public static void styleTable(JTable table) {
        table.setFont(TABLE_FONT);
        table.setForeground(TEXT);
        table.setBackground(SURFACE);
        table.setGridColor(BORDER);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        styleTableHeader(table);
    }

    public static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();

        if (header == null) {
            return;
        }

        header.setFont(TABLE_HEADER_FONT);
        header.setBackground(new Color(247, 247, 247));
        header.setForeground(TEXT);
        header.setReorderingAllowed(false);
        header.setBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 0, BORDER
                )
        );
        header.setPreferredSize(new Dimension(0, 32));
    }

    // ==================== AUTOMATIC COMPONENT STYLING ====================

    // Duyet toan bo component trong panel va cac panel con
    public static void applyTo(Container container) {
        for (Component component : container.getComponents()) {

            if (component instanceof JButton button) {
                styleSecondaryButton(button);
                stabilizeButtonSize(button);

            } else if (component instanceof JLabel label) {
                styleBodyLabel(label);

            } else if (component instanceof JTextField field) {
                styleTextField(field);

            } else if (component instanceof JComboBox<?> comboBox) {
                styleComboBox(comboBox);

            } else if (component instanceof JTable table) {
                styleTable(table);

            } else if (component instanceof JPanel panel) {
                panel.setBackground(BACKGROUND);
            }

            if (component instanceof Container child) {
                applyTo(child);
            }
        }
    }

    private static void stabilizeButtonSize(JButton button) {
        Dimension size = button.getPreferredSize();
        button.setMinimumSize(new Dimension(size));
        button.setPreferredSize(new Dimension(size));
        button.setMaximumSize(new Dimension(size));
    }
}
