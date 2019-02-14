
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

package net.usikkert.kouchat.ui.swing;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.event.SettingsListener;
import net.usikkert.kouchat.settings.Setting;
import net.usikkert.kouchat.settings.Settings;
import net.usikkert.kouchat.ui.swing.messages.SwingMessages;
import net.usikkert.kouchat.util.Validate;
import org.jetbrains.annotations.Nullable;

/**
 * This is the system tray.
 *
 * <p>Contains an icon that changes color when there is
 * activity from other users, and a right click menu to exit
 * the application.</p>
 *
 * @author Christian Ihle
 */
public class SysTray implements ActionListener, MouseListener, PropertyChangeListener, SettingsListener {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(SysTray.class.getName());

    /** Name of property to listen for tray icon changes. */
    private static final String TRAY_ICONS = "trayIcons";

    private static final String ACTION_NEW_PRIVATE_MESSAGE = "newPrivateMessage";

    private final UITools uiTools = new UITools();

    /** The settings. */
    private final Settings settings;

    private final ImageLoader imageLoader;

    private final SwingMessages swingMessages;

    /** The icon in the system tray. */
    @Nullable
    private TrayIcon trayIcon;

    /** The quit menu item. */
    @Nullable
    private MenuItem quitMI;

    /** The mediator. */
    private Mediator mediator;

    /** If the system tray is supported or not. */
    private boolean systemTraySupported;

    /** The icons to use in the system tray. */
    @Nullable
    private StatusIcons statusIcons;

    private ActionListener balloonActionListener = null;
    /**
     * Constructor.
     *
     * @param imageLoader The image loader for the system tray icons.
     * @param settings The settings to use.
     * @param swingMessages The swing messages to use in the system tray.
     */
    public SysTray(final ImageLoader imageLoader, final Settings settings, final SwingMessages swingMessages) {
        Validate.notNull(imageLoader, "Image loader can not be null");
        Validate.notNull(settings, "Settings can not be null");
        Validate.notNull(swingMessages, "Swing messages can not be null");

        this.imageLoader = imageLoader;
        this.settings = settings;
        this.swingMessages = swingMessages;

        settings.addSettingsListener(this);
    }

    /**
     * Activates the system tray icon, if supported, and enabled in the settings.
     *
     * <p>Not all window managers support a system tray, like TWM. And some window managers support one,
     * but does not have one enabled at all times. Like you can remove the system tray plasma widget in KDE.</p>
     *
     * <p>Use {@link #isSystemTraySupport()} to check if the system tray was activated successfully.</p>
     */
    public void activate() {
        if (!settings.isSystemTray()) {
            return;
        }

        if (uiTools.isSystemTraySupported()) {
            final PopupMenu menu = new PopupMenu();
            quitMI = new MenuItem(swingMessages.getMessage("swing.systemTray.menu.quit"));
            quitMI.addActionListener(this);
            menu.add(quitMI);

            final SystemTray sysTray = uiTools.getSystemTray();

            final StatusIconSize iconSize = chooseIconSize(sysTray);
            statusIcons = new StatusIcons(imageLoader, iconSize);

            trayIcon = createTrayIcon(statusIcons.getNormalIcon(), menu);

            if (iconSize == StatusIconSize.SIZE_32x32) {
                trayIcon.setImageAutoSize(true);
            }

            trayIcon.addMouseListener(this);
            trayIcon.addActionListener(new ActionListener(){
	      @Override
	      public void actionPerformed(ActionEvent e) {
                switch (e.getActionCommand()) {
                  default: {
                    LOG.log(Level.WARNING, "Unhandled action: {0}", e.getActionCommand());
                    break;
                  }
                  case ACTION_NEW_PRIVATE_MESSAGE: {
                    if (null != balloonActionListener) {
                      balloonActionListener.actionPerformed(e);
                      balloonActionListener = null;
                    }
                  }
                }
              }
            });

            trayIcon.setToolTip(Constants.APP_NAME);

            try {
                sysTray.add(trayIcon);
                sysTray.addPropertyChangeListener(TRAY_ICONS, this);
                systemTraySupported = true;
            }

            catch (final AWTException e) {
                // This may happen if the System Tray is hidden on a system
                // that actually supports a System Tray.
                LOG.log(Level.SEVERE, e.toString());
            }
        }

        else {
            LOG.log(Level.SEVERE, "System Tray is not supported. Deactivating System Tray support.");
        }
    }

    /**
     * Removes the system tray icon and cleans up the listeners.
     */
    public void deactivate() {
        if (systemTraySupported) {
            systemTraySupported = false;

            quitMI.removeActionListener(this);
            trayIcon.removeMouseListener(this);

            final SystemTray sysTray = uiTools.getSystemTray();
            sysTray.removePropertyChangeListener(TRAY_ICONS, this);
            sysTray.remove(trayIcon);

            quitMI = null;
            statusIcons = null;
            trayIcon = null;
        }
    }

    /**
     * Returns if the system tray is supported and activated.
     *
     * @return If the system tray is supported
     */
    public boolean isSystemTraySupport() {
        return systemTraySupported;
    }

    /**
     * Sets the mediator.
     *
     * @param mediator The mediator.
     */
    public void setMediator(final Mediator mediator) {
        this.mediator = mediator;
    }

    /**
     * Sets the tray icon to away.
     */
    public void setAwayState() {
        if (trayIcon != null) {
            setTrayIcon(statusIcons.getAwayIcon());
        }
    }

    /**
     * Sets the tray icon to away with activity.
     */
    public void setAwayActivityState() {
        if (trayIcon != null) {
            setTrayIcon(statusIcons.getAwayActivityIcon());
        }
    }

