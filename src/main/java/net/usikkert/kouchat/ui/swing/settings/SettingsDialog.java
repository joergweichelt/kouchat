
/***************************************************************************
 *   Copyright 2006-2018 by Christian Ihle                                 *
 *   contact@kouchat.net                                                   *
 *                                                                         *
 *   This file is part of KouChat.                                         *
 *                                                                         *
 *   KouChat is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Lesser General Public License as        *
 *   published by the Free Software Foundation, either version 3 of        *
 *   the License, or (at your option) any later version.                   *
 *                                                                         *
 *   KouChat is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU      *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with KouChat.                                           *
 *   If not, see <http://www.gnu.org/licenses/>.                           *
 ***************************************************************************/

package net.usikkert.kouchat.ui.swing.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.misc.ErrorHandler;
import net.usikkert.kouchat.net.NetworkInterfaceInfo;
import net.usikkert.kouchat.net.NetworkUtils;
import net.usikkert.kouchat.settings.Settings;
import net.usikkert.kouchat.ui.swing.CopyPastePopup;
import net.usikkert.kouchat.ui.swing.ImageLoader;
import net.usikkert.kouchat.ui.swing.LookAndFeelWrapper;
import net.usikkert.kouchat.ui.swing.Mediator;
import net.usikkert.kouchat.ui.swing.StatusIcons;
import net.usikkert.kouchat.ui.swing.UITools;
import net.usikkert.kouchat.ui.swing.messages.SwingMessages;
import net.usikkert.kouchat.util.Validate;

/**
 * This is the dialog window used to change settings.
 *
 * @author Christian Ihle
 */
@SuppressWarnings("serial")
public class SettingsDialog extends JDialog implements ActionListener {

    private static final Logger LOG = Logger.getLogger(SettingsDialog.class.getName());

    private final NetworkUtils networkUtils = new NetworkUtils();
    private final UITools uiTools = new UITools();

    private final JButton saveB, cancelB, chooseOwnColorB, chooseSysColorB, testBrowserB, chooseBrowserB;
    private final JTextField nickTF, browserTF;
    private final JLabel ownColorL, sysColorL;
    private final JCheckBox soundCB, loggingCB, smileysCB, balloonCB, systemTrayCB, minimizeToTrayCB;
    private final JComboBox<LookAndFeelWrapper> lookAndFeelCB;
    private final JComboBox<NetworkChoice> networkInterfaceCB; // Java 6 doesn't support generic JComboBox

    private final Settings settings;
    private final ErrorHandler errorHandler;
    private final SwingMessages swingMessages;

    private Mediator mediator;

