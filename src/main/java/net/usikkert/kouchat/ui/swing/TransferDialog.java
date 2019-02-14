
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import net.usikkert.kouchat.event.FileTransferListener;
import net.usikkert.kouchat.misc.ErrorHandler;
import net.usikkert.kouchat.misc.User;
import net.usikkert.kouchat.net.FileReceiver;
import net.usikkert.kouchat.net.FileTransfer;
import net.usikkert.kouchat.settings.Settings;
import net.usikkert.kouchat.ui.swing.messages.SwingMessages;
import net.usikkert.kouchat.util.Tools;
import net.usikkert.kouchat.util.Validate;

/**
 * This is the dialog window for file transfers in the swing user interface.
 *
 * @author Christian Ihle
 */
@SuppressWarnings("serial")
public class TransferDialog extends JDialog implements FileTransferListener, ActionListener {

    private static final String ZERO_KB = "0KB";

    private final UITools uiTools = new UITools();

    /** Button to cancel file transfer, or close the dialog when transfer is stopped. */
    private final JButton cancelB;

    /** Button to open the folder where the received file was saved. */
    private final JButton openB;

    /** Label for the file transfer status. */
    private final JLabel statusL;

    /** Label for the sender of the file. */
    private final JLabel sourceL;

    /** Label for the receiver of the file. */
    private final JLabel destinationL;

    /** Label for the file name. */
    private final JLabel filenameL;

    /** Label for transfer information. */
    private final JLabel transferredL;

    /** Progress bar to show file transfer progress in percent complete. */
    private final JProgressBar transferProgressPB;

    /** The file transfer object this dialog is showing the state of. */
    private final FileTransfer fileTransfer;

    private final Mediator mediator;
    private final Settings settings;
    private final SwingMessages swingMessages;
    private final ErrorHandler errorHandler;

    /** If the dialog is in a state where it will be closed when clicking the cancel button (with the text "Close"). */
    private boolean closeable;

