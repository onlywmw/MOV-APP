package com.hermes.android;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hermes.android.ai.AiProviderConfig;
import com.hermes.android.model.ModelConfig;
import com.hermes.android.model.ModelRegistry;

import java.util.List;

/**
 * 设置页: 多模型管理 + 语言 + 统计。
 * 模型列表 → 点击编辑 → 保存/测试/删除。
 */
public class HermesSettingsActivity extends AppCompatActivity {

    private static final String[] PROVIDER_NAMES = {"DeepSeek", "OpenAI", "通义千问", "Ollama"};
    private static final String[] PROVIDER_VALUES = {"deepseek", "openai", "qwen", "ollama"};
    private static final String[] ROLE_NAMES = {"通用", "产品", "技术", "数据", "自定义"};
    private static final String[] LANG_NAMES = {"中文", "English"};
    private static final String[] LANG_VALUES = {"zh", "en"};

    private ModelRegistry registry;
    private AiProviderConfig aiConfig;
    private StatsCollector stats;

    private LinearLayout modelListContainer;
    private LinearLayout editForm;
    private TextView tvEditTitle;
    private EditText etName, etBaseUrl, etApiKey, etModel;
    private Spinner spinnerProvider, spinnerRole, spinnerLanguage;

    /** 当前编辑的模型 ID, null = 新建 */
    private String editingId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hermes_settings);

        registry = new ModelRegistry(this);
        aiConfig = new AiProviderConfig(this);
        stats = new StatsCollector(this);

        modelListContainer = findViewById(R.id.modelListContainer);
        editForm = findViewById(R.id.editForm);
        tvEditTitle = findViewById(R.id.tvEditTitle);
        etName = findViewById(R.id.etName);
        etBaseUrl = findViewById(R.id.etBaseUrl);
        etApiKey = findViewById(R.id.etApiKey);
        etModel = findViewById(R.id.etModel);
        spinnerProvider = findViewById(R.id.spinnerProvider);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        // Spinners
        spinnerProvider.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, PROVIDER_NAMES));
        spinnerRole.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ROLE_NAMES));
        spinnerLanguage.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, LANG_NAMES));

        // 语言
        String lang = aiConfig.getLanguage();
        for (int i = 0; i < LANG_VALUES.length; i++) {
            if (LANG_VALUES[i].equals(lang)) { spinnerLanguage.setSelection(i, false); break; }
        }
        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                aiConfig.setLanguage(LANG_VALUES[pos]);
            }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // 统计
        Switch switchStats = findViewById(R.id.switchStats);
        switchStats.setChecked(stats.isEnabled());
        switchStats.setOnCheckedChangeListener((v, checked) -> stats.setEnabled(checked));
        findViewById(R.id.btnStatsPreview).setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("即将上报的数据")
                    .setMessage(stats.getPreviewJson())
                    .setPositiveButton("关闭", null)
                    .show();
        });

        // 添加模型
        findViewById(R.id.btnAddModel).setOnClickListener(v -> openEditForm(null));

        // 编辑表单按钮
        findViewById(R.id.btnSaveModel).setOnClickListener(v -> saveModel());
        findViewById(R.id.btnTestModel).setOnClickListener(v -> testModel());
        findViewById(R.id.btnCancelEdit).setOnClickListener(v -> closeEditForm());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        refreshStatus();
        renderModelList();
    }

    /* ── 模型列表渲染 ── */

    private void renderModelList() {
        modelListContainer.removeAllViews();
        List<ModelConfig> models = registry.list();
        for (ModelConfig m : models) {
            modelListContainer.addView(buildModelCard(m));
        }
    }

    private View buildModelCard(ModelConfig m) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(getDrawable(R.drawable.bg_input));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        card.setLayoutParams(lp);

        // 第一行: 名称 + 默认标签
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(m.name.isEmpty() ? m.getProviderDisplayName() : m.name);
        name.setTextColor(getColor(R.color.text_primary));
        name.setTextSize(15);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        row1.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        if (m.isDefault) {
            TextView def = new TextView(this);
            def.setText("默认");
            def.setTextColor(getColor(R.color.accent_light));
            def.setTextSize(11);
            def.setBackground(getDrawable(R.drawable.bg_chip));
            def.setPadding(dp(8), dp(2), dp(8), dp(2));
            row1.addView(def);
        }
        card.addView(row1);

        // 第二行: model · 状态
        TextView sub = new TextView(this);
        String status = m.isConfigured()
                ? m.getEffectiveModel() + " · 已配置"
                : "未配置 Key";
        sub.setText(m.getProviderDisplayName() + " · " + status + " · 角色: " + m.role);
        sub.setTextColor(getColor(R.color.text_secondary));
        sub.setTextSize(12);
        sub.setPadding(0, dp(4), 0, 0);
        card.addView(sub);

        // 第三行: 操作按钮
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setPadding(0, dp(8), 0, 0);

        row3.addView(makeSmallBtn("编辑", v -> openEditForm(m.id)));
        row3.addView(makeSmallBtn("测试", v -> {
            Toast.makeText(this, "测试中…", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                com.hermes.android.ai.AiClient client =
                        new com.hermes.android.ai.AiClient(m, "回复 OK");
                com.hermes.android.ai.AiClient.AiResponse resp = client.chat("ping");
                runOnUiThread(() -> Toast.makeText(this,
                        resp.success ? "连接正常" : "失败: " + resp.content,
                        Toast.LENGTH_SHORT).show());
            }).start();
        }));
        if (!m.isDefault) {
            row3.addView(makeSmallBtn("设为默认", v -> {
                registry.setDefault(m.id);
                renderModelList();
                refreshStatus();
            }));
        }
        if (registry.list().size() > 1) {
            row3.addView(makeSmallBtn("删除", v -> {
                registry.delete(m.id);
                renderModelList();
                refreshStatus();
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
            }));
        }
        card.addView(row3);
        return card;
    }

    private Button makeSmallBtn(String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(getColor(R.color.text_secondary));
        btn.setTextSize(11);
        btn.setAllCaps(false);
        btn.setBackground(getDrawable(R.drawable.bg_chip));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(34));
        lp.rightMargin = dp(6);
        btn.setLayoutParams(lp);
        btn.setPadding(dp(10), 0, dp(10), 0);
        btn.setOnClickListener(listener);
        return btn;
    }

    /* ── 编辑表单 ── */

    private void openEditForm(String modelId) {
        editingId = modelId;
        if (modelId != null) {
            ModelConfig m = registry.get(modelId);
            if (m == null) return;
            tvEditTitle.setText("编辑模型");
            etName.setText(m.name);
            etBaseUrl.setText(m.baseUrl);
            etApiKey.setText(m.apiKey);
            etModel.setText(m.model);
            for (int i = 0; i < PROVIDER_VALUES.length; i++) {
                if (PROVIDER_VALUES[i].equals(m.provider)) { spinnerProvider.setSelection(i, false); break; }
            }
            for (int i = 0; i < ROLE_NAMES.length; i++) {
                if (ROLE_NAMES[i].equals(m.role)) { spinnerRole.setSelection(i, false); break; }
            }
        } else {
            tvEditTitle.setText("添加模型");
            etName.setText("");
            etBaseUrl.setText("");
            etApiKey.setText("");
            etModel.setText("");
            spinnerProvider.setSelection(0, false);
            spinnerRole.setSelection(0, false);
        }
        editForm.setVisibility(View.VISIBLE);
        editForm.requestFocus();
    }

    private void closeEditForm() {
        editForm.setVisibility(View.GONE);
        editingId = null;
    }

    private void saveModel() {
        ModelConfig m = editingId != null ? registry.get(editingId) : new ModelConfig();
        if (m == null) return;

        m.name = etName.getText().toString().trim();
        m.provider = PROVIDER_VALUES[spinnerProvider.getSelectedItemPosition()];
        m.baseUrl = etBaseUrl.getText().toString().trim();
        m.apiKey = etApiKey.getText().toString().trim();
        m.model = etModel.getText().toString().trim();
        m.role = ROLE_NAMES[spinnerRole.getSelectedItemPosition()];

        if (m.name.isEmpty()) m.name = m.getProviderDisplayName();

        if (editingId != null) {
            registry.update(m);
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        } else {
            registry.add(m);
            Toast.makeText(this, "已添加: " + m.name, Toast.LENGTH_SHORT).show();
        }
        closeEditForm();
        renderModelList();
        refreshStatus();
    }

    private void testModel() {
        ModelConfig m = new ModelConfig();
        m.provider = PROVIDER_VALUES[spinnerProvider.getSelectedItemPosition()];
        m.baseUrl = etBaseUrl.getText().toString().trim();
        m.apiKey = etApiKey.getText().toString().trim();
        m.model = etModel.getText().toString().trim();

        Toast.makeText(this, "测试中…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            com.hermes.android.ai.AiClient client =
                    new com.hermes.android.ai.AiClient(m, "回复 OK");
            long start = System.currentTimeMillis();
            com.hermes.android.ai.AiClient.AiResponse resp = client.chat("ping");
            long ms = System.currentTimeMillis() - start;
            runOnUiThread(() -> Toast.makeText(this,
                    resp.success ? "连接正常 · " + ms + "ms" : "失败: " + resp.content,
                    Toast.LENGTH_SHORT).show());
        }).start();
    }

    /* ── 状态 ── */

    private void refreshStatus() {
        List<ModelConfig> models = registry.list();
        ModelConfig def = registry.getDefault();
        String status = models.size() + " 个模型";
        if (def != null) status += " · 默认: " + (def.name.isEmpty() ? def.getProviderDisplayName() : def.name);
        ((TextView) findViewById(R.id.tvSettingsStatus)).setText(status);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
