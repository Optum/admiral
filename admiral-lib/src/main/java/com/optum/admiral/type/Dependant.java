package com.optum.admiral.type;

public class Dependant {
    private final String serviceName;
    private final Condition condition;

    public Dependant(String serviceName) {
        this.serviceName = serviceName;
        this.condition = Condition.service_started;
    }

    public Dependant(String serviceName, Condition condition) {
        this.serviceName = serviceName;
        this.condition = condition;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Condition getCondition() {
        return condition;
    }

    public enum Condition {
        service_started("service_started", "started"),
        service_healthy("service_healthy", "healthy"),
        service_completed_successfully("service_completed_successfully", "successful");

        private final String name;
        private final String brief;
        Condition(String name, String brief) {
            this.name = name;
            this.brief = brief;
        }

        @Override
        public String toString() {
            return name;
        }

        public String toBrief() {
            return brief;
        }
    }
}
