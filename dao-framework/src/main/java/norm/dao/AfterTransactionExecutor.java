package norm.dao;

import norm.dao.exceptions.DaoRuntimeException;
import org.apache.log4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.ArrayList;
import java.util.List;

//@Component
public class AfterTransactionExecutor extends TransactionSynchronizationAdapter {
    private static final Logger log = Logger.getLogger(AfterTransactionExecutor.class);
    private static final ThreadLocal<List<RUNNABLE>> RUNNABLES = new ThreadLocal<>();

    public void schedule(DaoProxy daoProxy, AfterTransactionRunnable runAfterCommit, AfterTransactionRunnable runAfterRollback){
        log.debug("Scheduling "+daoProxy+" to be processed after commit for thread:"+Thread.currentThread().getId());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            //effectively enforces that all executions are wrapped in a transaction.
            throw new DaoRuntimeException("Transaction synchronization is NOT ACTIVE for daoProxy:"+daoProxy);
        }
        List<RUNNABLE> runnables = RUNNABLES.get();
        if (runnables == null) {
            runnables = new ArrayList<>();
            RUNNABLES.set(runnables);
            TransactionSynchronizationManager.registerSynchronization(this);
        }
        runnables.add(new RUNNABLE(daoProxy, runAfterCommit, runAfterRollback));
    }

    @Override
    public void afterCompletion(int status) {
        if (status == TransactionSynchronization.STATUS_COMMITTED)
            log.debug("Transaction completed with status COMMITTED");
        else if (status == TransactionSynchronization.STATUS_ROLLED_BACK)
            log.warn("Transaction completed with status ROLLED_BACK");
        else
            log.error("Transaction completed with UNKNOWN status ("+status+") !!!");

        for (RUNNABLE runnable : RUNNABLES.get()) {
            log.debug("Processing:" + runnable.daoProxy);

            if (status == TransactionSynchronization.STATUS_COMMITTED)
            {
                if (runnable.runAfterCommit != null)
                    runnable.runAfterCommit.run();

                //Re-set modified fields
                runnable.daoProxy.getModifiedFields().clear();
            }
            else {
                if (runnable.runAfterRollback != null)
                    runnable.runAfterRollback.run();
            }

            //Unlock for other threads
            runnable.daoProxy.lock(false);
            synchronized (runnable.daoProxy.getData()) {
                runnable.daoProxy.getData().notifyAll();//notify all threads (if any) that the lock is removed
            }
        }

        RUNNABLES.remove();
    }

    private class RUNNABLE {
        DaoProxy daoProxy;
        AfterTransactionRunnable runAfterCommit;
        AfterTransactionRunnable runAfterRollback;
        RUNNABLE(DaoProxy daoProxy, AfterTransactionRunnable runAfterCommit, AfterTransactionRunnable runAfterRollback) {
            this.daoProxy = daoProxy;
            this.runAfterCommit = runAfterCommit;
            this.runAfterRollback = runAfterRollback;
        }
    }
}

