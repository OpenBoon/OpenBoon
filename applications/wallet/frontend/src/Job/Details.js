import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import ItemTitle from '../Item/Title'
import ItemList from '../Item/List'
import ItemSeparator from '../Item/Separator'
import Value, { VARIANTS } from '../Value'
import ProgressBar from '../ProgressBar'

import JobMenu from './Menu'
import JobStatus, { VARIANTS as JOB_STATUS_VARIANTS } from './Status'

const JobDetails = () => {
  const {
    query: { projectId, jobId, action },
  } = useRouter()

  const { data: job, mutate: revalidate } = useSWR(
    `/api/v1/projects/${projectId}/jobs/${jobId}/`,
  )

  const { name, state, paused, priority, assetCounts, taskCounts: tC } = job

  const status = paused ? 'Paused' : state

  const taskCounts = { ...tC, tasksPending: tC.tasksWaiting + tC.tasksQueued }

  return (
    <div>
      {!!action && (
        <div css={{ display: 'flex', paddingBottom: spacing.base }}>
          <FlashMessage variant={FLASH_VARIANTS.INFO}>{action}</FlashMessage>
        </div>
      )}

      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-end',
        }}
      >
        <div>
          <ItemTitle type="Job" name={name} />

          <ItemList attributes={[['ID', jobId]]} />
        </div>

        <JobStatus variant={JOB_STATUS_VARIANTS.LARGE} status={status} />
      </div>

      <div css={{ height: spacing.normal }} />

      <ItemSeparator />

      <div css={{ display: 'flex', alignItems: 'center' }}>
        <JobMenu status={status} revalidate={revalidate} />

        <Value legend="Priority" variant={VARIANTS.PRIMARY}>
          {priority}
        </Value>

        <Value legend="# Assets" variant={VARIANTS.PRIMARY}>
          {assetCounts.assetTotalCount}
        </Value>

        <Value legend="Task Progress" variant={VARIANTS.PRIMARY}>
          <ProgressBar taskCounts={taskCounts} />
        </Value>
      </div>
    </div>
  )
}

export default JobDetails
