package br.com.kusharchives.kushclient.core.setting;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    public NumberSetting(String id, String name, double defaultValue, double min, double max, double step) {
        super(id, name, defaultValue);
        if (max < min || step <= 0) throw new IllegalArgumentException("Faixa inválida para NumberSetting");
        if (defaultValue < min || defaultValue > max) throw new IllegalArgumentException("Valor padrão fora da faixa");
        this.min = min; this.max = max; this.step = step;
    }
    @Override protected Double validate(Double value) {
        double clamped = Math.max(min, Math.min(max, value));
        double snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
}
