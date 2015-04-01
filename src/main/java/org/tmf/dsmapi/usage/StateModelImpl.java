package org.tmf.dsmapi.usage;

import org.tmf.dsmapi.usage.model.Status;
import org.tmf.dsmapi.commons.workflow.StateModelBase;

/**
 *
 * @author maig7313
 */
public class StateModelImpl extends StateModelBase<Status> {
    
    /**
     *
     */
    public StateModelImpl() {
        super(Status.class);
    }    

    /**
     *
     */
    @Override
    protected void draw() {
        // First
        from(Status.RECEIVED).to(
                Status.REJECTED,
                Status.GUIDED);

        // Somewhere
        from(Status.REJECTED).to(
                Status.RECYCLED);       
        from(Status.GUIDED).to(
                Status.RATED,
                Status.REJECTED);
        from(Status.RECYCLED).to(
                Status.REJECTED,
                Status.GUIDED);
        from(Status.RATED).to(
                Status.RERATE);
        from(Status.RERATE).to(
                Status.RATED);

        // Final
        from(Status.BILLED);
//        from(Status.REJECTED);
    }
}
