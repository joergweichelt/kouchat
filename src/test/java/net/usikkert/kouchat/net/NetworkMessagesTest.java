
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

package net.usikkert.kouchat.net;

import static org.mockito.Mockito.*;

import net.usikkert.kouchat.misc.CommandException;
import net.usikkert.kouchat.misc.Topic;
import net.usikkert.kouchat.misc.User;
import net.usikkert.kouchat.settings.Settings;

import org.junit.Test;

/**
 * Test of {@link NetworkMessages}.
 *
 * @author Christian Ihle
 */
public class NetworkMessagesTest {

    /** The settings. */
    private final Settings settings;

    /** The application user. */
    private final User me;

    /** The message class tested here. */
    private final NetworkMessages messages;

    /** Mocked network service used by messages. */
    private final NetworkService service;

    /**
     * Constructor.
     */
    public NetworkMessagesTest() {
        settings = mock(Settings.class);
        me = new User("TestUser", 123);
        me.setPrivateChatPort(2222);
        me.setTcpChatPort(4444);

        when(settings.getMe()).thenReturn(me);

        service = mock(NetworkService.class);
        when(service.sendMessageToAllUsers(anyString())).thenReturn(true);
        when(service.sendMessageToUser(anyString(), any(User.class))).thenReturn(true);
        messages = new NetworkMessages(service, settings);
    }

    /**
     * Tests sendAwayMessage().
     *
     */
    @Test
    public void testSendAwayMessage() {
        // Expects: 11515687!AWAY#Christian:I am away
        final String awayMsg = "I am away";
        messages.sendAwayMessage(awayMsg);
        verify(service).sendMessageToAllUsers(createMessage("AWAY") + awayMsg);
    }

    /**
     * Tests sendBackMessage().
     *
     */
    @Test
    public void testSendBackMessage() {
        // Expects: 12485102!BACK#Christian:
        messages.sendBackMessage();
        verify(service).sendMessageToAllUsers(createMessage("BACK"));
    }

    /**
     * Tests sendChatMessage().
     *
     * @throws CommandException In case the message could not be sent.
     */
    @Test
    public void testSendChatMessage() throws CommandException {
        // Expects: 16899115!MSG#Christian:[-15987646]Some chat message
        final String msg = "Some chat message";
        messages.sendChatMessage(msg);
        verify(service).sendMessageToAllUsers(createMessage("MSG") + "[" + settings.getOwnColor() + "]" + msg);
    }

    /**
     * Tests sendClient().
     *
     */
    @Test
    public void testSendClientMessage() {
        // Expects: 13132531!CLIENT#Christian:(KouChat v0.9.9-dev null)[134]{Linux}<2222>/4444\
        final String startsWith = "(" + me.getClient() + ")[";
        final String middle = ".+\\)\\[\\d+\\]\\{.+"; // like:)[134[{
        final String endsWidth = "]{" + me.getOperatingSystem() + "}<2222>/4444\\";

        messages.sendClient();

        verify(service).sendMessageToAllUsers(startsWith(createMessage("CLIENT") + startsWith));
        verify(service).sendMessageToAllUsers(matches(middle));
        verify(service).sendMessageToAllUsers(endsWith(endsWidth));
    }

    /**
     * Tests sendExposeMessage().
     *
     */
    @Test
    public void testSendExposeMessage() {
        // Expects: 16424378!EXPOSE#Christian:
        messages.sendExposeMessage();
        verify(service).sendMessageToAllUsers(createMessage("EXPOSE"));
    }

    /**
     * Tests sendExposingMessage().
     *
     */
    @Test
    public void testSendExposingMessage() {
        // Expects: 17871777!EXPOSING#Christian:
        messages.sendExposingMessage();
        verify(service).sendMessageToAllUsers(createMessage("EXPOSING"));
    }

    /**
     * Tests sendFile().
     *
     * @throws CommandException In case the message could not be sent.
     */
    @Test
    public void testSendFileMessage() throws CommandException {
        // Expects: 14394329!SENDFILE#Christian:(1234)[80800]{37563645}a_file.txt
        final int userCode = 1234;
        final long fileLength = 80800L;
        final String fileName = "a_file.txt";

        final FileToSend file = mock(FileToSend.class);
        when(file.getName()).thenReturn(fileName);
        when(file.length()).thenReturn(fileLength);
        final int fileHash = file.hashCode(); // Cannot be mocked it seems

        final String info = "(" + userCode + ")" +
                "[" + fileLength + "]" +
                "{" + fileHash + "}" +
                fileName;

        final User user = new User("TestUser", userCode);

        messages.sendFile(user, file);
        verify(service).sendMessageToAllUsers(createMessage("SENDFILE") + info);
    }

    /**
     * Tests sendFileAbort().
     *
     */
    @Test
    public void testSendFileAbortMessage() {
        // Expects: 15234876!SENDFILEABORT#Christian:(4321){8578765}another_file.txt
        final int userCode = 4321;
        final int fileHash = 8578765;
        final String fileName = "another_file.txt";

        final String info = "(" + userCode + ")" +
                "{" + fileHash + "}" +
                fileName;

        final User user = new User("TestUser", userCode);

        messages.sendFileAbort(user, fileHash, fileName);
        verify(service).sendMessageToAllUsers(createMessage("SENDFILEABORT") + info);
    }

