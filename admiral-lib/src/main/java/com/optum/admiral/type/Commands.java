package com.optum.admiral.type;

import com.optum.admiral.yaml.CommandsYaml;

public class Commands {
    public static final Commands AUTO = new Commands();

    public enum Binding {
        AUTO,
        MANUAL,
        NEVER
    }

    public final Binding bounce;
    public final Binding create;
    public final Binding down;
    public final Binding join;
    public final Binding restart;
    public final Binding rm;
    public final Binding start;
    public final Binding stop;
    public final Binding unjoin;
    public final Binding up;
    public final Binding wait;

    public Commands() {
        this.bounce = Binding.AUTO;
        this.create = Binding.AUTO;
        this.down = Binding.AUTO;
        this.join = Binding.AUTO;
        this.restart = Binding.AUTO;
        this.rm = Binding.AUTO;
        this.start = Binding.AUTO;
        this.stop = Binding.AUTO;
        this.unjoin = Binding.AUTO;
        this.up = Binding.AUTO;
        this.wait = Binding.AUTO;
    }

    public Commands(CommandsYaml commandsYaml) {
        this.bounce = commandsYaml.bounce;
        this.create = commandsYaml.create;
        this.down = commandsYaml.down;
        this.join = commandsYaml.join;
        this.restart = commandsYaml.restart;
        this.rm = commandsYaml.rm;
        this.start = commandsYaml.start;
        this.stop = commandsYaml.stop;
        this.unjoin = commandsYaml.unjoin;
        this.up = commandsYaml.up;
        this.wait = commandsYaml.wait;
    }

    public boolean allAUTO() {
        return ((bounce==Binding.AUTO) &&
                (create==Binding.AUTO) &&
                (down==Binding.AUTO) &&
                (join==Binding.AUTO) &&
                (restart==Binding.AUTO) &&
                (rm==Binding.AUTO) &&
                (start==Binding.AUTO) &&
                (stop==Binding.AUTO) &&
                (unjoin==Binding.AUTO) &&
                (up==Binding.AUTO) &&
                (wait==Binding.AUTO));
    }
}
