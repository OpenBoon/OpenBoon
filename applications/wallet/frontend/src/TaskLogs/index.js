import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors } from '../Styles'

import JsonDisplay from '../JsonDisplay'

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

  return (
    <div
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
  )
}

export default TaskLogs
