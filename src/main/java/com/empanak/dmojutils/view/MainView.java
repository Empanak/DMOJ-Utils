package com.empanak.dmojutils.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import com.empanak.dmojutils.common.JTextFieldPlaceholder;
import com.empanak.dmojutils.config.AppConfig;
import com.empanak.dmojutils.config.ConfigChangeListener;
import com.empanak.dmojutils.config.ConfigManager;
import com.empanak.dmojutils.dmojDTO.contest.ResponseContestData;
import com.empanak.dmojutils.dmojDTO.contestList.ContestData;
import com.empanak.dmojutils.dmojDTO.contestList.ResponseContestList;
import com.empanak.dmojutils.repository.ContestRepository;
import com.empanak.dmojutils.service.DmojCallsService;
import com.empanak.dmojutils.service.RankCalculatorService;
import com.empanak.dmojutils.view.component.ContestPanel;
import com.empanak.dmojutils.view.component.ContestTrackingPanel;
import com.empanak.dmojutils.view.component.ResultsPanel;
import com.empanak.dmojutils.view.component.UsersPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainView extends JFrame implements ConfigChangeListener {
    private JPanel mainPanel;
    private JTabbedPane tabbedMenu;
    private JTextField txtTokenAPI;
    private JLabel lbSelectedContests;
    private JLabel lbExcludedUsers;
    private JPanel contestPanelPoin;
    private JPanel contestPanelUps;
    private JPanel usersPanelUps;
    private JPanel usersPanelPoin;
    private JPanel resultsPanelPoin;
    private JPanel resultsPanelUps;
    private JButton btnSaveConfig;
    private JTextField txtServerURL;
    private JPanel excludedProblemsPanel;
    private DefaultTableModel problemsTableModel;
    private JTable problemsTable;
    private JTextField txtProblemsFilter;
    private JButton btnConfirmProblems;
    private JPanel contestPanelBalloons;
    private JTabbedPane tabbedPane1;
    private JButton addContestToTrackBtn;
    private TableRowSorter<DefaultTableModel> sorterExcludedProblems;

    private ContestPanel contestPanelControllerPoin, contestPanelControllerUps;
    private UsersPanel usersPanelControllerPoin, usersPanelControllerUps;
    private ResultsPanel resultsPanelControllerPoin, resultsPanelControllerUps;
    private List<Map.Entry<String, Pair<Integer, Long>>> rankList; //username, <points, time>
    @Getter
    private static ResponseContestList contestList;
    private List<Map.Entry<String, Pair<Integer, Long>>> rankUpsList; //username, <solved, time>
    private Map<String, Set<String>> excludedProblems;
    private Set<Pair<String, String>> problemsUps;

    //Cache
    private final ContestRepository contestRepository;

    private static final Logger logger = Logger.getLogger(MainView.class.getName());

    public MainView() {
        super();
        logger.info("Inizializando DMOJ Utils");
        this.contestRepository = new ContestRepository();
        $$$setupUI$$$();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setTitle("DMOJ server Utils");
        add(mainPanel);
        pack();
        setSize(800, 600);

        initComponents();
        ConfigManager.addConfigChangeListener(this);
        loadInitialData();

        logger.info("DMOJ Utils inicializado");
    }

    @Override
    public void onConfigChanged(AppConfig appConfig) {
        loadInitialData();
    }

    private void initComponents() {
        excludedProblems = new HashMap<>();
        btnSaveConfig.addActionListener(e -> {
            AppConfig appConfig = ConfigManager.getConfig();
            String url = txtServerURL.getText();
            String token = txtTokenAPI.getText();
            appConfig.setApiURL(url);
            appConfig.setApiToken(token);
            if (ConfigManager.saveConfig(appConfig)) {
                JOptionPane.showMessageDialog(this, "Configuración guardada correctamente.", ">)", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar la configuración.", ">(", JOptionPane.ERROR_MESSAGE);
            }
        });

        problemsTableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            if (column == 0) {
                problemsTable.setEnabled(true);
                if ((Boolean) problemsTableModel.getValueAt(row, column)) {
                    excludedProblems.get(usersPanelControllerUps.getSelectedUser()).remove((String) problemsTableModel.getValueAt(row, 1));
                } else {
                    excludedProblems.get(usersPanelControllerUps.getSelectedUser()).add((String) problemsTableModel.getValueAt(row, 1));
                }
            }
        });

        btnConfirmProblems.addActionListener(e -> {
            getRankUpsolving();
            resultsPanelControllerUps.showRank(rankUpsList, usersPanelControllerUps.getExcludedUsers());
        });

        addContestToTrackBtn.addActionListener(e -> {
            ContestWindow contestSelector = new ContestWindow(contestRepository, contestKey -> {
                tabbedPane1.addTab(contestKey, new ContestTrackingPanel(contestKey).getRootPanel());
            });
            contestSelector.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            contestSelector.pack();
            contestSelector.setSize(400, 300);
            contestSelector.setVisible(true);
        });

        txtProblemsFilter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterProblems();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterProblems();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterProblems();
            }
        });
    }

    private void createUIComponents() {
        // Tab 1 - CONFIGURACIÓN
        txtTokenAPI = new JTextFieldPlaceholder("Token API (No obligatorio)");
        txtServerURL = new JTextFieldPlaceholder("URL del servidor");

        // Tab 2 - ACUMULAR PUNTOS DE CONCURSO

        // Panel de Concursos
        contestPanelControllerPoin = new ContestPanel(contestRepository, true);
        contestPanelControllerPoin.setOnContestTableChange(() -> {
            lbSelectedContests.setText(contestPanelControllerPoin.getSelectedContestsCount() + " Concursos seleccionados");
            getRankPoints();
            usersPanelControllerPoin.updateUsersList(rankList);
            resultsPanelControllerPoin.showRank(rankList, usersPanelControllerPoin.getExcludedUsers());
        });
        contestPanelPoin = new JPanel(new BorderLayout());
        contestPanelPoin.add(contestPanelControllerPoin.getRootPanel(), BorderLayout.CENTER);

        // Panel de Usuarios
        usersPanelControllerPoin = new UsersPanel(UsersPanel.UsersMode.ONLY_PARTICIPANTS);
        usersPanelControllerPoin.setOnUserTableChange(() -> {
            lbExcludedUsers.setText(usersPanelControllerPoin.getExcludedUsersCount() + " Usuarios excluidos");
            resultsPanelControllerPoin.showRank(rankList, usersPanelControllerPoin.getExcludedUsers());
        });
        usersPanelPoin = new JPanel(new BorderLayout());
        usersPanelPoin.add(usersPanelControllerPoin.getRootPanel(), BorderLayout.CENTER);

        // Panel de Resultados
        resultsPanelControllerPoin = new ResultsPanel();
        resultsPanelPoin = new JPanel(new BorderLayout());
        resultsPanelPoin.add(resultsPanelControllerPoin.getRootPanel(), BorderLayout.CENTER);

        // Tab 3 - ACUMULAR UPSOLVING
        // Panel de Concursos
        contestPanelControllerUps = new ContestPanel(contestRepository, true);
        contestPanelControllerUps.setOnContestTableChange(() -> {
            getRankUpsolving();
            loadUpsolvingProblems();
            usersPanelControllerUps.updateUsersList(rankUpsList);
            resultsPanelControllerUps.showRank(rankUpsList, usersPanelControllerUps.getExcludedUsers());
        });
        contestPanelUps = new JPanel(new BorderLayout());
        contestPanelUps.add(contestPanelControllerUps.getRootPanel(), BorderLayout.CENTER);
        // Panel de Usuarios
        usersPanelControllerUps = new UsersPanel(UsersPanel.UsersMode.ALL);
        usersPanelControllerUps.setOnUserTableChange(() -> {
            problemsTableModel.setRowCount(0); // Reinicia la tabla de problemas de usuario para seleccionar otro
            resultsPanelControllerUps.showRank(rankUpsList, usersPanelControllerUps.getExcludedUsers());
        });
        usersPanelControllerUps.setOnUserTableSelection(() -> { // Al hacer click sobre un usuario
            loadUpsolvingProblemsForUser(usersPanelControllerUps.getSelectedUser());
        });
        usersPanelUps = new JPanel(new BorderLayout());
        usersPanelUps.add(usersPanelControllerUps.getRootPanel(), BorderLayout.CENTER);
        // Panel de Resultados
        resultsPanelControllerUps = new ResultsPanel();
        resultsPanelUps = new JPanel(new BorderLayout());
        resultsPanelUps.add(resultsPanelControllerUps.getRootPanel(), BorderLayout.CENTER);
        // Tabla de Problemas Excluidos
        String[] problemsColumns = new String[]{"", "id", "nombre"};
        problemsTableModel = new DefaultTableModel(problemsColumns, 0) {
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
        problemsTable = new JTable(problemsTableModel);

        problemsTable.getTableHeader().setReorderingAllowed(false);
        problemsTable.setRowHeight(25);
        problemsTable.getColumnModel().getColumn(0).setMaxWidth(50);

        sorterExcludedProblems = new TableRowSorter<>(problemsTableModel);
        problemsTable.setRowSorter(sorterExcludedProblems);

        // 4 - Seguimiento de Globos
    }

    private void loadInitialData() {
        if (DmojCallsService.isConfigReady()) {
            txtServerURL.setText(ConfigManager.getConfig().getApiURL());
            txtTokenAPI.setText(ConfigManager.getConfig().getApiToken());
            contestList = DmojCallsService.getContests();
            if (contestRepository.getContestListCache() == null) {
                JOptionPane.showMessageDialog(this, "Verifica la conexión con el servidor o el token.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            contestPanelControllerPoin.loadContests();
            contestPanelControllerUps.loadContests();
            usersPanelControllerUps.updateUsersList(null);
            usersPanelControllerPoin.updateUsersList(null);
        } else {
            contestPanelControllerUps.clearTable();
            contestPanelControllerPoin.clearTable();
            usersPanelControllerUps.updateUsersList(null);
            usersPanelControllerPoin.updateUsersList(null);
            resultsPanelControllerUps.clearTable();
            resultsPanelControllerPoin.clearTable();
            JOptionPane.showMessageDialog(this, "Verifica la configuración.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Obtiene el rank de los usuarios seleccionados
    private void getRankPoints() {
        List<ContestData> requestedContests = new ArrayList<>();
        for (Pair<ContestData, Boolean> entry : contestPanelControllerPoin.getContestList().values()) {
            if (entry.getValue()) {
                requestedContests.add(entry.getKey());
            }
        }
        rankList = RankCalculatorService.calculateAccumulatedRank(requestedContests);
    }

    // Obtiene el rank de upsolving de los concursos seleccionados con las exclusiones correspondientes
    private void getRankUpsolving() {
        List<ContestData> requestedContests = new ArrayList<>();
        for (Pair<ContestData, Boolean> entry : contestPanelControllerUps.getContestList().values()) {
            if (entry.getValue()) {
                requestedContests.add(entry.getKey());
            }
        }
        rankUpsList = RankCalculatorService.calculateUpsolvingRank(requestedContests, excludedProblems);

        // Actualiza el registro de problemas excluidos
        // Si no existe registro del usuario introduce todos los registros como incluidos
        // Si existe registro del usuario, actualiza los registros de los problemas excluidos
    }

    // Carga los problemas de los concursos seleccionados
    private void loadUpsolvingProblems() {
        problemsUps = new HashSet<>();
        for (Pair<ContestData, Boolean> entry : contestPanelControllerUps.getContestList().values()) {
            if (entry.getValue()) {
                ResponseContestData contest = DmojCallsService.getContestData(entry.getKey().key);
                problemsUps.addAll(contest.data.object.problems.stream().map(p -> Pair.of(p.code, p.name)).collect(Collectors.toSet()));
            }
        }
    }

    // Carga los problemas de los concursos seleccionados para un usuario en el panel de upsolving
    private void loadUpsolvingProblemsForUser(String user) {
        problemsTableModel.setRowCount(0);
        excludedProblems.computeIfAbsent(user, k -> new HashSet<>());
        for (Pair<String, String> problem : problemsUps) {
            problemsTableModel.addRow(new Object[]{!excludedProblems.get(user).contains(problem.getLeft()), problem.getLeft(), problem.getRight()});
        }
    }

    private void filterProblems() {
        String filter = txtProblemsFilter.getText();
        if (filter.trim().isEmpty()) {
            sorterExcludedProblems.setRowFilter(null);
        } else {
            sorterExcludedProblems.setRowFilter(RowFilter.regexFilter("(?i)" + filter, 1, 2));
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        tabbedMenu = new JTabbedPane();
        mainPanel.add(tabbedMenu, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 4, new Insets(0, 0, 0, 0), -1, -1));
        tabbedMenu.addTab("Bienvenida", panel1);
        final JLabel label1 = new JLabel();
        label1.setName("");
        label1.setText("Bienvenidooooo");
        panel1.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Si quieres realizar acciones es necesario que ingreses tu token API y que tengas los permisos necesarios.");
        panel1.add(label2, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtTokenAPI.setToolTipText("");
        panel1.add(txtTokenAPI, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Servidor:");
        panel1.add(label3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel1.add(txtServerURL, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnSaveConfig = new JButton();
        btnSaveConfig.setText("Guardar configuración");
        panel1.add(btnSaveConfig, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(5, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(2, 3, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(0, 0, 5, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, 1, new Dimension(20, -1), new Dimension(20, -1), new Dimension(20, -1), 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Token API:");
        panel1.add(label4, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedMenu.addTab("Acumular Puntos", panel2);
        panel2.add(contestPanelPoin, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.add(usersPanelPoin, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(null, "Resultados", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        lbSelectedContests = new JLabel();
        lbSelectedContests.setText("0 Concursos considerados");
        panel4.add(lbSelectedContests, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lbExcludedUsers = new JLabel();
        lbExcludedUsers.setText("0 Usuarios excluídos");
        panel4.add(lbExcludedUsers, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel3.add(resultsPanelPoin, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedMenu.addTab("Acumular Upsolving", panel5);
        panel5.add(contestPanelUps, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.add(usersPanelUps, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.add(resultsPanelUps, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultsPanelUps.setBorder(BorderFactory.createTitledBorder(null, "Resultados", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        excludedProblemsPanel = new JPanel();
        excludedProblemsPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(excludedProblemsPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        excludedProblemsPanel.setBorder(BorderFactory.createTitledBorder(null, "Exclusiones", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        excludedProblemsPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(problemsTable);
        final JLabel label5 = new JLabel();
        label5.setText("Buscar:");
        excludedProblemsPanel.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        txtProblemsFilter = new JTextField();
        excludedProblemsPanel.add(txtProblemsFilter, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        btnConfirmProblems = new JButton();
        btnConfirmProblems.setText("Aceptar");
        excludedProblemsPanel.add(btnConfirmProblems, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedMenu.addTab("Seguimiento Globos", panel6);
        contestPanelBalloons = new JPanel();
        contestPanelBalloons.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(contestPanelBalloons, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        contestPanelBalloons.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        addContestToTrackBtn = new JButton();
        addContestToTrackBtn.setText("Añadir un concurso a seguimiento");
        contestPanelBalloons.add(addContestToTrackBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}