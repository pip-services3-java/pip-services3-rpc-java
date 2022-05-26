package org.pipservices3.rpc.services;

import org.pipservices3.components.count.CounterTiming;
import org.pipservices3.components.count.ICounters;
import org.pipservices3.components.log.ILogger;
import org.pipservices3.components.trace.TraceTiming;

public class InstrumentTiming {

    private final String _correlationId;
    private final String _name;
    private final String _verb;
    private ILogger _logger;
    private ICounters _counters;
    private CounterTiming _counterTiming;
    private TraceTiming _traceTiming;

    public InstrumentTiming(String correlationId, String name, String verb,
                            ILogger logger, ICounters counters,
                            CounterTiming counterTiming, TraceTiming traceTiming) {
        this._correlationId = correlationId;
        this._name = name;
        this._verb = verb != null ? verb : "call";
        this._logger = logger;
        this._counters = counters;
        this._counterTiming = counterTiming;
        this._traceTiming = traceTiming;
    }

    private void clear() {
        // Clear references to avoid double processing
        this._counters = null;
        this._logger = null;
        this._counterTiming = null;
        this._traceTiming = null;
    }

    public void endTiming(Exception err) {
        if (err == null) {
            this.endSuccess();
        } else {
            this.endFailure(err);
        }
    }

    public void endTiming() {
        this.endTiming(null);
    }

    public void endSuccess() {
        if (this._counterTiming != null) {
            this._counterTiming.endTiming();
        }
        if (this._traceTiming != null) {
            this._traceTiming.endTrace();
        }

        this.clear();
    }

    public void endFailure(Exception err) {
        if (this._counterTiming != null) {
            this._counterTiming.endTiming();
        }

        if (err != null) {
            if (this._logger != null) {
                this._logger.error(this._correlationId, err, "Failed to call %s method", this._name);
            }
            if (this._counters != null) {
                this._counters.incrementOne(this._name + "." + this._verb + "_errors");
            }
            if (this._traceTiming != null) {
                this._traceTiming.endFailure(err);
            }
        } else {
            if (this._traceTiming != null) {
                this._traceTiming.endTrace();
            }
        }

        this.clear();
    }
}
