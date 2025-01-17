package com.optum.admiral.model;

import com.optum.admiral.event.StartWaitListener;

public interface StartWait {
    boolean waitForIt(StartWaitListener startWaitListener);
    int getSeconds();
}
