//
// Copyright Aliaksei Levin (levlam@telegram.org), Arseny Smirnov (arseny30@gmail.com) 2014-2019
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//
package io.github.sammers21.tg.cleaner;

import org.drinkless.tdlib.Client;
import org.drinkless.tdlib.TdApi;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Example class for TDLib usage from Java.
 */
public final class App {
    private static Client client = null;

    public static final long VOICY_USER_ID = 259276793L;
    public static String TGC_IGNORE = "#tgc_ignore";
    public static String TGC_ALLOW_TEXT_ONLY = "#tgc_allow_text_only";
    public static String TGC_VOICE_TRANSCRIPT = "#tgc_voice_transcript";

    private static TdApi.AuthorizationState authorizationState = null;
    private static volatile boolean haveAuthorization = false;
    private static volatile boolean quiting = false;

    private static final Client.ResultHandler defaultHandler = new DefaultHandler();

    private static final Lock authorizationLock = new ReentrantLock();
    private static final Condition gotAuthorization = authorizationLock.newCondition();

    private static final String newLine = System.getProperty("line.separator");
    private static final String commandsLine = "Enter command (lo - LogOut, q - Quit): ";
    private static volatile String currentPrompt = null;

    private static final TGCleanerConfig cleanConfig = new TGCleanerConfig();

    static {
        try {
            System.loadLibrary("tdjni");
        } catch (UnsatisfiedLinkError linkError) {
            System.out.println("Failed to load system lib: tdjni, loading from jar");
            try {
                Path tempDirWithPrefix = Files.createTempDirectory("tempload");
                File file = new File(tempDirWithPrefix.toString() + System.getProperty("file.separator") + "libtdjni.so");
                if (!file.exists()) {
                    InputStream link = (App.class.getResourceAsStream("/libtdjni.so"));
                    Files.copy(link, file.getAbsoluteFile().toPath());
                }
                file.deleteOnExit();
                System.out.println("Load jni lib from " + file.getAbsolutePath());
                System.load(file.getAbsolutePath());
            } catch (IOException e) {
                throw new IllegalStateException("Can't unpack native part", e);
            } catch (UnsatisfiedLinkError e) {
                throw new IllegalStateException("Couldn't load library from jar", e);
            }
        }
    }

    private static void print(String str) {
        if (currentPrompt != null) {
            System.out.println("");
        }
        System.out.println(str);
        if (currentPrompt != null) {
            System.out.print(currentPrompt);
        }
    }