    /**
     * Sets the tray icon to normal.
     */
    public void setNormalState() {
        if (trayIcon != null) {
            setTrayIcon(statusIcons.getNormalIcon());
        }
    }

    /**
     * Sets the tray icon to normal with activity.
     */
    public void setNormalActivityState() {
        if (trayIcon != null) {
            setTrayIcon(statusIcons.getNormalActivityIcon());
        }
    }

    /**
     * Sets the tooltip on the system tray icon.
     *
     * @param toolTip The tooltip to set.
     */
    public void setToolTip(final String toolTip) {
        if (trayIcon != null) {
            trayIcon.setToolTip(toolTip);
        }
    }

    /**
     * Handles clicks on the quit menu item.
     *
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == quitMI) {
          mediator.quit();
        }
    }

    /**
     * Handles left mouse click events.
     *
     * <p>Makes sure the main chat window is shown or hidden,
     * and updates the tray icon.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
        if (e.getSource() == trayIcon && e.getButton() == MouseEvent.BUTTON1) {
            if (trayIcon.getImage() == statusIcons.getNormalActivityIcon()) {
                trayIcon.setImage(statusIcons.getNormalIcon());
            } else if (trayIcon.getImage() == statusIcons.getAwayActivityIcon()) {
                trayIcon.setImage(statusIcons.getAwayIcon());
            }

            mediator.showOrHideWindow();
        }
    }

    /**
     * Not implemented.
     *
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(final MouseEvent e) {

    }

    /**
     * Not implemented.
     *
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(final MouseEvent e) {

    }

    /**
     * Not implemented.
     *
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(final MouseEvent e) {

    }

    /**
     * Not implemented.
     *
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent e) {

    }

    /**
     * Sets the system tray icon if it's different from the icon already in use.
     *
     * @param icon The tray icon to use.
     */
    public void setTrayIcon(final Image icon) {
        if (trayIcon.getImage() != icon) {
            trayIcon.setImage(icon);
        }
    }

    /**
     * Listens for changes to the available system tray icons.
     * After the initial icon has been added there will never be any
     * more changes made by the application. So if this event happens,
     * it means that an icon was removed by the jvm, and that is
     * usually caused by the system tray being removed.
     *
     * <p>To handle this, the system tray support is deactivated and
     * the main window is unhidden, so it's possible for the user
     * to continue without having to kill the application.</p>
     *
     * @param e A trayicon change event.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
        final TrayIcon[] icons = (TrayIcon[]) e.getNewValue();

        if (icons.length == 0) {
            LOG.log(Level.SEVERE, "System Tray removed. Deactivating System Tray support.");
            deactivate();
            mediator.minimizeWindowIfHidden();
        }
    }

    /**
     * Shows a balloon popup message by the system tray icon. The message
     * will disappear by itself after a few seconds, or if the user clicks
     * on it.
     *
     * <p>Can be enabled and disabled from the settings.</p>
     *
     * @param title The title of the message.
     * @param message The message to show in the popup.
     */
    public void showBalloonMessage(final String title, final String message) {
        if (settings.isBalloons() && trayIcon != null) {
            trayIcon.displayMessage(title, message, MessageType.NONE);
            trayIcon.setActionCommand(message);
        }
    }

    /**
     * Shows a balloon popup message by the system tray icon when a new private messages arrives.
     * The message will disappear by itself after a few seconds, or if the user clicks
     * on it.
     *
     * <p>Can be enabled and disabled from the settings.</p>
     *
     * @param title The title of the message.
     * @param message The message to show in the popup.
     * @param listener Action listener to handle balloon actions. May be null
     */
    public void showNewPrivateMessageNotification(String title, String message, ActionListener listener) {
      if (settings.isBalloons() && trayIcon != null) {
        trayIcon.displayMessage(title, message, MessageType.INFO);
	trayIcon.setActionCommand(ACTION_NEW_PRIVATE_MESSAGE);
        balloonActionListener = listener;
      }
    }

    /**
     * Choose a size to use for the icons in the system tray based on the size the system tray requests.
     *
     * <ul>
     *   <li>Windows: 16x16px</li>
     *   <li>Mac OS X: 20x20px</li>
     *   <li>Gnome: 24x24px</li>
     *   <li>KDE: 24x24</li>
     *   <li>Others: 32x32, with scaling applied.</li>
     * </ul>
     *
     * @param sysTray The system tray.
     * @return The best icon size.
     */
    private StatusIconSize chooseIconSize(final SystemTray sysTray) {
        final Dimension trayIconSize = sysTray.getTrayIconSize();

        if (trayIconSize.getHeight() == 16) {
            return StatusIconSize.SIZE_16x16;
        } else if (trayIconSize.getHeight() == 20) {
            return StatusIconSize.SIZE_20x20;
        } else if (trayIconSize.getHeight() == 24) {
            return StatusIconSize.SIZE_24x24;
        } else {
            return StatusIconSize.SIZE_32x32;
        }
    }

    TrayIcon createTrayIcon(final Image icon, final PopupMenu menu) {
        return new TrayIcon(icon, "", menu);
    }

    /**
     * Listens for changes to the system tray setting, and activates/deactivates the system tray icon
     * based on the new value.
     *
     * @param setting The setting which was changed.
     */
    @Override
    public void settingChanged(final Setting setting) {
        if (setting.equals(Setting.SYSTEM_TRAY)) {
            if (settings.isSystemTray()) {
                activate();
            } else {
                deactivate();
            }
        }
    }
}
