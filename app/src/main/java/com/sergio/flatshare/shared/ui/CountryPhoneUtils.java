package com.sergio.flatshare.shared.ui;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.sergio.flatshare.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CountryPhoneUtils {
    private static final String DEFAULT_REGION = "ES";

    private CountryPhoneUtils() {
    }

    public static final class CountryOption {
        public final String regionCode;
        public final String dialCode;
        public final String countryName;
        public final String label;

        CountryOption(@NonNull String regionCode, @NonNull String dialCode, @NonNull String countryName) {
            this.regionCode = regionCode;
            this.dialCode = dialCode;
            this.countryName = countryName;
            this.label = buildFlagEmoji(regionCode) + " " + dialCode + " " + countryName;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    public static final class ParsedPhone {
        public final int selectedIndex;
        public final String localNumber;
        @NonNull public final String fullPhone;

        ParsedPhone(int selectedIndex, @NonNull String localNumber, @NonNull String fullPhone) {
            this.selectedIndex = selectedIndex;
            this.localNumber = localNumber;
            this.fullPhone = fullPhone;
        }
    }

    @NonNull
    public static List<CountryOption> loadCountries() {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        Set<String> regions = phoneUtil.getSupportedRegions();
        Locale displayLocale = Locale.getDefault();

        Map<String, CountryOption> byRegion = new LinkedHashMap<>();
        for (String region : regions) {
            if (region == null || region.length() != 2) continue;
            int countryCode = phoneUtil.getCountryCodeForRegion(region);
            if (countryCode <= 0) continue;
            String countryName = new Locale("", region).getDisplayCountry(displayLocale);
            if (countryName == null || countryName.trim().isEmpty()) {
                countryName = region;
            }
            byRegion.put(region, new CountryOption(region, "+" + countryCode, countryName));
        }

        List<CountryOption> out = new ArrayList<>(byRegion.values());
        Collator collator = Collator.getInstance(displayLocale);
        out.sort(Comparator.comparing(option -> option.countryName, collator));
        return out;
    }

    @NonNull
    public static ArrayAdapter<CountryOption> buildAdapter(@NonNull Context context, @NonNull List<CountryOption> options) {
        ArrayAdapter<CountryOption> adapter = new ArrayAdapter<>(
                context,
                R.layout.item_spinner_selected,
                options
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        return adapter;
    }

    public static int findRegionIndex(@NonNull List<CountryOption> options, @Nullable String regionCode) {
        String normalized = normalizeRegion(regionCode);
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).regionCode.equalsIgnoreCase(normalized)) return i;
        }
        return -1;
    }

    public static void selectRegion(@NonNull Spinner spinner, @NonNull List<CountryOption> options, @Nullable String regionCode) {
        int index = findRegionIndex(options, regionCode);
        if (index < 0) index = findRegionIndex(options, DEFAULT_REGION);
        if (index >= 0) spinner.setSelection(index);
    }

    @NonNull
    public static CountryOption selected(@NonNull Spinner spinner, @NonNull List<CountryOption> options) {
        int pos = spinner.getSelectedItemPosition();
        if (pos < 0 || pos >= options.size()) {
            int fallback = findRegionIndex(options, DEFAULT_REGION);
            if (fallback >= 0) return options.get(fallback);
            return options.get(0);
        }
        return options.get(pos);
    }

    @NonNull
    public static String buildFullPhone(@Nullable CountryOption selectedOption, @Nullable String localRaw) {
        String local = cleanRawPhone(localRaw);
        if (local.isEmpty()) return "";

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        if (local.startsWith("+")) {
            try {
                Phonenumber.PhoneNumber parsed = phoneUtil.parse(local, DEFAULT_REGION);
                return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
            } catch (NumberParseException ignored) {
                return local;
            }
        }

        String region = selectedOption == null ? DEFAULT_REGION : selectedOption.regionCode;
        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(local, region);
            return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ignored) {
            String dialCode = selectedOption == null ? "+34" : selectedOption.dialCode;
            return dialCode + local.replaceAll("[^0-9]", "");
        }
    }

    @NonNull
    public static ParsedPhone splitStoredPhone(@Nullable String rawPhone, @NonNull List<CountryOption> options) {
        if (rawPhone == null || rawPhone.trim().isEmpty()) {
            int defaultIndex = findRegionIndex(options, DEFAULT_REGION);
            if (defaultIndex < 0) defaultIndex = 0;
            return new ParsedPhone(defaultIndex, "", "");
        }

        String clean = cleanRawPhone(rawPhone);
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(clean, DEFAULT_REGION);
            String region = phoneUtil.getRegionCodeForNumber(parsed);
            int index = findRegionIndex(options, region);
            if (index < 0) index = findRegionIndex(options, DEFAULT_REGION);
            if (index < 0) index = 0;
            String localNumber = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            String fullPhone = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
            return new ParsedPhone(index, localNumber, fullPhone);
        } catch (NumberParseException ignored) {
            int fallbackIndex = findRegionIndex(options, DEFAULT_REGION);
            if (fallbackIndex < 0) fallbackIndex = 0;
            return new ParsedPhone(fallbackIndex, clean.replaceFirst("^\\+[0-9]+", "").trim(), clean);
        }
    }

    @NonNull
    private static String cleanRawPhone(@Nullable String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("+")) {
            String body = trimmed.substring(1).replaceAll("[^0-9]", "");
            return body.isEmpty() ? "" : "+" + body;
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        return digits;
    }

    @NonNull
    private static String normalizeRegion(@Nullable String regionCode) {
        if (regionCode == null) return DEFAULT_REGION;
        String normalized = regionCode.trim().toUpperCase(Locale.ROOT);
        return normalized.length() == 2 ? normalized : DEFAULT_REGION;
    }

    @NonNull
    private static String buildFlagEmoji(@NonNull String regionCode) {
        String normalized = regionCode.toUpperCase(Locale.ROOT);
        if (normalized.length() != 2) return "";
        int first = normalized.charAt(0) - 'A' + 0x1F1E6;
        int second = normalized.charAt(1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }
}
