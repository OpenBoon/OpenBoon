import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import JobTasks from '../JobTasks'
import JobErrors from '../JobErrors'

import JobDetails from './Details'

const Job = () => {
  const { pathname } = useRouter()

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
        <JobDetails />

        <Tabs
          tabs={[
            { title: 'All Tasks', href: '/[projectId]/jobs/[jobId]' },
            { title: 'Errors', href: '/[projectId]/jobs/[jobId]/errors' },
          ]}
        />

        {pathname === '/[projectId]/jobs/[jobId]' && <JobTasks />}

        {pathname === '/[projectId]/jobs/[jobId]/errors' && <JobErrors />}
      </SuspenseBoundary>
    </>
  )
}

export default Job
