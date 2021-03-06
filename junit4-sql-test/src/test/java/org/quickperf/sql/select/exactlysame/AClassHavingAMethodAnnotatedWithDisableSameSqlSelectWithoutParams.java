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

package org.quickperf.sql.select.exactlysame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quickperf.junit4.QuickPerfJUnitRunner;
import org.quickperf.sql.SqlTestBase;
import org.quickperf.sql.annotation.DisableExactlySameSelects;
import org.quickperf.sql.entities.Book;

import javax.persistence.EntityManager;
import javax.persistence.Query;

@RunWith(QuickPerfJUnitRunner.class)
public class AClassHavingAMethodAnnotatedWithDisableSameSqlSelectWithoutParams extends SqlTestBase {

    @Test
    @DisableExactlySameSelects
    public void execute_two_same_selects_without_params() {

        EntityManager em = emf.createEntityManager();

        Query query1 = em.createQuery("FROM " + Book.class.getCanonicalName());
        query1.getResultList();

        Query query2 = em.createQuery("FROM " + Book.class.getCanonicalName());
        query2.getResultList();

    }
}
