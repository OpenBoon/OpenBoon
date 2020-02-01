import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography, colors } from '../Styles'

import Loading from '../Loading'
import Tabs from '../Tabs'
import Value from '../Value'
import ProgressBar from '../ProgressBar'

import JobErrorsJobMenu from './JobMenu'
import JobErrorsTable from './Table'

const JobErrorsContent = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  const { data: job, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/jobs/${jobId}`,
  )

  if (typeof job !== 'object') return <Loading />

  const { name, state, priority, taskCounts: tC } = job

  const taskCounts = { ...tC, tasksPending: tC.tasksWaiting + tC.tasksQueued }

  return (
    <div>
      <h3
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}>
        Job: {name}
      </h3>

      <div css={{ display: 'flex', alignItems: 'center' }}>
        <JobErrorsJobMenu
          projectId={projectId}
          jobId={jobId}
          revalidate={revalidate}
        />

        <Value legend="Job Status">
          <div
            css={{
              color: colors.structure.white,
              fontWeight: typography.weight.medium,
            }}>
            {state}
          </div>
        </Value>

        <Value legend="Job Status">
          <ProgressBar taskCounts={taskCounts} />
        </Value>

        <Value legend="Priority">{priority}</Value>
      </div>

      <Tabs
        tabs={[
          { title: 'All Errors', href: '/[projectId]/jobs/[jobId]/errors' },
        ]}
      />

      <JobErrorsTable />
    </div>
  )
}

export default JobErrorsContent
