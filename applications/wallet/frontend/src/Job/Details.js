import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

import Value, { VARIANTS } from '../Value'
import ProgressBar from '../ProgressBar'

import JobMenu from './Menu'

const JobDetails = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  const { data: job, mutate: revalidate } = useSWR(
    `/api/v1/projects/${projectId}/jobs/${jobId}/`,
  )

  const { name, state, paused, priority, assetCounts, taskCounts: tC } = job

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
        <JobMenu status={status} revalidate={revalidate} />

        <Value legend="ID" variant={VARIANTS.PRIMARY}>
          {jobId}
        </Value>

        <Value legend="Status" variant={VARIANTS.PRIMARY}>
          {status}
        </Value>

        <Value legend="Priority" variant={VARIANTS.PRIMARY}>
          {priority}
        </Value>

        <Value legend="# Assets" variant={VARIANTS.PRIMARY}>
          {assetCounts.assetTotalCount}
        </Value>

        <Value legend="Progress" variant={VARIANTS.PRIMARY}>
          <ProgressBar taskCounts={taskCounts} />
        </Value>
      </div>
    </div>
  )
}

export default JobDetails
