package dev.flagkit;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationContextTest {

    @Test
    void testCreateContext() {
        EvaluationContext ctx = EvaluationContext.create("user-123");

        assertEquals("user-123", ctx.getUserId());
        assertFalse(ctx.isAnonymous());
    }

    @Test
    void testAnonymousContext() {
        EvaluationContext ctx = EvaluationContext.anonymous();

        assertTrue(ctx.isAnonymous());
        assertNotNull(ctx.getUserId());
    }

    @Test
    void testBuilderMethods() {
        EvaluationContext ctx = EvaluationContext.create("user-123")
                .withEmail("user@example.com")
                .withName("Test User")
                .withCountry("US")
                .withDeviceType("mobile")
                .withOs("iOS")
                .withBrowser("Safari")
                .withCustom("plan", "premium")
                .withPrivateAttribute("email");

        assertEquals("user@example.com", ctx.getEmail());
        assertEquals("Test User", ctx.getName());
        assertEquals("US", ctx.getCountry());
        assertEquals("mobile", ctx.getDeviceType());
        assertEquals("iOS", ctx.getOs());
        assertEquals("Safari", ctx.getBrowser());
        assertEquals("premium", ctx.getCustom().get("plan"));
        assertTrue(ctx.getPrivateAttributes().contains("email"));
    }

    @Test
    void testMerge() {
        EvaluationContext ctx1 = EvaluationContext.create("user-123")
                .withEmail("old@example.com")
                .withCustom("key1", "value1");

        EvaluationContext ctx2 = EvaluationContext.create("user-456")
                .withEmail("new@example.com")
                .withName("New Name")
                .withCustom("key2", "value2");

        EvaluationContext merged = ctx1.merge(ctx2);

        assertEquals("user-456", merged.getUserId());
        assertEquals("new@example.com", merged.getEmail());
        assertEquals("New Name", merged.getName());
        assertEquals("value1", merged.getCustom().get("key1"));
        assertEquals("value2", merged.getCustom().get("key2"));
    }

    @Test
    void testMergeWithNull() {
        EvaluationContext ctx = EvaluationContext.create("user-123");
        EvaluationContext merged = ctx.merge(null);

        assertSame(ctx, merged);
    }

    @Test
    void testStripPrivateAttributes() {
        EvaluationContext ctx = EvaluationContext.create("user-123")
                .withEmail("user@example.com")
                .withName("Test User")
                .withCustom("secret", "value")
                .withPrivateAttribute("email")
                .withPrivateAttribute("secret");

        EvaluationContext stripped = ctx.stripPrivateAttributes();

        assertEquals("user-123", stripped.getUserId());
        assertNull(stripped.getEmail());
        assertEquals("Test User", stripped.getName());
        assertFalse(stripped.getCustom().containsKey("secret"));
    }

    @Test
    void testToMap() {
        EvaluationContext ctx = EvaluationContext.create("user-123")
                .withEmail("user@example.com")
                .withCustom("plan", "premium");

        Map<String, Object> map = ctx.toMap();

        assertEquals("user-123", map.get("userId"));
        assertEquals("user@example.com", map.get("email"));
        assertNotNull(map.get("custom"));
    }

    @Test
    void testCopy() {
        EvaluationContext ctx = EvaluationContext.create("user-123")
                .withEmail("user@example.com")
                .withCustom("key", "value");

        EvaluationContext copy = ctx.copy();

        assertEquals(ctx.getUserId(), copy.getUserId());
        assertEquals(ctx.getEmail(), copy.getEmail());
        assertEquals(ctx.getCustom().get("key"), copy.getCustom().get("key"));

        // Ensure it's a deep copy
        copy.withCustom("key", "modified");
        assertNotEquals(ctx.getCustom().get("key"), copy.getCustom().get("key"));
    }
}
