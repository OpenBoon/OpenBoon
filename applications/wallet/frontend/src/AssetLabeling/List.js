import PropTypes from 'prop-types'

import modelShape from '../Model/shape'

import { colors, constants, spacing, typography } from '../Styles'

import { decode, encode } from '../Filters/helpers'
import { getQueryString } from '../Fetch/helpers'

import AssetLabelingMenu from './Menu'

const AssetLabelingList = ({
  models,
  labels,
  projectId,
  assetId,
  query,
  triggerReload,
  setError,
}) => {
  if (!labels.length)
    return (
      <div css={{ padding: spacing.normal, color: colors.structure.white }}>
        No labels have been added
      </div>
    )

  return (
    <table
      css={{
        width: '100%',
        color: colors.structure.steel,
        borderSpacing: 0,
      }}
    >
      <thead>
        <tr
          css={{
            textAlign: 'left',
            fontFamily: typography.family.condensed,
            fontWeight: typography.weight.medium,
            th: {
              textTransform: 'uppercase',
              padding: spacing.moderate,
              paddingTop: spacing.normal,
              paddingLeft: spacing.normal,
            },
          }}
        >
          <th>Model</th>
          <th>Label</th>
        </tr>
      </thead>
      <tbody>
        {labels.map(({ modelId, label }) => {
          const { name } = models.find(({ id }) => id === modelId)

          const filters = decode({ query })

          const encodedQuery = encode({
            filters: [
              {
                attribute: `labels.${name}`,
                type: 'label',
                value: { labels: [label] },
              },
              ...filters,
            ],
          })

          const queryString = getQueryString({
            id: assetId,
            query: encodedQuery,
          })

          return (
            <tr
              key={modelId}
              css={{
                textAlign: 'left',
                fontFamily: typography.family.condensed,
                fontWeight: typography.weight.medium,
                '&:nth-last-of-type(2)': { width: '100%' },
                ':hover': {
                  backgroundColor: `${colors.signal.electricBlue.base}${constants.opacity.hex22Pct}`,
                },
                td: {
                  borderTop: constants.borders.regular.smoke,
                  padding: spacing.moderate,
                  paddingLeft: spacing.normal,
                },
              }}
            >
              <td>{name}</td>
              <td
                css={{
                  width: '100%',
                  wordBreak: 'break-all',
                }}
              >
                {label}
              </td>
              <td>
                <AssetLabelingMenu
                  projectId={projectId}
                  assetId={assetId}
                  queryString={queryString}
                  modelId={modelId}
                  label={label}
                  triggerReload={triggerReload}
                  setError={setError}
                />
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

AssetLabelingList.propTypes = {
  models: PropTypes.arrayOf(modelShape).isRequired,
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string,
      modelId: PropTypes.string,
    }),
  ).isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
  triggerReload: PropTypes.func.isRequired,
  setError: PropTypes.func.isRequired,
}

export default AssetLabelingList
