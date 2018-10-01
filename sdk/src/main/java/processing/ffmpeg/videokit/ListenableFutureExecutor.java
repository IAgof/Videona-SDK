package processing.ffmpeg.videokit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;

/**
 * Created by alvaro on 28/9/18.
 */

public class ListenableFutureExecutor {

  private static final int N_THREADS = 5;
  private final ListeningExecutorService executorPool;

  public ListenableFutureExecutor() {
    executorPool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(N_THREADS));
  }

  public ListenableFuture<?> execute(Runnable runnable) {
    return executorPool.submit(runnable);
  }
}
