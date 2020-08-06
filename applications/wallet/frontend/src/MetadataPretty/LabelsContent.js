import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing, colors, typography } from '../Styles'

import modelShape from '../Model/shape'

import { capitalizeFirstLetter } from '../Text/helpers'

const BBOX_SIZE = 56

const MetadataPrettyLabelsContent = ({ projectId, assetId, models }) => {
  const attr = `labels&width=${BBOX_SIZE}`

  const {
    data: { labels },
  } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  return labels.map((label) => {
    const { name } = models.find(({ id }) => id === label.modelId)

    return (
      <tr
        key={`${label.modelId}${label.label}`}
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
          {label.b64_image ? (
            <img
              css={{
                maxHeight: BBOX_SIZE,
                width: BBOX_SIZE,
                objectFit: 'contain',
              }}
              alt={label.bbox}
              title={label.bbox}
              src={label.b64_image}
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
  })
}

MetadataPrettyLabelsContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  models: PropTypes.arrayOf(modelShape).isRequired,
}

export default MetadataPrettyLabelsContent
