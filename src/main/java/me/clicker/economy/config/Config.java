package me.clicker.economy.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class Config {
    public static String currency_symbol;
    public static int currency_fraction_digits;
    public static double balance_starting;
    public static boolean pay_allow_offline;
    public static int baltop_page_size;

    public static void load(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) {}

        var file = dataDir.resolve("config.yml").toFile();

        if (!file.exists()) {
            try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.yml")) {

                if (in == null) {
                    throw new RuntimeException("config.yml not found in resources");
                }

                Files.copy(in, file.toPath());
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract config.yml", e);
            }
        }

        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Map<String, Object> root = new Yaml().load(r);

            var currency = (Map<String, Object>) root.get("currency");
            currency_symbol = (String) currency.get("symbol");
            currency_fraction_digits = ((Number) currency.get("fraction_digits")).intValue();

            var balance = (Map<String, Object>) root.get("balance");
            balance_starting = ((Number) balance.get("starting")).doubleValue();

            var pay = (Map<String, Object>) root.get("pay");
            pay_allow_offline = (Boolean) pay.get("allow_offline");

            var baltop = (Map<String, Object>) root.get("baltop");
            baltop_page_size = ((Number) baltop.get("page_size")).intValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.yml", e);
        }
    }
}