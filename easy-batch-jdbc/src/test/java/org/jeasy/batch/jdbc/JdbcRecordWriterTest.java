/*
 * The MIT License
 *
 *   Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package org.jeasy.batch.jdbc;

import org.jeasy.batch.core.job.Job;
import org.jeasy.batch.core.job.JobBuilder;
import org.jeasy.batch.core.job.JobExecutor;
import org.jeasy.batch.core.job.JobReport;
import org.jeasy.batch.core.reader.IterableRecordReader;
import org.jeasy.batch.test.common.AbstractDatabaseTest;
import org.jeasy.batch.test.common.Tweet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcRecordWriterTest extends AbstractDatabaseTest {

    private JdbcRecordWriter<Tweet> jdbcRecordWriter;
    private JobExecutor jobExecutor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        String query = "INSERT INTO tweet VALUES (?,?,?);";
        jdbcRecordWriter = new JdbcRecordWriter<>(embeddedDatabase, query, new BeanPropertiesPreparedStatementProvider(Tweet.class, "id", "user", "message"));
        jobExecutor = new JobExecutor();
    }

    @Test
    public void testRecordWriting() throws Exception {
        int nbTweetsToInsert = 5;
        List<Tweet> tweets = createTweets(nbTweetsToInsert);

        Job job = new JobBuilder<Tweet, Tweet>()
                .batchSize(2)
                .reader(new IterableRecordReader<>(tweets))
                .writer(jdbcRecordWriter)
                .build();

        JobReport jobReport = jobExecutor.execute(job);

        assertThat(jobReport).isNotNull();
        assertThat(jobReport.getMetrics().getReadCount()).isEqualTo(nbTweetsToInsert);
        assertThat(jobReport.getMetrics().getWriteCount()).isEqualTo(nbTweetsToInsert);

        int nbTweetsInDatabase = countRowsIn("tweet");
        assertThat(nbTweetsInDatabase).isEqualTo(nbTweetsToInsert);
    }

    @Test
    public void testRecordWritingWhenError() throws Exception {
        int nbTweetsToInsert = 5;
        int batchSize = 3; // two batches: [1,2,3] and [4,5]
        List<Tweet> tweets = createTweets(nbTweetsToInsert);

        // The following will make the second batch to fail
        tweets.get(4).setUser("ThisIsAVeryLongUsernameThatWillCauseAnError");

        Job job = new JobBuilder<Tweet, Tweet>()
                .batchSize(batchSize)
                .reader(new IterableRecordReader<>(tweets))
                .writer(jdbcRecordWriter)
                .build();

        JobReport jobReport = jobExecutor.execute(job);

        assertThat(jobReport).isNotNull();
        assertThat(jobReport.getMetrics().getReadCount()).isEqualTo(nbTweetsToInsert);
        assertThat(jobReport.getMetrics().getWriteCount()).isEqualTo(3L);

        int nbTweetsInDatabase = countRowsIn("tweet");
        assertThat(nbTweetsInDatabase).isEqualTo(3);
    }

    private List<Tweet> createTweets(int nbTweetsToInsert) {
        List<Tweet> tweets = new ArrayList<>();
        for (int i = 1; i <= nbTweetsToInsert; i++) {
            tweets.add(new Tweet(i, "user " + i, "hello " + i));
        }
        return tweets;
    }

    @After
    public void tearDown() throws Exception {
        jobExecutor.shutdown();
        super.tearDown();
    }

}
