package com.trezoragent.gui;

import com.trezoragent.sshagent.SSHAgent;
import com.trezoragent.sshagent.TrezorService;
import com.trezoragent.sshagent.TrezorWrapper;
import com.trezoragent.utils.AgentConstants;
import static com.trezoragent.utils.AgentConstants.*;
import com.trezoragent.utils.LocalizedLogger;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

/**
 *
 * @author Martin Lizner
 *
 * Class renders menu in System Tray
 *
 */
public class AgentPopUpMenu extends JPopupMenu {

    private final String ABOUT_BUTTON_LOCALIZED_KEY = "ABOUT";
    private final String SHOW_LOG_FILE_KEY = "SHOW_LOG_FILE";
    private final String EXIT_BUTTON_LOCALIZED_KEY = "EXIT";
    private final String VIEW_KEYS_BUTTON_LOCALIZED_KEY = "VIEW_KEYS";
    private final String APPLICATION_INFO_KEY = "APPLICATION_INFO";
    private final String DEVICE_NOT_PRESENT_KEY = "DEVICE_NOT_PRESENT";

    private final TrayIcon trayIcon;
    TrezorService trezorService;

    public AgentPopUpMenu(final SystemTray tray, final TrayIcon trayIcon, final SSHAgent agent, final TrezorService trezorService) {
        this.trayIcon = trayIcon;
        this.trezorService = trezorService;

        JMenuItem viewLog = new JMenuItem(LocalizedLogger.getLocalizedMessage(SHOW_LOG_FILE_KEY));
        JMenuItem aboutItem = new JMenuItem(LocalizedLogger.getLocalizedMessage(ABOUT_BUTTON_LOCALIZED_KEY));
        JMenuItem exitItem = new JMenuItem(LocalizedLogger.getLocalizedMessage(EXIT_BUTTON_LOCALIZED_KEY));
        JMenuItem viewKeys = new JMenuItem(LocalizedLogger.getLocalizedMessage(VIEW_KEYS_BUTTON_LOCALIZED_KEY));

        add(viewKeys);
        add(viewLog);
        addSeparator();
        add(aboutItem);
        add(exitItem);

        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, LocalizedLogger.getLocalizedMessage(APPLICATION_INFO_KEY, VERSION), LocalizedLogger.getLocalizedMessage(ABOUT_BUTTON_LOCALIZED_KEY), JOptionPane.INFORMATION_MESSAGE);
            }
        });
        viewKeys.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Request for operation: {0}", "GUI_GET_IDENTITIES");
                try {
                    if (trezorService.getHardwareWalletService().isDeviceReady()) {
                        TrezorWrapper.getIdentitiesRequest(trezorService);
                        final Timer timer = new Timer(AgentConstants.ASYNC_CHECK_INTERVAL, null);

                        ActionListener showWindowIfKeyProvided = new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent event) {
                                if (trezorService.getTrezorKey() != null) {
                                    List<String> s = new ArrayList<>();
                                    s.add(trezorService.getTrezorKey() + " " + KEY_COMMENT);

                                    PublicKeysFrame frame = new PublicKeysFrame(s, SSHURI.toString());
                                    frame.setVisible(true);

                                    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                                    frame.setLocation(dim.width / 2 - frame.getContentPane().getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);

                                    trezorService.setTrezorKey(null);
                                    trezorService.getAsyncData().setTrezorData(null); // clear cache data explicitly, since they were never read by standard call()
                                    timer.stop();
                                }
                            }
                        };

                        timer.addActionListener(showWindowIfKeyProvided);
                        timer.setRepeats(true);
                        timer.start();

                        Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, "Operation {0} executed successfully", "GUI_GET_IDENTITIES");
                    } else {
                        TrayProcess.createWarning(LocalizedLogger.getLocalizedMessage(DEVICE_NOT_PRESENT_KEY));
                    }
                } catch (Exception ex) {
                    TrayProcess.handleException(ex);
                }
            }

        });

        viewLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    File log = new File(System.getProperty("user.home") + File.separator + AgentConstants.LOG_FILE_NAME);
                    Desktop.getDesktop().open(log);
                } catch (Exception ex) {
                    TrayProcess.createError(LocalizedLogger.getLocalizedMessage("OPEN_LOG_FILE_ERROR", ex.getLocalizedMessage()), false);
                    Logger.getLogger(AgentPopUpMenu.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tray.remove(trayIcon);
                if (agent.isCreatedCorrectly() && agent.isMainLoopStarted()) {
                    agent.exitProcess();
                }
                System.exit(0);
            }
        });
    }

}
