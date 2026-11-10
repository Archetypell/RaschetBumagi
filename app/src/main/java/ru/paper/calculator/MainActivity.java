package ru.paper.calculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final double CORE_KG = 5.0;
    private static final String PREFS = "paper_calc";
    private static final String HISTORY_KEY = "history";

    private EditText coilMassInput, grammageInput, widthInput, lengthInput;
    private Spinner massUnitSpinner, widthUnitSpinner, lengthUnitSpinner;
    private LinearLayout historyContainer;
    private TextView resultText, detailText;
    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.##",
            DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        renderHistory();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244, 247, 250));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(22), dp(18), dp(28));
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("РАСЧЁТ БУМАГИ");
        title.setTextSize(25);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(24, 47, 72));
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Расчёт выхода листов из бумажной катушки");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.rgb(95, 111, 126));
        subtitle.setPadding(0, dp(5), 0, dp(18));
        page.addView(subtitle);

        LinearLayout inputCard = card();
        page.addView(inputCard);

        addSectionTitle(inputCard, "ПАРАМЕТРЫ КАТУШКИ");
        coilMassInput = addInput(inputCard, "Масса катушки вместе с втулкой", "Например, 1250");
        massUnitSpinner = addSpinner(inputCard, "Единица массы", new String[]{"кг", "г"});
        TextView core = new TextView(this);
        core.setText("Масса втулки вычитается автоматически: 5 кг");
        core.setTextSize(13);
        core.setTextColor(Color.rgb(30, 112, 87));
        core.setPadding(0, dp(4), 0, dp(14));
        inputCard.addView(core);

        addSectionTitle(inputCard, "ПАРАМЕТРЫ БУМАГИ");
        grammageInput = addInput(inputCard, "Граммаж бумаги (г/м²)", "Например, 80");
        widthInput = addInput(inputCard, "Ширина бумаги", "Например, 840");
        widthUnitSpinner = addSpinner(inputCard, "Единица ширины", new String[]{"мм", "см", "м"});
        lengthInput = addInput(inputCard, "Длина одного листа", "Например, 594");
        lengthUnitSpinner = addSpinner(inputCard, "Единица длины", new String[]{"мм", "см", "м"});

        Button calculate = button("РАССЧИТАТЬ ЛИСТЫ", Color.rgb(23, 104, 171));
        LinearLayout.LayoutParams calcParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        calcParams.topMargin = dp(16);
        inputCard.addView(calculate, calcParams);
        calculate.setOnClickListener(v -> calculate());

        LinearLayout resultCard = card();
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resultParams.topMargin = dp(16);
        page.addView(resultCard, resultParams);

        addSectionTitle(resultCard, "РЕЗУЛЬТАТ");
        resultText = new TextView(this);
        resultText.setText("— листов");
        resultText.setTextSize(30);
        resultText.setTypeface(null, Typeface.BOLD);
        resultText.setTextColor(Color.rgb(23, 104, 171));
        resultText.setGravity(Gravity.CENTER);
        resultText.setPadding(0, dp(8), 0, dp(8));
        resultCard.addView(resultText);

        detailText = new TextView(this);
        detailText.setText("Введите параметры катушки и нажмите «Рассчитать листы».");
        detailText.setTextSize(14);
        detailText.setTextColor(Color.rgb(75, 91, 106));
        detailText.setGravity(Gravity.CENTER);
        detailText.setPadding(dp(4), 0, dp(4), dp(8));
        resultCard.addView(detailText);

        LinearLayout historyCard = card();
        LinearLayout.LayoutParams histParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        histParams.topMargin = dp(16);
        page.addView(historyCard, histParams);

        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        historyHeader.setOrientation(LinearLayout.HORIZONTAL);
        historyCard.addView(historyHeader);

        TextView historyTitle = new TextView(this);
        historyTitle.setText("ИСТОРИЯ РАСЧЁТОВ");
        historyTitle.setTextSize(16);
        historyTitle.setTypeface(null, Typeface.BOLD);
        historyTitle.setTextColor(Color.rgb(24, 47, 72));
        historyHeader.addView(historyTitle, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView clear = new TextView(this);
        clear.setText("Очистить");
        clear.setTextSize(13);
        clear.setTextColor(Color.rgb(190, 70, 70));
        clear.setPadding(dp(8), dp(6), 0, dp(6));
        historyHeader.addView(clear);
        clear.setOnClickListener(v -> confirmClearHistory());

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyContainer.setPadding(0, dp(10), 0, 0);
        historyCard.addView(historyContainer);

        TextView note = new TextView(this);
        note.setText("Расчёт теоретический: без учёта обрезки, брака, влажности и технологических потерь.");
        note.setTextSize(12);
        note.setTextColor(Color.rgb(115, 125, 135));
        note.setPadding(dp(4), dp(14), dp(4), 0);
        page.addView(note);

        setContentView(scroll);
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        box.setBackground(bg);
        if (android.os.Build.VERSION.SDK_INT >= 21) box.setElevation(dp(2));
        return box;
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(Color.rgb(90, 108, 125));
        t.setPadding(0, dp(2), 0, dp(10));
        parent.addView(t);
    }

    private EditText addInput(LinearLayout parent, String label, String hint) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(Color.rgb(42, 57, 72));
        l.setPadding(0, dp(5), 0, dp(5));
        parent.addView(l);

        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(16);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setPadding(dp(12), 0, dp(12), 0);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(150, 165, 178)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        p.bottomMargin = dp(8);
        parent.addView(e, p);
        return e;
    }

    private Spinner addSpinner(LinearLayout parent, String label, String[] options) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(13);
        l.setTextColor(Color.rgb(95, 111, 126));
        l.setPadding(0, dp(2), 0, dp(2));
        parent.addView(l);

        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        p.bottomMargin = dp(8);
        parent.addView(s, p);
        return s;
    }

    private Button button(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        return b;
    }

    private void calculate() {
        try {
            double enteredMass = parse(coilMassInput, "массу катушки");
            double grammage = parse(grammageInput, "граммаж");
            double width = parse(widthInput, "ширину бумаги");
            double length = parse(lengthInput, "длину листа");

            double coilKg = enteredMass;
            if (massUnitSpinner.getSelectedItemPosition() == 1) coilKg /= 1000.0;
            double widthM = toMeters(width, widthUnitSpinner.getSelectedItemPosition());
            double lengthM = toMeters(length, lengthUnitSpinner.getSelectedItemPosition());

            if (coilKg <= CORE_KG) {
                throw new IllegalArgumentException("Масса катушки должна быть больше 5 кг (массы втулки).");
            }
            if (grammage <= 0 || widthM <= 0 || lengthM <= 0) {
                throw new IllegalArgumentException("Граммаж, ширина и длина должны быть больше нуля.");
            }

            double paperKg = coilKg - CORE_KG;
            double sheetKg = grammage * widthM * lengthM / 1000.0;
            long sheets = (long) Math.floor((paperKg / sheetKg) + 1e-10);
            double remainderG = (paperKg * 1000.0) - sheets * sheetKg * 1000.0;

            resultText.setText(numberFormat.format(sheets) + " листов");
            detailText.setText("Чистая масса бумаги: " + numberFormat.format(paperKg) + " кг\n"
                    + "Остаток после целых листов: " + numberFormat.format(remainderG) + " г\n"
                    + "Масса одного листа: " + numberFormat.format(sheetKg * 1000.0) + " г");

            hideKeyboard();
            saveHistory(coilKg, grammage, widthM, lengthM, sheets, paperKg, remainderG);
            renderHistory();
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "Проверьте введённые значения." : ex.getMessage();
            new AlertDialog.Builder(this)
                    .setTitle("Не удалось рассчитать")
                    .setMessage(message)
                    .setPositiveButton("Понятно", null)
                    .show();
        }
    }

    private double parse(EditText field, String name) {
        String value = field.getText().toString().trim().replace(',', '.').replace(" ", "");
        if (value.isEmpty()) throw new IllegalArgumentException("Введите " + name + ".");
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed <= 0)
                throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Проверьте значение: " + name + ".");
        }
    }

    private double toMeters(double value, int unitPosition) {
        if (unitPosition == 0) return value / 1000.0; // mm
        if (unitPosition == 1) return value / 100.0;  // cm
        return value;                                  // m
    }

    private void saveHistory(double coilKg, double gsm, double widthM, double lengthM,
                             long sheets, double paperKg, double remainderG) {
        try {
            JSONArray history = new JSONArray(getPreferences(MODE_PRIVATE).getString(HISTORY_KEY, "[]"));
            JSONObject item = new JSONObject();
            item.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru", "RU")).format(new Date()));
            item.put("coilKg", coilKg);
            item.put("gsm", gsm);
            item.put("widthM", widthM);
            item.put("lengthM", lengthM);
            item.put("sheets", sheets);
            item.put("paperKg", paperKg);
            item.put("remainderG", remainderG);
            history.put(item);
            JSONArray limited = new JSONArray();
            int start = Math.max(0, history.length() - 50);
            for (int i = start; i < history.length(); i++) limited.put(history.get(i));
            getPreferences(MODE_PRIVATE).edit().putString(HISTORY_KEY, limited.toString()).apply();
        } catch (Exception ignored) { }
    }

    private void renderHistory() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews();
        try {
            JSONArray history = new JSONArray(getPreferences(MODE_PRIVATE).getString(HISTORY_KEY, "[]"));
            if (history.length() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Пока нет сохранённых расчётов.");
                empty.setTextSize(14);
                empty.setTextColor(Color.rgb(120, 130, 140));
                historyContainer.addView(empty);
                return;
            }
            for (int i = history.length() - 1; i >= 0; i--) {
                JSONObject item = history.getJSONObject(i);
                TextView row = new TextView(this);
                row.setText(item.optString("date") + "\n"
                        + "Катушка: " + numberFormat.format(item.optDouble("coilKg")) + " кг"
                        + "  •  Бумага: " + numberFormat.format(item.optDouble("paperKg")) + " кг\n"
                        + numberFormat.format(item.optDouble("gsm")) + " г/м²"
                        + "  •  " + numberFormat.format(item.optDouble("widthM") * 1000) + " × "
                        + numberFormat.format(item.optDouble("lengthM") * 1000) + " мм"
                        + "\nРезультат: " + numberFormat.format(item.optLong("sheets")) + " листов"
                        + "  •  Остаток: " + numberFormat.format(item.optDouble("remainderG")) + " г");
                row.setTextSize(13);
                row.setTextColor(Color.rgb(53, 68, 82));
                row.setPadding(0, dp(10), 0, dp(10));
                historyContainer.addView(row);
                if (i > 0) {
                    View divider = new View(this);
                    divider.setBackgroundColor(Color.rgb(230, 235, 239));
                    historyContainer.addView(divider, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
                }
            }
        } catch (Exception ignored) { }
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Очистить историю?")
                .setMessage("Все сохранённые расчёты будут удалены.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Очистить", (d, w) -> {
                    getPreferences(MODE_PRIVATE).edit().remove(HISTORY_KEY).apply();
                    renderHistory();
                    Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(coilMassInput.getWindowToken(), 0);
        } catch (Exception ignored) { }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
