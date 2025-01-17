package com.optum.admiral.model;

import com.optum.admiral.event.ExecuteHookListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

public class ExecuteHook implements Callable<Integer> {
    final String cmdId;
    final String command;
    final String workingDir;
    ExecuteHookListener executeHookListener;

    private String lastNonErrorLine;

    public ExecuteHook(String cmdId, String command, String workingDir) {
        this.cmdId = cmdId;
        this.command = command;
        this.workingDir = workingDir;
    }

    public void setExecuteHookListener(ExecuteHookListener executeHookListener){
        this.executeHookListener = executeHookListener;
    }

    public String getCmdId() {
        return cmdId;
    }

    public String getCommand() {
        return command;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    @Override
    public Integer call() throws Exception {
        executeHookListener.startLine(cmdId, "Executing Post Create Hook for " + cmdId);

        Process process = Runtime.getRuntime().exec(command, null, new File(workingDir));

        Thread t = new Thread(() -> {
            String lastLine="";
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (true) {
                    if ((line = reader.readLine()) == null)
                        break;
                    executeHookListener.stdoutLine(cmdId, line);
                    lastLine = line;
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lastNonErrorLine = lastLine;
        });
        t.start();

        Thread t2 = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                String line;
                while (true) {
                    if ((line = reader.readLine()) == null)
                        break;
                    executeHookListener.stderrLine(cmdId, line);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t2.start();

        //  Wait for the executing hook.
        int result = process.waitFor();

        // Wait for the readers.
        t.join();
        t2.join();

        if (result==0) {
            executeHookListener.doneLine(cmdId, lastNonErrorLine);
        }

        return result;
    }
}
