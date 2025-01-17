package com.optum.admiral.io;

import com.optum.admiral.model.ProgressMessage;
import com.optum.admiral.model.admiral.AdmiralProgressMessage;

public class NoBarsProgressMessageRenderer implements ProgressMessageRenderer {
    private final OutputStyler os;

    public NoBarsProgressMessageRenderer(OutputStyler outputStyler) {
        this.os = outputStyler;
    }

    @Override
    public ProgressMessage renderProgressMessage(int current, int total, String id, String status, String progress) {
        return new AdmiralProgressMessage(id, os.lineheader.format(id) + os.linebody.format(": [" + status + "] " + progress));
    }

    @Override
    public ProgressMessage renderProgressMessage(String id, String status, String progress) {
        return new AdmiralProgressMessage(id, os.lineheader.format(id) + os.linebody.format(": [" + status + "] " + progress));
    }
}
