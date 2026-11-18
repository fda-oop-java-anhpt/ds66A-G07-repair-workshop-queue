package com.oop.project.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.time.format.DateTimeFormatter;

import com.oop.project.config.AppContext;
import com.oop.project.model.Customer;
import com.oop.project.model.Device;
import com.oop.project.model.JobStatus;
import com.oop.project.model.RepairJob;
import com.oop.project.ui.helpers.RefreshableTableModel;
import com.oop.project.ui.theme.AppTheme;

public class CustomerDevicePanel extends JPanel {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String[] DEVICE_TYPES = {
            "Điều hòa", "Máy giặt", "Tủ lạnh", "Laptop", "Điện thoại", "Máy tính bàn", "Tivi", "Khác"
    };
    private static final String DEVICE_TYPE_PROMPT = "Select device type...";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9_%+-]+(?:\\.[A-Za-z0-9_%+-]+)*@"
                    + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");
    private final AppContext context;
    private final Runnable afterJobCreated;
    private final RefreshableTableModel customerModel = new RefreshableTableModel(
            new String[] { "ID", "Name", "Phone", "Email", "Address" }, false);
    private final RefreshableTableModel deviceModel = new RefreshableTableModel(
            new String[] { "ID", "Type", "Brand", "Model", "Serial Number" }, false);
    private final RefreshableTableModel jobModel = new RefreshableTableModel(
            new String[] { "Repair ID", "Device", "Issue", "Status", "Received At", "Desired Days", "Ready At" }, false);
    private final JTable customersTable = new JTable(customerModel);
    private final JTable devicesTable = new JTable(deviceModel);
    private final JTable jobsTable = new JTable(jobModel);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<JobStatus> nextStatusCombo = new JComboBox<>();
    private final JTextField statusNoteField = new JTextField(18);
    private final JButton updateStatusButton = new JButton("Update status");
    private final JLabel customerDetails = new JLabel("Select a customer to view contact and account details.");
    private List<Customer> customers = List.of();
    private List<Device> devices = List.of();
    private List<RepairJob> jobs = List.of();

    public CustomerDevicePanel(AppContext context, Runnable afterJobCreated) {
        this.context = context;
        this.afterJobCreated = afterJobCreated;
        AppTheme.setup();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel customerActions = new JPanel(new BorderLayout(4, 4));
        JPanel customerTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customerTools.add(new JLabel("Search customers:"));
        customerTools.add(searchField);
        JButton searchButton = new JButton("Search");
        JButton addCustomerButton = new JButton("Add customer");
        JButton editCustomerButton = new JButton("Edit customer");
        JButton deleteCustomerButton = new JButton("Delete customer");
        JButton addDeviceButton = new JButton("Add device");
        JButton editDeviceButton = new JButton("Edit device");
        JButton deleteDeviceButton = new JButton("Delete device");
        JButton createJobButton = new JButton("Create repair job");
        customerTools.add(searchButton);
        customerTools.add(addCustomerButton);
        customerTools.add(editCustomerButton);
        customerTools.add(deleteCustomerButton);
        JPanel deviceTools = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deviceTools.add(addDeviceButton);
        deviceTools.add(editDeviceButton);
        deviceTools.add(deleteDeviceButton);
        deviceTools.add(createJobButton);
        customerActions.add(customerTools, BorderLayout.NORTH);
        customerActions.add(deviceTools, BorderLayout.SOUTH);

        customersTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        devicesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jobsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        AppTheme.styleTable(customersTable);
        AppTheme.styleTable(devicesTable);
        AppTheme.styleTable(jobsTable);
        AppTheme.styleComboBox(nextStatusCombo);
        AppTheme.styleTextField(statusNoteField);
        AppTheme.stylePrimaryButton(updateStatusButton);
        nextStatusCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof JobStatus ? ((JobStatus) value).getDisplayName() : "");
                return this;
            }
        });
        resetStatusControls();
        customerDetails.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        customerDetails.setVerticalAlignment(JLabel.TOP);
        customerDetails.setOpaque(true);
        customerDetails.setBackground(AppTheme.SURFACE);
        customerDetails.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        customersTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedCustomerDevices();
            }
        });
        jobsTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                prepareStatusUpdate();
            }
        });

        JPanel customerPanel = new JPanel(new BorderLayout());
        customerPanel.setBorder(BorderFactory.createTitledBorder("Customers"));
        customerPanel.add(new JScrollPane(customersTable), BorderLayout.CENTER);
        JPanel customerListWithDetails = new JPanel(new BorderLayout(8, 0));
        customerListWithDetails.add(customerPanel, BorderLayout.CENTER);
        customerListWithDetails.add(customerDetails, BorderLayout.EAST);
        customerDetails.setPreferredSize(new java.awt.Dimension(300, 100));

        JTabbedPane selectedCustomerTabs = new JTabbedPane();
        selectedCustomerTabs.addTab("Devices", new JScrollPane(devicesTable));
        JPanel repairHistoryPanel = new JPanel(new BorderLayout(8, 8));
        repairHistoryPanel.add(new JScrollPane(jobsTable), BorderLayout.CENTER);
        JPanel statusControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusControls.setBorder(BorderFactory.createTitledBorder("Update selected repair"));
        statusControls.add(new JLabel("Next status:"));
        statusControls.add(nextStatusCombo);
        statusControls.add(new JLabel("Note:"));
        statusControls.add(statusNoteField);
        statusControls.add(updateStatusButton);
        repairHistoryPanel.add(statusControls, BorderLayout.SOUTH);
        selectedCustomerTabs.addTab("Repair history", repairHistoryPanel);
        selectedCustomerTabs.setBorder(BorderFactory.createTitledBorder("Selected customer's records"));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, customerListWithDetails,
                selectedCustomerTabs);
        splitPane.setResizeWeight(0.48);
        splitPane.setOneTouchExpandable(true);

        add(customerActions, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        searchButton.addActionListener(event -> loadCustomers(searchField.getText().trim()));
        searchField.addActionListener(event -> loadCustomers(searchField.getText().trim()));
        addCustomerButton.addActionListener(event -> addCustomer());
        addDeviceButton.addActionListener(event -> addDevice());
        editCustomerButton.addActionListener(event -> editCustomer());
        deleteCustomerButton.addActionListener(event -> deleteCustomer());
        editDeviceButton.addActionListener(event -> editDevice());
        deleteDeviceButton.addActionListener(event -> deleteDevice());
        createJobButton.addActionListener(event -> createRepairJob());
        updateStatusButton.addActionListener(event -> updateSelectedRepairStatus());
        loadCustomers("");
    }

    private void loadCustomers(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        customers = context.customerRepository().findAll().stream()
                .filter(customer -> query.isBlank()
                        || customer.getFullName().toLowerCase(Locale.ROOT).contains(normalized)
                        || customer.getPhone().contains(query))
                .toList();
        List<Object[]> customerRows = new ArrayList<>();
        for (Customer customer : customers) {
            customerRows.add(new Object[] {
                    customer.getId(), customer.getFullName(), customer.getPhone(),
                    customer.getEmail(), customer.getAddress()
            });
        }
        customersTable.clearSelection();
        customerModel.replaceRows(customerRows);
        devices = List.of();
        jobs = List.of();
        deviceModel.replaceRows(List.of());
        jobModel.replaceRows(List.of());
        jobsTable.clearSelection();
        resetStatusControls();
        customerDetails.setText("Select a customer to view contact and account details.");
    }

    private void loadSelectedCustomerDevices() {
        int selectedRow = customersTable.getSelectedRow();
        devices = List.of();
        jobs = List.of();
        deviceModel.replaceRows(List.of());
        jobModel.replaceRows(List.of());
        jobsTable.clearSelection();
        resetStatusControls();
        if (selectedRow < 0 || selectedRow >= customers.size()) {
            customerDetails.setText("Select a customer to view contact and account details.");
            return;
        }
        Customer customer = customers.get(customersTable.convertRowIndexToModel(selectedRow));
        devices = context.deviceRepository().findByCustomerId(customer.getId());
        List<Object[]> deviceRows = new ArrayList<>();
        for (Device device : devices) {
            deviceRows.add(new Object[] {
                    device.getId(), device.getDeviceType(), device.getBrand(), device.getModel(),
                    device.getSerialNumber()
            });
        }
        deviceModel.replaceRows(deviceRows);
        jobs = context.repairJobRepository().findByCustomerId(customer.getId());
        customerDetails.setText(customerDetailsHtml(customer, devices.size(), jobs.size()));
        List<Object[]> jobRows = new ArrayList<>();
        for (RepairJob job : jobs) {
            jobRows.add(new Object[] {
                    job.getId(), displayDevice(job.getDevice()), job.getIssueDescription(),
                    job.getStatus().getDisplayName(), formatDateTime(job.getCreatedAt()),
                    job.getDesiredDays(), formatDateTime(job.getEstimatedReadyAt())
            });
        }
        jobModel.replaceRows(jobRows);
    }

    private static String customerDetailsHtml(Customer customer, int deviceCount, int jobCount) {
        return "<html><b>Customer #" + customer.getId() + "</b><br>"
                + "<br><b>Name:</b> " + html(customer.getFullName())
                + "<br><b>Phone:</b> " + html(customer.getPhone())
                + "<br><b>Email:</b> " + html(valueOrEmpty(customer.getEmail()))
                + "<br><b>Address:</b> " + html(valueOrEmpty(customer.getAddress()))
                + "<br><b>Devices:</b> " + deviceCount
                + "<br><b>Repair jobs:</b> " + jobCount
                + "<br><b>Created:</b> " + html(formatDateTime(customer.getCreatedAt()))
                + "<br><b>Updated:</b> " + html(formatDateTime(customer.getUpdatedAt()))
                + "</html>";
    }

    private static String html(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JLabel requiredLabel(String text) {
        JLabel label = new JLabel(text + " *");
        label.setForeground(AppTheme.DANGER);
        return label;
    }

    private static String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATE_TIME_FORMAT.format(dateTime);
    }

    private void addCustomer() {
        JTextField name = new JTextField(22);
        JTextField phone = new JTextField(22);
        JTextField email = new JTextField(22);
        JTextField address = new JTextField(22);
        JLabel nameValidation = validationLabel();
        JLabel phoneValidation = validationLabel();
        JLabel emailValidation = validationLabel();
        JPanel form = customerForm(name, phone, email, address, nameValidation, phoneValidation, emailValidation);
        if (!showCustomerDialog("Add customer", "Add", form, name, nameValidation, phone, phoneValidation,
                email, emailValidation, false)) {
            return;
        }
        try {
            Customer customer = context.customerRepository().save(
                    new Customer(name.getText(), phone.getText(), email.getText(), address.getText()));
            loadCustomers("");
            selectCustomer(customer.getId());
        } catch (RuntimeException exception) {
            showError("Could not save customer", exception);
        }
    }

    private void editCustomer() {
        Customer customer = selectedCustomer();
        if (customer == null) {
            showMessage("Select a customer first.");
            return;
        }
        JTextField name = new JTextField(customer.getFullName(), 22);
        JTextField phone = new JTextField(customer.getPhone(), 22);
        JTextField email = new JTextField(valueOrEmpty(customer.getEmail()), 22);
        JTextField address = new JTextField(valueOrEmpty(customer.getAddress()), 22);
        JLabel nameValidation = validationLabel();
        JLabel phoneValidation = validationLabel();
        JLabel emailValidation = validationLabel();
        JPanel form = customerForm(name, phone, email, address, nameValidation, phoneValidation, emailValidation);
        if (!showCustomerDialog("Edit customer", "Save", form, name, nameValidation, phone, phoneValidation,
                email, emailValidation, true)) {
            return;
        }
        try {
            Customer updated = new Customer(customer.getId(), name.getText(), phone.getText(),
                    email.getText(), address.getText(), customer.getCreatedAt(), customer.getUpdatedAt());
            context.customerRepository().update(updated);
            loadCustomers(searchField.getText().trim());
            selectCustomer(updated.getId());
        } catch (RuntimeException exception) {
            showError("Could not update customer", exception);
        }
    }

    private JPanel customerForm(JTextField name, JTextField phone, JTextField email, JTextField address,
            JLabel nameValidation, JLabel phoneValidation, JLabel emailValidation) {
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
        form.add(requiredLabel("Full name"));
        form.add(fieldWithValidation(name, nameValidation));
        form.add(requiredLabel("Phone"));
        form.add(fieldWithValidation(phone, phoneValidation));
        form.add(new JLabel("Email"));
        form.add(fieldWithValidation(email, emailValidation));
        form.add(new JLabel("Address"));
        form.add(address);
        return form;
    }

    private boolean showCustomerDialog(String title, String saveLabel, JPanel form,
            JTextField name, JLabel nameValidation, JTextField phone, JLabel phoneValidation,
            JTextField email, JLabel emailValidation, boolean showCancelButton) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        JButton saveButton = new JButton(saveLabel);
        AppTheme.stylePrimaryButton(saveButton);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = null;
        if (showCancelButton) {
            cancelButton = new JButton("Cancel");
            AppTheme.styleSecondaryButton(cancelButton);
            buttons.add(cancelButton);
        }
        buttons.add(saveButton);
        boolean[] saved = { false };
        boolean[] validationRequested = { false };
        boolean[] formValid = { false };

        Runnable validate = () -> {
            formValid[0] = isNameValid(name) && isPhoneValid(phone) && isEmailValid(email);
            if (validationRequested[0]) {
                validateNameInput(name, nameValidation);
                validatePhoneInput(phone, phoneValidation);
                validateEmailInput(email, emailValidation);
            }
            styleSubmitButton(saveButton, saveLabel, formValid[0]);
        };
        addValidationListener(name, validate);
        addValidationListener(phone, validate);
        addValidationListener(email, validate);
        validate.run();

        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(saveButton);
        saveButton.addActionListener(event -> {
            validationRequested[0] = true;
            validate.run();
            if (formValid[0]) {
                saved[0] = true;
                dialog.dispose();
            }
        });
        if (cancelButton != null) {
            cancelButton.addActionListener(event -> dialog.dispose());
        }
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return saved[0];
    }

    private static boolean validateNameInput(JTextField name, JLabel validation) {
        boolean valid = isNameValid(name);
        validation.setText(valid ? " " : "Name is required.");
        return valid;
    }

    private static boolean isNameValid(JTextField name) {
        return !name.getText().isBlank();
    }

    private static boolean validatePhoneInput(JTextField phone, JLabel validation) {
        String value = phone.getText().trim();
        if (value.isEmpty()) {
            validation.setText("Phone is required.");
        } else if (!value.matches("\\d+")) {
            validation.setText("Phone must contain digits only.");
        } else if (value.length() != 10) {
            validation.setText("Phone must contain exactly 10 digits.");
        } else {
            validation.setText(" ");
        }
        return validation.getText().isBlank();
    }

    private static boolean isPhoneValid(JTextField phone) {
        return phone.getText().trim().matches("\\d{10}");
    }

    private static boolean validateEmailInput(JTextField email, JLabel validation) {
        String value = email.getText().trim();
        if (value.isEmpty()) {
            validation.setText(" ");
        } else if (value.length() > 100) {
            validation.setText("Email must be 100 characters or fewer.");
        } else if (!EMAIL_PATTERN.matcher(value).matches()) {
            validation.setText("Enter a valid email address.");
        } else {
            validation.setText(" ");
        }
        return validation.getText().isBlank();
    }

    private static boolean isEmailValid(JTextField email) {
        String value = email.getText().trim();
        return value.isEmpty() || (value.length() <= 100 && EMAIL_PATTERN.matcher(value).matches());
    }

    private void deleteCustomer() {
        Customer customer = selectedCustomer();
        if (customer == null) {
            showMessage("Select a customer first.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Delete customer " + customer.getFullName() + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            context.customerRepository().delete(customer.getId());
            loadCustomers(searchField.getText().trim());
        } catch (RuntimeException exception) {
            showError("Could not delete customer", exception);
        }
    }

    private void addDevice() {
        Customer customer = selectedCustomer();
        if (customer == null) {
            showMessage("Select a customer first.");
            return;
        }
        JComboBox<String> type = createDeviceTypeCombo();
        JLabel typeValidation = validationLabel();
        JTextField brand = new JTextField(20);
        JTextField model = new JTextField(20);
        JTextField serial = new JTextField(20);
        JPanel form = deviceForm(type, brand, model, serial, typeValidation);
        if (!showDeviceDialog("Add device", "Add", form, type, typeValidation, false)) {
            return;
        }
        try {
            context.deviceRepository().save(new Device(customer, type.getSelectedItem().toString(), brand.getText(),
                    model.getText(), serial.getText()));
            loadSelectedCustomerDevices();
        } catch (RuntimeException exception) {
            showError("Could not save device", exception);
        }
    }

    private void editDevice() {
        Device device = selectedDevice();
        if (device == null) {
            showMessage("Select a device first.");
            return;
        }
        JComboBox<String> type = createDeviceTypeCombo();
        type.setSelectedItem(device.getDeviceType());
        JLabel typeValidation = validationLabel();
        JTextField brand = new JTextField(valueOrEmpty(device.getBrand()), 20);
        JTextField model = new JTextField(valueOrEmpty(device.getModel()), 20);
        JTextField serial = new JTextField(valueOrEmpty(device.getSerialNumber()), 20);
        JPanel form = deviceForm(type, brand, model, serial, typeValidation);
        if (!showDeviceDialog("Edit device", "Save", form, type, typeValidation, true)) {
            return;
        }
        try {
            Device updated = new Device(device.getId(), device.getCustomer(),
                    type.getSelectedItem().toString(), brand.getText(),
                    model.getText(), serial.getText(), device.getCreatedAt(), device.getUpdatedAt());
            context.deviceRepository().update(updated);
            loadSelectedCustomerDevices();
        } catch (RuntimeException exception) {
            showError("Could not update device", exception);
        }
    }

    private static JComboBox<String> createDeviceTypeCombo() {
        JComboBox<String> type = new JComboBox<>();
        type.addItem(DEVICE_TYPE_PROMPT);
        for (String deviceType : DEVICE_TYPES) {
            type.addItem(deviceType);
        }
        AppTheme.styleComboBox(type);
        return type;
    }

    private JPanel deviceForm(JComboBox<String> type, JTextField brand, JTextField model, JTextField serial,
            JLabel typeValidation) {
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
        form.add(requiredLabel("Device type"));
        form.add(fieldWithValidation(type, typeValidation));
        form.add(new JLabel("Brand"));
        form.add(brand);
        form.add(new JLabel("Model"));
        form.add(model);
        form.add(new JLabel("Serial number"));
        form.add(serial);
        return form;
    }

    private boolean showDeviceDialog(String title, String saveLabel, JPanel form, JComboBox<String> type,
            JLabel typeValidation, boolean showCancelButton) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        JButton saveButton = new JButton(saveLabel);
        AppTheme.stylePrimaryButton(saveButton);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = null;
        if (showCancelButton) {
            cancelButton = new JButton("Cancel");
            AppTheme.styleSecondaryButton(cancelButton);
            buttons.add(cancelButton);
        }
        buttons.add(saveButton);
        boolean[] saved = { false };
        boolean[] validationRequested = { false };
        boolean[] typeValid = { false };
        Runnable validate = () -> {
            typeValid[0] = isDeviceTypeValid(type);
            if (validationRequested[0]) {
                validateDeviceType(type, typeValidation);
            }
            styleSubmitButton(saveButton, saveLabel, typeValid[0]);
        };
        type.addActionListener(event -> validate.run());
        validate.run();

        dialog.setLayout(new BorderLayout(8, 8));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(saveButton);
        saveButton.addActionListener(event -> {
            validationRequested[0] = true;
            validate.run();
            if (typeValid[0]) {
                saved[0] = true;
                dialog.dispose();
            }
        });
        if (cancelButton != null) {
            cancelButton.addActionListener(event -> dialog.dispose());
        }
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return saved[0];
    }

    private static boolean validateDeviceType(JComboBox<String> type, JLabel validation) {
        boolean valid = isDeviceTypeValid(type);
        validation.setText(valid ? " " : "Select a device type.");
        return valid;
    }

    private static boolean isDeviceTypeValid(JComboBox<String> type) {
        Object selectedType = type.getSelectedItem();
        return selectedType != null && !DEVICE_TYPE_PROMPT.equals(selectedType);
    }

    private static void styleSubmitButton(JButton button, String label, boolean valid) {
        button.setText(label);
        button.setEnabled(true);
        button.setBackground(valid ? AppTheme.PRIMARY : AppTheme.SOFT_SURFACE);
        button.setForeground(valid ? AppTheme.TEXT : AppTheme.TEXT_SECONDARY);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
    }

    private void deleteDevice() {
        Device device = selectedDevice();
        if (device == null) {
            showMessage("Select a device first.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Delete " + displayDevice(device) + "?",
                "Confirm delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            context.deviceRepository().delete(device.getId());
            loadSelectedCustomerDevices();
        } catch (RuntimeException exception) {
            showError("Could not delete device", exception);
        }
    }

    private void createRepairJob() {
        int selectedRow = devicesTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= devices.size()) {
            showMessage("Select a device first.");
            return;
        }
        JTextField description = new JTextField(24);
        JTextField desiredDays = new JTextField("7", 8);
        JLabel descriptionError = validationLabel();
        JLabel daysError = validationLabel();
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
        form.add(requiredLabel("Issue description"));
        form.add(fieldWithValidation(description, descriptionError));
        form.add(requiredLabel("Desired days"));
        form.add(fieldWithValidation(desiredDays, daysError));

        Runnable validate = () -> {
            descriptionError.setText(description.getText().isBlank() ? "Issue description is required." : " ");
            String daysText = desiredDays.getText().trim();
            try {
                if (Integer.parseInt(daysText) <= 0) {
                    daysError.setText("Enter a number greater than 0.");
                } else {
                    daysError.setText(" ");
                }
            } catch (NumberFormatException exception) {
                daysError.setText("Enter a whole number greater than 0.");
            }
        };
        addValidationListener(description, validate);
        addValidationListener(desiredDays, validate);
        validate.run();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Create repair job", Dialog.ModalityType.APPLICATION_MODAL);
        JButton createButton = new JButton("Create");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(createButton);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(createButton);
        createButton.addActionListener(event -> {
            validate.run();
            if (description.getText().isBlank() || !daysError.getText().isBlank()) {
                return;
            }
            try {
                int modelRow = devicesTable.convertRowIndexToModel(selectedRow);
                Device device = devices.get(modelRow);
                context.jobService().createJob(device.getId(), description.getText().trim(),
                        Integer.parseInt(desiredDays.getText().trim()));
                dialog.dispose();
                loadSelectedCustomerDevices();
                showMessage("Repair job created.");
                afterJobCreated.run();
            } catch (RuntimeException exception) {
                showError("Could not create repair job", exception);
            }
        });
        cancelButton.addActionListener(event -> dialog.dispose());
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void prepareStatusUpdate() {
        nextStatusCombo.removeAllItems();
        statusNoteField.setText("");
        int selectedRow = jobsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= jobs.size()) {
            resetStatusControls();
            return;
        }

        RepairJob job = jobs.get(jobsTable.convertRowIndexToModel(selectedRow));
        List<JobStatus> nextStatuses;
        try {
            nextStatuses = context.jobService().getAvailableNextStatuses(job.getId());
        } catch (RuntimeException exception) {
            showError("Could not load repair actions", exception);
            resetStatusControls();
            return;
        }
        for (JobStatus status : nextStatuses) {
                nextStatusCombo.addItem(status);
        }
        boolean canUpdate = nextStatusCombo.getItemCount() > 0;
        nextStatusCombo.setEnabled(canUpdate);
        statusNoteField.setEnabled(canUpdate);
        updateStatusButton.setEnabled(canUpdate);
        if (canUpdate) {
            JobStatus nextStatus = (JobStatus) nextStatusCombo.getSelectedItem();
            updateStatusButton.setText("Update to " + nextStatus.getDisplayName());
        } else {
            updateStatusButton.setText("Update status");
            if (job.getStatus() == JobStatus.DIAGNOSING) {
                statusNoteField.setText("Create a draft estimate before sending it for approval.");
            } else if (job.getStatus() == JobStatus.WAITING_FOR_APPROVAL) {
                statusNoteField.setText("Approve or reject the estimate in the Estimate tab.");
            } else if (job.getStatus() == JobStatus.CLOSED) {
                statusNoteField.setText("This repair is already closed.");
            }
        }
    }

    private void resetStatusControls() {
        nextStatusCombo.removeAllItems();
        nextStatusCombo.setEnabled(false);
        statusNoteField.setText("");
        statusNoteField.setEnabled(false);
        updateStatusButton.setEnabled(false);
        updateStatusButton.setText("Update status");
    }

    private void updateSelectedRepairStatus() {
        int selectedRow = jobsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= jobs.size()) {
            showMessage("Select a repair from the history list first.");
            return;
        }
        JobStatus newStatus = (JobStatus) nextStatusCombo.getSelectedItem();
        if (newStatus == null) {
            showMessage("Choose the next repair status.");
            return;
        }

        RepairJob selectedJob = jobs.get(jobsTable.convertRowIndexToModel(selectedRow));
        try {
            context.jobService().updateJobStatusWithHistory(
                    selectedJob.getId(), newStatus, statusNoteField.getText().trim());
            loadSelectedCustomerDevices();
            afterJobCreated.run();
            showMessage("Repair #" + selectedJob.getId() + " updated to "
                    + newStatus.getDisplayName() + ".");
        } catch (RuntimeException exception) {
            showError("Could not update repair status", exception);
        }
    }

    private Customer selectedCustomer() {
        int row = customersTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return customers.get(customersTable.convertRowIndexToModel(row));
    }

    private Device selectedDevice() {
        int row = devicesTable.getSelectedRow();
        return row < 0 ? null : devices.get(devicesTable.convertRowIndexToModel(row));
    }

    private static String displayDevice(Device device) {
        return ((device.getBrand() == null ? "" : device.getBrand() + " ")
                + (device.getModel() == null ? device.getDeviceType() : device.getModel())).trim();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static JLabel validationLabel() {
        JLabel label = new JLabel(" ");
        label.setForeground(AppTheme.DANGER);
        label.setFont(AppTheme.BODY);
        return label;
    }

    private static JPanel fieldWithValidation(JComponent field, JLabel validation) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.add(field, BorderLayout.CENTER);
        panel.add(validation, BorderLayout.SOUTH);
        return panel;
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

    private void selectCustomer(int customerId) {
        for (int row = 0; row < customers.size(); row++) {
            if (customers.get(row).getId() == customerId) {
                customersTable.setRowSelectionInterval(row, row);
                break;
            }
        }
    }

    private void showError(String title, RuntimeException exception) {
        exception.printStackTrace();
        JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }
}
