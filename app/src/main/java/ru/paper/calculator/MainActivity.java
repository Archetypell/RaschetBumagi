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
    private static final String HISTORY_KEY = "history";
    private static final int BG = Color.rgb(25, 29, 35);
    private static final int CARD = Color.rgb(38, 44, 52);
    private static final int TEXT = Color.rgb(235, 239, 243);
    private static final int MUTED = Color.rgb(165, 175, 186);
    private static final int ACCENT = Color.rgb(65, 150, 210);

    private EditText coilMassInput, grammageInput, widthInput, lengthInput;
    private Spinner massUnitSpinner, widthUnitSpinner, lengthUnitSpinner;
    private LinearLayout historyContainer;
    private TextView resultText, detailText;
    private EditText resinLength, resinWidth, resinSheets, resinGsm, resinOsm;
    private TextView resinResult;
    private EditText labPaperBase, labAStart, labAEnd, labBStart, labBEnd, flowStart, flowEnd;
    private TextView labResult;
    private final DecimalFormat numberFormat = new DecimalFormat("#,##0.##",
            DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        LinearLayout page = newPage();
        addTitle(page, "РАСЧЁТ БУМАГИ", "Производственные калькуляторы");
        addHomeCard(page, "РАСЧЁТ ЛИСТОВ", () -> buildPaperScreen());
        addHomeCard(page, "РАСЧЁТ СМОЛЫ", () -> buildResinScreen());
        addHomeCard(page, "ЛАБОРАТОРИЯ", () -> buildLabScreen());
        Button history = button("ИСТОРИЯ РАСЧЁТОВ", ACCENT);
        page.addView(history, buttonParams());
        history.setOnClickListener(v -> buildPaperScreen());
        setContentView(wrap(page));
    }

    private void addHomeCard(LinearLayout page, String title, Runnable action) {
        LinearLayout c = card();
        page.addView(c, spacedParams(12));
        TextView t = text(title, 17, TEXT, true);
        c.addView(t);
        Button b = button("ОТКРЫТЬ", ACCENT);
        c.addView(b, buttonParams());
        b.setOnClickListener(v -> action.run());
        c.setOnClickListener(v -> action.run());
    }

    private void buildPaperScreen() {
        LinearLayout page = newPage();
        addNavigation(page, "РАСЧЁТ ЛИСТОВ");
        LinearLayout inputCard = card(); page.addView(inputCard);
        addSectionTitle(inputCard, "ПАРАМЕТРЫ КАТУШКИ");
        coilMassInput = addInput(inputCard, "Масса катушки вместе с втулкой, кг или г", "Например, 1250");
        massUnitSpinner = addSpinner(inputCard, "Единица массы", new String[]{"кг", "г"});
        addSectionTitle(inputCard, "ПАРАМЕТРЫ БУМАГИ");
        grammageInput = addInput(inputCard, "Граммаж бумаги (г/м²)", "Например, 80");
        widthInput = addInput(inputCard, "Ширина бумаги", "Например, 840");
        widthUnitSpinner = addSpinner(inputCard, "Единица ширины", new String[]{"мм", "см", "м"});
        lengthInput = addInput(inputCard, "Длина одного листа", "Например, 594");
        lengthUnitSpinner = addSpinner(inputCard, "Единица длины", new String[]{"мм", "см", "м"});
        Button calculate = button("РАССЧИТАТЬ ЛИСТЫ", ACCENT); inputCard.addView(calculate, buttonParams());
        calculate.setOnClickListener(v -> calculate());

        LinearLayout resultCard = card(); page.addView(resultCard, spacedParams(12));
        addSectionTitle(resultCard, "РЕЗУЛЬТАТ");
        resultText = text("— листов", 28, ACCENT, true); resultText.setGravity(Gravity.CENTER);
        resultCard.addView(resultText);
        detailText = text("", 13, MUTED, false);
        detailText.setGravity(Gravity.CENTER); resultCard.addView(detailText);

        LinearLayout historyCard = card(); page.addView(historyCard, spacedParams(12));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); historyCard.addView(header);
        TextView ht = text("ИСТОРИЯ РАСЧЁТОВ", 15, TEXT, true); header.addView(ht, new LinearLayout.LayoutParams(0,-2,1));
        TextView clear = text("Очистить", 13, Color.rgb(230,110,110), false); header.addView(clear);
        clear.setOnClickListener(v -> confirmClearHistory());
        historyContainer = new LinearLayout(this); historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyContainer.setPadding(0, dp(8), 0, 0); historyCard.addView(historyContainer); renderHistory();
        setContentView(wrap(page));
    }

    private void buildResinScreen() {
        LinearLayout page = newPage(); addNavigation(page, "РАСЧЁТ СМОЛЫ");
        LinearLayout c = card(); page.addView(c);
        addSectionTitle(c, "ПАРАМЕТРЫ ЗАКАЗА");
        resinLength = addInput(c, "Длина листа (м)", "3,11"); resinLength.setText("3,11");
        resinWidth = addInput(c, "Ширина листа (мм)", "");
        resinSheets = addInput(c, "Количество листов", "Например, 500");
        resinGsm = addInput(c, "Граммаж бумаги (г/м²)", "Например, 80");
        resinOsm = addInput(c, "Осмоление (%)", "Обычно 50–60");
        Button calc = button("РАССЧИТАТЬ РАСХОД СМОЛЫ", ACCENT); c.addView(calc, buttonParams());
        calc.setOnClickListener(v -> calculateResin());
        LinearLayout out = card(); page.addView(out, spacedParams(12));
        addSectionTitle(out, "РАСХОД СМОЛЫ");
        resinResult = text("— кг", 30, ACCENT, true); resinResult.setGravity(Gravity.CENTER); out.addView(resinResult);
        setContentView(wrap(page));
    }

    private void calculateResin() {
        try {
            double l = parse(resinLength, "длину листа");
            double w = parse(resinWidth, "ширину листа") / 1000.0;
            double n = parse(resinSheets, "количество листов");
            double gsm = parse(resinGsm, "граммаж");
            double osm = parse(resinOsm, "осмоление");
            if (osm <= 0 || osm >= 100) throw new IllegalArgumentException("Осмоление должно быть больше 0 и меньше 100%.");
            double paperKg = n * l * w * gsm / 1000.0;
            double resinKg = paperKg * osm / (100.0 - osm);
            resinResult.setText(numberFormat.format(resinKg) + " кг");
            hideKeyboard(resinLength);
        } catch (Exception ex) { showError(ex); }
    }

    private void buildLabScreen() {
        LinearLayout page = newPage(); addNavigation(page, "ЛАБОРАТОРИЯ");
        LinearLayout samples = card(); page.addView(samples);
        addSectionTitle(samples, "ПРОБЫ С ДВУХ СТОРОН ПОЛОТНА");
        labPaperBase = addInput(samples, "Масса бумаги до пропитки (г)", "");
        labAStart = addInput(samples, "Операторская сторона — масса сразу после отбора (г)", "");
        labAEnd = addInput(samples, "Операторская сторона — масса после 5 минут сушки (г)", "");
        labBStart = addInput(samples, "Неоператорская сторона — масса сразу после отбора (г)", "");
        labBEnd = addInput(samples, "Неоператорская сторона — масса после 5 минут сушки (г)", "");
        LinearLayout flow = card(); page.addView(flow, spacedParams(12));
        addSectionTitle(flow, "ТЕКУЧЕСТЬ");
        flowStart = addInput(flow, "Масса до прессования (г)", "");
        flowEnd = addInput(flow, "Масса после прессования (г)", "");
        Button calc = button("РАССЧИТАТЬ", ACCENT); flow.addView(calc, buttonParams());
        calc.setOnClickListener(v -> calculateLab());
        LinearLayout result = card(); page.addView(result, spacedParams(12));
        addSectionTitle(result, "РЕЗУЛЬТАТЫ");
        labResult = text("—", 16, TEXT, false); result.addView(labResult);
        setContentView(wrap(page));
    }

    private void calculateLab() {
        try {
            double base = parse(labPaperBase, "массу бумаги до пропитки");
            double aBefore = parse(labAStart, "массу операторской стороны сразу после отбора");
            double aAfter = parse(labAEnd, "массу операторской стороны после сушки");
            double bBefore = parse(labBStart, "массу неоператорской стороны сразу после отбора");
            double bAfter = parse(labBEnd, "массу неоператорской стороны после сушки");
            double fBefore = parse(flowStart, "массу до прессования");
            double fAfter = parse(flowEnd, "массу после прессования");
            if (aBefore < aAfter || bBefore < bAfter) {
                throw new IllegalArgumentException("Масса после сушки не может быть больше массы до сушки.");
            }
            if (aBefore <= base || bBefore <= base) {
                throw new IllegalArgumentException("Масса после пропитки должна быть больше исходной массы бумаги.");
            }
            if (fAfter > fBefore) {
                throw new IllegalArgumentException("Масса после прессования не может быть больше массы до прессования.");
            }
            double osmA = (aBefore - base) / base * 100.0;
            double osmB = (bBefore - base) / base * 100.0;
            double volA = (aBefore - aAfter) / aBefore * 100.0;
            double volB = (bBefore - bAfter) / bBefore * 100.0;
            double flow = (fBefore - fAfter) / fBefore * 100.0;
            labResult.setText(
                    "ОПЕРАТОРСКАЯ СТОРОНА\\nОсмоление: " + numberFormat.format(osmA) + " %\\nЛетучие: " + numberFormat.format(volA) + " %\\n\\n" +
                    "НЕОПЕРАТОРСКАЯ СТОРОНА\\nОсмоление: " + numberFormat.format(osmB) + " %\\nЛетучие: " + numberFormat.format(volB) + " %\\n\\n" +
                    "ТЕКУЧЕСТЬ\\n" + numberFormat.format(flow) + " %");
            hideKeyboard(flowEnd);
        } catch (Exception ex) { showError(ex); }
    }

    private void addNavigation(LinearLayout page, String title) {
        Button back = button("‹  ГЛАВНОЕ МЕНЮ", Color.rgb(65,75,87));
        page.addView(back, buttonParams()); back.setOnClickListener(v -> showHome());
        TextView t = text(title, 22, TEXT, true); t.setPadding(0, dp(16), 0, dp(14)); page.addView(t);
    }

    private LinearLayout newPage() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(18), dp(16), dp(28)); return p;
    }
    private ScrollView wrap(LinearLayout page) {
        ScrollView s = new ScrollView(this); s.setFillViewport(true); s.setBackgroundColor(BG); s.addView(page); return s;
    }
    private void addTitle(LinearLayout p, String title, String subtitle) {
        TextView t = text(title, 25, TEXT, true); p.addView(t);
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD); return t;
    }
    private LinearLayout card() {
        LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(15), dp(15), dp(15), dp(15));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(CARD); bg.setCornerRadius(dp(15)); b.setBackground(bg);
        if (android.os.Build.VERSION.SDK_INT >= 21) b.setElevation(dp(2)); return b;
    }
    private LinearLayout.LayoutParams spacedParams(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(top); return p;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50)); p.topMargin = dp(8); return p;
    }
    private void addSectionTitle(LinearLayout parent, String value) {
        TextView t = text(value, 13, MUTED, true); t.setPadding(0, dp(2), 0, dp(10)); parent.addView(t);
    }
    private EditText addInput(LinearLayout parent, String label, String hint) {
        TextView l = text(label, 13, TEXT, false); l.setPadding(0, dp(5), 0, dp(4)); parent.addView(l);
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(16); e.setTextColor(TEXT); 
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setPadding(dp(11), 0, dp(11), 0); e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(46)); p.bottomMargin = dp(7); parent.addView(e,p); return e;
    }
    private Spinner addSpinner(LinearLayout parent, String label, String[] options) {
        TextView l = text(label, 13, MUTED, false); parent.addView(l);
        Spinner s = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(a);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(42)); p.bottomMargin = dp(8); parent.addView(s,p); return s;
    }
    private Button button(String value, int color) {
        Button b = new Button(this); b.setText(value); b.setTextSize(14); b.setTypeface(null, Typeface.BOLD); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(11)); b.setBackground(bg); return b;
    }

    // Existing paper-roll calculation retained.
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
            if (coilKg <= CORE_KG) throw new IllegalArgumentException("Масса катушки должна быть больше 5 кг (массы втулки).");
            if (grammage <= 0 || widthM <= 0 || lengthM <= 0) throw new IllegalArgumentException("Граммаж, ширина и длина должны быть больше нуля.");
            double paperKg = coilKg - CORE_KG;
            double sheetKg = grammage * widthM * lengthM / 1000.0;
            long sheets = (long) Math.floor((paperKg / sheetKg) + 1e-10);
            double remainderG = (paperKg * 1000.0) - sheets * sheetKg * 1000.0;
            resultText.setText(numberFormat.format(sheets) + " листов");
            detailText.setText("Чистая масса бумаги: " + numberFormat.format(paperKg) + " кг\nОстаток после целых листов: " + numberFormat.format(remainderG) + " г\nМасса одного листа: " + numberFormat.format(sheetKg * 1000.0) + " г");
            hideKeyboard(coilMassInput); saveHistory(coilKg, grammage, widthM, lengthM, sheets, paperKg, remainderG); renderHistory();
        } catch (Exception ex) { showError(ex); }
    }
    private double parse(EditText field, String name) {
        String value = field.getText().toString().trim().replace(',', '.').replace(" ", "");
        if (value.isEmpty()) throw new IllegalArgumentException("Введите " + name + ".");
        try { double x = Double.parseDouble(value); if (Double.isNaN(x) || Double.isInfinite(x) || x <= 0) throw new NumberFormatException(); return x; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Проверьте значение: " + name + "."); }
    }
    private double toMeters(double value, int pos) { if (pos == 0) return value/1000.0; if (pos == 1) return value/100.0; return value; }
    private void saveHistory(double coilKg, double gsm, double widthM, double lengthM, long sheets, double paperKg, double remainderG) {
        try {
            JSONArray h = new JSONArray(getPreferences(MODE_PRIVATE).getString(HISTORY_KEY,"[]")); JSONObject o = new JSONObject();
            o.put("date", new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru","RU")).format(new Date())); o.put("coilKg",coilKg); o.put("gsm",gsm); o.put("widthM",widthM); o.put("lengthM",lengthM); o.put("sheets",sheets); o.put("paperKg",paperKg); o.put("remainderG",remainderG); h.put(o);
            JSONArray limited = new JSONArray(); for(int i=Math.max(0,h.length()-50);i<h.length();i++) limited.put(h.get(i));
            getPreferences(MODE_PRIVATE).edit().putString(HISTORY_KEY,limited.toString()).apply();
        } catch(Exception ignored) { }
    }
    private void renderHistory() {
        if(historyContainer==null)return; historyContainer.removeAllViews();
        try {
            JSONArray h=new JSONArray(getPreferences(MODE_PRIVATE).getString(HISTORY_KEY,"[]"));
            if(h.length()==0){historyContainer.addView(text("Пока нет сохранённых расчётов.",13,MUTED,false));return;}
            for(int i=h.length()-1;i>=0;i--){ JSONObject o=h.getJSONObject(i); TextView row=text(o.optString("date")+"\nКатушка: "+numberFormat.format(o.optDouble("coilKg"))+" кг • Бумага: "+numberFormat.format(o.optDouble("paperKg"))+" кг\n"+numberFormat.format(o.optDouble("gsm"))+" г/м² • "+numberFormat.format(o.optDouble("widthM")*1000)+" × "+numberFormat.format(o.optDouble("lengthM")*1000)+" мм\nРезультат: "+numberFormat.format(o.optLong("sheets"))+" листов • Остаток: "+numberFormat.format(o.optDouble("remainderG"))+" г",12,TEXT,false); row.setPadding(0,dp(9),0,dp(9)); historyContainer.addView(row); }
        }catch(Exception ignored){}
    }
    private void confirmClearHistory() {
        new AlertDialog.Builder(this).setTitle("Очистить историю?").setMessage("Все сохранённые расчёты будут удалены.")
                .setNegativeButton("Отмена",null).setPositiveButton("Очистить",(d,w)->{getPreferences(MODE_PRIVATE).edit().remove(HISTORY_KEY).apply();renderHistory();Toast.makeText(this,"История очищена",Toast.LENGTH_SHORT).show();}).show();
    }
    private void showError(Exception ex) { String m=ex.getMessage()==null?"Проверьте введённые значения.":ex.getMessage(); new AlertDialog.Builder(this).setTitle("Не удалось рассчитать").setMessage(m).setPositiveButton("Понятно",null).show(); }
    private void hideKeyboard(View view) { try { InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); imm.hideSoftInputFromWindow(view.getWindowToken(),0); } catch(Exception ignored){} }
    private int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}
