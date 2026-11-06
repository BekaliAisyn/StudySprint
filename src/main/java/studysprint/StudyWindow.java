package studysprint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/** Presentation only: all domain mutations pass through StudyFacade. */
public final class StudyWindow extends JFrame {
    private final StudyFacade facade;
    private final ActivityLog activity;
    private final DefaultTableModel taskModel=tableModel("Title","Subject","Minutes","Priority","Due","Done");
    private final DefaultTableModel sessionModel=tableModel("Session","Subject","Minutes","Done");
    private final JTable taskTable=new JTable(taskModel), sessionTable=new JTable(sessionModel);
    private final DefaultListModel<String> planModel=new DefaultListModel<>();
    private final JList<String> planList=new JList<>(planModel);
    private final JTextArea preview=new JTextArea(), activityText=new JTextArea();
    private final JTextField planName=new JTextField("My study sprint",18);
    private final JSpinner budget=new JSpinner(new SpinnerNumberModel(90,5,480,5));
    private final JComboBox<String> strategy=new JComboBox<>(new String[]{"Deadline first","Priority first"});
    private final JCheckBox includeProgress=new JCheckBox("Include progress summary",true);
    private final JLabel status=new JLabel("Ready"), metrics=new JLabel();
    private final JTabbedPane tabs=new JTabbedPane();
    private List<Task> displayedTasks=List.of();
    private List<Plan> displayedPlans=List.of();
    private String selectedPlanId;
    private boolean refreshing;
    private static final Color NAVY=new Color(23,38,57), TEAL=new Color(0,108,112), BG=new Color(245,247,250);

