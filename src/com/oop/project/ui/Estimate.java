package com.oop.project.ui;

import com.oop.project.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.oop.project.config.AppContext;
import com.oop.project.model.LaborService;
import com.oop.project.model.JobStatus;
import com.oop.project.model.Part;
import com.oop.project.model.RepairJob;
import com.oop.project.ui.helpers.RefreshableTableModel;

public class Estimate extends JPanel {
    private final JTextField jobIdField = new JTextField(12);
    private final JLabel jobSummaryLabel = new JLabel("Choose a repair to begin.");
    private final JComboBox<com.oop.project.model.Estimate> versionSelector = new JComboBox<>();
    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[] { "DRAFT", "APPROVED", "REJECTED" });
    private final JTextArea noteArea = new JTextArea(3, 30);
    private final RefreshableTableModel partsModel = createEditableModel(
            new String[] { "Part name *", "Unit price *", "Quantity *", "Subtotal" });
    private final RefreshableTableModel laborModel = createEditableModel(
            new String[] { "Labor description *", "Hours *", "Hourly rate *", "Subtotal" });
    private final JTable partsTable = new JTable(partsModel);
    private final JTable laborTable = new JTable(laborModel);
    private final JLabel totalLabel = new JLabel("Total estimate: 0.00");
    private final JLabel validationLabel = new JLabel("Prototype only - not connected to PostgreSQL.");
    private final JLabel partsInputValidation = validationLabel();
    private final JLabel laborInputValidation = validationLabel();
    private final AppContext context;
    private final Runnable afterEstimateStatusChange;
    private final JButton approveButton = new JButton("Approve latest");
    private final JButton rejectButton = new JButton("Reject latest");
    private final JButton saveDraftButton = new JButton("Save as new draft version");
    private final List<JButton> lineItemActionButtons = new ArrayList<>();
    private int activeEstimateId;
    private int selectedJobId;
    private JobStatus selectedJobStatus;
    private boolean changingVersionSelection;
    private boolean calculatingTotal;
    private boolean loadingEstimateData;
    private boolean estimateDirty;

    public Estimate() {
        this("");
    }

    public Estimate(String typeOfPanel) {
        this(typeOfPanel, null);
    }

    public Estimate(AppContext context) {
        this(context, () -> { });
    }

    public Estimate(AppContext context, Runnable afterEstimateStatusChange) {
        this("", context, afterEstimateStatusChange);
    }

    private Estimate(String typeOfPanel, AppContext context) {
        this(typeOfPanel, context, () -> { });
    }

    private Estimate(String typeOfPanel, AppContext context, Runnable afterEstimateStatusChange) {
        this.context = context;
        this.afterEstimateStatusChange = afterEstimateStatusChange;
        AppTheme.setup();
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(AppTheme.BACKGROUND);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(createHeader(), BorderLayout.NORTH);
        top.add(createEstimateForm(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(createLineItemsPanel(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        partsModel.addTableModelListener(event -> {
            if (!loadingEstimateData) {
                estimateDirty = true;
            }
            updateTotal(event);
        });
        laborModel.addTableModelListener(event -> {
            if (!loadingEstimateData) {
                estimateDirty = true;
            }
            updateTotal(event);
        });
        noteArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void changed() {
                if (!loadingEstimateData) {
                    estimateDirty = true;
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                changed();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                changed();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                changed();
            }
        });
        versionSelector.addActionListener(event -> {
            if (!changingVersionSelection) {
                com.oop.project.model.Estimate selected =
                        (com.oop.project.model.Estimate) versionSelector.getSelectedItem();
                if (selected != null) {
                    if (!confirmDiscardUnsavedChanges()) {
                        restoreActiveVersionSelection();
                        return;
                    }
                    showEstimate(selected);
                }
            }
        });
        updateWorkflowButtons(false);
        updateTotal(null);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);

        JLabel title = new JLabel("Repair Estimate");
        AppTheme.styleSectionTitle(title);
        JLabel subtitle = new JLabel("Prepare an estimate from parts and labor charges.");
        AppTheme.styleSecondaryLabel(subtitle);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        return header;
    }

    private JPanel createEstimateForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Repair job and estimate details"));

        jobIdField.setToolTipText("Repair job ID this estimate belongs to");
        AppTheme.styleTextField(jobIdField);
        jobIdField.setEditable(false);
        statusCombo.setEnabled(false);
        AppTheme.styleComboBox(statusCombo);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(AppTheme.BODY);
        noteArea.setForeground(AppTheme.TEXT);
        noteArea.setBackground(AppTheme.SURFACE);
        noteArea.setBorder(AppTheme.INPUT_BORDER);

        JButton chooseJobButton = new JButton("Find repair...");
        chooseJobButton.setEnabled(context != null);
        AppTheme.stylePrimaryButton(chooseJobButton);
        chooseJobButton.addActionListener(event -> chooseRepairJob());
        JButton loadButton = new JButton("Refresh versions");
        AppTheme.styleSecondaryButton(loadButton);
        loadButton.setEnabled(context != null);
        loadButton.addActionListener(event -> loadLatestEstimate());

        JPanel jobSelection = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        jobSelection.setOpaque(false);
        jobSelection.add(jobIdField);
        jobSelection.add(chooseJobButton);
        jobSelection.add(loadButton);
        jobSummaryLabel.setForeground(AppTheme.TEXT_SECONDARY);
        jobSummaryLabel.setVerticalAlignment(JLabel.TOP);
        AppTheme.styleComboBox(versionSelector);
        versionSelector.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof com.oop.project.model.Estimate estimate) {
                    setText("Version " + estimate.getVersion() + " · " + estimate.getStatus().name());
                } else {
                    setText("No saved versions");
                }
                return this;
            }
        });
        versionSelector.setEnabled(false);

        addFormRow(form, 0, "Repair job", jobSelection);
        addFormRow(form, 1, "Saved versions", versionSelector);
        addFormRow(form, 2, "Status", statusCombo);
        addFormRow(form, 3, "Note", new JScrollPane(noteArea));
        GridBagConstraints summaryConstraints = new GridBagConstraints();
        summaryConstraints.gridx = 2;
        summaryConstraints.gridy = 0;
        summaryConstraints.gridheight = 4;
        summaryConstraints.weightx = 1;
        summaryConstraints.weighty = 1;
        summaryConstraints.fill = GridBagConstraints.BOTH;
        summaryConstraints.anchor = GridBagConstraints.NORTHWEST;
        summaryConstraints.insets = new Insets(8, 12, 8, 12);
        form.add(jobSummaryLabel, summaryConstraints);
        form.setPreferredSize(new Dimension(1000, 205));
        return form;
    }

    private JPanel createLineItemsPanel() {
        JPanel lineItems = new JPanel(new BorderLayout(8, 8));
        lineItems.setBorder(BorderFactory.createTitledBorder("Line items (* required)"));

        AppTheme.styleTable(partsTable);
        AppTheme.styleTable(laborTable);
        partsTable.getColumnModel().getColumn(1).setCellRenderer(numberRenderer("#,##0.00"));
        partsTable.getColumnModel().getColumn(2).setCellRenderer(numberRenderer("#,##0"));
        partsTable.getColumnModel().getColumn(3).setCellRenderer(numberRenderer("#,##0.00"));
        laborTable.getColumnModel().getColumn(1).setCellRenderer(numberRenderer("#,##0.##"));
        laborTable.getColumnModel().getColumn(2).setCellRenderer(numberRenderer("#,##0.00"));
        laborTable.getColumnModel().getColumn(3).setCellRenderer(numberRenderer("#,##0.00"));

        JButton addPartButton = new JButton("Add part");
        lineItemActionButtons.add(addPartButton);
        AppTheme.stylePrimaryButton(addPartButton);
        addPartButton.addActionListener(event -> partsModel.addRow(new Object[] { "", "0.00", "1", "0.00" }));

        JButton removePartButton = new JButton("Remove selected part");
        lineItemActionButtons.add(removePartButton);
        AppTheme.styleSecondaryButton(removePartButton);
        removePartButton.addActionListener(event -> removeSelectedRow(partsTable, partsModel));

        JButton addLaborButton = new JButton("Add labor");
        lineItemActionButtons.add(addLaborButton);
        AppTheme.stylePrimaryButton(addLaborButton);
        addLaborButton.addActionListener(event -> laborModel.addRow(new Object[] { "", "1.00", "0.00", "0.00" }));

        JButton removeLaborButton = new JButton("Remove selected labor");
        lineItemActionButtons.add(removeLaborButton);
        AppTheme.styleSecondaryButton(removeLaborButton);
        removeLaborButton.addActionListener(event -> removeSelectedRow(laborTable, laborModel));

        JPanel partsPanel = createItemsPanel("Parts", partsTable, addPartButton, removePartButton);
        JPanel laborPanel = createItemsPanel("Labor", laborTable, addLaborButton, removeLaborButton);
        javax.swing.JTabbedPane itemTabs = new javax.swing.JTabbedPane();
        itemTabs.addTab("Parts", partsPanel);
        itemTabs.addTab("Labor", laborPanel);
        lineItems.add(itemTabs, BorderLayout.CENTER);
        return lineItems;
    }

    private JPanel createItemsPanel(String title, JTable table, JButton addButton, JButton removeButton) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(addButton);
        actions.add(removeButton);
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        footer.add(actions, BorderLayout.NORTH);
        footer.add(table == partsTable ? partsInputValidation : laborInputValidation, BorderLayout.SOUTH);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setOpaque(false);

        totalLabel.setFont(AppTheme.BODY_BOLD);
        totalLabel.setForeground(AppTheme.PRIMARY);
        validationLabel.setFont(AppTheme.BODY);

        AppTheme.stylePrimaryButton(saveDraftButton);
        saveDraftButton.addActionListener(event -> saveDraft());
        AppTheme.stylePrimaryButton(approveButton);
        AppTheme.styleSecondaryButton(rejectButton);
        approveButton.setEnabled(false);
        rejectButton.setEnabled(false);
        approveButton.addActionListener(event -> updateEstimateStatus(true));
        rejectButton.addActionListener(event -> updateEstimateStatus(false));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(saveDraftButton);
        actions.add(approveButton);
        actions.add(rejectButton);

        JPanel summary = new JPanel(new BorderLayout(0, 4));
        summary.setOpaque(false);
        summary.add(totalLabel, BorderLayout.NORTH);
        summary.add(validationLabel, BorderLayout.CENTER);

        footer.add(summary, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private static RefreshableTableModel createEditableModel(String[] columns) {
        return new RefreshableTableModel(columns, true) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 3;
            }
        };
    }

    private static void addFormRow(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        labelConstraints.insets = new Insets(8, 8, 8, 8);
        panel.add(new JLabel(label + ":"), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 0.4;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(8, 0, 8, 8);
        panel.add(component, fieldConstraints);
    }

    private void chooseRepairJob() {
        if (context == null) {
            return;
        }
        try {
            List<RepairJob> jobs = context.repairJobRepository().findAll();
            if (jobs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "There are no repair jobs to select.");
                return;
            }
            DefaultTableModel jobsModel = new DefaultTableModel(
                    new String[] { "ID", "Device", "Customer", "Status", "Issue", "Received At" }, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            for (RepairJob job : jobs) {
                jobsModel.addRow(new Object[] {
                        job.getId(), deviceName(job), job.getDevice().getCustomer().getFullName(),
                        job.getStatus().getDisplayName(), job.getIssueDescription(), job.getCreatedAt()
                });
            }
            JTable jobsTable = new JTable(jobsModel);
            jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            AppTheme.styleTable(jobsTable);
            TableRowSorter<DefaultTableModel> jobSorter = new TableRowSorter<>(jobsModel);
            jobsTable.setRowSorter(jobSorter);
            JTextField searchField = new JTextField(28);
            AppTheme.styleTextField(searchField);
            searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void applySearch() {
                    String query = searchField.getText().trim();
                    jobSorter.setRowFilter(query.isEmpty()
                            ? null
                            : RowFilter.regexFilter("(?iu)" + java.util.regex.Pattern.quote(query)));
                }

                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent event) {
                    applySearch();
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent event) {
                    applySearch();
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent event) {
                    applySearch();
                }
            });
            JPanel picker = new JPanel(new BorderLayout(8, 8));
            JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
            searchPanel.add(new JLabel("Search by repair, device, customer or issue:"), BorderLayout.NORTH);
            searchPanel.add(searchField, BorderLayout.CENTER);
            JScrollPane scrollPane = new JScrollPane(jobsTable);
            scrollPane.setPreferredSize(new Dimension(900, 420));
            picker.add(searchPanel, BorderLayout.NORTH);
            picker.add(scrollPane, BorderLayout.CENTER);
            int choice = JOptionPane.showConfirmDialog(this, picker, "Find repair job",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            int selected = jobsTable.getSelectedRow();
            if (choice == JOptionPane.OK_OPTION && selected >= 0) {
                RepairJob job = jobs.get(jobsTable.convertRowIndexToModel(selected));
                if (!confirmDiscardUnsavedChanges()) {
                    return;
                }
                selectedJobId = job.getId();
                selectedJobStatus = job.getStatus();
                jobIdField.setText(Integer.toString(job.getId()));
                jobSummaryLabel.setText("<html><b>" + escapeHtml(deviceName(job)) + "</b><br>"
                        + escapeHtml(job.getDevice().getCustomer().getFullName()) + " · "
                        + escapeHtml(job.getStatus().getDisplayName()) + "<br>"
                        + escapeHtml(job.getIssueDescription()) + "</html>");
                loadLatestEstimate(false);
            }
        } catch (RuntimeException exception) {
            showPersistenceError("Could not load repair jobs", exception);
        }
    }

    private static JLabel validationLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(AppTheme.DANGER);
        label.setFont(AppTheme.BODY);
        return label;
    }

    private static String deviceName(RepairJob job) {
        String brand = job.getDevice().getBrand() == null ? "" : job.getDevice().getBrand() + " ";
        String model = job.getDevice().getModel() == null
                ? job.getDevice().getDeviceType() : job.getDevice().getModel();
        return (brand + model).trim();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void clearLoadedEstimate() {
        loadingEstimateData = true;
        activeEstimateId = 0;
        statusCombo.setSelectedItem("DRAFT");
        noteArea.setText("");
        partsModel.replaceRows(List.of());
        laborModel.replaceRows(List.of());
        loadingEstimateData = false;
        estimateDirty = false;
        updateWorkflowButtons(false);
        updateTotal(null);
    }

    private static void removeSelectedRow(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            model.removeRow(table.convertRowIndexToModel(selectedRow));
        }
    }

    private void updateTotal(TableModelEvent event) {
        if (calculatingTotal) {
            return;
        }
        calculatingTotal = true;
        try {
            String partsError = validatePartsInput();
            String laborError = validateLaborInput();
            partsInputValidation.setText(partsError.isEmpty() ? " " : partsError);
            laborInputValidation.setText(laborError.isEmpty() ? " " : laborError);
            if (!partsError.isEmpty() || !laborError.isEmpty()) {
                totalLabel.setText("Total estimate: —");
                validationLabel.setText("Correct the highlighted line item fields.");
                validationLabel.setForeground(AppTheme.DANGER);
                return;
            }
            BigDecimal total = calculatePartsTotal().add(calculateLaborTotal());
            totalLabel.setText("Total estimate: "
                    + formatNumber(total.setScale(2, RoundingMode.HALF_UP), "#,##0.00"));
            validationLabel.setText(context == null
                    ? "Prototype mode - not connected to PostgreSQL."
                    : "Ready to save to PostgreSQL.");
            validationLabel.setForeground(AppTheme.TEXT_SECONDARY);
        } catch (NumberFormatException | ArithmeticException exception) {
            totalLabel.setText("Total estimate: —");
            validationLabel.setText("Check prices, quantities, and labor hours.");
            validationLabel.setForeground(AppTheme.DANGER);
        } finally {
            calculatingTotal = false;
        }
    }

    private BigDecimal calculatePartsTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int row = 0; row < partsModel.getRowCount(); row++) {
            BigDecimal unitPrice = new BigDecimal(partsModel.getValueAt(row, 1).toString().trim());
            int quantity = Integer.parseInt(partsModel.getValueAt(row, 2).toString().trim());
            if (unitPrice.signum() < 0 || quantity <= 0) {
                throw new NumberFormatException("Part price and quantity must be non-negative/positive.");
            }
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            partsModel.setValueAt(subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString(), row, 3);
            total = total.add(subtotal);
        }
        return total;
    }

    private String validatePartsInput() {
        BigDecimal maxUnitPrice = new BigDecimal("9999999999.99");
        for (int row = 0; row < partsModel.getRowCount(); row++) {
            String name = cellText(partsModel, row, 0);
            if (name.isEmpty()) {
                return "Row " + (row + 1) + ": enter a part name.";
            }
            BigDecimal price;
            try {
                price = new BigDecimal(cellText(partsModel, row, 1));
            } catch (NumberFormatException exception) {
                return "Row " + (row + 1) + ": unit price must be a number.";
            }
            if (price.signum() < 0 || price.compareTo(maxUnitPrice) > 0 || price.scale() > 2) {
                return "Row " + (row + 1) + ": price must be 0–9,999,999,999.99 with at most 2 decimals.";
            }
            try {
                if (Integer.parseInt(cellText(partsModel, row, 2)) <= 0) {
                    return "Row " + (row + 1) + ": quantity must be greater than 0.";
                }
            } catch (NumberFormatException exception) {
                return "Row " + (row + 1) + ": quantity must be a whole number greater than 0.";
            }
        }
        return "";
    }

    private String validateLaborInput() {
        BigDecimal maxHours = new BigDecimal("9999.99");
        BigDecimal maxHourlyRate = new BigDecimal("9999999999.99");
        for (int row = 0; row < laborModel.getRowCount(); row++) {
            if (cellText(laborModel, row, 0).isEmpty()) {
                return "Row " + (row + 1) + ": enter a labor description.";
            }
            BigDecimal hours;
            try {
                hours = new BigDecimal(cellText(laborModel, row, 1));
            } catch (NumberFormatException exception) {
                return "Row " + (row + 1) + ": hours must be a number.";
            }
            if (hours.signum() <= 0 || hours.compareTo(maxHours) > 0 || hours.scale() > 2) {
                return "Row " + (row + 1) + ": hours must be greater than 0, at most 9,999.99, with 2 decimals.";
            }
            BigDecimal hourlyRate;
            try {
                hourlyRate = new BigDecimal(cellText(laborModel, row, 2));
            } catch (NumberFormatException exception) {
                return "Row " + (row + 1) + ": hourly rate must be a number.";
            }
            if (hourlyRate.signum() < 0 || hourlyRate.compareTo(maxHourlyRate) > 0
                    || hourlyRate.scale() > 2) {
                return "Row " + (row + 1) + ": rate must be 0–9,999,999,999.99 with at most 2 decimals.";
            }
        }
        return "";
    }

    private static String cellText(DefaultTableModel model, int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }

    private static javax.swing.table.TableCellRenderer numberRenderer(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value == null || value.toString().isBlank()) {
                    setText("");
                    return;
                }
                try {
                    setText(formatNumber(new BigDecimal(value.toString()), format));
                } catch (NumberFormatException exception) {
                    setText(value.toString());
                }
            }
        };
    }

    private static String formatNumber(BigDecimal value, String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return formatNumber(value, format);
    }

    private static String formatNumber(BigDecimal value, DecimalFormat format) {
        return format.format(value);
    }

    private BigDecimal calculateLaborTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int row = 0; row < laborModel.getRowCount(); row++) {
            BigDecimal hours = new BigDecimal(laborModel.getValueAt(row, 1).toString().trim());
            BigDecimal hourlyRate = new BigDecimal(laborModel.getValueAt(row, 2).toString().trim());
            if (hours.signum() <= 0 || hourlyRate.signum() < 0) {
                throw new NumberFormatException("Labor hours must be positive and hourly rate non-negative.");
            }
            BigDecimal subtotal = hours.multiply(hourlyRate);
            laborModel.setValueAt(subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString(), row, 3);
            total = total.add(subtotal);
        }
        return total;
    }

    private void saveDraft() {
        if (jobIdField.getText().trim().isEmpty()) {
            validationLabel.setText("Choose a repair job before saving.");
            validationLabel.setForeground(AppTheme.DANGER);
            return;
        }
        try {
            Integer.parseInt(jobIdField.getText().trim());
            finishTableEdit(partsTable);
            finishTableEdit(laborTable);
            updateTotal(null);
            if (!validatePartsInput().isEmpty() || !validateLaborInput().isEmpty()) {
                validationLabel.setText("Correct the highlighted line item fields before saving.");
                validationLabel.setForeground(AppTheme.DANGER);
                return;
            }
            readParts();
            readLaborServices();
        } catch (RuntimeException exception) {
            validationLabel.setText(exception.getMessage());
            validationLabel.setForeground(AppTheme.DANGER);
            return;
        }

        if (context == null) {
            JOptionPane.showMessageDialog(this,
                    "The estimate is ready in the UI, but this panel is not connected to PostgreSQL.",
                    "Persistence not connected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int jobId = Integer.parseInt(jobIdField.getText().trim());
            context.jobService().createEstimate(
                    jobId, readParts(), readLaborServices(), noteArea.getText());
            estimateDirty = false;
            loadLatestEstimate();
            validationLabel.setText("Saved as a new draft version for repair #" + jobId + ".");
            validationLabel.setForeground(AppTheme.SUCCESS);
            afterEstimateStatusChange.run();
        } catch (RuntimeException exception) {
            showPersistenceError("Could not save estimate", exception);
        }
    }

    private List<Part> readParts() {
        List<Part> parts = new ArrayList<>();
        for (int row = 0; row < partsModel.getRowCount(); row++) {
            String name = partsModel.getValueAt(row, 0).toString().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Enter a name for every part row.");
            }
            parts.add(new Part(0,
                    name,
                    new BigDecimal(partsModel.getValueAt(row, 1).toString().trim()),
                    Integer.parseInt(partsModel.getValueAt(row, 2).toString().trim())));
        }
        return parts;
    }

    private List<LaborService> readLaborServices() {
        List<LaborService> laborServices = new ArrayList<>();
        for (int row = 0; row < laborModel.getRowCount(); row++) {
            String description = laborModel.getValueAt(row, 0).toString().trim();
            if (description.isEmpty()) {
                throw new IllegalArgumentException("Enter a description for every labor row.");
            }
            laborServices.add(new LaborService(0,
                    description,
                    new BigDecimal(laborModel.getValueAt(row, 1).toString().trim()),
                    new BigDecimal(laborModel.getValueAt(row, 2).toString().trim())));
        }
        return laborServices;
    }

    private void loadLatestEstimate() {
        loadLatestEstimate(true);
    }

    private void loadLatestEstimate(boolean confirmDiscard) {
        if (context == null) {
            JOptionPane.showMessageDialog(this, "Database mode is required to load estimates.");
            return;
        }
        if (selectedJobId <= 0) {
            validationLabel.setText("Find and select a repair job first.");
            validationLabel.setForeground(AppTheme.DANGER);
            return;
        }
        if (confirmDiscard && !confirmDiscardUnsavedChanges()) {
            return;
        }
        try {
            RepairJob selectedJob = context.repairJobRepository().findById(selectedJobId)
                    .orElseThrow(() -> new IllegalStateException(
                            "The selected repair job no longer exists."));
            selectedJobStatus = selectedJob.getStatus();
            jobSummaryLabel.setText("<html><b>" + escapeHtml(deviceName(selectedJob)) + "</b><br>"
                    + escapeHtml(selectedJob.getDevice().getCustomer().getFullName()) + " · "
                    + escapeHtml(selectedJobStatus.getDisplayName()) + "<br>"
                    + escapeHtml(selectedJob.getIssueDescription()) + "</html>");
            updateWorkflowButtons(false);
            List<com.oop.project.model.Estimate> estimates =
                    context.estimateRepository().findByJobId(selectedJobId);
            changingVersionSelection = true;
            versionSelector.removeAllItems();
            for (com.oop.project.model.Estimate estimate : estimates) {
                versionSelector.addItem(estimate);
            }
            versionSelector.setEnabled(!estimates.isEmpty());
            if (estimates.isEmpty()) {
                clearLoadedEstimate();
                validationLabel.setText("No saved estimate yet. Add line items to prepare the first draft.");
                validationLabel.setForeground(AppTheme.TEXT_SECONDARY);
                return;
            }
            versionSelector.setSelectedIndex(estimates.size() - 1);
            showEstimate(estimates.get(estimates.size() - 1));
        } catch (RuntimeException exception) {
            showPersistenceError("Could not load estimate versions", exception);
        } finally {
            changingVersionSelection = false;
        }
    }

    private void showEstimate(com.oop.project.model.Estimate estimate) {
        loadingEstimateData = true;
        try {
            activeEstimateId = estimate.getId();
            statusCombo.setSelectedItem(estimate.getStatus().name());
            noteArea.setText(estimate.getNote() == null ? "" : estimate.getNote());
            List<Object[]> partRows = new ArrayList<>(estimate.getParts().size());
            for (Part part : estimate.getParts()) {
                partRows.add(new Object[] {
                        part.getName(), part.getUnitPrice().toPlainString(), part.getQuantity(),
                        part.getCost().setScale(2, RoundingMode.HALF_UP).toPlainString()
                });
            }
            partsModel.replaceRows(partRows);
            List<Object[]> laborRows = new ArrayList<>(estimate.getLaborServices().size());
            for (LaborService labor : estimate.getLaborServices()) {
                laborRows.add(new Object[] {
                        labor.getDescription(), labor.getHours().toPlainString(),
                        labor.getHourlyRate().toPlainString(),
                        labor.getCost().setScale(2, RoundingMode.HALF_UP).toPlainString()
                });
            }
            laborModel.replaceRows(laborRows);
            estimateDirty = false;
            boolean latest = versionSelector.getSelectedIndex() == versionSelector.getItemCount() - 1;
            boolean draft = estimate.isDraft() && latest;
            updateWorkflowButtons(draft);
            updateTotal(null);
            validationLabel.setText(latest
                    ? "Viewing latest version " + estimate.getVersion() + ". Saving creates a new draft version."
                    : "Viewing historical version " + estimate.getVersion()
                            + ". Status actions are available only on the latest draft.");
            validationLabel.setForeground(AppTheme.TEXT_SECONDARY);
        } catch (RuntimeException exception) {
            showPersistenceError("Could not display estimate", exception);
        } finally {
            loadingEstimateData = false;
        }
    }

    private boolean confirmDiscardUnsavedChanges() {
        if (!estimateDirty) {
            return true;
        }
        return JOptionPane.showConfirmDialog(this,
                "Discard the unsaved changes to this estimate?",
                "Unsaved estimate changes", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    private void updateWorkflowButtons(boolean latestDraft) {
        boolean waitingForApproval = selectedJobStatus == JobStatus.WAITING_FOR_APPROVAL;
        boolean canEdit = context == null || selectedJobStatus == JobStatus.DIAGNOSING
                || waitingForApproval;
        saveDraftButton.setEnabled(context == null || selectedJobId > 0 && canEdit);
        for (JButton button : lineItemActionButtons) {
            button.setEnabled(canEdit);
        }
        partsTable.setEnabled(canEdit);
        laborTable.setEnabled(canEdit);
        noteArea.setEditable(canEdit);
        approveButton.setEnabled(latestDraft && waitingForApproval);
        rejectButton.setEnabled(latestDraft && waitingForApproval);
    }

    private void restoreActiveVersionSelection() {
        changingVersionSelection = true;
        try {
            for (int index = 0; index < versionSelector.getItemCount(); index++) {
                if (versionSelector.getItemAt(index).getId() == activeEstimateId) {
                    versionSelector.setSelectedIndex(index);
                    break;
                }
            }
        } finally {
            changingVersionSelection = false;
        }
    }

    private void updateEstimateStatus(boolean approve) {
        if (context == null || activeEstimateId <= 0) {
            return;
        }
        if (estimateDirty) {
            JOptionPane.showMessageDialog(this,
                    "Save these changes as a new draft version or discard them before processing the saved estimate.",
                    "Unsaved estimate changes", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!approve && JOptionPane.showConfirmDialog(this,
                "Rejecting this estimate will close the repair job. Continue?",
                "Confirm rejection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (approve) {
                context.jobService().approveEstimate(activeEstimateId);
            } else {
                context.jobService().rejectEstimate(activeEstimateId, noteArea.getText());
            }
            loadLatestEstimate();
            validationLabel.setText(approve ? "Estimate approved." : "Estimate rejected.");
            validationLabel.setForeground(AppTheme.SUCCESS);
            afterEstimateStatusChange.run();
        } catch (RuntimeException exception) {
            showPersistenceError("Could not update estimate status", exception);
        }
    }

    private void showPersistenceError(String title, RuntimeException exception) {
        exception.printStackTrace();
        JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private static void finishTableEdit(JTable table) {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            throw new IllegalArgumentException("Finish editing the current table cell before saving.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Repair Estimate");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new Estimate("test"));
            frame.setSize(1100, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