    /**
     * Constructor. Initializes components and registers this dialog
     * as a listener on the file transfer object.
     *
     * <p>Use {@link #open()} to open the dialog.</p>
     *
     * @param mediator The mediator.
     * @param fileTransfer The file transfer object this dialog is showing the state of.
     * @param imageLoader The image loader.
     * @param settings The settings to use.
     * @param swingMessages The swing messages to use in the dialog.
     * @param errorHandler The error handler to use.
     */
    public TransferDialog(final Mediator mediator, final FileTransfer fileTransfer, final ImageLoader imageLoader,
                          final Settings settings, final SwingMessages swingMessages, final ErrorHandler errorHandler) {
        Validate.notNull(mediator, "Mediator can not be null");
        Validate.notNull(fileTransfer, "File transfer can not be null");
        Validate.notNull(imageLoader, "Image loader can not be null");
        Validate.notNull(settings, "Settings can not be null");
        Validate.notNull(swingMessages, "Swing messages can not be null");
        Validate.notNull(errorHandler, "Error handler can not be null");

        this.mediator = mediator;
        this.fileTransfer = fileTransfer;
        this.settings = settings;
        this.swingMessages = swingMessages;
        this.errorHandler = errorHandler;

        cancelB = new JButton(swingMessages.getMessage("swing.button.cancel"));
        cancelB.addActionListener(this);

        openB = new JButton(swingMessages.getMessage("swing.transferDialog.button.openFolder"));
        openB.addActionListener(this);
        openB.setVisible(false);
        openB.setEnabled(false);

        transferProgressPB = new JProgressBar(0, 100);
        transferProgressPB.setStringPainted(true);
        transferProgressPB.setPreferredSize(new Dimension(410, 25));

        final JLabel transferredHeaderL = new JLabel(swingMessages.getMessage("swing.transferDialog.transferred.header"));
        final int headerHeight = transferredHeaderL.getPreferredSize().height;
        final int headerWidth = transferredHeaderL.getPreferredSize().width + 8;
        transferredHeaderL.setPreferredSize(new Dimension(headerWidth, headerHeight));
        transferredL = new JLabel(createTransferStatusText(ZERO_KB, ZERO_KB, ZERO_KB));

        final JLabel filenameHeaderL = new JLabel(swingMessages.getMessage("swing.transferDialog.filename.header"));
        filenameHeaderL.setPreferredSize(new Dimension(headerWidth, headerHeight));
        filenameL = new JLabel(swingMessages.getMessage("swing.transferDialog.filename.defaultValue"));
        filenameL.setPreferredSize(new Dimension(410 - headerWidth, headerHeight));

        final JLabel statusHeaderL = new JLabel(swingMessages.getMessage("swing.transferDialog.status.header"));
        statusHeaderL.setPreferredSize(new Dimension(headerWidth, headerHeight));
        statusL = new JLabel(swingMessages.getMessage("swing.transferDialog.status.waiting"));

        final JLabel sourceHeaderL = new JLabel(swingMessages.getMessage("swing.transferDialog.source.header"));
        sourceHeaderL.setPreferredSize(new Dimension(headerWidth, headerHeight));
        sourceL = new JLabel(swingMessages.getMessage("swing.transferDialog.source.defaultValue"));

        final JLabel destinationHeaderL = new JLabel(swingMessages.getMessage("swing.transferDialog.destination.header"));
        destinationHeaderL.setPreferredSize(new Dimension(headerWidth, headerHeight));
        destinationL = new JLabel(swingMessages.getMessage("swing.transferDialog.destination.defaultValue"));

        final JPanel topP = new JPanel();
        topP.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        topP.setLayout(new BoxLayout(topP, BoxLayout.PAGE_AXIS));

        final JPanel bottomP = new JPanel();
        bottomP.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        bottomP.setLayout(new BoxLayout(bottomP, BoxLayout.LINE_AXIS));

        final JPanel statusP = new JPanel();
        statusP.setLayout(new BoxLayout(statusP, BoxLayout.LINE_AXIS));
        statusP.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        statusP.add(statusHeaderL);
        statusP.add(statusL);
        statusP.add(Box.createHorizontalGlue());

        final JPanel sourceP = new JPanel();
        sourceP.setLayout(new BoxLayout(sourceP, BoxLayout.LINE_AXIS));
        sourceP.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        sourceP.add(sourceHeaderL);
        sourceP.add(sourceL);
        sourceP.add(Box.createHorizontalGlue());

        final JPanel destP = new JPanel();
        destP.setLayout(new BoxLayout(destP, BoxLayout.LINE_AXIS));
        destP.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        destP.add(destinationHeaderL);
        destP.add(destinationL);
        destP.add(Box.createHorizontalGlue());

        final JPanel fileP = new JPanel();
        fileP.setLayout(new BoxLayout(fileP, BoxLayout.LINE_AXIS));
        fileP.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        fileP.add(filenameHeaderL);
        fileP.add(filenameL);
        fileP.add(Box.createHorizontalGlue());

        final JPanel progressP = new JPanel(new BorderLayout());
        progressP.add(transferProgressPB, BorderLayout.CENTER);

        final JPanel transP = new JPanel();
        transP.setLayout(new BoxLayout(transP, BoxLayout.LINE_AXIS));
        transP.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        transP.add(transferredHeaderL);
        transP.add(transferredL);
        transP.add(Box.createHorizontalGlue());

        topP.add(statusP);
        topP.add(sourceP);
        topP.add(destP);
        topP.add(fileP);
        topP.add(progressP);
        topP.add(transP);

        bottomP.add(Box.createHorizontalGlue());
        bottomP.add(openB);
        bottomP.add(Box.createRigidArea(new Dimension(8, 0)));
        bottomP.add(cancelB);

        getContentPane().add(topP, BorderLayout.NORTH);
        getContentPane().add(bottomP, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        updateTitle(0);
        setIconImage(new StatusIcons(imageLoader).getNormalIcon());
        getRootPane().setDefaultButton(cancelB);

        pack();
        setResizable(false);

        fileTransfer.registerListener(this);
    }

    /**
     * Opens this dialog.
     */
    public void open() {
        setVisible(true);
    }

    /**
     * Prepares the dialog so that it can be closed by clicking the "Close" button.
     *
     * <p>The text on the cancel button changes to "Close".</p>
     */
    public void registerAsCloseable() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                setAsCloseable();
            }
        });
    }

    /**
     * Checks if the dialog will be closed when clicking on the button.
     *
     * <p>If not, the button cancels the file transfer.</p>
     *
     * @return If the dialog closes when clicking the button.
     */
    public boolean isCloseable() {
        return closeable;
    }

    /**
     * Gets the file transfer object this dialog is listening to.
     *
     * @return The file transfer object.
     */
    public FileTransfer getFileTransfer() {
        return fileTransfer;
    }

    /**
     * Listener for the buttons.
     *
     * <p>The buttons:</p>
     * <ul>
     *   <li>Cancel/Close: cancels the file transfer, or closes the dialog
     *       window if it's done transferring.</li>
     *   <li>Open: opens the folder where the file was saved.</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        if (event.getSource() == cancelB) {
            mediator.transferCancelled(this);
        } else if (event.getSource() == openB) {
            final FileReceiver fileReceiver = (FileReceiver) fileTransfer;
            final File folder = fileReceiver.getFile().getParentFile();
            uiTools.open(folder, settings, errorHandler, swingMessages);
        }
    }

    /**
     * This method is called from the file transfer object when
     * the file transfer was completed successfully.
     */
    @Override
    public void statusCompleted() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusL.setForeground(new Color(0, 176, 0));

                if (fileTransfer.getDirection() == FileTransfer.Direction.RECEIVE) {
                    statusL.setText(swingMessages.getMessage("swing.transferDialog.status.completed.receive"));
                    openB.setEnabled(true);
                }

                else if (fileTransfer.getDirection() == FileTransfer.Direction.SEND) {
                    statusL.setText(swingMessages.getMessage("swing.transferDialog.status.completed.send"));
                }

                setAsCloseable();
            }
        });
    }

    /**
     * This method is called from the file transfer object when
     * it is ready to connect.
     */
    @Override
    public void statusConnecting() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusL.setText(swingMessages.getMessage("swing.transferDialog.status.connecting"));
            }
        });
    }

    /**
     * This method is called from the file transfer object when
     * a file transfer was canceled or failed somehow.
     */
    @Override
    public void statusFailed() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusL.setForeground(Color.RED);

                if (fileTransfer.getDirection() == FileTransfer.Direction.RECEIVE) {
                    statusL.setText(swingMessages.getMessage("swing.transferDialog.status.failed.receive"));
                } else if (fileTransfer.getDirection() == FileTransfer.Direction.SEND) {
                    statusL.setText(swingMessages.getMessage("swing.transferDialog.status.failed.send"));
                }

                setAsCloseable();
            }
        });
    }

    /**
     * This method is called from the file transfer object when
     * the connection was successful and the transfer is in progress.
     */
    @Override
    public void statusTransferring() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (fileTransfer.getDirection() == FileTransfer.Direction.RECEIVE) {
                    statusL.setText(swingMessages.getMessage("swing.transferDialog.status.transferring.receive"));
                } else if (fileTransfer.getDirection() == FileTransfer.Direction.SEND) {
                    statusL.setText(swingMessages.getMessage("swing.transferDialog.status.transferring.send"));
                }
            }
        });
    }

    /**
     * This method is called from the file transfer object when
     * this dialog registers as a listener. Nothing is happening
     * with the file transfer, but the necessary information to
     * initialize the dialog fields are ready.
     */
    @Override
    public void statusWaiting() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                final User me = settings.getMe();
                final User other = fileTransfer.getUser();

                statusL.setText(swingMessages.getMessage("swing.transferDialog.status.waiting"));

                if (fileTransfer.getDirection() == FileTransfer.Direction.RECEIVE) {
                    sourceL.setText(other.getNick() + " (" + other.getIpAddress() + ")");
                    destinationL.setText(me.getNick() + " (" + me.getIpAddress() + ")");
                    openB.setVisible(true);
                }

                else if (fileTransfer.getDirection() == FileTransfer.Direction.SEND) {
                    destinationL.setText(other.getNick() + " (" + other.getIpAddress() + ")");
                    sourceL.setText(me.getNick() + " (" + me.getIpAddress() + ")");
                }

                final String fileName = fileTransfer.getFileName();
                filenameL.setText(fileName);
                final double width = uiTools.getTextWidth(fileName, getGraphics(), filenameL.getFont());

                if (width > filenameL.getSize().width) {
                    filenameL.setToolTipText(fileName);
                } else {
                    filenameL.setToolTipText(null);
                }

                transferredL.setText(createTransferStatusText(ZERO_KB, Tools.byteToString(fileTransfer.getFileSize()), ZERO_KB));
                transferProgressPB.setValue(0);
            }
        });
    }

    /**
     * This method is called from the file transfer object when
     * it's time to update the status of the file transfer.
     * This happens several times while the file transfer is
     * in progress.
     */
    @Override
    public void transferUpdate() {
        uiTools.invokeLater(new Runnable() {
            @Override
            public void run() {
                transferredL.setText(createTransferStatusText(
                        Tools.byteToString(fileTransfer.getTransferred()),
                        Tools.byteToString(fileTransfer.getFileSize()),
                        Tools.byteToString(fileTransfer.getSpeed())));
                transferProgressPB.setValue(fileTransfer.getPercent());
                updateTitle(fileTransfer.getPercent());
            }
        });
    }

    /**
     * Updates the window title with percentage transferred.
     *
     * @param percent The percentage of the file transferred.
     */
    private void updateTitle(final int percent) {
        setTitle(uiTools.createTitle(swingMessages.getMessage("swing.transferDialog.title", percent)));
    }

    private void setAsCloseable() {
        closeable = true;
        cancelB.setText(swingMessages.getMessage("swing.button.close"));
    }

    private String createTransferStatusText(final String transferred, final String fileSize, final String speed) {
        return swingMessages.getMessage("swing.transferDialog.transferred.value", transferred, fileSize, speed);
    }
}
