package com.stm.evenote.rsync.model;

public interface Operation {

    void execute();

    default Operation concat(Operation other) {
        return () -> {
            this.execute();
            other.execute();
        };
    }
}
