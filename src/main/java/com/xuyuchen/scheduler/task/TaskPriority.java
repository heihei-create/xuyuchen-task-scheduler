package com.xuyuchen.scheduler.task;

public enum TaskPriority {
    LOW(10), NORMAL(50), HIGH(80), URGENT(100);
    private final int weight;
    TaskPriority(int weight) { this.weight = weight; }
    public int weight() { return weight; }
}
