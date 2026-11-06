package studysprint;

import javax.swing.*;
import java.nio.file.*;
import java.nio.channels.*;

public final class Main {
    // Retained for the application's lifetime; prevents two processes losing each other's edits.
    private static FileChannel lockChannel;
    private static FileLock lock;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                java.util.Enumeration<Object> keys=UIManager.getDefaults().keys();
                while(keys.hasMoreElements()) {
                    Object key=keys.nextElement();
                    if(UIManager.get(key) instanceof javax.swing.plaf.FontUIResource)
                        UIManager.put(key,new javax.swing.plaf.FontUIResource("SansSerif",java.awt.Font.PLAIN,14));
                }
                Path data=Path.of(System.getProperty("studysprint.data","data/study.properties")).toAbsolutePath();
                Files.createDirectories(data.getParent());
                lockChannel=FileChannel.open(data.resolveSibling(data.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
                lock=lockChannel.tryLock();
                if(lock==null) throw new IllegalStateException("Study Sprint is already using this data file. Close the other window first.");
                boolean firstRun=!Files.exists(data);
                EventBus events=new EventBus(); ActivityLog log=new ActivityLog(); events.subscribe(log);
                StudyFacade facade=new StudyFacade(new PropertiesRepository(data),events);
                if(firstRun) facade.seedIfEmpty();
                new StudyWindow(facade,events,log).setVisible(true);
            } catch(Exception error) {
                JOptionPane.showMessageDialog(null,error.getMessage(),"Study Sprint could not start",JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
