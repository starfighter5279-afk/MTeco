package com.dirt.scheduler;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class PlatformScheduler {
    private final Plugin plugin;
    private final FoliaLib foliaLib;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.foliaLib = new FoliaLib(plugin);
    }

    public boolean isFolia() {
        return foliaLib.isFolia();
    }

    public ScheduledTask runGlobal(Runnable task) {
        if (isFolia()) {
            foliaLib.getScheduler().runNextTick(wrappedTask -> task.run());
            return new CompletedTask();
        }
        return wrap(plugin.getServer().getScheduler().runTask(plugin, task));
    }

    public ScheduledTask runGlobalLater(Runnable task, long delayTicks) {
        if (isFolia()) {
            return wrap(foliaLib.getScheduler().runLater(task, delayTicks));
        }
        return wrap(plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            return wrap(foliaLib.getScheduler().runTimer(task, delayTicks, periodTicks));
        }
        return wrap(plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    public ScheduledTask runAsync(Runnable task) {
        if (isFolia()) {
            foliaLib.getScheduler().runAsync(wrappedTask -> task.run());
            return new CompletedTask();
        }
        return wrap(plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task));
    }

    public ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            return wrap(foliaLib.getScheduler().runTimerAsync(task, delayTicks, periodTicks));
        }
        return wrap(plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks));
    }

    public ScheduledTask runAtEntity(Entity entity, Runnable task) {
        if (isFolia()) {
            foliaLib.getScheduler().runAtEntity(entity, wrappedTask -> task.run());
            return new CompletedTask();
        }
        return wrap(plugin.getServer().getScheduler().runTask(plugin, task));
    }

    public ScheduledTask runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (isFolia()) {
            return wrap(foliaLib.getScheduler().runAtEntityLater(entity, task, delayTicks));
        }
        return wrap(plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public ScheduledTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia()) {
            return wrap(foliaLib.getScheduler().runAtEntityTimer(entity, task, delayTicks, periodTicks));
        }
        return wrap(plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks));
    }

    public void teleport(Entity entity, Location destination, Runnable onSuccess) {
        if (isFolia()) {
            foliaLib.getScheduler().teleportAsync(entity, destination).thenAccept(success -> {
                if (success) {
                    runAtEntity(entity, onSuccess);
                }
            });
            return;
        }

        if (entity.teleport(destination)) {
            onSuccess.run();
        }
    }

    public void cancelAllTasks() {
        if (isFolia()) {
            foliaLib.getScheduler().cancelAllTasks();
        } else {
            plugin.getServer().getScheduler().cancelTasks(plugin);
        }
    }

    private ScheduledTask wrap(BukkitTask task) {
        return new ScheduledTask() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    private ScheduledTask wrap(WrappedTask task) {
        return new ScheduledTask() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }

    private static class CompletedTask implements ScheduledTask {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    }
}