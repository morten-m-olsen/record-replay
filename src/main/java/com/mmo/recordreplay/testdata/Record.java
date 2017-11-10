package com.mmo.recordreplay.testdata;

import java.util.ArrayList;
import java.util.List;

import com.mmo.recordreplay.messages.Message;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A record is a record of both a sent request as well as what is received as a direct response of sending the request (a response and possibly some messages).
 *
 * @author Morten Meiling Olsen
 */
@Data
@NoArgsConstructor
public abstract class Record<T extends Message> {
    private String request;
    private Object response;
    private List<T> messages = new ArrayList<>();
}
