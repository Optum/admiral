package com.optum.admiral.model.dockerjava;

import com.github.dockerjava.api.model.ResponseItem;
import com.optum.admiral.model.ProgressDetail;

public class DockerJavaProgressDetail implements ProgressDetail {
    private final Long current;
    private final Long start;
    private final Long total;

    public DockerJavaProgressDetail(ResponseItem.ProgressDetail progressDetail) {
        if (progressDetail==null) {
            this.current = 0L;
            this.start = 0L;
            this.total = 0L;
        } else {
            this.current = progressDetail.getCurrent();
            this.start = progressDetail.getStart();
            this.total = progressDetail.getTotal();
        }
    }

    @Override
    public Long current() {
        return current;
    }

    @Override
    public Long start() {
        return start;
    }

    @Override
    public Long total() {
        return total;
    }
}
