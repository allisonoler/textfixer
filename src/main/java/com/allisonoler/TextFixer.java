package com.allisonoler;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;
import com.vdurmont.emoji.EmojiParser;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;


import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TextFixer {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME =
            "Gmail API Java Quickstart";

    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/gmail-java-quickstart");

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/gmail-java-quickstart
     */
    private static final List<String> SCOPES =
            Arrays.asList(GmailScopes.GMAIL_MODIFY);

    private static final Collection<User> USERS =
            Arrays.asList(new User(System.getenv("ALLISON_PHONE_NUMBER"), System.getenv("ALLISON_REFRESH_TOKEN"), "Label_6", "Allison"),
                    new User(System.getenv("STEVE_PHONE_NUMBER"), System.getenv("STEVE_REFRESH_TOKEN"), "Label_1", "Steve"));

//    private static final Collection<User> USERS =
//            Arrays.asList(new User("8016611745@messaging.sprintpcs.com", "1/XD8pAM3-8p3N0Gi4Y4kL_Zvngju9WIJdfYfUUw_hIwPaND79kcle5ef_-94yxOW9", "Label_6", "Allison"),
//                    new User("8018358630@messaging.sprintpcs.com", "1/1rmWilEGZnb59ppoHJwYPHXjU06gTU1064qAYLs2-WcCFWB8EeHdDgEtOmtwk_gD", "Label_1", "Steve"));


    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize(String refreshToken) throws IOException {
        // Load client secrets.
//        InputStream in =
//                TextFixer.class.getResourceAsStream("/client_secret.json");
//        GoogleClientSecrets clientSecrets =
//                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow =
//                new GoogleAuthorizationCodeFlow.Builder(
//                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                        .setDataStoreFactory(DATA_STORE_FACTORY)
//                        .setAccessType("offline")
//                        .build();
//        Credential credential = new AuthorizationCodeInstalledApp(
//                flow, new LocalServerReceiver()).authorize("user");
        GoogleCredential credential = new GoogleCredential.Builder().setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(System.getenv("CLIENT_ID"), System.getenv("CLIENT_SECRET"))
                .build();
        credential.setRefreshToken(refreshToken);
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Gmail client service.
     *
     * @return an authorized Gmail client service
     * @throws IOException
     */
    public static Gmail getGmailService(String refreshToken) throws IOException {
        Credential credential = authorize(refreshToken);
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Create a Message from an email
     *
     * @param email Email to be set to raw of message
     * @return Message containing base64url encoded email.
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage email)
            throws MessagingException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        email.writeTo(baos);
        String encodedEmail = Base64.encodeBase64URLSafeString(baos.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static List<Message> getMessageList(User user) throws IOException {
        Gmail service = user.getService();
        Gmail.Users.Messages.List listMessages = service.users().messages().list("me");
        listMessages.setQ("-label:processedtexts subject:\"New text message from\" in:inbox");
        ListMessagesResponse listResponse = listMessages.execute();
        return listResponse.getMessages();
    }

    private static String getSubject(Message actualMessage) {
        List<MessagePartHeader> headers = actualMessage.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            if (header.getName().equals("Subject")) {
                return header.getValue();
            }
        }
        return "";
    }

    private static String getDecodedBody(MessagePart messagePart) throws UnsupportedEncodingException {
        String bodyData = messagePart.getBody().getData();
        byte[] byteArray = Base64.decodeBase64(bodyData.getBytes());
        return new String(byteArray, "UTF-8");
    }

    private static void setLabel(Message message, User user) throws IOException {
        Gmail service = user.getService();
        BatchModifyMessagesRequest content = new BatchModifyMessagesRequest();
        content.setAddLabelIds(Arrays.asList(user.getLableID()));
        content.setIds(Arrays.asList(message.getId()));
        service.users().messages().batchModify("me",content).execute();
    }

    private static MimeSearchResults findPlainTextMessagePart(Message actualMessage) {
        for (MessagePart messagePart : actualMessage.getPayload().getParts()) {
            if (messagePart.getMimeType().equals("text/plain")) {
                MimeSearchResults mimeSearchResults = new MimeSearchResults(messagePart, false);
                return mimeSearchResults;
            }
            for (MessagePart messagePartOfPart : messagePart.getParts()) {
                if (messagePartOfPart.getMimeType().equals("text/plain")) {
                    MimeSearchResults mimeSearchResults = new MimeSearchResults(messagePartOfPart, true);
                    return mimeSearchResults;
                }
            }
        }
        return null;
    }

    private static String fixMessage(String message) {
        String changedMessage = message.replace("\u201c", "\"");
        changedMessage = changedMessage.replace("\u201d", "\"");
        changedMessage = changedMessage.replace("\u2018", "\'");
        changedMessage = changedMessage.replace("\u2019", "\'");
        if (!changedMessage.equals(message)) {
            System.out.println("Smart punctuation found and changed.");
        }
        String punctuationFixedMessage = changedMessage;
        changedMessage = EmojiParser.parseToAliases(changedMessage);
        if (!punctuationFixedMessage.equals(changedMessage)) {
            System.out.println("Emoji(s) found and changed.");
        }
        return changedMessage;
    }

    private static String chopOffUselessParts(String message) {
        String changedMessage = message;
        changedMessage = changedMessage.replaceAll("(?s)^\\s*<https://voice.google.com>\\s*(.*)\\s*YOUR ACCOUNT.*$", "$1");
        changedMessage = changedMessage.replaceAll("(?s)^(.*)\\s*To respond to this text message, reply to this email or.*$", "$1");
        return changedMessage;
    }

    private static String fixMessagesWithPictures(String message) {
        System.out.println("Picture found and changed.");
        return message + "[PICTURE]";
    }

    private static Collection<String> breakUpMessage(String message) {
        Collection<String> brokenUpMessages = new ArrayList<>();
        for (int index = 0; index<message.length(); index = index + 160) {
            brokenUpMessages.add(message.substring(index, Math.min(index + 160, message.length())));
        }
        return brokenUpMessages;
    }

    private static void processMessage(Message message, User user) throws IOException, MessagingException, InterruptedException {
        Gmail service = user.getService();
        Message actualMessage = service.users().messages().get("me", message.getId()).execute();
        MimeSearchResults mimeSearchResults = findPlainTextMessagePart(actualMessage);
        MessagePart messagePart = mimeSearchResults.getMessage();
        if (messagePart != null) {
            String subjectData = getSubject(actualMessage);
            String personFrom = subjectData.replace("New text message from ", "");
            System.out.println("This message is from " + personFrom);
            String decodedMessage = getDecodedBody(messagePart);
            String changedMessage = fixMessage(decodedMessage);
            String simplifiedMessage = chopOffUselessParts(changedMessage);
            if (mimeSearchResults.getPictureOrNot()) {
                simplifiedMessage = fixMessagesWithPictures(simplifiedMessage);
            }
            simplifiedMessage = personFrom + " " + simplifiedMessage;
            Collection<String> messageParts = breakUpMessage(simplifiedMessage);
            if (!changedMessage.equals(decodedMessage) || mimeSearchResults.getPictureOrNot()) {
                sendMessage(messageParts, user, subjectData);
                System.out.println("Message changed and sent");
            }
            else {
                System.out.println("Message not changed");
            }
        }
        else {
            System.out.println("No plain text version of body");
        }
        setLabel(message, user);
        System.out.println("Finished processing message");
    }

    private static void sendMessage(Collection<String> messageArray, User user, String subject) throws AddressException, MessagingException, IOException {
        Gmail service = user.getService();
        Session session = Session.getDefaultInstance(new Properties(), null);
        for (String messageString : messageArray) {
            MimeMessage mimeMessage = new MimeMessage(session);
            String to = user.getPhoneNumber();
            mimeMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
           // mimeMessage.setSubject(subject);
            mimeMessage.setSentDate(new Date());
            // setup message body
            mimeMessage.setText(messageString);
            Message message = createMessageWithEmail(mimeMessage);
            service.users().messages().send("me", message).execute();
        }
    }

    public static void main(String[] args) throws IOException, AddressException, MessagingException, InterruptedException {
        // Build a new authorized API client service.
        for (User user : USERS) {
            user.setService(getGmailService(user.getRefreshToken()));
        }
        // Print the messages in the user's account.
        while (true) {
            for (User user : USERS) {
                List<Message> messages = getMessageList(user);
                if (messages == null) {
                    System.out.println("No messages found.");
                } else {
                    for (Message message : messages) {
                        System.out.println("Message found for "+ user.getUserName() +".  Processing...");
                        processMessage(message, user);
                    }
                }
            }
            TimeUnit.SECONDS.sleep(5);
        }
    }
}