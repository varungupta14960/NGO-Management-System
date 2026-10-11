package com.ngo.util;

import com.ngo.model.FieldDef;
import com.ngo.model.Option;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Input validation: required, numeric, positive, date, e-mail and phone checks. */
public final class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10,15}$");

    private Validator() { }

    public static String required(String label, String v) throws AppException {
        if (v == null || v.trim().isEmpty()) throw new AppException(label + " is required.");
        return v.trim();
    }

    public static String email(String label, String v) throws AppException {
        if (!EMAIL.matcher(v).matches()) throw new AppException(label + " is not a valid e-mail address (e.g. name@example.com).");
        return v;
    }

    public static String phone(String label, String v) throws AppException {
        if (!PHONE.matcher(v).matches()) throw new AppException(label + " must contain 10 to 15 digits only.");
        return v;
    }

    public static BigDecimal decimal(String label, String v) throws AppException {
        try {
            BigDecimal d = new BigDecimal(v.trim());
            if (d.scale() > 2) throw new AppException(label + " can have at most 2 decimal places.");
            if (d.precision() - d.scale() > 10) throw new AppException(label + " is too large.");
            return d;
        } catch (NumberFormatException e) {
            throw new AppException(label + " must be a valid number.");
        }
    }

    public static BigDecimal positive(String label, BigDecimal d) throws AppException {
        if (d.signum() <= 0) throw new AppException(label + " must be greater than zero.");
        return d;
    }

    public static Integer integer(String label, String v) throws AppException {
        try { return Integer.valueOf(v.trim()); }
        catch (NumberFormatException e) { throw new AppException(label + " must be a whole number."); }
    }

    public static LocalDate localDate(String label, String v) throws AppException {
        try { return LocalDate.parse(v.trim()); }
        catch (DateTimeParseException e) { throw new AppException(label + " must be a valid date in yyyy-MM-dd format."); }
    }

    public static Date date(String label, String v) throws AppException { return Date.valueOf(localDate(label, v)); }

    /** Validates and converts one form value according to its field definition. Returns null for an empty optional value. */
    public static Object field(FieldDef f, Object raw) throws AppException {
        if (f.kind == FieldDef.Kind.LOOKUP) {
            Option o = (Option) raw;
            if (o == null || o.id == 0) {
                if (f.required) throw new AppException("Please select a value for " + f.label + ".");
                return null;
            }
            return o.id;
        }
        String s = raw == null ? "" : raw.toString().trim();
        if (s.isEmpty()) {
            if (f.required) throw new AppException(f.label + " is required.");
            return null;
        }
        switch (f.kind) {
            case TEXT:
                if (s.length() > f.maxLength) throw new AppException(f.label + " can have at most " + f.maxLength + " characters.");
                if (f.check == FieldDef.Check.EMAIL) email(f.label, s);
                if (f.check == FieldDef.Check.PHONE) phone(f.label, s);
                return s;
            case INT: return integer(f.label, s);
            case DECIMAL:
                BigDecimal d = decimal(f.label, s);
                if (f.positive) positive(f.label, d);
                else if (d.signum() < 0) throw new AppException(f.label + " cannot be negative.");
                return d;
            case DATE: return date(f.label, s);
            default: return s;   // CHOICE
        }
    }
}
