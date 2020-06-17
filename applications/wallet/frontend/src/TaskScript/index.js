import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import JsonDisplay from '../JsonDisplay'

const TaskScript = () => {
  const {
    query: { projectId, taskId },
  } = useRouter()

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/tasks/${taskId}/script/`,
  )

  return (
    <div css={{ paddingBottom: spacing.spacious }}>
      <JsonDisplay json={data} />
    </div>
  )
}

export default TaskScript
