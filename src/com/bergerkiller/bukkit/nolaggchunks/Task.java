package com.bergerkiller.bukkit.nolaggchunks;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

public class Task implements Runnable {
    private JavaPlugin plugin;
    private Object[] arguments;
    private int taskID = 0;
    
    public static Task create(JavaPlugin plugin, Object... arguments) {
        return new Task(plugin, arguments);
    }
    public Task(JavaPlugin plugin, Object... arguments) {
        this.plugin = plugin;
        this.arguments = arguments;
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }
    public static Server getServer() {
        return Bukkit.getServer();
    }
    public Object getArg(int index) {
        return arguments[index];
    }
    public int getIntArg(int index) {
        return (Integer) getArg(index);
    }
    public long getLongArg(int index) {
        return (Long) getArg(index);
    }
    public float getFloatArg(int index) {
        return (Float) getArg(index);
    }
    public double getDoubleArg(int index) {
        return (Double) getArg(index);
    }
    public String getStringArg(int index) {
        return (String) getArg(index);
    }

    public void run() {

    }

    public boolean isQueued() {
        return getServer().getScheduler().isQueued(this.taskID);
    }
    public boolean isRunning() {
        return getServer().getScheduler().isCurrentlyRunning(this.taskID);
    }
    public void stop() {
        getServer().getScheduler().cancelTask(this.taskID);
    }
    public void start() {
        start(false);
    }
    public void start(boolean Async) {
        startDelayed(0, Async);
    }
    public void startDelayed(long tickDelay) {
        startDelayed(tickDelay, false);
    }
    public void startDelayed(long tickDelay, boolean Async) {
        if (Async) {
            this.taskID = getServer().getScheduler().scheduleAsyncDelayedTask(this.plugin, this, tickDelay);
        } else {
            this.taskID = getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, this, tickDelay);
        }
    }
    public void startRepeating(long tickInterval) {
        startRepeating(tickInterval, false);
    }
    public void startRepeating(long tickInterval, boolean Async) {
        startRepeating(0, tickInterval, Async);
    }
    public void startRepeating(long tickDelay, long tickInterval, boolean Async) {
        if (Async) {
            this.taskID = getServer().getScheduler().scheduleAsyncRepeatingTask(this.plugin, this, tickDelay, tickInterval);
        } else {
            this.taskID = getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, this, tickDelay, tickInterval);
        }
    }

}