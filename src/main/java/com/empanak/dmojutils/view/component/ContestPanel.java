package com.empanak.dmojutils.view.component;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.empanak.dmojutils.dmojDTO.contestList.ContestData;
import com.empanak.dmojutils.repository.ContestRepository;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ContestPanel {
    private JTextField txtContestFilter;
    private JTable contestTable;
    private JButton clearSelectionsBtn;
    @Getter
    private JPanel rootPanel;
    private DefaultTableModel contestTableModel;
    @Getter
    private Map<String, Pair<ContestData, Boolean>> contestList;
    private TableRowSorter<DefaultTableModel> sorter;
    @Setter
    private Runnable onContestTableChange;
    @Getter
    private String selectedContest;
    @Getter
    private int selectedContestsCount = 0;
    private boolean clearingSelections = false;
    private final ContestRepository contestRepository;

    public ContestPanel(ContestRepository contestRepository, boolean enableSelection) {
        $$$setupUI$$$();
        this.contestRepository = contestRepository;
        initComponents();
        if (!enableSelection) {
            contestTable.getColumnModel().getColumn(0).setCellEditor(null);
        }
    }

    public void initComponents() {
        contestTableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            selectedContest = (String) contestTableModel.getValueAt(row, 1);
            if (column != 0) return;
            Boolean selected = (Boolean) contestTableModel.getValueAt(row, column);
            contestList.get((String) contestTableModel.getValueAt(row, 1)).setValue(selected);
            if (selected)
                selectedContestsCount++;
            else
                selectedContestsCount--;
            if (clearingSelections) return;
            if(onContestTableChange != null)
                onContestTableChange.run();
        });

        clearSelectionsBtn.addActionListener(e -> {
            clearingSelections = true;
            for (int i = 0; i < contestTable.getRowCount(); i++) {
                contestTable.setValueAt(false, i, 0);
            }
            clearingSelections = false;
            onContestTableChange.run();
        });
        txtContestFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }
        });
    }

    private void createUIComponents() {
        String[] contestColumns = {"X", "ID", "Nombre"};
        contestTableModel = new DefaultTableModel(contestColumns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return Boolean.class;
                }
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        contestTable = new JTable(contestTableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                Object value = getValueAt(row, column);
                return value != null ? value.toString() : null;
            }
        };

        contestTable.getTableHeader().setReorderingAllowed(false);
        contestTable.setRowHeight(25);
        contestTable.getColumnModel().getColumn(0).setMaxWidth(30);

        sorter = new TableRowSorter<>(contestTableModel);
        contestTable.setRowSorter(sorter);
    }

    private void filter() {
        String filter = txtContestFilter.getText();
        if (filter.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filter, 1, 2));
        }
    }

    public void clearTable() {
        contestTableModel.setRowCount(0);
    }

    public void loadContests() {
        contestList = new HashMap<>();
        contestTableModel.setRowCount(0);
        Map<String, ContestData> contests = contestRepository.getContestListCache();
        if(contests == null){
            System.out.println("La cache de concursos esta vacia");
            return;
        }
        for (ContestData contest : contests.values()) {
            contestTableModel.addRow(new Object[]{false, contest.key, contest.name});
            contestList.put(contest.key, MutablePair.of(contest, false));
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.setBorder(BorderFactory.createTitledBorder(null, "Concursos", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Buscar:");
        rootPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(contestTable);
        clearSelectionsBtn = new JButton();
        clearSelectionsBtn.setText("Limpiar selecciones");
        rootPanel.add(clearSelectionsBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtContestFilter = new JTextField();
        rootPanel.add(txtContestFilter, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
