import { useReducer } from 'react'
import { useRouter } from 'next/router'

import { reducer } from './reducer'

import { spacing } from '../Styles'

import ModelMatrixNavigation from './Navigation'
import ModelMatrixLayout from './Layout'

const ModelMatrixContent = () => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const [matrixDetails, setMatrixDetails] = useReducer(reducer, {
    name: '',
    overallAccuracy: 0,
    labels: [],
  })

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
      {!matrixDetails.name ? null : (
        <ModelMatrixNavigation
          projectId={projectId}
          modelId={modelId}
          name={matrixDetails.name}
        />
      )}

      <ModelMatrixLayout
        projectId={projectId}
        modelId={modelId}
        matrixDetails={matrixDetails}
        setMatrixDetails={setMatrixDetails}
      />
    </div>
  )
}

export default ModelMatrixContent
