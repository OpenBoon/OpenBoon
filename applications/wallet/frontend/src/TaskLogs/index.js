import { useRef } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, spacing, constants } from '../Styles'

import BackSvg from '../Icons/back.svg'

import JsonDisplay from '../JsonDisplay'
import Button, { VARIANTS } from '../Button'

const TaskLogs = () => {
  const {
    query: { projectId, taskId },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/tasks/${taskId}/logs/`,
    {
      refreshInterval: 1000,
      refreshWhenHidden: true,
    },
  )

  const logsContainer = useRef()

  return (
    <div
      css={{
        height: '100%',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div
        css={{
          display: 'flex',
          justifyContent: 'flex-end',
          paddingBottom: spacing.normal,
        }}
      >
        <Button
          aria-label="Scroll to top"
          variant={VARIANTS.SECONDARY_SMALL}
          style={{ padding: spacing.base }}
          onClick={() => {
            logsContainer.current?.scrollTo(
              0,
              -logsContainer.current.scrollHeight,
            )
          }}
        >
          <BackSvg
            height={constants.icons.regular}
            css={{ transform: 'rotate(90deg)' }}
          />
        </Button>

        <div css={{ width: spacing.base }} />

        <Button
          aria-label="Scroll to bottom"
          variant={VARIANTS.SECONDARY_SMALL}
          style={{ padding: spacing.base }}
          onClick={() => {
            logsContainer.current?.scrollTo(0, 0)
          }}
        >
          <BackSvg
            height={constants.icons.regular}
            css={{ transform: 'rotate(-90deg)' }}
          />
        </Button>
      </div>

      <div
        ref={logsContainer}
        css={{
          height: '100%',
          overflow: 'auto',
          backgroundColor: colors.structure.coal,
          display: 'flex',
          flexDirection: 'column-reverse',
        }}
      >
        <JsonDisplay json={data} />
      </div>
    </div>
  )
}

export default TaskLogs
