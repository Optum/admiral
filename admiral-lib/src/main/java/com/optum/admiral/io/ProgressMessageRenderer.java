package com.optum.admiral.io;

import com.optum.admiral.model.ProgressMessage;

public interface ProgressMessageRenderer {
    ProgressMessage renderProgressMessage(int current, int total, String id, String status, String progress);
    ProgressMessage renderProgressMessage(String id, String status, String progress);
}
