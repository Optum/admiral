package com.optum.admiral.model.admiral;

import com.optum.admiral.model.ProgressDetail;

public class AdmiralProgressDetail implements ProgressDetail {
    final Long current;
    final Long start;
    final Long total;

    public AdmiralProgressDetail(long current, long total) {
        this.current = current;
        this.start = 0L;
        this.total = total;
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
