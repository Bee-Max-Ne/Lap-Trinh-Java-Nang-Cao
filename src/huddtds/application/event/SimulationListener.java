package huddtds.application.event;

@FunctionalInterface
public interface SimulationListener {
    void onUpdate(SimulationEvent event);
}
