import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static javax.swing.SwingUtilities.updateComponentTreeUI;

public class GUI extends JPanel implements ActionListener {

    private JTextArea textArea;
    private JButton openButton;
    private JProgressBar progressBar;
    private JLabel loadLabel;
    private JButton zipButton;

    private File textFile;

    private SwingWorker<Integer, String> workerLoadFile;
    private SwingWorker<Integer, String> workerZipFile;

    private static final Logger LOGGER = Logger.getLogger(GUI.class.getName());

    public static void createAndShowGUI(){
        LOGGER.entering(GUI.class.getName(), "createAndShowGUI");

        JFrame frame = new JFrame("File opener");
        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            updateComponentTreeUI(frame);
        } catch (Exception ee) {
            ee.printStackTrace();
        }

        GUI gui = new GUI();

        frame.setContentPane(gui.createContentPane());
        frame.setLayout(null);
        frame.setVisible(true);
        LOGGER.exiting(GUI.class.getName(), "createAndShowGUI");
    }

    private JPanel createContentPane() {
        LOGGER.entering(GUI.class.getName(), "createContentPane");
        JPanel totalGUI = new JPanel();
        totalGUI.setBorder(new EtchedBorder());

        totalGUI.setSize(600, 500);
        totalGUI.setLayout(null);

        textArea = new JTextArea(16,58);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setBounds(20,20,540,320);

        loadLabel = new JLabel("Load Time: ");
        loadLabel.setBounds(20,360,250,30);

        progressBar = new JProgressBar();
        progressBar.setBounds(20, 400, 400, 30);
        progressBar.setValue(0);
        progressBar.setString("Progress: 0%");
        progressBar.setStringPainted(true);

        openButton = new JButton("Open File");
        openButton.setBounds(460,360,100,30);
        openButton.addActionListener(this);

        zipButton = new JButton("Zip File");
        zipButton.setBounds(460,400,100,30);
        zipButton.setEnabled(false);
        zipButton.addActionListener(this);

        totalGUI.add(scroll);
        totalGUI.add(loadLabel);
        totalGUI.add(progressBar);
        totalGUI.add(openButton);
        totalGUI.add(zipButton);

        LOGGER.exiting(GUI.class.getName(), "createContentPane");
        return totalGUI;
    }

    public void actionPerformed(ActionEvent e) {
        LOGGER.entering(GUI.class.getName(), "actionPerformed");
        if(e.getSource() == openButton) {
            LOGGER.log(Level.INFO, "Open File Button clicked. Opening file chooser");
            progressBar.setValue(0);
            progressBar.setString("Load Progress: " + 0 + "%");
            long startTime = System.currentTimeMillis();
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

            int returnVal = fileChooser.showOpenDialog(new GUI());

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                LOGGER.log(Level.INFO, "File selected: " + fileChooser.getSelectedFile());
                textArea.setText("");
                textFile = fileChooser.getSelectedFile();

                loadFile(textFile);
                long endTime = System.currentTimeMillis();
                long loadTime = endTime - startTime;
                System.out.println("load time: " + loadTime);
                loadLabel.setText("Load time: " + loadTime + "ms");
                LOGGER.log(Level.INFO, "File contents printed to textArea");
            }
        } else if(e.getSource() == zipButton) {
            LOGGER.log(Level.INFO, "Zip Button clicked. Zipping file");
            zipFile(textFile);
        }
        LOGGER.exiting(GUI.class.getName(), "actionPerformed");
    }

    private void zipFile(File file) {
        LOGGER.entering(GUI.class.getName(), "zipFile with params: File="+file.getPath());
        progressBar.setValue(0);
        progressBar.setString("Zip Progress: " + 0 + "%");
        String fileName = file.getName();
        String zipFilePath = file.getPath().replace("txt", "zip");

        workerZipFile = new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                LOGGER.entering(GUI.class.getName(), "zipFile::doInBackground");
                try (
                        FileOutputStream fos = new FileOutputStream(new File(zipFilePath));
                        ZipOutputStream zos = new ZipOutputStream(fos);
                        FileReader fr = new FileReader(fileName);
                        BufferedReader br = new BufferedReader(fr)
                        )
                {
                    ZipEntry e = new ZipEntry(fileName);
                    zos.putNextEntry(e);

                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\r\n");
                    }
                    byte[] data = sb.toString().getBytes();
                    zos.write(data, 0, data.length);
                    zos.closeEntry();

                    zos.close();
                    LOGGER.log(Level.INFO, "Zip file created. Path to file: " + zipFilePath);

                    publish("");
                } catch (Exception ee) {
                    ee.printStackTrace();
                    LOGGER.log(Level.SEVERE, ee.getMessage());
                }

                LOGGER.exiting(GUI.class.getName(), "zipFile::doInBackground");
                return 1;
            }

            @Override
            protected void process(List<String> chunks) {
                LOGGER.entering(GUI.class.getName(), "zipFile::process");

                int progress = workerZipFile.getProgress();
                progressBar.setValue(progress);
                progressBar.setString("Zip Progress: " + progress + "%");

                LOGGER.exiting(GUI.class.getName(), "zipFile::process");
            }

            @Override
            protected void done() {
                LOGGER.entering(GUI.class.getName(), "zipFile::done");

                Path file = Paths.get(fileName);
                Path zipFile = Paths.get(fileName.replace("txt", "zip"));

                BasicFileAttributes fileAttr = null;
                BasicFileAttributes zipFileAttr = null;
                try {
                    fileAttr = Files.readAttributes(file, BasicFileAttributes.class);
                    zipFileAttr = Files.readAttributes(zipFile, BasicFileAttributes.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.log(Level.SEVERE, e.getMessage());
                }

                progressBar.setValue(100);
                progressBar.setString("Zip Progress: " + 100 + "%");

                String creationTime = "" + zipFileAttr.creationTime();

                creationTime = creationTime.replace("T", " ");
                creationTime = creationTime.substring(0, creationTime.indexOf("."));
                System.out.println("Zip file creation time: " + creationTime);
                System.out.println("Original file size: " + fileAttr.size());
                System.out.println("Zip file size: " + zipFileAttr.size());
                float percentageDecrease = ((float)(fileAttr.size()-zipFileAttr.size())/fileAttr.size())*100;
                System.out.println("Percentage decrease: " + percentageDecrease);

                StringBuilder sb = new StringBuilder();
                sb.append("Zip file creation time: " + creationTime + "\n\r");
                sb.append("Original file size: " + fileAttr.size() + " bytes\n\r");
                sb.append("Zip file size: " + zipFileAttr.size() + " bytes\n\r");
                sb.append("Percentage decrease: " + percentageDecrease + "%\n\r");

                JOptionPane.showMessageDialog(null, sb.toString(),
                        "Zip File Information", JOptionPane.INFORMATION_MESSAGE);

                LOGGER.exiting(GUI.class.getName(), "zipFile::done");
            }
        };

        workerZipFile.execute();
        LOGGER.exiting(GUI.class.getName(), "zipFile");
    }
    private void loadFile(File file) {
        LOGGER.entering(GUI.class.getName(), "loadFile with params: File="+file.getName());

        workerLoadFile = new SwingWorker<Integer, String>() {
            @Override
            protected Integer doInBackground() throws Exception {
                LOGGER.entering(GUI.class.getName(), "workerLoadFile::doInBackground");
                try (
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr)
                )
                {
                    String line;
                    StringBuilder sb = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\r\n");
                    }
                    publish(sb.toString());

                } catch (Exception ee) {
                    LOGGER.log(Level.SEVERE, ee.getMessage());
                    ee.printStackTrace();
                }

                LOGGER.exiting(GUI.class.getName(), "workerLoadFile::doInBackground");
                return 1;
            }

            @Override
            protected void process(List<String> chunks) {
                LOGGER.entering(GUI.class.getName(), "workerLoadFile::process");
                for(String lines : chunks) {
                    textArea.append(lines + "\r\n");
                }
                int progress = workerLoadFile.getProgress();
                System.out.println("progress " + progress);
                progressBar.setValue(progress);
                progressBar.setString("Load Progress: " + progress + "%");

                LOGGER.exiting(GUI.class.getName(), "workerLoadFile::process");
            }

            @Override
            protected void done() {
                LOGGER.entering(GUI.class.getName(), "workerLoadFile::done");

                zipButton.setEnabled(true);

                //Sometimes stops at 99%
                progressBar.setValue(100);
                progressBar.setString("Load Progress: " + 100 + "%");

                LOGGER.exiting(GUI.class.getName(), "workerLoadFile::done");
            }
        };
        workerLoadFile.execute();
        LOGGER.exiting(GUI.class.getName(), "loadFile");
    }
}
