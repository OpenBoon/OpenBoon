import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing, colors, typography } from '../Styles'

import modelShape from '../Model/shape'

import { capitalizeFirstLetter } from '../Text/helpers'

const BBOX_SIZE = 56

const MetadataPrettyLabelsRow = ({ projectId, assetId, models, label }) => {
  const { name, moduleName } = models.find(({ id }) => id === label.modelId)

  const attr = `analysis.${moduleName}&width=${BBOX_SIZE}`

  const { data = {} } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  const { predictions = [] } = data[moduleName] || {}

  const prediction = predictions.find(
    ({ bbox }) => JSON.stringify(bbox) === JSON.stringify(label.bbox),
  )

  return (
    <tr
      css={{
        verticalAlign: 'bottom',
        td: {
          paddingTop: spacing.base,
          paddingBottom: spacing.base,
          paddingRight: spacing.base,
          borderBottom: constants.borders.regular.smoke,
        },
      }}
    >
      <td>
        {prediction ? (
          <img
            css={{
              maxHeight: BBOX_SIZE,
              width: BBOX_SIZE,
              objectFit: 'contain',
            }}
            alt={prediction.bbox}
            title={prediction.bbox}
            src={prediction.b64_image}
          />
        ) : (
          <div
            css={{
              fontFamily: typography.family.condensed,
              fontWeight: typography.weight.regular,
              color: colors.structure.steel,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'flex-end',
              minWidth: BBOX_SIZE,
              minHeight: BBOX_SIZE,
            }}
          >
            N/A
          </div>
        )}
      </td>

      <td title={`Model ID: ${label.modelId}`}>
        <span
          css={{
            fontFamily: typography.family.condensed,
            fontWeight: typography.weight.regular,
            color: colors.structure.steel,
          }}
        >
          {name}
        </span>
        <br />
        {label.label}
      </td>

      <td
        css={{
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
          color: colors.structure.steel,
          textAlign: 'right',
          paddingRight: 0,
        }}
      >
        {capitalizeFirstLetter({ word: label.scope })}
      </td>
    </tr>
  )
}

MetadataPrettyLabelsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  models: PropTypes.arrayOf(modelShape).isRequired,
  label: PropTypes.shape({
    bbox: PropTypes.arrayOf(PropTypes.number),
    modelId: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    scope: PropTypes.oneOf(['TEST', 'TRAIN']).isRequired,
  }).isRequired,
}

export default MetadataPrettyLabelsRow
