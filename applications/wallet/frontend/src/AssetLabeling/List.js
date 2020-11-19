import PropTypes from 'prop-types'

import modelShape from '../Model/shape'

import { colors, constants, spacing, typography } from '../Styles'

import AssetLabelingMenu from './Menu'

const AssetLabelingList = ({ models, labels, triggerReload, setError }) => {
  if (!labels.length)
    return (
      <div
        css={{
          padding: spacing.normal,
          color: colors.structure.white,
          fontStyle: typography.style.italic,
        }}
      >
        No labels have been added
      </div>
    )

  return (
    <table css={{ color: colors.structure.steel }}>
      <thead>
        <tr
          css={{
            th: {
              textAlign: 'left',
              fontFamily: typography.family.condensed,
              fontWeight: typography.weight.regular,
              textTransform: 'uppercase',
              padding: spacing.moderate,
              paddingTop: spacing.normal,
              paddingLeft: spacing.normal,
            },
          }}
        >
          <th>Model</th>
          <th>Label</th>
          <th>Scope</th>
        </tr>
      </thead>
      <tbody>
        {labels.map(({ modelId, label, scope }) => {
          const { name = '', moduleName = '' } =
            models.find(({ id }) => id === modelId) || {}

          return (
            <tr
              key={modelId}
              css={{
                textAlign: 'left',
                fontFamily: typography.family.condensed,
                fontWeight: typography.weight.medium,
                '&:nth-last-of-type(2)': { width: '100%' },
                ':hover': {
                  backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
                  color: colors.structure.white,
                  svg: { color: colors.structure.white },
                },
                td: {
                  borderTop: constants.borders.regular.smoke,
                  padding: spacing.moderate,
                  paddingLeft: spacing.normal,
                },
              }}
            >
              <td css={{ whiteSpace: 'nowrap' }}>{name}</td>

              <td
                css={{
                  width: '100%',
                  wordBreak: 'break-all',
                }}
              >
                {label}
              </td>

              <td css={{ wordBreak: 'nowrap' }}>{scope}</td>

              <td>
                <AssetLabelingMenu
                  label={label}
                  modelId={modelId}
                  moduleName={moduleName}
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
  triggerReload: PropTypes.func.isRequired,
  setError: PropTypes.func.isRequired,
}

export default AssetLabelingList
