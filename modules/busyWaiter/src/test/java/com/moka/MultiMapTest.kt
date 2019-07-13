package com.moka

import com.moka.waiter.internal.SetMultiMap
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.api.Java6Assertions.fail
import org.junit.Before
import org.junit.Test

class MultiMapTest {

    private var map: SetMultiMap<Int, String>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        map = SetMultiMap()
    }

    @Test
    @Throws(Exception::class)
    fun whenValueAdd_thenCanBeRetrieved() {
        val map = this.map
        map!!.add(1, "1")
        map.add(1, "2")
        map.add(2, "4")
        map.add(2, "3")

        assertThat(map.values(1)).containsExactlyInAnyOrder("1", "2")
        assertThat(map.values(2)).containsExactlyInAnyOrder("4", "3")
        assertThat(map.allValues()).containsExactlyInAnyOrder("1", "2", "3", "4")
    }

    @Test
    @Throws(Exception::class)
    fun whenValueAddedFirstTime_thenReturnTrue() {
        val wasAdded = map!!.add(1, "2")

        assertThat(wasAdded).isTrue
    }

    @Test
    @Throws(Exception::class)
    fun whenKeyValueAddedSecondTime_thenReturnFalse() {
        map!!.add(2, "2")
        val resultForSecondAdd = map!!.add(2, "2")

        assertThat(resultForSecondAdd).isFalse
    }

    @Test
    @Throws(Exception::class)
    fun whenNoValues_thenReturnEmptySet() {
        assertThat(map!!.values(1)).isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun whenKeyFor_thenReturnValue() {
        map!!.add(1, "1")
        map!!.add(2, "3")
        map!!.add(1, "2")

        assertThat(map!!.keyFor("1")).isEqualTo(1)
    }


    @Test
    @Throws(Exception::class)
    fun whenAddSameValue_thenThrowException() {
        try {
            map!!.add(1, "1")
            map!!.add(2, "1")
            fail("Adding the same value twice must throw an exception")
        } catch (e: IllegalStateException) {
            // expected
        }

    }

    @Test
    @Throws(Exception::class)
    fun whenRemoveValue_thenCanAddBackWithDifferentKey() {
        map!!.add(1, "1")
        map!!.add(1, "3")
        map!!.removeValue("1")
        map!!.add(2, "1")
        map!!.add(1, "2")

        assertThat(map!!.keyFor("1")).isEqualTo(2)
    }

    @Test
    @Throws(Exception::class)
    fun whenRemoveTwice_thenReturnFalseOnSecondTime() {
        map!!.add(1, "1")
        map!!.add(2, "2")
        val resultWhenValuePresent = map!!.removeValue("1")
        val resultAfterValueRemoved = map!!.removeValue("1")

        assertThat(resultWhenValuePresent).isTrue
        assertThat(resultAfterValueRemoved).isFalse
    }

    @Test
    @Throws(Exception::class)
    fun whenAllKeys_thenAllKeysArePresent() {
        map!!.add(1, "1")
        map!!.add(2, "2")
        map!!.add(2, "3")

        assertThat(map!!.allKeys()).containsExactlyInAnyOrder(1, 2)
    }

    @Test
    @Throws(Exception::class)
    fun whenAllValues_thenAllValuesArePresent() {
        map!!.add(1, "1")
        map!!.add(2, "2")
        map!!.add(2, "3")

        assertThat(map!!.allValues()).containsExactlyInAnyOrder("1", "2", "3")
    }

    @Test
    @Throws(Exception::class)
    fun valuesReturnsOnlyValuesForTheSpecifiedKey() {
        map!!.add(1, "1")
        map!!.add(2, "2")
        map!!.add(2, "3")

        assertThat(map!!.values(2)).containsExactlyInAnyOrder("2", "3")
    }

    @Test
    @Throws(Exception::class)
    fun whenAllValuesRemoved_ThenIsEmpty() {
        map!!.add(1, "1")
        map!!.removeValue("1")

        assertThat(map!!.hasNoValues()).isTrue
    }

    @Test
    @Throws(Exception::class)
    fun whenHasValue_ThenNotIsEmpty() {
        map!!.add(1, "1")

        assertThat(map!!.hasNoValues()).isFalse
    }

    @Test
    @Throws(Exception::class)
    fun whenIterateRemove_ThenIsActuallyRemoved() {
        map!!.add(1, "A")
        map!!.add(1, "B")
        map!!.add(2, "C")
        map!!.add(2, "C")
        map!!.add(3, "E")
        map!!.add(3, "D")

        val stringIterator = map!!.valuesIterator()
        while (stringIterator.hasNext()) {
            val next = stringIterator.next()
            if (next == "C") {
                stringIterator.remove()
            }
        }

        assertThat(map!!.allValues()).containsExactlyInAnyOrder("A", "B", "D", "E")
        assertThat(map!!.allKeys()).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(map!!.values(2)).isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun whenIterateValuesForKeyAndRemove_ThenIsActuallyRemoved() {
        map!!.add(1, "A")
        map!!.add(1, "B")
        map!!.add(2, "X")
        map!!.add(2, "Y")
        map!!.add(3, "E")
        map!!.add(3, "C")
        map!!.add(3, "D")

        val stringIterator = map!!.valuesIterator(3)
        while (stringIterator.hasNext()) {
            val next = stringIterator.next()
            if (next == "C") {
                stringIterator.remove()
            }
        }

        assertThat(map!!.allValues()).containsExactlyInAnyOrder("A", "B", "X", "E", "Y", "D")
        assertThat(map!!.allKeys()).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(map!!.values(3)).containsExactlyInAnyOrder("D", "E")
    }

    @Test
    @Throws(Exception::class)
    fun whenIterateValuesForKeyWithNoValues_thenNoExceptionThrown() {
        map!!.add(1, "A")
        map!!.add(1, "B")
        map!!.add(2, "X")
        map!!.add(2, "Y")
        map!!.add(3, "E")
        map!!.add(3, "C")
        map!!.add(3, "D")

        val stringIterator = map!!.valuesIterator(4)
        while (stringIterator.hasNext()) {
            val next = stringIterator.next()
            if (next == "C") {
                stringIterator.remove()
            }
        }

        assertThat(map!!.allValues()).containsExactlyInAnyOrder("A", "C", "B", "X", "E", "Y", "D")
        assertThat(map!!.allKeys()).containsExactlyInAnyOrder(1, 2, 3)
        assertThat(map!!.values(3)).containsExactlyInAnyOrder("D", "E", "C")
    }

    @Test
    @Throws(Exception::class)
    fun toString_thenPrintSomethingReasonable() {
        map!!.add(1, "A")
        map!!.add(1, "B")
        map!!.add(2, "X")
        map!!.add(2, "Y")
        map!!.add(3, "E")
        map!!.add(3, "C")
        map!!.add(3, "D")

        assertThat(map!!.toString()).isEqualTo("SetMultiMap{\n"
                + " '1' =>\n"
                + "  'A'\n"
                + "  'B'\n"
                + " '2' =>\n"
                + "  'X'\n"
                + "  'Y'\n"
                + " '3' =>\n"
                + "  'C'\n"
                + "  'D'\n"
                + "  'E'\n"
                + "}")
    }
}
