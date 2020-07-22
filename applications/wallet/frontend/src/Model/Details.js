import { useRouter } from 'next/router'
import useSWR from 'swr'

import { typography } from '../Styles'

const LINE_HEIGHT = '23px'

const ModelDetails = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const { data: job } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
  )

  const { name, type, moduleName } = job

  return (
    <div>
      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          fontSize: typography.size.medium,
          lineHeight: LINE_HEIGHT,
        }}
      >
        <li>
          <strong>Model Name:</strong> {name}
        </li>
        <li>
          <strong>Model Type:</strong> {type}
        </li>
        <li>
          <strong>Module Name:</strong> {moduleName}
        </li>
      </ul>
    </div>
  )
}

export default ModelDetails
