import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing, colors, typography } from '../Styles'

import { capitalizeFirstLetter } from '../Text/helpers'

import MetadataPrettyLabelsMenu from './LabelsMenu'

const MetadataPrettyLabelsContent = ({ projectId, assetId, datasets }) => {
  const attr = `labels&width=${constants.bbox}`

  const {
    data: { labels },
  } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/box_images/?attr=${attr}`,
  )

  return labels.map((label) => {
    const { name = '' } =
      datasets.find(({ id }) => id === label.datasetId) || {}

    return (
      <tr
        key={`${label.datasetId}${label.label}`}
        css={{
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
          verticalAlign: 'bottom',
          ':hover': {
            backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
            button: {
              svg: { color: colors.structure.steel },
              ':hover, &.focus-visible:focus': {
                svg: { color: `${colors.structure.white} !important` },
              },
            },
          },
        }}
      >
        <td />
        <td
          css={{
            padding: spacing.base,
            paddingLeft: 0,
            borderBottom: constants.borders.regular.smoke,
          }}
        >
          {label.b64Image ? (
            <img
              css={{
                maxHeight: constants.bbox,
                width: constants.bbox,
                objectFit: 'contain',
              }}
              alt={label.bbox}
              title={label.bbox}
              src={label.b64Image}
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
                minWidth: constants.bbox,
                minHeight: constants.bbox,
              }}
            >
              N/A
            </div>
          )}
        </td>

        <td
          title={`Dataset ID: ${label.datasetId}`}
          css={{
            padding: spacing.base,
            paddingLeft: 0,
            borderBottom: constants.borders.regular.smoke,
          }}
        >
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
            padding: spacing.base,
            paddingLeft: 0,
            paddingRight: 0,
            borderBottom: constants.borders.regular.smoke,
          }}
        >
          {capitalizeFirstLetter({ word: label.scope })}
        </td>

        <td
          css={{
            paddingTop: spacing.base,
            verticalAlign: 'top',
          }}
        >
          <MetadataPrettyLabelsMenu label={label} datasetName={name} />
        </td>

        <td css={{ borderBottom: 'none !important' }} />
      </tr>
    )
  })
}

MetadataPrettyLabelsContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  datasets: PropTypes.arrayOf(
    PropTypes.shape({ name: PropTypes.string.isRequired }).isRequired,
  ).isRequired,
}

export default MetadataPrettyLabelsContent
