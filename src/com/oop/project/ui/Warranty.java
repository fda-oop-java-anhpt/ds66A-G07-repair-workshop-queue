package com.oop.project.ui;

import com.oop.project.adapter.LegacyWarrantyImportAdapter;
import com.oop.project.adapter.WarrantyFileImporter;
import com.oop.project.adapter.WarrantyResult;
import com.oop.project.model.LegacyWarrantyImport;
import com.oop.project.model.WarrantyCandidate;
import com.oop.project.model.WarrantyRecord;
import com.oop.project.repository.WarrantyRepository;
import com.oop.project.ui.helpers.RefreshableTableModel;
import com.oop.project.ui.helpers.WrappedTableCellRenderer;
import com.oop.project.ui.theme.AppTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class Warranty extends JPanel {
    private static final String[] WARRANTY_COLUMNS = {
            "Warranty ID", "Repair ID", "Device", "Customer", "Start Date", "End Date", "Status", "Source"
    };
    private static final String[] CANDIDATE_COLUMNS = {
            "Repair ID", "Serial Number", "Device", "Customer", "Received At", "Completed At"
    };
    private final WarrantyRepository repository;
    private final RefreshableTableModel warrantyModel = new RefreshableTableModel(WARRANTY_COLUMNS, false);
    private final RefreshableTableModel candidateModel = new RefreshableTableModel(CANDIDATE_COLUMNS, false);
    private final JTable warrantyTable = new JTable(warrantyModel);
    private final JTable candidateTable = new JTable(candidateModel);
    private final TableRowSorter<DefaultTableModel> warrantySorter = new TableRowSorter<>(warrantyModel);
    private final TableRowSorter<DefaultTableModel> candidateSorter = new TableRowSorter<>(candidateModel);
    private final JTextField warrantySearchField = new JTextField(18);
    private final JTextField repairSearchField = new JTextField(18);
    private final JTextField serialNumberField = new JTextField(18);
    private final JTextField deviceNameField = new JTextField(18);
    private final JTextField customerNameField = new JTextField(18);
    private final JTextField startDateField = new JTextField(12);
    private final JTextField endDateField = new JTextField(12);
    private final JLabel startDateValidation = validationLabel();
    private final JLabel endDateValidation = validationLabel();
    private final JButton createWarrantyButton = new JButton("Create warranty");
    private final JLabel totalCount = new JLabel("0");
    private final JLabel validCount = new JLabel("0");
    private final JLabel expiredCount = new JLabel("0");
    private int selectedRepairId;
    private List<WarrantyRecord> warranties = List.of();
    private List<WarrantyCandidate> candidates = List.of();

    public Warranty() {
        this("");
    }

    public Warranty(String typeOfPanel) {
        this.repository = null;
        AppTheme.setup();
        buildUi();
        if ("test".equalsIgnoreCase(typeOfPanel)) {
            addSampleRows();
        }
    }

    public Warranty(WarrantyRepository repository) {
        this.repository = repository;
        AppTheme.setup();
        buildUi();
        refresh();
    }

    private void buildUi() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel dashboard = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 8));
        dashboard.add(stat("Total warranties", totalCount));
        dashboard.add(stat("Valid", validCount));
        dashboard.add(stat("Expired", expiredCount));
        add(dashboard, BorderLayout.NORTH);

        warrantyTable.setRowSorter(warrantySorter);
        candidateTable.setRowSorter(candidateSorter);
        AppTheme.styleTable(warrantyTable);
        AppTheme.styleTable(candidateTable);
        WrappedTableCellRenderer.install(warrantyTable);
        WrappedTableCellRenderer.install(candidateTable);

        JPanel warrantyPanel = new JPanel(new BorderLayout(8, 8));
        warrantyPanel.setBorder(BorderFactory.createTitledBorder("All warranties"));
        JPanel warrantyFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        warrantyFilter.add(new JLabel("Search:"));
        warrantyFilter.add(warrantySearchField);
        JButton searchButton = new JButton("Search");
        JButton resetButton = new JButton("Reset");
        JButton importButton = new JButton("Import legacy file");
        warrantyFilter.add(searchButton);
        warrantyFilter.add(resetButton);
        warrantyFilter.add(importButton);
        searchButton.addActionListener(event -> applyWarrantyFilter());
        warrantySearchField.addActionListener(event -> applyWarrantyFilter());
        resetButton.addActionListener(event -> {
            warrantySearchField.setText("");
            applyWarrantyFilter();
        });
        importButton.addActionListener(event -> importLegacyFile());
        importButton.setEnabled(repository != null);
        warrantyPanel.add(warrantyFilter, BorderLayout.NORTH);
        warrantyPanel.add(new JScrollPane(warrantyTable), BorderLayout.CENTER);

        JPanel candidatesPanel = new JPanel(new BorderLayout(8, 8));
        candidatesPanel.setBorder(BorderFactory.createTitledBorder("Repairs eligible for warranty"));
        JPanel repairFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        repairFilter.add(new JLabel("Search repair/device/customer:"));
        repairFilter.add(repairSearchField);
        JButton repairSearchButton = new JButton("Search");
        JButton repairResetButton = new JButton("Reset");
        repairFilter.add(repairSearchButton);
        repairFilter.add(repairResetButton);
        repairSearchButton.addActionListener(event -> applyCandidateFilter());
        repairSearchField.addActionListener(event -> applyCandidateFilter());
        repairResetButton.addActionListener(event -> {
            repairSearchField.setText("");
            applyCandidateFilter();
        });
        candidatesPanel.add(repairFilter, BorderLayout.NORTH);

        makeSelectionField(serialNumberField);
        makeSelectionField(deviceNameField);
        makeSelectionField(customerNameField);
        addValidationListener(startDateField, this::validateWarrantyDates);
        addValidationListener(endDateField, this::validateWarrantyDates);
        JPanel repairDetails = new JPanel(new GridLayout(0, 2, 6, 6));
        repairDetails.setBorder(BorderFactory.createTitledBorder("Create warranty"));
        repairDetails.add(new JLabel("Serial Number:"));
        repairDetails.add(serialNumberField);
        repairDetails.add(new JLabel("Device:"));
        repairDetails.add(deviceNameField);
        repairDetails.add(new JLabel("Customer:"));
        repairDetails.add(customerNameField);
        repairDetails.add(requiredLabel("Start date (yyyy-MM-dd)"));
        repairDetails.add(fieldWithValidation(startDateField, startDateValidation));
        repairDetails.add(requiredLabel("End date (yyyy-MM-dd)"));
        repairDetails.add(fieldWithValidation(endDateField, endDateValidation));
        JButton createButton = createWarrantyButton;
        repairDetails.add(new JLabel());
        repairDetails.add(createButton);
        createButton.setEnabled(false);
        createButton.addActionListener(event -> createForSelectedRepair());

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Find a repair and create its warranty"));
        rightPanel.add(new JScrollPane(candidateTable), BorderLayout.CENTER);
        rightPanel.add(repairDetails, BorderLayout.SOUTH);
        candidateTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedRepair();
            }
        });

        JSplitPane lists = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, warrantyPanel, rightPanel);
        lists.setResizeWeight(0.5);
        lists.setOneTouchExpandable(true);
        add(lists, BorderLayout.CENTER);
    }

    private static void makeSelectionField(JTextField field) {
        field.setEditable(false);
    }

    private static JPanel stat(String title, JLabel value) {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel(title));
        value.setFont(value.getFont().deriveFont(java.awt.Font.BOLD, 20f));
        panel.add(value);
        return panel;
    }

    private void addSampleRows() {
        warrantyModel.addRow(new Object[] { 1, 101, "Dell Inspiron 15", "Nguyen Van An",
                "2026-01-15", "2027-01-15", "VALID", "NATIVE" });
        warrantyModel.addRow(new Object[] { 2, 102, "iPhone 15", "Tran Thi Binh",
                "2025-03-10", "2026-03-10", "EXPIRED", "LEGACY" });
        totalCount.setText("2");
        validCount.setText("1");
        expiredCount.setText("1");
        candidateModel.addRow(new Object[] { 201, "IP13000", "Dell Inspiron 15", "Nguyen Van An",
                "2026-01-10", "2026-01-15" });
        // Sử dụng chuỗi Serial Number thay vì int deviceId ở tham số thứ 2
        candidates = List.of(new WarrantyCandidate(201, "IP13000", "Dell Inspiron 15", "Nguyen Van An",
                LocalDate.now().atStartOfDay(), LocalDate.now().atStartOfDay()));
    }

    public void refresh() {
        if (repository == null) {
            return;
        }
        warranties = repository.findAll();
        candidates = repository.findCandidates();

        int valid = 0;
        int expired = 0;
        List<Object[]> warrantyRows = new ArrayList<>(warranties.size());
        for (WarrantyRecord warranty : warranties) {
            warrantyRows.add(new Object[] {
                    warranty.getWarrantyId(), warranty.getRepairId(), warranty.getDeviceName(),
                    warranty.getCustomerName(), warranty.getStartDate(), warranty.getEndDate(),
                    warranty.getStatus(), warranty.getSource()
            });
            if ("VALID".equals(warranty.getStatus())) {
                valid++;
            } else {
                expired++;
            }
        }
        List<Object[]> candidateRows = new ArrayList<>(candidates.size());
        for (WarrantyCandidate candidate : candidates) {
            candidateRows.add(new Object[] {
                    candidate.getRepairId(), candidate.getSerialNumber(), candidate.getDeviceName(),
                    candidate.getCustomerName(), candidate.getReceivedAt(), candidate.getCompletedAt()
            });
        }
        candidateTable.clearSelection();
        warrantyModel.replaceRows(warrantyRows);
        candidateModel.replaceRows(candidateRows);
        clearSelectedRepair();
        totalCount.setText(Integer.toString(warranties.size()));
        validCount.setText(Integer.toString(valid));
        expiredCount.setText(Integer.toString(expired));
        applyWarrantyFilter();
    }

    private void applyWarrantyFilter() {
        String query = warrantySearchField.getText().trim();
        warrantySorter.setRowFilter(query.isEmpty()
                ? null
                : RowFilter.regexFilter("(?iu)" + java.util.regex.Pattern.quote(query)));
    }

    private void applyCandidateFilter() {
        String query = repairSearchField.getText().trim();
        candidateSorter.setRowFilter(query.isEmpty()
                ? null
                : RowFilter.regexFilter("(?iu)" + java.util.regex.Pattern.quote(query)));
        clearSelectedRepair();
    }

    private void showSelectedRepair() {
        int selected = candidateTable.getSelectedRow();
        if (selected < 0) {
            clearSelectedRepair();
            return;
        }
        int row = candidateTable.convertRowIndexToModel(selected);
        if (row >= candidates.size()) {
            clearSelectedRepair();
            return;
        }
        WarrantyCandidate candidate = candidates.get(row);
        selectedRepairId = candidate.getRepairId();
        serialNumberField.setText(candidate.getSerialNumber() == null ? "" : candidate.getSerialNumber());
        deviceNameField.setText(candidate.getDeviceName());
        customerNameField.setText(candidate.getCustomerName());
        startDateField.setText("");
        endDateField.setText("");
        validateWarrantyDates();
    }

    private void clearSelectedRepair() {
        candidateTable.clearSelection();
        selectedRepairId = 0;
        serialNumberField.setText("");
        deviceNameField.setText("");
        customerNameField.setText("");
        startDateField.setText("");
        endDateField.setText("");
        startDateValidation.setText(" ");
        endDateValidation.setText(" ");
        createWarrantyButton.setEnabled(false);
    }

    private void validateWarrantyDates() {
        boolean selectedRepair = candidateTable.getSelectedRow() >= 0;
        String startText = startDateField.getText().trim();
        String endText = endDateField.getText().trim();
        LocalDate start = parseDate(startText);
        LocalDate end = parseDate(endText);

        startDateValidation.setText(start == null
                ? (startText.isEmpty() ? "Start date is required." : "Use yyyy-MM-dd.")
                : " ");
        endDateValidation.setText(end == null
                ? (endText.isEmpty() ? "End date is required." : "Use yyyy-MM-dd.")
                : " ");
        if (start != null && end != null && end.isBefore(start)) {
            endDateValidation.setText("End date must be on/after start date.");
        }
        createWarrantyButton.setEnabled(repository != null && selectedRepair
                && start != null && end != null && !end.isBefore(start));
    }

    private static String validateImportRows(DefaultTableModel model, List<WarrantyResult> results) {
        for (int row = 0; row < results.size(); row++) {
            WarrantyResult result = results.get(row);
            if (result.getWarrantyStatus() == com.oop.project.model.WarrantyStatus.NOT_COVERED) {
                continue;
            }
            String startText = String.valueOf(model.getValueAt(row, 3)).trim();
            String endText = String.valueOf(model.getValueAt(row, 4)).trim();
            LocalDate start = parseDate(startText);
            LocalDate end = parseDate(endText);
            if (start == null) {
                return "Row " + (row + 1) + ": enter a start date using yyyy-MM-dd.";
            }
            if (end == null) {
                return "Row " + (row + 1) + ": enter an end date using yyyy-MM-dd.";
            }
            if (end.isBefore(start)) {
                return "Row " + (row + 1) + ": end date must be on/after start date.";
            }
            if (result.isUnderWarranty() != !end.isBefore(LocalDate.now())) {
                return "Row " + (row + 1) + ": end date conflicts with the legacy warranty status.";
            }
        }
        return "";
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            return null;
        }
    }

    private void createForSelectedRepair() {
        if (repository == null) {
            showMessage("Database mode is required to create warranties.");
            return;
        }
        if (candidateTable.getSelectedRow() < 0) {
            showMessage("Select an eligible repair first.");
            return;
        }
        validateWarrantyDates();
        if (!createWarrantyButton.isEnabled()) {
            return;
        }
        try {
            LocalDate start = LocalDate.parse(startDateField.getText().trim());
            LocalDate end = LocalDate.parse(endDateField.getText().trim());
            repository.createForRepair(selectedRepairId, start, end);
            refresh();
            showMessage("Warranty created.");
        } catch (RuntimeException exception) {
            showError("Could not create warranty", exception);
        }
    }

    private void importLegacyFile() {
        if (repository == null) {
            showMessage("Database mode is required to import warranties.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        try {
            List<WarrantyResult> results = new WarrantyFileImporter().importFromFile(file);
            if (results.isEmpty()) {
                showMessage("The selected file contains no warranty records.");
                return;
            }
            importLegacyRows(file, results);
        } catch (IOException | IllegalArgumentException exception) {
            showError("Could not read legacy warranty file", exception);
        }
    }

    private void importLegacyRows(Path file, List<WarrantyResult> results) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file);
        String fileHash = sha256(fileBytes);
        String[] columns = { "Serial Number", "Legacy status", "Description", "Start date (yyyy-MM-dd)",
                "End date (yyyy-MM-dd)" };
        DefaultTableModel importModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 3;
            }
        };
        for (WarrantyResult result : results) {
            importModel.addRow(new Object[] {
                    result.getSerialNumber(), result.getWarrantyStatus().name(), // Đổi từ getDeviceId() sang
                                                                                 // getSerialNumber()
                    result.getDescription(), "", ""
            });
        }
        JTable table = new JTable(importModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(2).setPreferredWidth(240);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(760, Math.min(360, 70 + results.size() * 24)));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        JLabel importValidation = validationLabel();
        importValidation.setText(validateImportRows(importModel, results));
        content.add(new JLabel(
                "Enter actual dates for COVERED/EXPIRED rows. NOT_COVERED rows are not saved."),
                BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(importValidation, BorderLayout.SOUTH);
        importModel.addTableModelListener(event -> importValidation.setText(
                validateImportRows(importModel, results)));
        while (true) {
            if (JOptionPane.showConfirmDialog(this, content, "Review legacy warranty import",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
                importValidation.setText("Finish editing the current import date before continuing.");
                continue;
            }
            String validation = validateImportRows(importModel, results);
            importValidation.setText(validation);
            if (!validation.isEmpty()) {
                continue;
            }
            break;
        }

        List<LegacyWarrantyImport> imports = new ArrayList<>();
        int notCoveredSkipped = 0;
        LegacyWarrantyImportAdapter importAdapter = new LegacyWarrantyImportAdapter();
        for (int row = 0; row < importModel.getRowCount(); row++) {
            WarrantyResult result = results.get(row);
            if (result.getWarrantyStatus() == com.oop.project.model.WarrantyStatus.NOT_COVERED) {
                notCoveredSkipped++;
                continue;
            }
            LocalDate start = LocalDate.parse(String.valueOf(importModel.getValueAt(row, 3)).trim());
            LocalDate end = LocalDate.parse(String.valueOf(importModel.getValueAt(row, 4)).trim());
            importAdapter.adapt(result, start, end, fileHash + "-" + (row + 1))
                    .ifPresent(imports::add);
        }

        try {
            int importedCount = repository.importLegacy(imports);
            refresh();
            showMessage("Imported " + importedCount + " warranty record(s). "
                    + (importedCount < imports.size()
                            ? (imports.size() - importedCount) + " duplicate record(s) were skipped."
                            : "")
                    + (notCoveredSkipped > 0
                            ? " " + notCoveredSkipped + " NOT_COVERED record(s) were not saved."
                            : ""));
        } catch (RuntimeException exception) {
            showError("Could not import legacy warranties", exception);
        }
    }

    private static String sha256(byte[] input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private static JLabel validationLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(AppTheme.DANGER);
        label.setFont(AppTheme.BODY);
        return label;
    }

    private static JLabel requiredLabel(String text) {
        JLabel label = new JLabel(text + " *");
        label.setForeground(AppTheme.DANGER);
        return label;
    }

    private static JPanel fieldWithValidation(JTextField field, JLabel validation) {
        JPanel input = new JPanel(new BorderLayout(6, 0));
        input.add(field, BorderLayout.CENTER);
        JButton chooseDateButton = new JButton("Choose");
        chooseDateButton.setToolTipText("Choose a date from the calendar");
        AppTheme.styleSecondaryButton(chooseDateButton);
        chooseDateButton.addActionListener(event -> chooseDate(field, chooseDateButton));
        input.add(chooseDateButton, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.add(input, BorderLayout.CENTER);
        panel.add(validation, BorderLayout.SOUTH);
        return panel;
    }

    private static void chooseDate(JTextField field, Component invoker) {
        Window owner = SwingUtilities.getWindowAncestor(field);
        LocalDate initialDate = parseDate(field.getText().trim());
        CalendarDialog dialog = new CalendarDialog(owner, initialDate);
        dialog.setLocationRelativeTo(invoker);
        dialog.setVisible(true);
        if (dialog.getSelectedDate() != null) {
            field.setText(DateTimeFormatter.ISO_LOCAL_DATE.format(dialog.getSelectedDate()));
        }
    }

    private static final class CalendarDialog extends JDialog {
        private static final String[] WEEKDAYS = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

        private LocalDate selectedDate;
        private LocalDate displayedMonth;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel daysPanel = new JPanel(new GridBagLayout());

        private CalendarDialog(Window owner, LocalDate initialDate) {
            super(owner, "Choose date", ModalityType.APPLICATION_MODAL);
            displayedMonth = (initialDate == null ? LocalDate.now() : initialDate).withDayOfMonth(1);
            selectedDate = initialDate;
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JButton previousMonth = new JButton("<");
            JButton nextMonth = new JButton(">");
            previousMonth.addActionListener(event -> {
                displayedMonth = displayedMonth.minusMonths(1);
                refreshCalendar();
            });
            nextMonth.addActionListener(event -> {
                displayedMonth = displayedMonth.plusMonths(1);
                refreshCalendar();
            });

            JPanel monthNavigation = new JPanel(new BorderLayout(8, 0));
            monthNavigation.add(previousMonth, BorderLayout.WEST);
            monthNavigation.add(monthLabel, BorderLayout.CENTER);
            monthNavigation.add(nextMonth, BorderLayout.EAST);
            add(monthNavigation, BorderLayout.NORTH);
            add(daysPanel, BorderLayout.CENTER);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(event -> dispose());
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.add(cancelButton);
            add(footer, BorderLayout.SOUTH);

            refreshCalendar();
            pack();
            setMinimumSize(new java.awt.Dimension(300, 260));
        }

        private LocalDate getSelectedDate() {
            return selectedDate;
        }

        private void refreshCalendar() {
            monthLabel.setText(displayedMonth.format(
                    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())));
            daysPanel.removeAll();

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1;
            constraints.weighty = 1;
            for (int column = 0; column < WEEKDAYS.length; column++) {
                constraints.gridx = column;
                constraints.gridy = 0;
                daysPanel.add(new JLabel(WEEKDAYS[column], SwingConstants.CENTER), constraints);
            }

            LocalDate firstDay = displayedMonth.withDayOfMonth(1);
            int column = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
            for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
                LocalDate date = displayedMonth.withDayOfMonth(day);
                JButton dayButton = new JButton(Integer.toString(day));
                dayButton.setFocusPainted(false);
                if (date.equals(selectedDate)) {
                    dayButton.setBackground(AppTheme.SELECTION);
                }
                dayButton.addActionListener(event -> {
                    selectedDate = date;
                    dispose();
                });

                constraints.gridx = column;
                constraints.gridy = 1 + (day + firstDay.getDayOfWeek().getValue() - 2) / 7;
                daysPanel.add(dayButton, constraints);
                column = (column + 1) % 7;
            }

            daysPanel.revalidate();
            daysPanel.repaint();
            pack();
        }
    }

    private static void addValidationListener(JTextField field, Runnable validation) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                validation.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                validation.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                validation.run();
            }
        });
    }

    private void showError(String title, Exception exception) {
        exception.printStackTrace();
        JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Warranty Management");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new Warranty());
            frame.setSize(1100, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}