    public StudyWindow(StudyFacade facade,EventBus events,ActivityLog activity) {
        super("Study Sprint - Bekali Aisyn - SE-2517");
        this.facade=facade; this.activity=activity;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1180,800); setMinimumSize(new Dimension(980,680)); setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(0,16)); root.setBackground(BG); root.setBorder(new EmptyBorder(22,26,16,26)); setContentPane(root);
        JPanel header=new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel title=new JLabel("STUDY SPRINT"); title.setFont(new Font("SansSerif",Font.BOLD,28)); title.setForeground(NAVY);
        JLabel subtitle=new JLabel("Make time for what matters.  /  Bekali Aisyn  /  SE-2517"); subtitle.setForeground(new Color(80,94,110));
        JPanel heading=new JPanel(new GridLayout(2,1,0,6)); heading.setOpaque(false); heading.add(title); heading.add(subtitle);
        header.add(heading,BorderLayout.WEST); metrics.setForeground(TEAL); header.add(metrics,BorderLayout.EAST); root.add(header,BorderLayout.NORTH);
        tabs.addTab("01  Tasks",taskPanel()); tabs.addTab("02  Plans & progress",planPanel());
        activityText.setEditable(false); activityText.setFont(new Font("Monospaced",Font.PLAIN,13));
        JPanel logPanel=new JPanel(new BorderLayout(0,10)); logPanel.setBorder(new EmptyBorder(16,16,16,16));
        logPanel.add(new JLabel("Activity from this application session. Changes appear automatically."),BorderLayout.NORTH);
        logPanel.add(new JScrollPane(activityText)); tabs.addTab("03  Activity",logPanel); root.add(tabs,BorderLayout.CENTER);
        status.setBorder(new EmptyBorder(8,4,0,4)); root.add(status,BorderLayout.SOUTH);
        events.subscribe(event->{refresh(); status.setText(event.message());});
        refresh();
    }
    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns,0) { public boolean isCellEditable(int row,int column) { return false; } };
    }
    private static JButton button(String text,Runnable action) {
        JButton b=new JButton(text); b.setFocusPainted(false); b.addActionListener(e->action.run()); return b;
    }
    private static void styleTable(JTable table) {
        table.setRowHeight(34); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false); table.setFillsViewportHeight(true); table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13));
    }
    private JPanel taskPanel() {
        JPanel panel=new JPanel(new BorderLayout(0,14)); panel.setBorder(new EmptyBorder(16,16,16,16));
        panel.add(new JLabel("Your study backlog  |  Priority 3 is highest. Completed tasks are excluded from new plans."),BorderLayout.NORTH);
        styleTable(taskTable); taskTable.setAutoCreateRowSorter(true); panel.add(new JScrollPane(taskTable));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("+ New task",()->editTask(null)));
        actions.add(button("Edit selected",()->perform(()->editTask(selectedTask()))));
        actions.add(button("Delete selected",()->perform(()->{
            Task task=selectedTask(); if(confirm("Delete '"+task.title()+"'? Saved plans keep their snapshots.")) facade.deleteTask(task.id());
        })));
        actions.add(button("Create a study plan →",()->tabs.setSelectedIndex(1)));
        panel.add(actions,BorderLayout.SOUTH); return panel;
    }
    private JPanel planPanel() {
        JPanel panel=new JPanel(new BorderLayout(12,14)); panel.setBorder(new EmptyBorder(16,16,16,16));
        JPanel config=new JPanel(new FlowLayout(FlowLayout.LEFT,9,8));
        config.add(new JLabel("Name")); config.add(planName); config.add(new JLabel("Minutes")); config.add(budget); config.add(strategy);
        config.add(button("Generate & save",()->perform(()->{
            budget.commitEdit();
            Plan plan=facade.generate(planName.getText(),(Integer)budget.getValue(),strategy.getSelectedIndex()==0?new DeadlineStrategy():new PriorityStrategy());
            selectedPlanId=plan.id(); refresh();
        }))); panel.add(config,BorderLayout.NORTH);
        planList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); planList.setFixedCellHeight(38);
        planList.addListSelectionListener(e->{if(!refreshing && !e.getValueIsAdjusting()) {
            int index=planList.getSelectedIndex(); selectedPlanId=index<0?null:displayedPlans.get(index).id(); showPlan();
        }});
        JPanel saved=new JPanel(new BorderLayout(0,8)); saved.add(new JLabel("SAVED PLANS"),BorderLayout.NORTH); saved.add(new JScrollPane(planList));
        saved.add(button("Delete plan",()->perform(()->{String id=requirePlan(); if(confirm("Delete this saved plan?")) {facade.deletePlan(id); selectedPlanId=null; refresh();}})),BorderLayout.SOUTH);
        JPanel detail=new JPanel(new BorderLayout(0,10));
        styleTable(sessionTable);
        preview.setEditable(false); preview.setFont(new Font("Monospaced",Font.PLAIN,13)); preview.setMargin(new Insets(12,12,12,12));
        JSplitPane vertical=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(sessionTable),new JScrollPane(preview)); vertical.setResizeWeight(0.42); vertical.setDividerLocation(200);
        detail.add(vertical);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Toggle session done",()->perform(()->facade.toggleSession(requirePlan(),sessionTable.getSelectedRow()))));
        includeProgress.addActionListener(e->showPlan()); actions.add(includeProgress);
        actions.add(button("Export .txt",()->perform(this::export))); detail.add(actions,BorderLayout.SOUTH);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,saved,detail); split.setDividerLocation(245); panel.add(split);
        panel.add(new JLabel("The last task may be split to fit the budget. Session progress belongs to this plan; edit a task to mark the whole task done."),BorderLayout.SOUTH);
        return panel;
    }
    private Task selectedTask() {
        int row=taskTable.getSelectedRow(); if(row<0) throw new IllegalArgumentException("Select a task first.");
        return displayedTasks.get(taskTable.convertRowIndexToModel(row));
    }
    private String requirePlan() { if(selectedPlanId==null) throw new IllegalArgumentException("Select or generate a plan first."); return selectedPlanId; }
    private void editTask(Task old) {
        JTextField title=new JTextField(old==null?"":old.title(),24), subject=new JTextField(old==null?"":old.subject(),24);
        JSpinner minutes=new JSpinner(new SpinnerNumberModel(old==null?30:old.minutes(),5,480,5));
        JSpinner priority=new JSpinner(new SpinnerNumberModel(old==null?2:old.priority(),1,3,1));
        JTextField due=new JTextField(old==null?LocalDate.now().plusDays(1).toString():old.due().toString());
        JCheckBox done=new JCheckBox("Entire task completed",old!=null && old.done());
        JPanel fields=new JPanel(new GridLayout(0,2,12,12));
        fields.add(new JLabel("Title")); fields.add(title); fields.add(new JLabel("Subject")); fields.add(subject);
        fields.add(new JLabel("Duration (5-480 min)")); fields.add(minutes); fields.add(new JLabel("Priority (3 = highest)")); fields.add(priority);
        fields.add(new JLabel("Due date (YYYY-MM-DD)")); fields.add(due); fields.add(new JLabel("Status")); fields.add(done);
        while(JOptionPane.showConfirmDialog(this,fields,old==null?"New study task":"Edit study task",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                minutes.commitEdit(); priority.commitEdit();
                facade.saveTask(old==null?null:old.id(),title.getText(),subject.getText(),(Integer)minutes.getValue(),(Integer)priority.getValue(),LocalDate.parse(due.getText().trim()),done.isSelected());
                return;
            } catch(Exception error) { showError(error); }
        }
    }
    private void export() throws Exception {
        String text=facade.report(requirePlan(),includeProgress.isSelected());
        JFileChooser chooser=new JFileChooser(); chooser.setSelectedFile(new java.io.File("study-plan.txt"));
        if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
            java.nio.file.Path path=chooser.getSelectedFile().toPath();
            if(Files.exists(path) && !confirm("Replace existing file "+path.getFileName()+"?")) return;
            Files.writeString(path,text,StandardCharsets.UTF_8); status.setText("Exported plan: "+path.toAbsolutePath());
        }
    }
    private boolean confirm(String text) { return JOptionPane.showConfirmDialog(this,text,"Study Sprint",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION; }
    private void refresh() {
        refreshing=true;
        try {
            displayedTasks=facade.tasks(); taskModel.setRowCount(0);
            for(Task t:displayedTasks) taskModel.addRow(new Object[]{t.title(),t.subject(),t.minutes(),t.priority(),t.due(),t.done()?"Yes":"No"});
            displayedPlans=facade.plans(); planModel.clear(); int selected=-1;
            for(int i=0;i<displayedPlans.size();i++) {Plan p=displayedPlans.get(i); planModel.addElement(p.name()+" · "+p.scheduledMinutes()+" min"); if(p.id().equals(selectedPlanId)) selected=i;}
            if(selected<0 && !displayedPlans.isEmpty()) selected=displayedPlans.size()-1;
            selectedPlanId=selected<0?null:displayedPlans.get(selected).id(); planList.setSelectedIndex(selected);
            metrics.setText(displayedTasks.size()+" tasks   /   "+displayedPlans.size()+" plans");
            activityText.setText(String.join("\n\n",activity.entries())); activityText.setCaretPosition(0); showPlan();
        } finally { refreshing=false; }
    }
    private void showPlan() {
        sessionModel.setRowCount(0);
        if(selectedPlanId==null) {preview.setText("Your next focused session starts here.\n\nChoose a name, time budget and strategy,\nthen click Generate & save."); return;}
        Plan p=facade.plan(selectedPlanId);
        for(Session s:p.sessions()) sessionModel.addRow(new Object[]{s.title(),s.subject(),s.minutes(),s.done()?"Yes":"No"});
        preview.setText(facade.report(p.id(),includeProgress.isSelected())); preview.setCaretPosition(0);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private void perform(Action action) { try {action.run();} catch(Exception error) {showError(error);} }
    private void showError(Exception error) {
        String message=error instanceof java.time.format.DateTimeParseException?"Enter a valid due date in YYYY-MM-DD format.":error.getMessage();
        JOptionPane.showMessageDialog(this,message,"Please check your input",JOptionPane.ERROR_MESSAGE);
    }
}
