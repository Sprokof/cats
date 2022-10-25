package com.endava.cats.model.generator.impl;

import com.endava.cats.model.generator.api.PropertySanitizer;
import com.endava.cats.model.generator.api.ValidDataFormatGenerator;
import io.swagger.v3.oas.models.media.Schema;

import javax.inject.Singleton;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

@Singleton
public class CountryCodeGenerator implements ValidDataFormatGenerator {
    private static final Random RANDOM = new Random();

    @Override
    public Object generate(Schema<?> schema) {
        Locale.IsoCountryCode isoCountryCode = Locale.IsoCountryCode.PART1_ALPHA3;
        if (schema.getMinLength() != null && schema.getMinLength() == 2) {
            isoCountryCode = Locale.IsoCountryCode.PART1_ALPHA2;
        }
        Set<String> isoCountries = Locale.getISOCountries(isoCountryCode);
        return isoCountries.stream().skip(RANDOM.nextInt(isoCountries.size())).findFirst().orElse(Locale.UK.getCountry());
    }

    @Override
    public boolean appliesTo(String format, String propertyName) {
        return "iso3166".equalsIgnoreCase(PropertySanitizer.sanitize(format)) ||
                "countrycode".equalsIgnoreCase(PropertySanitizer.sanitize(format)) ||
                PropertySanitizer.sanitize(propertyName).toLowerCase(Locale.ROOT).endsWith("countrycode");
    }
}