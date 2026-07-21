package com.hermes.android.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 多模型注册表。
 * 存储: SharedPreferences "mov_models" → JSONArray of ModelConfig。
 * 至少保留一个模型。
 */
public class ModelRegistry {

    private static final String PREFS = "mov_models";
    private static final String KEY = "models_json";

    private final SharedPreferences prefs;

    public ModelRegistry(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        seedIfEmpty();
    }

    /** 首次启动种子: 一个 DeepSeek 默认模型 */
    private void seedIfEmpty() {
        if (prefs.getString(KEY, null) != null) return;
        ModelConfig seed = new ModelConfig();
        seed.id = "deepseek_default";
        seed.name = "DeepSeek";
        seed.provider = "deepseek";
        seed.role = "通用";
        seed.color = "#4D6BFE";
        seed.isDefault = true;
        List<ModelConfig> list = new ArrayList<>();
        list.add(seed);
        save(list);
    }

    public List<ModelConfig> list() {
        List<ModelConfig> result = new ArrayList<>();
        try {
            String json = prefs.getString(KEY, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(ModelConfig.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public String listJson() {
        try {
            JSONArray arr = new JSONArray();
            for (ModelConfig mc : list()) arr.put(mc.toJson());
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    public ModelConfig get(String id) {
        for (ModelConfig mc : list()) {
            if (mc.id.equals(id)) return mc;
        }
        return null;
    }

    public ModelConfig getDefault() {
        for (ModelConfig mc : list()) {
            if (mc.isDefault) return mc;
        }
        List<ModelConfig> all = list();
        return all.isEmpty() ? null : all.get(0);
    }

    public String add(ModelConfig mc) {
        List<ModelConfig> models = list();
        if (mc.id == null || mc.id.isEmpty()) {
            mc.id = "model_" + System.currentTimeMillis();
        }
        models.add(mc);
        save(models);
        return mc.id;
    }

    public boolean update(ModelConfig mc) {
        List<ModelConfig> models = list();
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i).id.equals(mc.id)) {
                models.set(i, mc);
                save(models);
                return true;
            }
        }
        return false;
    }

    public boolean delete(String id) {
        List<ModelConfig> models = list();
        if (models.size() <= 1) return false;  // 至少保留一个
        boolean removed = false;
        for (int i = models.size() - 1; i >= 0; i--) {
            if (models.get(i).id.equals(id)) {
                models.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            // 如果删的是默认，把第一个设为默认
            if (!models.isEmpty() && models.stream().noneMatch(m -> m.isDefault)) {
                models.get(0).isDefault = true;
            }
            save(models);
        }
        return removed;
    }

    public boolean setDefault(String id) {
        List<ModelConfig> models = list();
        boolean found = false;
        for (ModelConfig mc : models) {
            if (mc.id.equals(id)) {
                mc.isDefault = true;
                found = true;
            } else {
                mc.isDefault = false;
            }
        }
        if (found) save(models);
        return found;
    }

    private void save(List<ModelConfig> models) {
        try {
            JSONArray arr = new JSONArray();
            for (ModelConfig mc : models) arr.put(mc.toJson());
            prefs.edit().putString(KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
