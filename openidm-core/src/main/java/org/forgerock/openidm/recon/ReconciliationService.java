package org.forgerock.openidm.recon;

import org.forgerock.openidm.recon.impl.ReconciliationException;

import java.util.Map;

/**
 * Control reconciliation for a given {@link org.forgerock.openidm.recon.impl.ReconciliationConfiguration}.
 */
public interface ReconciliationService {
    /**
     * Begin reconciliation specified by the given reconciliation configuration name. If
     * the specified reconciliation in already running, then subsequent calls will be ignored.
     *
     * @param reconciliationConfigurationName
     *         of the configured reconciliation process to start
     * @throws org.forgerock.openidm.recon.impl.ReconciliationException
     *          if there is an error in starting reconciliation
     */
    void startReconciliation(String reconciliationConfigurationName) throws ReconciliationException;

    /**
     * Get the most recent reconciliation status for the specified configuration. If the reconciliation
     * process has finished this will be the last run status, however if the given configuration is currently
     * executing it will return the latest status update.
     * <p/>
     * Status is in the form of a simple mapped json object.
     *
     * @param reconciliationConfigurationName
     *         of the reconciliation process to get status for
     * @return statusObject that is a simple mapped json object
     * @throws org.forgerock.openidm.recon.impl.ReconciliationException
     *          if there was an error in getting the status
     */
    Map<String, Object> reconciliationStatus(String reconciliationConfigurationName) throws ReconciliationException;

    /**
     * Manually cancel a running reconciliation. If the reconciliation configuration is not
     * running than this request is ignored.
     *
     * @param reconciliationConfigurationName
     *         of the running reconciliation process to stop
     * @throws org.forgerock.openidm.recon.impl.ReconciliationException
     *          if there was an exception raised in trying to stop reconciliation
     */
    void cancelReconciliation(String reconciliationConfigurationName) throws ReconciliationException;
}
