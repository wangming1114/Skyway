package com.skyway.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

public class VpsPortRateLimitParserTest {

    @Test
    public void parseTcPortRulesIgnoresCommentsAndInvalidLines() {
        String output = "# comment\n"
                + "111:50:20\n"
                + "\n"
                + "bad\n"
                + "222:100:30\n";

        List<VpsSshCommandService.PortRateLimitRule> rules = VpsSshCommandService.parseTcPortRules(output);

        assertEquals(2, rules.size());
        assertEquals(111, rules.get(0).getPort());
        assertEquals(50, rules.get(0).getDownloadMbps());
        assertEquals(20, rules.get(0).getUploadMbps());
        assertEquals(222, rules.get(1).getPort());
        assertEquals(100, rules.get(1).getDownloadMbps());
        assertEquals(30, rules.get(1).getUploadMbps());
    }

    @Test
    public void buildTcPortRuleLineValidatesPortAndRates() {
        assertEquals("111:50:20", VpsSshCommandService.buildTcPortRuleLine(111, 50, 20));
        assertThrows(IllegalArgumentException.class, () -> VpsSshCommandService.buildTcPortRuleLine(0, 50, 20));
        assertThrows(IllegalArgumentException.class, () -> VpsSshCommandService.buildTcPortRuleLine(65536, 50, 20));
        assertThrows(IllegalArgumentException.class, () -> VpsSshCommandService.buildTcPortRuleLine(111, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> VpsSshCommandService.buildTcPortRuleLine(111, 50, 0));
    }
}
