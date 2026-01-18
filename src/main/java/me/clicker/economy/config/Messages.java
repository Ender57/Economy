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

public final class Messages {
    public static String must_be_positive;
    public static String player_not_found;
    public static String amount_invalid;
    public static String internal_error;

    public static String pay_no_permission;
    public static String pay_player_only;
    public static String pay_self;
    public static String pay_pay_offline;
    public static String pay_not_enough;
    public static String pay_sender;
    public static String pay_target;

    public static String eco_no_permission;

    public static String eco_give_sender;
    public static String eco_give_target;

    public static String eco_take_not_enough;
    public static String eco_take_sender;
    public static String eco_take_target;

    public static String eco_set_sender;
    public static String eco_set_target;

    public static String balance_console_requires_player;
    public static String balance_no_permission;
    public static String balance_others_no_permission;
    public static String balance_self;
    public static String balance_other;

    public static String baltop_no_permission;
    public static String baltop_header;
    public static String baltop_record;

    public static void load(Path dataDir) {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception ignored) {}

        var file = dataDir.resolve("messages.yml").toFile();

        if (!file.exists()) {
            try (InputStream in = Messages.class.getClassLoader().getResourceAsStream("messages.yml")) {

                if (in == null) {
                    throw new RuntimeException("messages.yml not found in resources");
                }

                Files.copy(in, file.toPath());
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract messages.yml", e);
            }
        }

        try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Map<String, Object> root = new Yaml().load(r);

            must_be_positive = (String) root.get("must_be_positive");
            player_not_found = (String) root.get("player_not_found");
            amount_invalid = (String) root.get("amount_invalid");
            internal_error = (String) root.get("internal_error");

            var pay = (Map<String, Object>) root.get("pay");
            pay_no_permission = (String) pay.get("no_permission");
            pay_player_only = (String) pay.get("player_only");
            pay_self = (String) pay.get("self");
            pay_pay_offline = (String) pay.get("pay_offline");
            pay_not_enough = (String) pay.get("not_enough");
            pay_sender = (String) pay.get("sender");
            pay_target = (String) pay.get("target");

            var eco = (Map<String, Object>) root.get("eco");
            eco_no_permission = (String) eco.get("no_permission");

            var ecoGive = (Map<String, Object>) eco.get("give");
            eco_give_sender = (String) ecoGive.get("sender");
            eco_give_target = (String) ecoGive.get("target");

            var ecoTake = (Map<String, Object>) eco.get("take");
            eco_take_not_enough = (String) ecoTake.get("not_enough");
            eco_take_sender = (String) ecoTake.get("sender");
            eco_take_target = (String) ecoTake.get("target");

            var ecoSet = (Map<String, Object>) eco.get("set");
            eco_set_sender = (String) ecoSet.get("sender");
            eco_set_target = (String) ecoSet.get("target");

            var balance = (Map<String, Object>) root.get("balance");
            balance_console_requires_player = (String) balance.get("console_requires_player");
            balance_no_permission = (String) balance.get("no_permission");
            balance_others_no_permission = (String) balance.get("others_no_permission");
            balance_self = (String) balance.get("self");
            balance_other = (String) balance.get("other");

            var baltop = (Map<String, Object>) root.get("baltop");
            baltop_no_permission = (String) baltop.get("no_permission");
            baltop_header = (String) baltop.get("header");
            baltop_record = (String) baltop.get("record");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load messages.yml", e);
        }
    }
}