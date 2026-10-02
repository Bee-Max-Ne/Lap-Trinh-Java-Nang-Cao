package huddtds.application.event;

public enum EventType {
    TRANSACTION_PROCESSED,
    CHECKPOINT_CREATED,
    GLOBAL_DRIFT,
    LOCAL_DRIFT,
    SIMULATION_FINISHED,
    SIMULATION_ERROR
}
