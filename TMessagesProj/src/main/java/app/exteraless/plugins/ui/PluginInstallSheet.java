package app.exteraless.plugins.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.exteraless.plugins.Plugin;
import app.exteraless.plugins.PluginCapabilityScan;
import app.exteraless.plugins.PluginPermissions;

/**
 * Лист установки плагина.
 *
 * Раньше здесь был системный AlertDialog со списком галочек: он не показывал ни
 * иконки, ни описания, а на длинном списке разрешений упирался в собственную
 * высоту. exteraGram ({@code plugins/ui/components/InstallPluginBottomSheet}) на
 * этом месте показывает карточку — иконка, имя, версия и автор, описание, — и
 * ставит рядом с кнопкой отдельную галочку «включить после установки».
 *
 * Наше отличие от exteraGram одно и намеренное: в списке под описанием стоят
 * разрешения, которые нашёл статический разбор, и у каждого своя раскрывашка с
 * именами из исходника. exteraGram вместо этого показывает бейдж «источник
 * неизвестен»; бейдж говорит о канале, а не о плагине, и ничего не решает.
 */
public class PluginInstallSheet extends BottomSheet {

    public interface Delegate {
        /**
         * @param granted           отмеченные разрешения
         * @param enableAfterInstall включать ли плагин сразу после установки
         */
        void onInstall(List<String> granted, boolean enableAfterInstall);
    }

    private final List<PluginPermissionCell> cells = new ArrayList<>();
    private boolean enableAfterInstall = true;

    public PluginInstallSheet(Activity activity, File file, Plugin plugin,
                              Map<String, List<String>> capabilities, Delegate delegate) {
        super(activity, false);
        setApplyBottomPadding(false);
        setApplyTopPadding(false);

        final Context context = activity;
        final List<String> permissions = PluginCapabilityScan.ordered(capabilities);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        content.addView(createIcon(context, plugin),
                LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL, 0, 22, 0, 0));

        TextView name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        name.setTypeface(AndroidUtilities.bold());
        name.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        name.setGravity(Gravity.CENTER);
        name.setText(plugin != null ? plugin.getDisplayName() : file.getName());
        content.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 40, 14, 40, 0));

        TextView subtitle = new TextView(context);
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setText(buildSubtitle(plugin));
        content.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 21, 4, 21, 0));

        if (plugin != null && !TextUtils.isEmpty(plugin.description)) {
            LinkSpanDrawable.LinksTextView description =
                    new LinkSpanDrawable.LinksTextView(context);
            description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            description.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            description.setGravity(Gravity.CENTER);
            description.setText(com.exteragram.messenger.utils.text.LocaleUtils
                    .fullyFormatText(plugin.description));
            content.addView(description, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT, 21, 18, 21, 0));
        }

        TextView note = new TextView(context);
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        note.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        note.setText(getString(plugin == null || TextUtils.isEmpty(plugin.id)
                ? R.string.PluginsInstallUnknownConfirm
                : permissions.isEmpty()
                    ? R.string.PluginsInstallNothingFound
                    : R.string.PluginsInstallScanned));
        content.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, 21, 20, 21, permissions.isEmpty() ? 0 : 6));

        for (int i = 0; i < permissions.size(); i++) {
            final String permission = permissions.get(i);
            PluginPermissionCell cell = new PluginPermissionCell(context,
                    PluginPermissionCell.TYPE_CHECKBOX);
            cell.set(permission,
                    PluginPermissionsActivity.shortTitleOf(permission),
                    PluginPermissionsActivity.infoOf(permission),
                    PluginCapabilityScan.evidenceOf(capabilities, permission),
                    i < permissions.size() - 1);
            cell.setChecked(!PluginPermissions.isDangerous(permission), false);
            cell.setOnToggle(() -> cell.setChecked(!cell.isChecked(), true));
            cells.add(cell);
            content.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT));
        }

        ButtonWithCounterView button = new ButtonWithCounterView(context, true, null);
        button.setText(getString(R.string.PluginsInstallAction), false);
        button.setOnClickListener(v -> {
            dismiss();
            if (delegate != null) {
                delegate.onInstall(checkedPermissions(), enableAfterInstall);
            }
        });
        content.addView(button, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48,
                16, permissions.isEmpty() ? 20 : 14, 16, 10));

        content.addView(createEnableAfterInstall(context),
                LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);
        setCustomView(scroll);
    }

    /**
     * Иконка: стикер из набора плагина, если он его указал, иначе наш значок.
     *
     * Загрузка идёт сетевым запросом за набором стикеров, поэтому картинка
     * появляется позже остального листа — это нормально и лучше, чем держать
     * лист закрытым до её приезда.
     */
    private static android.view.View createIcon(Context context, Plugin plugin) {
        FrameLayout frame = new FrameLayout(context);
        org.telegram.ui.Components.BackupImageView image =
                new org.telegram.ui.Components.BackupImageView(context);
        ImageView fallback = new ImageView(context);
        fallback.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fallback.setImageResource(R.drawable.msg_plugins);
        fallback.setColorFilter(new PorterDuffColorFilter(
                Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.SRC_IN));
        frame.addView(fallback, LayoutHelper.createFrame(56, 56, Gravity.CENTER));
        frame.addView(image, LayoutHelper.createFrame(72, 72, Gravity.CENTER));
        image.setVisibility(android.view.View.GONE);
        // Наш значок стоит до тех пор, пока не приедет иконка плагина: она
        // может и не приехать, а пустое место вместо неё — хуже заглушки.
        PluginIcons.apply(image, plugin, () -> fallback.setVisibility(android.view.View.GONE));
        return frame;
    }

    private static CharSequence buildSubtitle(Plugin plugin) {
        if (plugin == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(LocaleController.formatString(R.string.PluginsInstallVersion,
                plugin.version != null ? plugin.version : "1.0"));
        if (!TextUtils.isEmpty(plugin.author)) {
            sb.append(" • ").append(plugin.author);
        }
        return sb;
    }

    private android.view.View createEnableAfterInstall(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        CheckBox2 checkBox = new CheckBox2(context, 21);
        checkBox.setColor(Theme.key_checkbox, Theme.key_checkboxDisabled, Theme.key_checkboxCheck);
        checkBox.setDrawUnchecked(true);
        checkBox.setDrawBackgroundAsArc(10);
        checkBox.setChecked(enableAfterInstall, false);
        row.addView(checkBox, LayoutHelper.createLinear(21, 21, Gravity.CENTER_VERTICAL, 0, 0, 8, 0));

        TextView text = new TextView(context);
        text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        text.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        text.setText(getString(R.string.PluginsEnableAfterInstallation));
        row.addView(text, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        row.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6),
                AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        row.setOnClickListener(v -> {
            enableAfterInstall = !enableAfterInstall;
            checkBox.setChecked(enableAfterInstall, true);
        });
        return row;
    }

    private List<String> checkedPermissions() {
        List<String> granted = new ArrayList<>();
        for (PluginPermissionCell cell : cells) {
            if (cell.isChecked() && cell.getPermission() != null) {
                granted.add(cell.getPermission());
            }
        }
        return granted;
    }
}
