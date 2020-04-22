import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

import Value, { VARIANTS } from '../Value'
import ProgressBar from '../ProgressBar'
import { SIZE } from '../Table/Content'

import JobMenu from './Menu'

const JobDetails = () => {
  const {
    query: { projectId, jobId, page = 1 },
  } = useRouter()

  const parsedPage = parseInt(page, 10)
  const from = parsedPage * SIZE - SIZE

  const { data: job, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/jobs/${jobId}`,
  )

  const { name, state, paused, priority, taskCounts: tC } = job

  const status = paused ? 'Paused' : state

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
        }}
      >
        Job: {name}
      </h3>

      <div css={{ display: 'flex', alignItems: 'center' }}>
        <JobMenu
          projectId={projectId}
          jobId={jobId}
          status={status}
          revalidate={revalidate}
          refreshKeys={[
            `/api/v1/projects/${projectId}/jobs/${jobId}/errors?from=${from}&size=${SIZE}`,
            `/api/v1/projects/${projectId}/jobs/${jobId}/tasks?from=${from}&size=${SIZE}`,
          ]}
        />

        <Value legend="Job Status" variant={VARIANTS.PRIMARY}>
          {status}
        </Value>

        <Value legend="Priority" variant={VARIANTS.PRIMARY}>
          {priority}
        </Value>

        <Value legend="Job Progress" variant={VARIANTS.PRIMARY}>
          <ProgressBar taskCounts={taskCounts} />
        </Value>
      </div>
    </div>
  )
}

export default JobDetails
