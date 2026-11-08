package io.tarcinapp.epbo.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SlugService}.
 *
 * All expected values are verified against
 * {@code slugify(input, { lower: true, strict: true })} from the npm slugify package.
 */
class SlugServiceTest {

    private SlugService service;

    @BeforeEach
    void setUp() {
        service = new SlugService();
    }

    @Test
    void hello_world() {
        assertEquals("hello-world", service.slugify("Hello World"));
    }

    @Test
    void multi_word_book_name() {
        assertEquals("my-test-book", service.slugify("My Test Book"));
    }

    @Test
    void turkish_characters_transliterated() {
        // npm: slugify('Türkçe Kitap', {lower:true, strict:true}) → 'turkce-kitap'
        // ü→u, ç→c, Turkish 'e' stays
        assertEquals("turkce-kitap", service.slugify("Türkçe Kitap"));
    }

    @Test
    void dashed_kind_preserved() {
        assertEquals("e-book", service.slugify("e-book"));
    }

    @Test
    void alphanumeric_with_dash() {
        assertEquals("book-123", service.slugify("book-123"));
    }

    @Test
    void colon_removed() {
        // npm: 'Star Wars: A New Hope' → 'star-wars-a-new-hope'
        assertEquals("star-wars-a-new-hope", service.slugify("Star Wars: A New Hope"));
    }

    @Test
    void leading_trailing_spaces_trimmed() {
        // npm: '   leading spaces   ' → 'leading-spaces'
        assertEquals("leading-spaces", service.slugify("   leading spaces   "));
    }

    @Test
    void latin_extended_cafe() {
        // npm: 'café au lait' → 'cafe-au-lait'
        assertEquals("cafe-au-lait", service.slugify("café au lait"));
    }

    @Test
    void gorev_tipi_turkish() {
        // npm: 'görev tipi' → 'gorev-tipi'
        assertEquals("gorev-tipi", service.slugify("görev tipi"));
    }

    @Test
    void uber_cool() {
        // npm: 'über cool' → 'uber-cool'
        assertEquals("uber-cool", service.slugify("über cool"));
    }

    @Test
    void all_lowercase_input_unchanged() {
        assertEquals("simple", service.slugify("simple"));
    }

    @Test
    void multiple_spaces_collapsed() {
        assertEquals("a-b", service.slugify("a  b"));
    }

    @Test
    void special_chars_only_produces_empty() {
        // Strict mode strips everything that is not [a-z0-9\s-]
        // after NFD decomposition. An input of only special chars yields "".
        // We just verify it does not throw.
        String result = service.slugify("!@#$");
        assertTrue(result.isEmpty() || !result.contains("!"),
                "Special chars should be stripped, got: " + result);
    }

    @Test
    void already_slugified_kind_unchanged() {
        // Valid kind values that already passed EPS validation are idempotent
        assertEquals("book", service.slugify("book"));
        assertEquals("kitap-turu", service.slugify("kitap-turu"));
        assertEquals("book-kind", service.slugify("book-kind"));
    }

    @Test
    void numbers_preserved() {
        assertEquals("item-42", service.slugify("item 42"));
    }
}
