import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors } from '../Styles'

import JsonDisplay from '../JsonDisplay'

const TaskScript = () => {
  const {
    query: { projectId, taskId },
  } = useRouter()

  const { data } = useSWR(`/api/v1/projects/${projectId}/tasks/${taskId}/`)

  return (
    <div
      css={{
        height: '100%',
        overflow: 'auto',
        backgroundColor: colors.structure.coal,
        display: 'flex',
      }}
    >
      <JsonDisplay json={data} />
    </div>
  )
}

export default TaskScript
