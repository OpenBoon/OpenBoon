import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing } from '../../Styles'

import MetadataAnalysisClassification from './Classification'

const MetadataAnalysis = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const {
    data: {
      metadata: { analysis, analysis: modules },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return Object.keys(modules).map((moduleName, index) => {
    const { type } = analysis[moduleName]
    if (type === 'labels') {
      return (
        <MetadataAnalysisClassification
          key={moduleName}
          moduleName={moduleName}
          moduleIndex={index}
        />
      )
    }

    return (
      <div key={moduleName}>
        <div
          css={{
            borderTop: index !== 0 ? constants.borders.largeDivider : '',
            fontFamily: 'Roboto Mono',
            color: colors.structure.white,
            padding: spacing.normal,
            ':hover': {
              backgroundColor: colors.signal.electricBlue.background,
              td: {
                color: colors.structure.white,
                svg: {
                  display: 'inline-block',
                },
              },
            },
          }}
        >
          {moduleName}
        </div>
        <div css={{ padding: spacing.normal, wordWrap: 'break-word' }}>
          {JSON.stringify(modules[moduleName])}
        </div>
      </div>
    )
  })
}

export default MetadataAnalysis
