/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2019-2019 the original author or authors.
 */

package org.quickperf.sql.select.number;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.experimental.results.PrintableResult;

public class SqlExpectSelectTest {

    @Test public void
    should_fail_if_the_number_of_select_requests_is_not_equal_to_the_number_expected() {

        // GIVEN
        Class<?> testClass = AClassHavingAMethodAnnotatedWithExpectSelect.class;

        // WHEN
        PrintableResult printableResult = PrintableResult.testResult(testClass);

        // THEN
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(printableResult.failureCount())
            .isEqualTo(1);

        softAssertions.assertThat(printableResult.toString())
                      .contains("You may think that <5> select requests were sent to the database")
                      .contains("But in fact <1>...")
                      .contains("select")
                      .contains("book0_.id as id1_0");

        softAssertions.assertAll();

    }

    @Test public void
    should_not_display_round_trip_and_n_plus_one_select_messages_if_select_number_less_than_expected() {

        // GIVEN
        Class<?> testClass = AClassHavingAMethodAnnotatedWithExpectSelectAndSelectsLessThanExpected.class;

        // WHEN
        PrintableResult printableResult = PrintableResult.testResult(testClass);

        // THEN
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(printableResult.toString())
                      .doesNotContain("server roundtrips")
                      .doesNotContain("N+1");
        softAssertions.assertAll();

    }

}
