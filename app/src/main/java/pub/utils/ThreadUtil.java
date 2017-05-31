package pub.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by able on 2017/4/25.
 */

public class ThreadUtil {

    private static ExecutorService  cachedThreadPool;
    public static ExecutorService getCachedThreadPool(){
        if(cachedThreadPool == null){
            cachedThreadPool = Executors.newCachedThreadPool();
        }
        return cachedThreadPool;
    }

    private static ExecutorService  singleThreadPool;
    public static ExecutorService getSingleThreadPool(){
        if(singleThreadPool == null){
            singleThreadPool = Executors.newSingleThreadExecutor();
        }
        return singleThreadPool;
    }
}
