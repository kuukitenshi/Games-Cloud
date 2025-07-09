package pt.ulisboa.tecnico.cnv.javassistagent.tools;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.CannotCompileException;

public class ThreadedICount extends AbstractJavassistTool {

    private static final Map<Long, Long> INSTRUCTIONS_MAP = new ConcurrentHashMap<>();

    public ThreadedICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int length) {
        long tid = Thread.currentThread().getId();
        if (INSTRUCTIONS_MAP.containsKey(tid)) {
            long data = INSTRUCTIONS_MAP.get(tid);
            data += length;
            INSTRUCTIONS_MAP.put(tid, data);
        }
    }

    public static void startInstrumentation() {
        long tid = Thread.currentThread().getId();
        INSTRUCTIONS_MAP.put(tid, 0L);
    }

    public static long finishInstrumentation() {
        long tid = Thread.currentThread().getId();
        long data = INSTRUCTIONS_MAP.get(tid);
        INSTRUCTIONS_MAP.remove(tid);
        return data;
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        String code = String.format("%s.incBasicBlock(%s);", ThreadedICount.class.getName(), block.length);
        block.behavior.insertAt(block.line, code);
    }

}
