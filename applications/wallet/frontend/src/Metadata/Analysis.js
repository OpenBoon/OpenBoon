// import PropTypes from 'prop-types'

import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

const MODULES = {
  'zvi-object-detection': { columns: ['bbox', 'label', 'score'] },
  'zvi-label-detection': { columns: ['label', 'score'] },
}

const MetadataAnalysis = () => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const {
    data: {
      metadata: { analysis: modules },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  return Object.keys(modules).map((moduleName, index) => {
    return (
      <>
        <div
          key={moduleName}
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
        {modules[moduleName].predictions ? (
          <div css={{ display: 'flex', padding: spacing.normal }}>
            {MODULES[moduleName].columns.map((column) => {
              return <div>{column}</div>
            })}
            {/* {JSON.stringify(modules[moduleName].predictions)} */}
          </div>
        ) : (
          <div css={{ padding: spacing.normal }}>
            {JSON.stringify(modules[moduleName])}
          </div>
        )}
      </>
    )
  })
}

// MetadataAnalysis.propTypes = {
//   analysis: PropTypes.shape({}).isRequired,
// }

export default MetadataAnalysis
