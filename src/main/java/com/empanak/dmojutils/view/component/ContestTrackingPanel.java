package com.empanak.dmojutils.view.component;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import com.empanak.dmojutils.common.ColorRowRenderer;
import com.empanak.dmojutils.dmojDTO.contest.Contest;
import com.empanak.dmojutils.dmojDTO.contest.Problem;
import com.empanak.dmojutils.dmojDTO.contest.Ranking;
import com.empanak.dmojutils.dmojDTO.submissions.ResponseSubmissionsData;
import com.empanak.dmojutils.dmojDTO.submissions.Submission;
import com.empanak.dmojutils.service.DmojCallsService;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContestTrackingPanel {
    @Getter
    private JPanel rootPanel;
    private JTable trackingTable;
    private JLabel lbRemainingTime;
    private JLabel lbParticipants;
    private JLabel lbWarnings;
    private DefaultTableModel trackingTableModel;
    private Contest contest;
    private Color[] colors;
    private List<Ranking> rankings;
    private final List<String> problemOrder = new ArrayList<>();
    private final Map<String, String> positions = new HashMap<>();
    private final Map<String, Map<String, Integer>> userStatus = new HashMap<>(); //Username <ProblemCode, 0 No, 1 AC, 2 FtoSolve>
    private final Set<String> participants = new HashSet<>();
    private final Set<String> deliveredBalloons = new HashSet<>(); //Username~ProblemKey
    private boolean updatingTable = false;

    private final String contestKey;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public ContestTrackingPanel(String contestKey) {
        this.contestKey = contestKey;
        contest = DmojCallsService.getContestData(contestKey).data.object;
        rankings = contest.rankings;
        colors = new Color[contest.problems.size()];

        $$$setupUI$$$();
        initComponents();
    }

    private void initComponents() {
        trackingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = trackingTable.rowAtPoint(e.getPoint());
                int column = trackingTable.columnAtPoint(e.getPoint());
                if (row == 0 && column >= 3) {
                    Color newColor = JColorChooser.showDialog(null, "Selecciona un color", colors[column - 3]);
                    if (newColor != null) {
                        colors[column - 3] = newColor;
                        trackingTable.repaint();
                    }
                }
            }
        });

        trackingTableModel.addTableModelListener(e -> {
            if(updatingTable) return;
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (row >= 1) {
                    if (column == 2) {
                        Object val = trackingTableModel.getValueAt(row, column);
                        if (val != null && !val.toString().trim().isEmpty()) {
                            positions.put(trackingTableModel.getValueAt(row, 1).toString(), val.toString().trim());
                        }
                    }
                    if (column >= 3) {
                        Object val = trackingTableModel.getValueAt(row, column);
                        updatingTable = true;
                        try {
                            String user = trackingTableModel.getValueAt(row, 1).toString();
                            String problemCode = problemOrder.get(column - 3);
                            int problemStatus = userStatus.get(user).getOrDefault(problemCode, 0);
                            if (val != null && !val.toString().trim().isEmpty() && ("X".equals(val.toString().trim()) || "x".equals(val.toString().trim()))) {
                                switch (problemStatus) {
                                    case 0:
                                        trackingTableModel.setValueAt("", row, column);
                                        return;
                                    case 2:
                                        trackingTableModel.setValueAt("XFtS", row, column);
                                        break;
                                }
                                deliveredBalloons.add(user + "~" + problemCode);
                                System.out.println("Globo entregado a" + user + " para el problema" + problemCode);
                            } else {
                                System.out.println("Globo eliminado de " + user + " para el problema" + problemCode);
                                trackingTableModel.setValueAt(stringOfStatus(problemStatus), row, column);
                                deliveredBalloons.remove(trackingTableModel.getValueAt(row, 1).toString() + "~" + problemOrder.get(column - 3));
                            }
                        } finally {
                            updatingTable = false;
                        }
                    }
                }
            }
        });
        updateTableCycle();
    }

    private void createUIComponents() {
        List<String> columns = new ArrayList<>();
        columns.add("Rank");
        columns.add("User");
        columns.add("Posicion");
        for (Problem problem : contest.problems) {
            problemOrder.add(problem.code);
            columns.add(problem.name);
        }
        trackingTableModel = new DefaultTableModel(columns.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 2 && row > 0;
            }
        };
        trackingTable = new JTable(trackingTableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int column = columnAtPoint(e.getPoint());
                if (column == 0 || column >= 3) return null;
                Object value = getValueAt(row, column);
                return value != null ? value.toString() : null;
            }
        };
        ColorRowRenderer renderer = new ColorRowRenderer(colors);
        renderer.setNegativeOffset(3);
        trackingTable.getTableHeader().setReorderingAllowed(false);
        trackingTable.setDefaultRenderer(Object.class, renderer);
        trackingTable.setDefaultRenderer(Boolean.class, renderer);
        trackingTable.setRowHeight(25);
        trackingTable.getColumnModel().getColumn(0).setPreferredWidth(25);
        trackingTable.getColumnModel().getColumn(1).setPreferredWidth(50);
    }

    private void updateTableCycle() {
        final Runnable updateTable = () -> {
            try {
                SwingUtilities.invokeLater(() -> lbWarnings.setText("Actualizando..."));
                System.out.println("[" + this.contestKey + "] Actualizando tabla");
                contest = DmojCallsService.getContestData(contestKey).data.object;
                if (contest == null || contest.problems == null || contest.rankings == null) {
                    System.out.println("[" + this.contestKey + "] La API no responde o hay una respuesta inválida");
                    return;
                }
                rankings = contest.rankings;
                participants.clear();
                for (Ranking r : rankings) {
                    participants.add(r.user);
                }

                for (Problem problem : contest.problems) {
                    ResponseSubmissionsData submissions = DmojCallsService.getSubmissionsByProblemAndStatus(problem.code, DmojCallsService.STATUS.AC);
                    if (submissions == null || submissions.data == null) continue;
                    String fToSolveUser = null;
                    Instant fToSolveTime = Instant.MAX;
                    for (Submission submission : submissions.data.objects) {
                        if (submission.date.isBefore(contest.start_time) || submission.date.isAfter(contest.end_time))
                            continue;
                        if (submission.date.isBefore(fToSolveTime) && submission.contest != null && participants.contains(submission.user)) {
                            fToSolveTime = submission.date;
                            fToSolveUser = submission.user;
                        }
                        userStatus.compute(submission.user, (user, status) -> status == null ? new HashMap<>() : status);
                        userStatus.get(submission.user).put(problem.code, 1);
                    }
                    if (fToSolveUser != null) {
                        userStatus.compute(fToSolveUser, (user, status) -> status == null ? new HashMap<>() : status);
                        userStatus.get(fToSolveUser).put(problem.code, 2);
                    }
                }
                System.out.println("[" + this.contestKey + "] Globos entregados: " + deliveredBalloons.size());
                SwingUtilities.invokeLater(this::updateScore);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> lbWarnings.setText("Error de red o actualización. Reintentando..."));
                System.out.println("[" + this.contestKey + "] Error de red o procesamiento de tabla." + e.getMessage());
            }
        };
        final Runnable updateClock = () -> {
            if (contest != null && contest.end_time.isAfter(Instant.now()))
                SwingUtilities.invokeLater(() -> lbRemainingTime.setText(DurationFormatUtils.formatDuration(contest.end_time.toEpochMilli() - Instant.now().toEpochMilli(), "HH:mm:ss")));
            else {
                SwingUtilities.invokeLater(() -> lbRemainingTime.setText("00:00:00"));
                scheduler.shutdown();
                SwingUtilities.invokeLater(this::stopTracking);
            }
        };
        scheduler.scheduleAtFixedRate(updateTable, 0, 30, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(updateClock, 0, 1, TimeUnit.SECONDS);
        System.out.println("Seguimiento de concurso : " + contest.key + " iniciado");

    }

    private void stopTracking() {
        scheduler.shutdown();
        JOptionPane.showMessageDialog(null, "Seguimiento de concurso : " + contest.key + " detenido", "Seguimiento de globos", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateScore() {
        lbWarnings.setText("");
        lbParticipants.setText(contest.rankings.size() + " Participantes");
        trackingTableModel.setRowCount(1);
        int rank = 0;
        for (Ranking ranking : rankings) {
            List<Object> values = new ArrayList<>();
            values.add(++rank);
            values.add(ranking.user);
            values.add((positions.get(ranking.user) == null ? "" : positions.get(ranking.user)));
            for (String problemCode : problemOrder) {
                if (userStatus.get(ranking.user) == null) continue;
                int problemStatus = userStatus.get(ranking.user).getOrDefault(problemCode, -1);

                if (deliveredBalloons.contains(ranking.user + "~" + problemCode)) {
                    values.add((problemStatus == 1 ? "X" : "XFtS"));
                    continue;
                }

                values.add(stringOfStatus(problemStatus));
            }
            trackingTableModel.addRow(values.toArray());
        }
    }

    private String stringOfStatus(int status){
        return switch (status) {
            case 1 -> "AC";
            case 2 -> "FtS"; //First to solve
            default -> "";
        };
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
        rootPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(trackingTable);
        lbRemainingTime = new JLabel();
        lbRemainingTime.setText("Tiempo Restante: 00:00:00");
        rootPanel.add(lbRemainingTime, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(243, 17), null, 0, false));
        lbParticipants = new JLabel();
        lbParticipants.setText("0 Participantes");
        rootPanel.add(lbParticipants, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lbWarnings = new JLabel();
        lbWarnings.setText("");
        rootPanel.add(lbWarnings, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
