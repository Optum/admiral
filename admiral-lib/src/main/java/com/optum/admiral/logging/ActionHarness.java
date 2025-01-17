package com.optum.admiral.logging;

import com.optum.admiral.exception.PortInUseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Admiral already has an ActionMonitor - a feature that inspects the logs to detect events; using those detections
 * to signify the status through implied Actions.  This isn't that.  This ActionHarness doesn't look into, or inspect
 * what is happening in an Action (something the verb "monitor" would imply).  Instead, this "goes around" an Action,
 * constructed at the beginning and "mopping up" the details at the end.  This "before/after but nothing in the middle"
 * seemed appropriate to call a harness.  So: ActionHarness.  All actions need to be executed within a harness.
 *
 * The best way to describe the purpose of ActionHarness:
 *      When an action is done, you should be able to ask the ActionHarness: "What just happened?"
 *
 * The answer will include:
 *   - What just happend? (which action)
 *   - What was the actual request? (the action arguments)
 *   - How long did it take? (elapsed time)
 *   - Was the action successful? How did it end? (the ending condition)
 *   - Are there any notes about how it ended? (the ending condition message)
 *
 * The beginning of an action nee
 */
public class ActionHarness {
    private final String action;
    private final List<String> arguments;

    private final Timer timer;
    private Exception e;

    private EndingCondition endingCondition = EndingCondition.NORMAL;
    private String endingConditionMessage = "";

    enum EndingCondition {
        NORMAL,
        USERINTERRUPTED,
        SYSTEMINTERRUPTED,
        TIMEOUT,
        UNKNOWNEXCEPTION,
        KNOWNEXCEPTION;
    }

    public ActionHarness(final String action, final List<String> arguments) {
        this.action = action;
        this.arguments = new ArrayList<String>(arguments);
        this.timer = new SimpleTimer();
    }

    public Timer getTimer() {
        return timer;
    }

    public String getAction() {
        return action;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public EndingCondition getEndingCondition() {
        return endingCondition;
    }

    public String getEndingConditionMessage() {
        return endingConditionMessage;
    }

    public void setException(Exception e) {
        if (e instanceof PortInUseException) {
            this.endingCondition = EndingCondition.KNOWNEXCEPTION;
            this.endingConditionMessage = e.getMessage();
        } else {
            this.endingCondition = EndingCondition.UNKNOWNEXCEPTION;
            this.endingConditionMessage = e.getMessage();
        }
        this.e = e;
    }

}
