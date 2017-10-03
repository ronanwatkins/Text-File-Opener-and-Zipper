import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger( GUI.class.getName() );

    public static void main(String[] args) {
        LOGGER.entering(Main.class.getName(), "main()");
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GUI gui = new GUI();
                gui.createAndShowGUI();
                LOGGER.log(Level.INFO, "Creating and showing GUI");
            }
        });
        LOGGER.exiting(Main.class.getName(), "main()");
    }
}
