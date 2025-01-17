package com.optum.admiral.io;

import com.optum.admiral.model.ProgressDetail;
import com.optum.admiral.model.ProgressMessage;
import com.optum.admiral.model.admiral.AdmiralProgressDetail;
import com.optum.admiral.model.admiral.AdmiralProgressMessage;

public class BarsProgressMessageRenderer implements ProgressMessageRenderer {
    private final OutputStyler os;
    private final RenderWidthProvider renderWidthProvider;

    public BarsProgressMessageRenderer(OutputStyler os, RenderWidthProvider renderWidthProvider) {
        this.os = os;
        this.renderWidthProvider = renderWidthProvider;
    }

    @Override
    public ProgressMessage renderProgressMessage(int current, int total, String id, String status, String progress) {
        int totalWidth = renderWidthProvider.getRenderWidth();
        String effectiveId = id;

        int columnsUsed = 2; // For [ ]
        columnsUsed+=3; // There are three characters added by AnsiProgressHandler
        columnsUsed+=status.length();
        columnsUsed+=progress.length();
        columnsUsed+=4; // For " - ["
        columnsUsed+=3; // For "] /"
        columnsUsed+=effectiveId.length();
        columnsUsed+=Integer.toString(current).length();
        columnsUsed+=Integer.toString(total).length();
        columnsUsed+=4; // So we don't write in the last column

        int columnsAvailable = totalWidth-columnsUsed;
        int barwidth = columnsAvailable - 1;

        int bardone = barwidth * current / total;
        int bartogo = barwidth - bardone;

        StringBuilder sb = new StringBuilder();
        sb.append("- [");
        for(int i=0;i<bardone;i++) {
            sb.append("=");
        }
        sb.append(">");
        for(int i=0;i<bartogo;i++) {
            sb.append("-");
        }
        sb.append("] ");
        sb.append(current);
        sb.append("/");
        sb.append(total);

        final String bar = sb.toString();
        ProgressDetail progressDetail = new AdmiralProgressDetail(current, total);
        return new AdmiralProgressMessage(os.barlineheader.format(effectiveId),
                os.barlinebody.format("[" + status + "] " + progress), null, null, bar, progressDetail);
    }

    @Override
    public ProgressMessage renderProgressMessage(String id, String status, String progress) {
        int totalWidth = renderWidthProvider.getRenderWidth();
        String effectiveId = id;
        int columnsUsed=2; // For []
        columnsUsed+=3; // There are three characters added by AnsiProgressHandler
        columnsUsed+=effectiveId.length();
        columnsUsed+=status.length();
        columnsUsed+=4; // So we don't write in the last column
        int columnsAvailable = totalWidth-columnsUsed;
        String show = progress.substring(Math.max(0,progress.length()-columnsAvailable));

        ProgressDetail progressDetail = new AdmiralProgressDetail(1,1);
        return new AdmiralProgressMessage(os.barlineheader.format(effectiveId),
                os.barlinebody.format("[" + status + "]"), null, null, show, progressDetail);
    }
}