    /**
     * Tests sendFileAccept().
     *
     * @throws CommandException In case the message could not be sent.
     */
    @Test
    public void testSendFileAcceptMessage() throws CommandException {
        // Expects: 17247198!SENDFILEACCEPT#Christian:(4321)[20103]{8578765}some_file.txt
        final int userCode = 4321;
        final int port = 20103;
        final int fileHash = 8578765;
        final String fileName = "some_file.txt";

        final String info = "(" + userCode + ")" +
                "[" + port + "]" +
                "{" + fileHash + "}" +
                fileName;

        final User user = new User("TestUser", userCode);

        messages.sendFileAccept(user, port, fileHash, fileName);
        verify(service).sendMessageToAllUsers(createMessage("SENDFILEACCEPT") + info);
    }

    /**
     * Tests sendGetTopicMessage().
     *
     */
    @Test
    public void testSendGetTopicMessage() {
        // Expects: 19909338!GETTOPIC#Christian:
        messages.sendGetTopicMessage();
        verify(service).sendMessageToAllUsers(createMessage("GETTOPIC"));
    }

    /**
     * Tests sendIdleMessage().
     *
     */
    @Test
    public void testSendIdleMessage() {
        // Expects: 10223997!IDLE#Christian:
        messages.sendIdleMessage();
        verify(service).sendMessageToAllUsers(createMessage("IDLE"));
    }

    /**
     * Tests sendLogoffMessage().
     *
     */
    @Test
    public void testSendLogoffMessage() {
        // Expects: 18265486!LOGOFF#Christian:
        messages.sendLogoffMessage();
        verify(service).sendMessageToAllUsers(createMessage("LOGOFF"));
    }

    /**
     * Tests sendLogonMessage().
     *
     */
    @Test
    public void testSendLogonMessage() {
        // Expects: 10794786!LOGON#Christian:
        messages.sendLogonMessage();
        verify(service).sendMessageToAllUsers(createMessage("LOGON"));
    }

    /**
     * Tests sendNickCrashMessage().
     *
     */
    @Test
    public void testSendNickCrashMessage() {
        // Expects: 16321536!NICKCRASH#Christian:niles
        final String nick = "niles";
        messages.sendNickCrashMessage(nick);
        verify(service).sendMessageToAllUsers(createMessage("NICKCRASH") + nick);
    }

    /**
     * Tests sendNickMessage().
     *
     */
    @Test
    public void testSendNickMessage() {
        // Expects: 14795611!NICK#Christian:
        final String newNick = "Cookie";
        messages.sendNickMessage(newNick);
        verify(service).sendMessageToAllUsers(createMessage("NICK", newNick));
    }

    /**
     * Tests sendPrivateMessage().
     *
     *
     * @throws CommandException In case the message could not be sent.
     */
    @Test
    public void testSendPrivateMessage() throws CommandException {
        // Expects: 10897608!PRIVMSG#Christian:(435435)[-15987646]this is a private message
        final String privmsg = "this is a private message";
        final String userIP = "192.168.5.155";
        final int userPort = 12345;
        final int userCode = 435435;

        final String message = "(" + userCode + ")" +
                "[" + settings.getOwnColor() + "]" +
                privmsg;

        final User user = new User("TestUser", userCode);
        user.setPrivateChatPort(userPort);
        user.setIpAddress(userIP);

        messages.sendPrivateMessage(privmsg, user);
        verify(service).sendMessageToUser(createMessage("PRIVMSG") + message, user);
    }

    /**
     * Tests sendStoppedWritingMessage().
     *
     */
    @Test
    public void testSendStoppedWritingMessage() {
        // Expects: 15140738!STOPPEDWRITING#Christian:
        messages.sendStoppedWritingMessage();
        verify(service).sendMessageToAllUsers(createMessage("STOPPEDWRITING"));
    }

    /**
     * Tests sendTopicChangeMessage().
     *
     */
    @Test
    public void testSendTopicChangeMessage() {
        // Expects: 18102542!TOPIC#Christian:(Snoopy)[2132321323]Interesting changed topic
        final Topic topic = new Topic("Interesting changed topic", "Snoopy", 2132321323L);
        final String message = "(" + topic.getNick() + ")" +
                "[" + topic.getTime() + "]" +
                topic.getTopic();

        messages.sendTopicChangeMessage(topic);
        verify(service).sendMessageToAllUsers(createMessage("TOPIC") + message);
    }

    /**
     * Tests sendTopicRequestedMessage().
     *
     */
    @Test
    public void testSendTopicRequestedMessage() {
        // Expects: 18102542!TOPIC#Christian:(Snoopy)[66532345]Interesting requested topic
        final Topic topic = new Topic("Interesting requested topic", "Snoopy", 66532345L);
        final String message = "(" + topic.getNick() + ")" +
                "[" + topic.getTime() + "]" +
                topic.getTopic();

        messages.sendTopicRequestedMessage(topic);
        verify(service).sendMessageToAllUsers(createMessage("TOPIC") + message);
    }

    /**
     * Tests sendWritingMessage().
     *
     */
    @Test
    public void testSendWritingMessage() {
        // Expects: 19610068!WRITING#Christian:
        messages.sendWritingMessage();
        verify(service).sendMessageToAllUsers(createMessage("WRITING"));
    }

    /**
     * Creates the standard part for most of the message types.
     *
     * @param type The message type.
     * @return A message.
     */
    private String createMessage(final String type) {
        return me.getCode() + "!" + type + "#" + me.getNick() + ":";
    }

    /**
     * Creates the standard part for most of the message types.
     *
     * @param type The message type.
     * @param nick Nick name to use in the message instead of the default.
     * @return A message.
     */
    private String createMessage(final String type, final String nick) {
        return me.getCode() + "!" + type + "#" + nick + ":";
    }
}
