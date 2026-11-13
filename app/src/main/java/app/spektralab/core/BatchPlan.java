package app.spektralab.core;

/**
 * The frame order a batch export walks.
 *
 * <p>Frames without a film stock cannot be developed, so they are skipped
 * instead of being counted as failures. The progress denominator counts only
 * frames that will actually export, otherwise "2/3" can include a frame the
 * run never touches.
 */
public final class BatchPlan {

    private final int[] frames;
    private final int total;

    private BatchPlan(int[] frames, int total) {
        this.frames = frames;
        this.total = total;
    }

    /** Build the plan from a per-frame "has a film stock" flag, in frame order. */
    public static BatchPlan of(boolean[] eligible) {
        int count = 0;
        for (boolean yes : eligible) {
            if (yes) {
                ++count;
            }
        }
        final int[] picked = new int[count];
        int at = 0;
        for (int i = 0; i < eligible.length; ++i) {
            if (eligible[i]) {
                picked[at++] = i;
            }
        }
        return new BatchPlan(picked, eligible.length);
    }

    /** Number of frames that will export. */
    public int size() {
        return frames.length;
    }

    /** Number of frames in the collection, including skipped ones. */
    public int totalFrames() {
        return total;
    }

    public boolean isEmpty() {
        return frames.length == 0;
    }

    /** Frame index for the n-th export, 0-based. */
    public int frameAt(int position) {
        return frames[position];
    }

    /** Progress label for the n-th export, 1-based in the text. */
    public String label(int position) {
        final int skipped = total - frames.length;
        return "批量导出 " + (position + 1) + "/" + frames.length
                + (skipped > 0 ? "（跳过 " + skipped + " 帧）" : "");
    }

    /**
     * Completion line. Skipped frames are their own count: they were never
     * attempted, so folding them into the failures would overstate breakage.
     */
    public String summary(int ok, int failed) {
        final int skipped = total - frames.length;
        return "批量导出完成：" + ok + " 张成功"
                + (failed > 0 ? "，" + failed + " 张失败" : "")
                + (skipped > 0 ? "，" + skipped + " 张跳过（没有胶片）" : "");
    }
}
