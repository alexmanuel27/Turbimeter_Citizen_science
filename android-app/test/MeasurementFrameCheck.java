package org.citizenscience.turbimeter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MeasurementFrameCheck {
    public static void main(String[] args) {
        assert MeasurementFrame.parse("TURB1,DEMO,0.2500") != null;
        assert MeasurementFrame.parse("TURB1,RATIO,0.2500").kind == MeasurementFrame.Kind.RATIO;
        assert MeasurementFrame.parse("TURB1,RESULT,12.3,FNU,CAL-001,OK").kind == MeasurementFrame.Kind.RESULT;
        assert MeasurementFrame.parse("TURB1,RESULT,12.3,FNU,,OK") == null;
        assert MeasurementFrame.parse("TURB1,RESULT,12.3,FNU,CAL-001,UNCALIBRATED") == null;
        assert MeasurementFrame.parse("TURB1,RATIO,NaN") == null;
        assert MeasurementFrame.parse("TURB1,RATIO,-1") == null;
        assert MeasurementFrame.parse("garbage") == null;
        LineFramer framer = new LineFramer();
        List<String> lines = new ArrayList<>();
        byte[] first = "TURB1,DE".getBytes(StandardCharsets.US_ASCII);
        byte[] second = "MO,0.2500\r\nTURB1,RATIO,0.1\n".getBytes(StandardCharsets.US_ASCII);
        framer.accept(first, first.length, lines::add);
        framer.accept(second, second.length, lines::add);
        assert lines.size() == 2;
        assert "TURB1,DEMO,0.2500".equals(lines.get(0));
        assert "TURB1,RATIO,0.1".equals(lines.get(1));
        byte[] oversized = ("x".repeat(121) + "TURB1,RESULT,12.3,FNU,CAL-001,OK\n")
                .getBytes(StandardCharsets.US_ASCII);
        framer.accept(oversized, oversized.length, lines::add);
        assert lines.size() == 2;
        byte[] corrupted = "TURB1,RATIO,1\0.2\n".getBytes(StandardCharsets.US_ASCII);
        framer.accept(corrupted, corrupted.length, lines::add);
        assert lines.size() == 2;
        System.out.println("Measurement frame checks passed");
    }
}
