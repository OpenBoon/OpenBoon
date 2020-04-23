import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import JobTasks from '../JobTasks'
import JobErrors from '../JobErrors'

import JobDetails from './Details'

const Job = () => {
  const { pathname, query } = useRouter()

  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Job Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <JobDetails key={pathname} />

        <Tabs
          tabs={[
            { title: 'All Tasks', href: '/[projectId]/jobs/[jobId]' },
            { title: 'Errors', href: '/[projectId]/jobs/[jobId]/errors' },
          ]}
        />

        {pathname === '/[projectId]/jobs/[jobId]' && (
          <JobTasks key={query.refreshParam} />
        )}

        {pathname === '/[projectId]/jobs/[jobId]/errors' && (
          <JobErrors key={query.refreshParam} />
        )}
      </SuspenseBoundary>
    </>
  )
}

export default Job
