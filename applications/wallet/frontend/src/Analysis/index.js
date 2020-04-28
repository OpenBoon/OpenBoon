import { colors, constants, spacing } from '../Styles'

import AnalysisClassification from './Classification'

const Analysis = ({ analysis }) => {
  return Object.keys(analysis).map((moduleName, index) => {
    const { type } = analysis[moduleName]
    if (type === 'labels') {
      return (
        <AnalysisClassification
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
          {JSON.stringify(analysis[moduleName])}
        </div>
      </div>
    )
  })
}

export default Analysis
