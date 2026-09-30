package huddtds.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vector utility cua mot itemset tren cung thu tu cac item da quy uoc.
 */
public class ItemsetVector {
    private final List<String> dimensions;
    private final Map<String, Double> values;

    public ItemsetVector(List<String> dimensions) {
        this.dimensions = new ArrayList<>(dimensions);
        Collections.sort(this.dimensions);
        this.values = new LinkedHashMap<>();
        for (String dimension : this.dimensions) {
            values.put(dimension, 0.0);
        }
    }

    public void setValue(String item, double value) {
        if (values.containsKey(item)) {
            values.put(item, value);
        }
    }

    public double getValue(String item) {
        return values.getOrDefault(item, 0.0);
    }

    public List<String> getDimensions() {
        return Collections.unmodifiableList(dimensions);
    }

    public Map<String, Double> getValues() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String toString() {
        List<String> vectorValues = new ArrayList<>();
        for (String dimension : dimensions) {
            vectorValues.add(String.format(java.util.Locale.US, "%.2f", values.get(dimension)));
        }
        return vectorValues.toString();
    }
}