    /**
     * Constructor. Creates the dialog.
     *
     * @param imageLoader The image loader.
     * @param settings The settings to use.
     * @param errorHandler The handler to use for showing error messages.
     * @param swingMessages The swing messages to use for the dialog.
     */
    public SettingsDialog(final ImageLoader imageLoader, final Settings settings, final ErrorHandler errorHandler,
                          final SwingMessages swingMessages) {
        Validate.notNull(imageLoader, "Image loader can not be null");
        Validate.notNull(settings, "Settings can not be null");
        Validate.notNull(errorHandler, "Error handler can not be null");
        Validate.notNull(swingMessages, "Swing messages can not be null");

        this.settings = settings;
        this.errorHandler = errorHandler;
        this.swingMessages = swingMessages;

        final JLabel nickL = new JLabel(swingMessages.getMessage("swing.settings.chooseNick.nickName.label"));
        nickTF = new JTextField(10);
        new CopyPastePopup(nickTF, swingMessages);

        final JPanel nickP = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nickP.add(nickL);
        nickP.add(nickTF);
        nickP.setBorder(BorderFactory.createTitledBorder(swingMessages.getMessage("swing.settings.chooseNick.border")));

        ownColorL = new JLabel(swingMessages.getMessage("swing.settings.chooseLook.ownTextColor.label"));
        ownColorL.setToolTipText(swingMessages.getMessage("swing.settings.chooseLook.ownTextColor.tooltip"));

        sysColorL = new JLabel(swingMessages.getMessage("swing.settings.chooseLook.systemTextColor.label"));
        sysColorL.setToolTipText(swingMessages.getMessage("swing.settings.chooseLook.systemTextColor.tooltip"));

        chooseOwnColorB = new JButton(swingMessages.getMessage("swing.button.change"));
        chooseOwnColorB.addActionListener(this);
        chooseSysColorB = new JButton(swingMessages.getMessage("swing.button.change"));
        chooseSysColorB.addActionListener(this);

        final JPanel ownColorP = new JPanel();
        ownColorP.setLayout(new BoxLayout(ownColorP, BoxLayout.LINE_AXIS));
        ownColorP.add(ownColorL);
        ownColorP.add(Box.createHorizontalGlue());
        ownColorP.add(chooseOwnColorB);

        final JPanel sysColorP = new JPanel();
        sysColorP.setLayout(new BoxLayout(sysColorP, BoxLayout.LINE_AXIS));
        sysColorP.add(sysColorL);
        sysColorP.add(Box.createHorizontalGlue());
        sysColorP.add(chooseSysColorB);

        final JLabel lookAndFeelL = new JLabel(swingMessages.getMessage("swing.settings.chooseLook.lookAndFeel.label"));
        lookAndFeelL.setToolTipText(swingMessages.getMessage("swing.settings.chooseLook.lookAndFeel.tooltip", Constants.APP_NAME));
        lookAndFeelCB = new JComboBox<>();

        final JPanel lookAndFeelP = new JPanel();
        lookAndFeelP.setLayout(new BoxLayout(lookAndFeelP, BoxLayout.LINE_AXIS));
        lookAndFeelP.add(lookAndFeelL);
        lookAndFeelP.add(Box.createHorizontalGlue());
        lookAndFeelP.add(lookAndFeelCB);

        final JPanel lookP = new JPanel(new GridLayout(3, 1, 1, 4));
        lookP.add(ownColorP);
        lookP.add(sysColorP);
        lookP.add(lookAndFeelP);
        lookP.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(swingMessages.getMessage("swing.settings.chooseLook.border")),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)));

        soundCB = new JCheckBox(swingMessages.getMessage("swing.settings.misc.enableSound.label"));
        soundCB.setToolTipText(swingMessages.getMessage("swing.settings.misc.enableSound.tooltip", Constants.APP_NAME));

        loggingCB = new JCheckBox(swingMessages.getMessage("swing.settings.misc.enableLogging.label"));
        loggingCB.setToolTipText(swingMessages.getMessage("swing.settings.misc.enableLogging.tooltip", settings.getLogLocation()));

        smileysCB = new JCheckBox(swingMessages.getMessage("swing.settings.misc.enableSmileys.label"));
        smileysCB.setToolTipText(swingMessages.getMessage("swing.settings.misc.enableSmileys.tooltip"));

        balloonCB = new JCheckBox(swingMessages.getMessage("swing.settings.misc.enableBalloons.label"));
        balloonCB.setToolTipText(swingMessages.getMessage("swing.settings.misc.enableBalloons.tooltip"));

        systemTrayCB = new JCheckBox(swingMessages.getMessage("swing.settings.misc.enableSystemTray.label"));
        systemTrayCB.setToolTipText(swingMessages.getMessage("swing.settings.misc.enableSystemTray.tooltip"));
        systemTrayCB.addActionListener(this);

        minimizeToTrayCB = new JCheckBox(swingMessages.getMessage("swing.settings.misc.minimizeToTray.label"));
        minimizeToTrayCB.setToolTipText(swingMessages.getMessage("swing.settings.misc.minimizeToTray.tooltip"));
        minimizeToTrayCB.addActionListener(this);

        final JPanel miscCheckBoxP = new JPanel(new GridLayout(3, 2));
        miscCheckBoxP.add(soundCB);
        miscCheckBoxP.add(systemTrayCB);
        miscCheckBoxP.add(smileysCB);
        miscCheckBoxP.add(balloonCB);
        miscCheckBoxP.add(loggingCB);
				miscCheckBoxP.add(minimizeToTrayCB);

        final JLabel networkInterfaceL = new JLabel(swingMessages.getMessage("swing.settings.misc.networkInterface.label"));
        networkInterfaceL.setToolTipText(swingMessages.getMessage("swing.settings.misc.networkInterface.tooltip", Constants.APP_NAME));
        networkInterfaceCB = new JComboBox<>();
        networkInterfaceCB.setRenderer(new NetworkChoiceCellRenderer());

        final JPanel networkInterfaceP = new JPanel();
        networkInterfaceP.setLayout(new BoxLayout(networkInterfaceP, BoxLayout.LINE_AXIS));
        networkInterfaceP.add(networkInterfaceL);
        networkInterfaceP.add(Box.createHorizontalGlue());
        networkInterfaceP.add(networkInterfaceCB);

        final JPanel miscP = new JPanel(new BorderLayout(0, 0));
        miscP.add(miscCheckBoxP, BorderLayout.NORTH);
        miscP.add(networkInterfaceP, BorderLayout.SOUTH);
        miscP.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(swingMessages.getMessage("swing.settings.misc.border")),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)));

        final JLabel browserL = new JLabel(swingMessages.getMessage("swing.settings.chooseBrowser.browser.label"));
        browserTF = new JTextField(22);
        browserTF.setToolTipText(swingMessages.getMessage("swing.settings.chooseBrowser.browser.tooltip"));
        new CopyPastePopup(browserTF, swingMessages);

        final JPanel browserTopP = new JPanel(new FlowLayout(FlowLayout.LEFT));
        browserTopP.add(browserL);
        browserTopP.add(browserTF);

        chooseBrowserB = new JButton(swingMessages.getMessage("swing.button.choose"));
        chooseBrowserB.addActionListener(this);
        testBrowserB = new JButton(swingMessages.getMessage("swing.button.test"));
        testBrowserB.addActionListener(this);

        final JPanel browserBottomP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        browserBottomP.add(chooseBrowserB);
        browserBottomP.add(testBrowserB);

        final JPanel browserP = new JPanel(new BorderLayout());
        browserP.add(browserTopP, BorderLayout.NORTH);
        browserP.add(browserBottomP, BorderLayout.SOUTH);
        browserP.setBorder(BorderFactory.createTitledBorder(swingMessages.getMessage("swing.settings.chooseBrowser.border")));

        final JPanel centerP = new JPanel(new BorderLayout());
        centerP.add(lookP, BorderLayout.CENTER);
        centerP.add(miscP, BorderLayout.SOUTH);
        centerP.add(browserP, BorderLayout.NORTH);

        saveB = new JButton(swingMessages.getMessage("swing.button.ok"));
        saveB.addActionListener(this);
        cancelB = new JButton(swingMessages.getMessage("swing.button.cancel"));
        cancelB.addActionListener(this);
        saveB.setPreferredSize(cancelB.getPreferredSize());

        final JPanel buttonP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonP.add(saveB);
        buttonP.add(cancelB);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(nickP, BorderLayout.NORTH);
        panel.add(centerP, BorderLayout.CENTER);
        panel.add(buttonP, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        getContentPane().add(panel);

        pack();
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setIconImage(new StatusIcons(imageLoader).getNormalIcon());
        setTitle(uiTools.createTitle(swingMessages.getMessage("swing.settings.title")));
        setResizable(false);
        setModal(true);
        hideWithEscape();

        // So the save button activates using Enter
        getRootPane().setDefaultButton(saveB);

        disableLogSettingIfAlwaysLogIsEnabled();
    }

    /**
     * Makes sure the log setting can't be changed when always log is enabled.
     */
    private void disableLogSettingIfAlwaysLogIsEnabled() {
        if (settings.isAlwaysLog()) {
            loggingCB.setEnabled(false);
        }
    }

    /**
     * Adds a shortcut to hide the window when escape is pressed.
     */
    private void hideWithEscape() {
        final KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);

        final Action escapeAction = new AbstractAction() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                setVisible(false);
            }
        };

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    /**
     * Sets the mediator for this window.
     *
     * @param mediator The mediator to use.
     */
    public void setMediator(final Mediator mediator) {
        Validate.notNull(mediator, "Mediator can not be null");
        this.mediator = mediator;
    }

    /**
     * Handles all the buttons in this window.
     *
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == saveB) {
            uiTools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (mediator.changeNick(nickTF.getText())) {
                        settings.setSysColor(sysColorL.getForeground().getRGB());
                        settings.setOwnColor(ownColorL.getForeground().getRGB());
                        settings.setSound(soundCB.isSelected());
                        settings.setLogging(loggingCB.isSelected());
                        settings.setBrowser(browserTF.getText());
                        settings.setSmileys(smileysCB.isSelected());
                        settings.setBalloons(balloonCB.isSelected());
                        settings.setSystemTray(systemTrayCB.isSelected());
                        final LookAndFeelWrapper lnfw = (LookAndFeelWrapper) lookAndFeelCB.getSelectedItem();
                        settings.setLookAndFeel(lnfw.getLookAndFeelInfo().getName());
                        settings.setNetworkInterface(getSelectedNetworkInterface().getDeviceName());
												settings.setMinimizeToTray(minimizeToTrayCB.isSelected());
                        mediator.saveSettings();
                        setVisible(false);
                        notifyLookAndFeelChange(lnfw);
                        mediator.checkNetwork();
                    }
                }

                private NetworkChoice getSelectedNetworkInterface() {
                    return (NetworkChoice) networkInterfaceCB.getSelectedItem();
                }
            });
        }

        else if (e.getSource() == cancelB) {
            uiTools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setVisible(false);
                }
            });
        }

        else if (e.getSource() == chooseOwnColorB) {
            uiTools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final Color newColor = uiTools.showColorChooser(
                            swingMessages.getMessage("swing.settings.chooseLook.ownTextColor.chooseColorDialog.title"),
                            new Color(settings.getOwnColor()));

                    if (newColor != null) {
                        ownColorL.setForeground(newColor);
                    }
                }
            });
        }

        else if (e.getSource() == chooseSysColorB) {
            uiTools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final Color newColor = uiTools.showColorChooser(
                            swingMessages.getMessage("swing.settings.chooseLook.systemTextColor.chooseColorDialog.title"),
                            new Color(settings.getSysColor()));

                    if (newColor != null) {
                        sysColorL.setForeground(newColor);
                    }
                }
            });
        }

        else if (e.getSource() == testBrowserB) {
            uiTools.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final String browser = browserTF.getText();

                    if (browser.trim().length() > 0) {
                        try {
                            uiTools.runCommand(browser + " " + Constants.APP_WEB);
                        }

                        catch (final IOException e) {
                            LOG.log(Level.WARNING, e.toString());
                            errorHandler.showError(
                                    swingMessages.getMessage("swing.settings.chooseBrowser.testButton.errorPopup.couldNotOpenChosen",
                                                             browser));
                        }
                    }

                    else if (uiTools.isDesktopActionSupported(Desktop.Action.BROWSE)) {
                        try {
                            uiTools.browse(Constants.APP_WEB);
                        }

                        catch (final IOException e) {
                            LOG.log(Level.WARNING, e.toString());
                            errorHandler.showError(
                                    swingMessages.getMessage("swing.settings.chooseBrowser.testButton.errorPopup.couldNotOpenDefault"));
                        }

                        catch (final URISyntaxException e) {
                            LOG.log(Level.WARNING, e.toString());
                            errorHandler.showError(
                                    swingMessages.getMessage("swing.settings.chooseBrowser.testButton.errorPopup.invalidUrl",
                                                             Constants.APP_WEB));
                        }
                    }

                    else {
                        errorHandler.showError(
                                swingMessages.getMessage("swing.settings.chooseBrowser.testButton.errorPopup.defaultBrowserUnsupported"));
                    }
                }
            });
        }

        else if (e.getSource() == chooseBrowserB) {
            final JFileChooser chooser = uiTools.createFileChooser(
                    swingMessages.getMessage("swing.settings.chooseBrowser.chooseBrowserDialog.title"));
            final int returnVal = chooser.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                final File file = chooser.getSelectedFile().getAbsoluteFile();
                browserTF.setText(file.getAbsolutePath());
            }
        }

        else if (e.getSource() == systemTrayCB) {
            setBalloonStateBasedOnSystemTrayState();
        }
    }

    /**
     * Notifies the user that the application needs to be restarted before
     * the new look and feel is used.
     *
     * @param lnfw Information about the chosen look and feel.
     */
    private void notifyLookAndFeelChange(final LookAndFeelWrapper lnfw) {
        final String newLookAndFeel = lnfw.getLookAndFeelInfo().getName();
        final LookAndFeelInfo currentLookAndFeel = uiTools.getCurrentLookAndFeel();

        if (!newLookAndFeel.equals(currentLookAndFeel.getName())) {
            uiTools.showInfoMessage(
                    swingMessages.getMessage("swing.settings.chooseLook.lookAndFeel.infoPopup.lookAndFeelChanged", Constants.APP_NAME),
                    swingMessages.getMessage("swing.settings.chooseLook.lookAndFeel.infoPopup.lookAndFeelChanged.title"));
        }
    }

    /**
     * Loads the current settings, and shows the window.
     */
    public void showSettings() {
        nickTF.setText(settings.getMe().getNick());
        sysColorL.setForeground(new Color(settings.getSysColor()));
        ownColorL.setForeground(new Color(settings.getOwnColor()));
        soundCB.setSelected(settings.isSound());
        loggingCB.setSelected(settings.isLogging());
				minimizeToTrayCB.setSelected(settings.getMinimizeToTray());
        browserTF.setText(settings.getBrowser());
        smileysCB.setSelected(settings.isSmileys());

        if (uiTools.isSystemTraySupported()) {
            balloonCB.setSelected(settings.isBalloons());
            systemTrayCB.setSelected(settings.isSystemTray());
            setBalloonStateBasedOnSystemTrayState();
        }

        else {
            balloonCB.setSelected(false);
            balloonCB.setEnabled(false);
            systemTrayCB.setSelected(false);
            systemTrayCB.setEnabled(false);
        }

        networkInterfaceCB.setModel(new DefaultComboBoxModel<>(getNetworkChoices()));
        lookAndFeelCB.setModel(new DefaultComboBoxModel<>(uiTools.getLookAndFeels()));

        selectLookAndFeel();
        selectNetworkInterface();

        setVisible(true);
        nickTF.requestFocusInWindow();
    }

    private void setBalloonStateBasedOnSystemTrayState() {
        if (systemTrayCB.isSelected()) {
            balloonCB.setEnabled(true);
        } else {
            balloonCB.setSelected(false);
            balloonCB.setEnabled(false);
        }
    }

    /**
     * Selects the correct look and feel in the combobox.
     *
     * <p>The correct item is either the saved look and feel,
     * or the current look and feel if none is saved yet.</p>
     */
    private void selectLookAndFeel() {
        final LookAndFeelInfo savedLookAndFeel = uiTools.getLookAndFeel(settings.getLookAndFeel());
        final String lnfClass;

        if (savedLookAndFeel == null) {
            lnfClass = uiTools.getCurrentLookAndFeel().getClassName();
        } else {
            lnfClass = savedLookAndFeel.getClassName();
        }

        for (int i = 0; i < lookAndFeelCB.getItemCount(); i++) {
            final LookAndFeelWrapper lookAndFeelWrapper = lookAndFeelCB.getItemAt(i);

            if (lookAndFeelWrapper.getLookAndFeelInfo().getClassName().equals(lnfClass)) {
                lookAndFeelCB.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Selects the saved network interface in the combobox.
     *
     * <p>If the saved network interface is not found in the list of the combobox then
     * the first element is shown, which is Auto.</p>
     */
    private void selectNetworkInterface() {
        final String savedNetworkInterface = settings.getNetworkInterface();

        for (int i = 0; i < networkInterfaceCB.getItemCount(); i++) {
            final NetworkChoice networkChoice = networkInterfaceCB.getItemAt(i);

            if (networkChoice.match(savedNetworkInterface)) {
                networkInterfaceCB.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Returns an array of network interfaces that can be chosen by the user in the combobox.
     *
     * @return Currently available and usable network interfaces to choose from.
     */
    private NetworkChoice[] getNetworkChoices() {
        final ArrayList<NetworkChoice> networkChoices = new ArrayList<>();
        networkChoices.add(new NetworkChoice(
                swingMessages.getMessage("swing.settings.misc.networkInterface.item.auto"),
                swingMessages.getMessage("swing.settings.misc.networkInterface.item.auto.tooltip", Constants.APP_NAME)));

        final List<NetworkInterfaceInfo> usableNetworkInterfaces = networkUtils.getUsableNetworkInterfaces();

        for (final NetworkInterfaceInfo usableNetworkInterface : usableNetworkInterfaces) {
            networkChoices.add(new NetworkChoice(usableNetworkInterface, networkUtils));
        }

        return networkChoices.toArray(new NetworkChoice[networkChoices.size()]);
    }
}
