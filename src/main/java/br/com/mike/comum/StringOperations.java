package br.com.mike.comum;

public abstract class StringOperations {

    public static String join(String value) {
        return join(value.toCharArray(), 0, "");
    }

    public static String join(char[] caracters, Integer position, String value) {
        value += (String.valueOf(caracters[position]).matches("^[A-Z]+$") ? "_" : "") + String.valueOf(caracters[position]).toLowerCase();
        if ((caracters.length - 1) > position) {
            return join(caracters, ++position, value);
        }
        return value;
    }
}
