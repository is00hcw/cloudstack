// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.framework.jobs.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

public class AsyncJobDaoImpl extends GenericDaoBase<AsyncJobVO, Long> implements AsyncJobDao {
    private static final Logger s_logger = Logger.getLogger(AsyncJobDaoImpl.class.getName());

    private final SearchBuilder<AsyncJobVO> pendingAsyncJobSearch;
    private final SearchBuilder<AsyncJobVO> pendingAsyncJobsSearch;
    private final SearchBuilder<AsyncJobVO> expiringAsyncJobSearch;
    private final SearchBuilder<AsyncJobVO> pseudoJobSearch;
    private final SearchBuilder<AsyncJobVO> pseudoJobCleanupSearch;
    private final SearchBuilder<AsyncJobVO> expiringUnfinishedAsyncJobSearch;
    private final SearchBuilder<AsyncJobVO> expiringCompletedAsyncJobSearch;

    public AsyncJobDaoImpl() {
        pendingAsyncJobSearch = createSearchBuilder();
        pendingAsyncJobSearch.and("instanceType", pendingAsyncJobSearch.entity().getInstanceType(), SearchCriteria.Op.EQ);
        pendingAsyncJobSearch.and("instanceId", pendingAsyncJobSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        pendingAsyncJobSearch.and("status", pendingAsyncJobSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        pendingAsyncJobSearch.done();

        expiringAsyncJobSearch = createSearchBuilder();
        expiringAsyncJobSearch.and("created", expiringAsyncJobSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        expiringAsyncJobSearch.done();

        pendingAsyncJobsSearch = createSearchBuilder();
        pendingAsyncJobsSearch.and("instanceType", pendingAsyncJobsSearch.entity().getInstanceType(), SearchCriteria.Op.EQ);
        pendingAsyncJobsSearch.and("accountId", pendingAsyncJobsSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        pendingAsyncJobsSearch.and("status", pendingAsyncJobsSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        pendingAsyncJobsSearch.done();

        expiringUnfinishedAsyncJobSearch = createSearchBuilder();
        expiringUnfinishedAsyncJobSearch.and("created", expiringUnfinishedAsyncJobSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        expiringUnfinishedAsyncJobSearch.and("completeMsId", expiringUnfinishedAsyncJobSearch.entity().getCompleteMsid(), SearchCriteria.Op.NULL);
        expiringUnfinishedAsyncJobSearch.and("jobStatus", expiringUnfinishedAsyncJobSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        expiringUnfinishedAsyncJobSearch.done();

        expiringCompletedAsyncJobSearch = createSearchBuilder();
        expiringCompletedAsyncJobSearch.and("created", expiringCompletedAsyncJobSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        expiringCompletedAsyncJobSearch.and("completeMsId", expiringCompletedAsyncJobSearch.entity().getCompleteMsid(), SearchCriteria.Op.NNULL);
        expiringCompletedAsyncJobSearch.and("jobStatus", expiringCompletedAsyncJobSearch.entity().getStatus(), SearchCriteria.Op.NEQ);
        expiringCompletedAsyncJobSearch.done();

        pseudoJobSearch = createSearchBuilder();
        pseudoJobSearch.and("jobDispatcher", pseudoJobSearch.entity().getDispatcher(), Op.EQ);
        pseudoJobSearch.and("instanceType", pseudoJobSearch.entity().getInstanceType(), Op.EQ);
        pseudoJobSearch.and("instanceId", pseudoJobSearch.entity().getInstanceId(), Op.EQ);
        pseudoJobSearch.done();

        pseudoJobCleanupSearch = createSearchBuilder();
        pseudoJobCleanupSearch.and("initMsid", pseudoJobCleanupSearch.entity().getInitMsid(), Op.EQ);
        pseudoJobCleanupSearch.done();

    }

    @Override
    public AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId) {
        SearchCriteria<AsyncJobVO> sc = pendingAsyncJobSearch.create();
        sc.setParameters("instanceType", instanceType);
        sc.setParameters("instanceId", instanceId);
        sc.setParameters("status", JobInfo.Status.IN_PROGRESS);

        List<AsyncJobVO> l = listIncludingRemovedBy(sc);
        if (l != null && l.size() > 0) {
            if (l.size() > 1) {
                s_logger.warn("Instance " + instanceType + "-" + instanceId + " has multiple pending async-job");
            }

            return l.get(0);
        }
        return null;
    }

    @Override
    public List<AsyncJobVO> findInstancePendingAsyncJobs(String instanceType, Long accountId) {
        SearchCriteria<AsyncJobVO> sc = pendingAsyncJobsSearch.create();
        sc.setParameters("instanceType", instanceType);

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }
        sc.setParameters("status", JobInfo.Status.IN_PROGRESS);

        return listBy(sc);
    }

    @Override
    public AsyncJobVO findPseudoJob(long threadId, long msid) {
        SearchCriteria<AsyncJobVO> sc = pseudoJobSearch.create();
        sc.setParameters("jobDispatcher", AsyncJobVO.JOB_DISPATCHER_PSEUDO);
        sc.setParameters("instanceType", AsyncJobVO.PSEUDO_JOB_INSTANCE_TYPE);
        sc.setParameters("instanceId", threadId);

        List<AsyncJobVO> result = listBy(sc);
        if (result != null && result.size() > 0) {
            assert (result.size() == 1);
            return result.get(0);
        }

        return null;
    }

    @Override
    public void cleanupPseduoJobs(long msid) {
        SearchCriteria<AsyncJobVO> sc = pseudoJobCleanupSearch.create();
        sc.setParameters("initMsid", msid);
        this.expunge(sc);
    }

    @Override
    public List<AsyncJobVO> getExpiredJobs(Date cutTime, int limit) {
        SearchCriteria<AsyncJobVO> sc = expiringAsyncJobSearch.create();
        sc.setParameters("created", cutTime);
        Filter filter = new Filter(AsyncJobVO.class, "created", true, 0L, (long)limit);
        return listIncludingRemovedBy(sc, filter);
    }

    @Override
    public List<AsyncJobVO> getExpiredUnfinishedJobs(Date cutTime, int limit) {
        SearchCriteria<AsyncJobVO> sc = expiringUnfinishedAsyncJobSearch.create();
        sc.setParameters("created", cutTime);
        sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);
        Filter filter = new Filter(AsyncJobVO.class, "created", true, 0L, (long)limit);
        return listIncludingRemovedBy(sc, filter);
    }

    @Override
    public List<AsyncJobVO> getExpiredCompletedJobs(Date cutTime, int limit) {
        SearchCriteria<AsyncJobVO> sc = expiringCompletedAsyncJobSearch.create();
        sc.setParameters("created", cutTime);
        sc.setParameters("jobStatus", JobInfo.Status.IN_PROGRESS);
        Filter filter = new Filter(AsyncJobVO.class, "created", true, 0L, (long)limit);
        return listIncludingRemovedBy(sc, filter);
    }

    @Override
    @DB
    public void resetJobProcess(long msid, int jobResultCode, String jobResultMessage) {
        String sql =
            "UPDATE async_job SET job_status=" + JobInfo.Status.FAILED.ordinal() + ", job_result_code=" + jobResultCode + ", job_result='" + jobResultMessage +
                "' where job_status=" + JobInfo.Status.IN_PROGRESS.ordinal() + " AND (job_executing_msid=? OR (job_executing_msid IS NULL AND job_init_msid=?))";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setLong(1, msid);
            pstmt.setLong(2, msid);
            pstmt.execute();
        } catch (SQLException e) {
            s_logger.warn("Unable to reset job status for management server " + msid, e);
        } catch (Throwable e) {
            s_logger.warn("Unable to reset job status for management server " + msid, e);
        }
    }
}
