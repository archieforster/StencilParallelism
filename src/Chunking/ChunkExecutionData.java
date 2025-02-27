package Chunking;

import java.util.concurrent.Semaphore;

public class ChunkExecutionData {

    public volatile int iters_complete;
    public Semaphore[] nbrs_complete;
    public volatile boolean inner_complete;

}
