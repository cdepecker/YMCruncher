package ymcruncher.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class Plugin {

    // Options registered for a Plugin
    private final Map<String, Boolean> hmBooleanOption = new TreeMap<>();
    private final Map<String, OptionList> hmListOption = new TreeMap<>();

    /******************************************************************************************
     * Manage the Hashmap of Options
     *****************************************************************************************/

    public boolean blnHasOptions() {
        return blnHasBooleanOptions()
                || blnHasListOptions();
    }

    //
    // Booleans
    //
    public boolean blnHasBooleanOptions() {
        return !hmBooleanOption.isEmpty();
    }

    public Set<Map.Entry<String, Boolean>> getBooleanOptionList() {
        return hmBooleanOption.entrySet();
    }

    public Boolean getBooleanOption(String key) {
        Boolean b = hmBooleanOption.get(key);
        return (b != null) ? b : false;
    }

    public void setBooleanOption(String key, Boolean value) {
        hmBooleanOption.put(key, value);
    }

    //
    // List
    //
    public boolean blnHasListOptions() {
        return !hmListOption.isEmpty();
    }

    public Set<Map.Entry<String, OptionList>> getListOptionList() {
        return hmListOption.entrySet();
    }

    public void setListOption(String key, Object[] aList, int selected, boolean radio) {
        hmListOption.put(key, new OptionList(key, aList, selected, radio));
    }

    public void setListOption(String key, Object[] aList, int selected) {
        this.setListOption(key, aList, selected, false);
    }

    public void setListOptionIndex(String key, int selected) {
        OptionList ol = hmListOption.get(key);
        ol.selected = selected;
    }

    public int getListOptionIndex(String key) {
        OptionList ol = (OptionList) hmListOption.get(key);
        return ol.selected;
    }

    public Object getListOptionSelected(String key) {
        OptionList ol = (OptionList) hmListOption.get(key);
        return ol.aList[ol.selected];
    }

    public OptionList getListOptionArray(String key) {
        OptionList ol = (OptionList) hmListOption.get(key);
        return ol;
//        if (ol.aList == null) return null;
//        String[] arrList = new String[ol.aList.length];
//        for (int i = 0; i < ol.aList.length; i++)
//            arrList[i] = ol.aList[i].toString();
//        return arrList;
    }

    public boolean isListOptionRadioType(String key) {
        OptionList ol = (OptionList) hmListOption.get(key);
        return ol.radio;
    }

    class OptionList {
        private String label = null;
        private HashMap<String, Boolean> items = null;
        private int selected = 0;
        private boolean radio = false;

        public OptionList(String pLabel, HashMap<String, Boolean> pItems, int pSelected, boolean pRadio) {
            this.label = pLabel;
            this.items = pItems;
            this.selected = pSelected;
            this.radio = pRadio;
        }

        public String getLabel() {
            return label;
        }

        public HashMap<String, Boolean> getItems() {
            return items;
        }

        public int indexSelected() {
            return selected;
        }

        public boolean isRadio(){
            return radio;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setItems(HashMap<String, Boolean> items) {
            this.items = items;
        }

        public int getSelected() {
            return selected;
        }

        public void setSelected(int selected) {
            this.selected = selected;
        }

        public void setRadio(boolean radio) {
            this.radio = radio;
        }
    }

    @Override
    public String toString() {
        return getMenuLabel();
    }

    /**
     * Abstract function that should return the String that will be displayed in the Menu
     *
     * @return String
     */
    public abstract String getMenuLabel();
}
