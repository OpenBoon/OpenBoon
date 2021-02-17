import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

import { formatFullDate, getDuration, formatDuration } from '../Date/helpers'

import Value, { VARIANTS } from '../Value'

import TaskMenu from './Menu'

const TaskDetails = () => {
  const {
    query: { projectId, taskId },
  } = useRouter()

  const { data: task, mutate: revalidate } = useSWR(
    `/api/v1/projects/${projectId}/tasks/${taskId}/`,
  )

  const { name, state, host, timeStarted, timeStopped } = task

  const isStarted = timeStarted !== -1

  const taskDuration = getDuration({
    timeStarted,
    timeStopped,
    now: Date.now(),
  })

  return (
    <div css={{ flexShrink: 0 }}>
      <h3
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        Task: {name}
      </h3>

      <div css={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center' }}>
        <TaskMenu revalidate={revalidate} />

        <Value legend="ID" variant={VARIANTS.PRIMARY}>
          {taskId}
        </Value>

        <Value legend="State" variant={VARIANTS.PRIMARY}>
          {state}
        </Value>

        <Value legend="Host" variant={VARIANTS.PRIMARY}>
          {host}
        </Value>

        <Value legend="Started" variant={VARIANTS.PRIMARY}>
          {isStarted ? (
            formatFullDate({ timestamp: timeStarted })
          ) : (
            <div
              css={{ fontStyle: typography.style.italic }}
            >{`${state}...`}</div>
          )}
        </Value>

        <Value legend="Duration" variant={VARIANTS.PRIMARY}>
          {isStarted ? (
            formatDuration({
              seconds: taskDuration / 1000,
            })
          ) : (
            <div
              css={{ fontStyle: typography.style.italic }}
            >{`${state}...`}</div>
          )}
        </Value>
      </div>
    </div>
  )
}

export default TaskDetails
