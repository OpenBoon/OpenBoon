import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { constants, spacing, colors, typography } from '../Styles'

import MetadataPrettyLabelsRow from './LabelsRow'

const COLUMNS = ['bbox', 'model name/label', 'scope']

const MetadataPrettyLabels = ({ labels }) => {
  const {
    query: { projectId, assetId, id },
  } = useRouter()

  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/`)

  return (
    <div css={{ padding: spacing.normal, paddingBottom: spacing.comfy }}>
      <table
        css={{
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
          width: '100%',
          borderSpacing: 0,
        }}
      >
        <thead>
          <tr>
            {COLUMNS.map((column) => {
              return (
                <th
                  key={column}
                  css={{
                    fontFamily: typography.family.condensed,
                    fontWeight: typography.weight.regular,
                    textTransform: 'uppercase',
                    color: colors.structure.steel,
                    paddingBottom: spacing.base,
                    paddingLeft: 0,
                    borderBottom: constants.borders.regular.smoke,
                    textAlign: 'left',
                    '&:nth-of-type(2)': {
                      width: '100%',
                    },
                    '&:last-of-type': {
                      textAlign: 'right',
                      whiteSpace: 'nowrap',
                      paddingRight: 0,
                    },
                  }}
                >
                  {column}
                </th>
              )
            })}
          </tr>
        </thead>

        <tbody>
          {labels.map((label, index) => {
            return (
              <MetadataPrettyLabelsRow
                // eslint-disable-next-line react/no-array-index-key
                key={`${label.label}-${index}`}
                projectId={projectId}
                assetId={id || assetId}
                models={models}
                label={label}
              />
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

MetadataPrettyLabels.propTypes = {
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      bbox: PropTypes.arrayOf(PropTypes.number),
      modelId: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
      scope: PropTypes.oneOf(['TEST', 'TRAIN']).isRequired,
    }).isRequired,
  ).isRequired,
}

export default MetadataPrettyLabels
