package com.optum.admiral.model.admiral;

import com.optum.admiral.model.ProgressDetail;
import com.optum.admiral.model.ProgressMessage;

public class AdmiralProgressMessage implements ProgressMessage {
    final String id;
    final String status;
    final String stream;
    final String error;
    final String progress;
    final ProgressDetail progressDetail;

    public AdmiralProgressMessage(String id, String status) {
        this.id = id;
        this.status = status;
        this.stream = null;
        this.error = null;
        this.progress = null;
        this.progressDetail = null;
    }

    public AdmiralProgressMessage(String id, String status, String stream, String error, String progress, ProgressDetail progressDetail) {
        this.id = id;
        this.status = status;
        this.stream = stream;
        this.error = error;
        this.progress = progress;
        this.progressDetail = progressDetail;
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
