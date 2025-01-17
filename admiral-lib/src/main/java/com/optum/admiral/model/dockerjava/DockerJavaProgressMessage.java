package com.optum.admiral.model.dockerjava;

import com.github.dockerjava.api.model.PullResponseItem;
import com.optum.admiral.model.ProgressDetail;
import com.optum.admiral.model.ProgressMessage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DockerJavaProgressMessage implements ProgressMessage {
    final String id;
    final String status;
    final String stream;
    final String error;
    final String progress;
    final ProgressDetail progressDetail;

    public DockerJavaProgressMessage(PullResponseItem pullResponseItem) {
        this.id = pullResponseItem.getId();
        this.status = pullResponseItem.getStatus();
        this.stream = pullResponseItem.getStream();
        this.error = pullResponseItem.getError();
        this.progress = pullResponseItem.getProgress();
        this.progressDetail = new DockerJavaProgressDetail(pullResponseItem.getProgressDetail());
        write(pullResponseItem.toString());
    }

    private static void write(String s) {
        try(FileWriter fw = new FileWriter("./pulllog.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(s);
        } catch (IOException e) {
        }
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String status() {
        return status;
    }

    @Override
    public String stream() {
        return stream;
    }

    @Override
    public String error() {
        return error;
    }

    @Override
    public String progress() {
        return progress;
    }

    @Override
    public ProgressDetail progressDetail() {
        return progressDetail;
    }
}
