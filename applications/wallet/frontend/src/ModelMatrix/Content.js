import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import ModelMatrixNavigation from './Navigation'
import ModelMatrixLayout from './Layout'

const ModelMatrixContent = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const {
    data: { name },
  } = useSWR(`/api/v1/projects/${projectId}/models/${modelId}/`)

  return (
    <div
      css={{
        flex: 1,
        height: '100%',
        marginLeft: -spacing.spacious,
        marginRight: -spacing.spacious,
        marginBottom: -spacing.spacious,
        paddingTop: spacing.hairline,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <ModelMatrixNavigation
        projectId={projectId}
        modelId={modelId}
        name={name}
      />

      <ModelMatrixLayout />
    </div>
  )
}

export default ModelMatrixContent