    private static void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
        if (authorizationState != null) {
            App.authorizationState = authorizationState;
        }
        switch (App.authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR:
                TdApi.TdlibParameters parameters = new TdApi.TdlibParameters();
                parameters.databaseDirectory = "tdlib";
                parameters.useMessageDatabase = true;
                parameters.useSecretChats = false;
                parameters.apiId = 949093;
                parameters.apiHash = "ee530101b3b27560755a8261753f5f7b";
                parameters.systemLanguageCode = "en";
                parameters.deviceModel = "Server";
                parameters.systemVersion = "Linux";
                parameters.applicationVersion = "0.1.beta";
                parameters.enableStorageOptimizer = true;
                client.send(new TdApi.SetTdlibParameters(parameters), new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR:
                client.send(new TdApi.CheckDatabaseEncryptionKey(), new AuthorizationRequestHandler());
                break;
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                String phoneNumber = promptString("Please enter phone number: ");
                client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                String code = promptString("Please enter authentication code: ");
                client.send(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                String password = promptString("Please enter password: ");
                client.send(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequestHandler());
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR:
                haveAuthorization = true;
                authorizationLock.lock();
                try {
                    gotAuthorization.signal();
                } finally {
                    authorizationLock.unlock();
                }
                fetchConfig(TGC_IGNORE, 0, 0, 0);
                fetchConfig(TGC_VOICE_TRANSCRIPT, 0, 0, 0);
                fetchConfig(TGC_ALLOW_TEXT_ONLY, 0, 0, 0);
                break;
            case TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR:
                haveAuthorization = false;
                print("Logging out");
                break;
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR:
                haveAuthorization = false;
                print("Closing");
                break;
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR:
                print("Closed");
                if (!quiting) {
                    client = Client.create(new UpdatesHandler(), null, null); // recreate client after previous has closed
                }
                break;
            default:
                System.err.println("Unsupported authorization state:" + newLine + App.authorizationState);
        }
    }

    private static String promptString(String prompt) {
        System.out.print(prompt);
        currentPrompt = prompt;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String str = "";
        try {
            str = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentPrompt = null;
        return str;
    }

    private static void getCommand() {
        String command = promptString(commandsLine);
        String[] commands = command.split(" ", 2);
        try {
            switch (commands[0]) {
                case "lo":
                    haveAuthorization = false;
                    client.send(new TdApi.LogOut(), defaultHandler);
                    break;
                case "q":
                    quiting = true;
                    haveAuthorization = false;
                    client.send(new TdApi.Close(), defaultHandler);
                    break;
                default:
                    System.err.println("Unsupported command: " + command);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            print("Not enough arguments");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // disable TDLib log
        Client.execute(new TdApi.SetLogVerbosityLevel(0));
        if (Client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        // create client
        client = Client.create(new UpdatesHandler(), null, null);

        // test Client.execute
        defaultHandler.onResult(Client.execute(new TdApi.GetTextEntities("@telegram /test_command https://telegram.org telegram.me @gif @test")));

        // main loop
        while (!quiting) {
            // await authorization
            authorizationLock.lock();
            try {
                while (!haveAuthorization) {
                    gotAuthorization.await();
                }
            } finally {
                authorizationLock.unlock();
            }

            while (haveAuthorization) {
                getCommand();
            }
        }
    }

    private static void fetchConfig(String q, int offsetDate, long chat_id, long message_id) {
        client.send(new TdApi.SearchMessages(new TdApi.ChatListMain(), q, offsetDate, chat_id, message_id, 100), object -> {
            TdApi.Messages messages = (TdApi.Messages) object;
            int length = messages.messages.length;
            fillWithMessages(messages);
            if (length != 0) {
                TdApi.Message lastMsg = messages.messages[messages.totalCount - 1];
                fetchConfig(q, lastMsg.date, lastMsg.chatId, lastMsg.id);
            }
        });
    }

    private static void fillWithMessages(TdApi.Messages messages) {
        for (TdApi.Message message : messages.messages) {
            handleMessage(message);
        }
    }

    private static class DefaultHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            print(object.toString());
        }
    }

    private static void handleMessage(TdApi.Message msg) {
        TdApi.MessageContent msgContent = msg.content;
        if (cleanConfig.isTextOnlyChat(msg.chatId) && msgContent.getConstructor() != TdApi.MessageText.CONSTRUCTOR) {
            client.send(new TdApi.DeleteMessages(msg.chatId, new long[]{msg.id}, true), res -> {

            });
        } else {
            switch (msgContent.getConstructor()) {
                case TdApi.MessageSticker.CONSTRUCTOR:
                    TdApi.MessageSticker sticker = (TdApi.MessageSticker) msgContent;
                    if (cleanConfig.isStickerIgnored(sticker.sticker.setId, sticker.sticker.emoji)) {
                        client.send(new TdApi.DeleteMessages(msg.chatId, new long[]{msg.id}, true), res -> {
                            System.out.println(String.format("Sticker set=%d emoji=%s is ignored: " + res, sticker.sticker.setId, sticker.sticker.emoji));
                        });
                    } else {
                        System.out.println(String.format("Sticker set=%d emoji=%s is not ignored ", sticker.sticker.setId, sticker.sticker.emoji));
                    }
                    break;

                case TdApi.MessageText.CONSTRUCTOR:
                    TdApi.MessageText text = (TdApi.MessageText) msgContent;
                    long replyToMessageId = msg.replyToMessageId;
                    String msgText = text.text.text;
                    if (replyToMessageId != 0 && msgText.equals(TGC_IGNORE)) {
                        client.send(new TdApi.GetMessage(msg.chatId, replyToMessageId), response -> {
                            TdApi.Message message = (TdApi.Message) response;
                            TdApi.MessageContent content = message.content;
                            if (content.getConstructor() == TdApi.MessageSticker.CONSTRUCTOR) {
                                TdApi.MessageSticker messageSticker = (TdApi.MessageSticker) content;
                                cleanConfig.ignoreSticker(messageSticker.sticker.setId, messageSticker.sticker.emoji);
                            }
                        });
                    } else if (msgText.equals(TGC_ALLOW_TEXT_ONLY)) {
                        cleanConfig.addTextOnlyChat(msg.chatId);
                    } else if (msgText.contains(TGC_VOICE_TRANSCRIPT)) {
                        cleanConfig.configTranscript(msg.chatId, "ru");
                    }
                    break;
                case TdApi.MessageVoiceNote.CONSTRUCTOR:
                    if (cleanConfig.isTranscriptEnabled(msg.chatId)) {
                        String language = cleanConfig.translationLanguage(msg.chatId);
                        System.out.println("Start translation process with lang: " + language);
                        voicyTranscript(msg);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static void voicyTranscript(TdApi.Message toFwd) {
        client.send(new TdApi.SearchPublicChat("voicybot"), object -> {
            TdApi.Chat chat = (TdApi.Chat) object;
            voicyRespondOnChatAndMsg.set(new Map.Entry<Long, Long>() {
                @Override
                public Long getKey() {
                    return toFwd.chatId;
                }

                @Override
                public Long getValue() {
                    return toFwd.id;
                }

                @Override
                public Long setValue(Long value) {
                    return null;
                }
            });
            client.send(
                new TdApi.ForwardMessages(
                    chat.id,
                    toFwd.chatId,
                    new long[]{toFwd.id},
                    new TdApi.SendMessageOptions(true, false, null),
                    false,
                    false,
                    false
                ), res -> {
                    System.out.println("Message has been forwarded " + toFwd.id);
                });
        });
    }

    private static class UpdatesHandler implements Client.ResultHandler {

        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.UpdateAuthorizationState.CONSTRUCTOR:
                    onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) object).authorizationState);
                    break;
                case TdApi.UpdateNewMessage.CONSTRUCTOR:
                    TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage) object;
                    TdApi.Message msg = newMessage.message;
                    handleMessage(msg);
                    break;
                case TdApi.UpdateChatLastMessage.CONSTRUCTOR:
                    final TdApi.UpdateChatLastMessage message = (TdApi.UpdateChatLastMessage) object;
                    final TdApi.MessageText text = (TdApi.MessageText) message.lastMessage.content;
                    if (message.lastMessage.senderUserId == VOICY_USER_ID) {
                        final String txt = text.text.text;
                        System.out.println("Message from voicy catched: " + txt);
                        if (txt.contains("...")) {
                            handleNextVoicyMessage.set(true);
                            System.out.println("Handle next voicy message");
                            return;
                        }
                        if (handleNextVoicyMessage.compareAndSet(true, false)) {
                            final Map.Entry<Long, Long> entry = voicyRespondOnChatAndMsg.get();
                            System.out.println(String.format("Sending translation to chat: %s reply:%s, input:%s", entry.getKey(), entry.getValue(), txt));
                            client.send(
                                new TdApi.SendMessage(
                                    entry.getKey(),
                                    entry.getValue(),
                                    null,
                                    null,
                                    new TdApi.InputMessageText(new TdApi.FormattedText(txt, null), false, true)
                                ), proxyResp -> {
                                    System.out.println("Proxyresp");
                                }
                            );
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static final AtomicBoolean handleNextVoicyMessage = new AtomicBoolean(false);
    private static final AtomicReference<Map.Entry<Long, Long>> voicyRespondOnChatAndMsg = new AtomicReference<>(null);

    private static class AuthorizationRequestHandler implements Client.ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.Error.CONSTRUCTOR:
                    System.err.println("Receive an error:" + newLine + object);
                    onAuthorizationStateUpdated(null); // repeat last action
                    break;
                case TdApi.Ok.CONSTRUCTOR:
                    // result is already received through UpdateAuthorizationState, nothing to do
                    break;
                default:
                    System.err.println("Receive wrong response from TDLib:" + newLine + object);
            }
        }
    }
}