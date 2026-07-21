package com.hermes.android;

import java.util.HashMap;
import java.util.Map;

/**
 * Parsed command with capability ID and arguments.
 */
public class ParsedCommand {
    private final String capability;
    private final Map<String, Object> args = new HashMap<>();
    private String error;

    public ParsedCommand(String capability) {
        this.capability = capability;
    }

    public static ParsedCommand error(String msg) {
        ParsedCommand cmd = new ParsedCommand("error");
        cmd.error = msg;
        return cmd;
    }

    public ParsedCommand arg(String key, Object value) {
        args.put(key, value);
        return this;
    }

    public String getCapability() { return capability; }
    public Map<String, Object> getArgs() { return args; }
    public String getError() { return error; }
    public boolean isError() { return error != null; }

    public int getIntArg(String key, int defaultVal) {
        Object v = args.get(key);
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException e) { return defaultVal; }
        }
        return defaultVal;
    }

    public String getStringArg(String key, String defaultVal) {
        Object v = args.get(key);
        return v != null ? v.toString() : defaultVal;
    }
}
