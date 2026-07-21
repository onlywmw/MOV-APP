package com.hermes.android.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.hermes.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RemoteViewsService that provides the list of quick commands for the widget.
 */
public class HermesWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CommandListFactory(getApplicationContext());
    }

    static class CommandListFactory implements RemoteViewsFactory {

        private final Context context;
        private final List<QuickCommand> commands = new ArrayList<>();

        CommandListFactory(Context context) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            loadCommands();
        }

        @Override
        public void onDataSetChanged() {
            loadCommands();
        }

        private void loadCommands() {
            commands.clear();
            commands.add(new QuickCommand("🔦 打开手电筒", "打开手电筒"));
            commands.add(new QuickCommand("🔦 关闭手电筒", "关闭手电筒"));
            commands.add(new QuickCommand("🔋 电量查询", "电量多少"));
            commands.add(new QuickCommand("🔊 当前音量", "当前音量"));
            commands.add(new QuickCommand("📶 WiFi 状态", "WiFi状态"));
            commands.add(new QuickCommand("📳 震动", "震动"));
            commands.add(new QuickCommand("🔆 亮度调到 128", "亮度调到 128"));
            commands.add(new QuickCommand("📱 设备信息", "设备信息"));
            commands.add(new QuickCommand("📸 截屏", "截屏"));
            commands.add(new QuickCommand("🌐 IP 地址", "ip地址"));
            commands.add(new QuickCommand("📱 应用列表", "应用列表"));
            commands.add(new QuickCommand("👤 联系人", "联系人"));
            commands.add(new QuickCommand("📩 最近短信", "最近短信"));
            commands.add(new QuickCommand("📋 读取剪贴板", "读取剪贴板"));
        }

        @Override
        public int getCount() {
            return commands.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position >= commands.size()) return null;

            QuickCommand cmd = commands.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.hermes_widget_item);
            views.setTextViewText(R.id.widget_item_text, cmd.label);

            // Fill-in intent carries the command string
            Intent fillIn = new Intent();
            fillIn.putExtra(HermesWidgetProvider.EXTRA_COMMAND, cmd.command);
            views.setOnClickFillInIntent(R.id.widget_item_root, fillIn);

            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public void onDestroy() {
            commands.clear();
        }
    }

    static class QuickCommand {
        final String label;
        final String command;

        QuickCommand(String label, String command) {
            this.label = label;
            this.command = command;
        }
    }
}
