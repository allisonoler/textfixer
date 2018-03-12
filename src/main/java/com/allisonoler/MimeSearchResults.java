package com.allisonoler;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;

public class MimeSearchResults {
    private MessagePart actualMessage;
    private boolean pictureOrNot;

    public MimeSearchResults(MessagePart actualMessage, boolean pictureOrNot) {
        this.actualMessage = actualMessage;
        this.pictureOrNot = pictureOrNot;
    }

    public MessagePart getMessage(){
        return actualMessage;
    }
    public boolean getPictureOrNot() {
        return pictureOrNot;
    }
}
