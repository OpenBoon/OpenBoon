import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

import ButtonCopy from '../Button/Copy'

import { formatDisplayName, formatDisplayValue } from '../Metadata/helpers'

const MetadataPrettyRow = ({ name, value, path }) => {
  if (name === '' && value === '') {
    return (
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.regular.smoke,
          },
          padding: spacing.moderate,
          paddingLeft: spacing.normal,
          color: colors.structure.pebble,
          fontStyle: typography.style.italic,
        }}
      >
        empty
      </div>
    )
  }

  return (
    <div
      css={{
        display: 'flex',
        color: colors.structure.steel,
        svg: { opacity: 0 },
        '&:not(:first-of-type)': {
          borderTop: constants.borders.regular.smoke,
        },
        ':hover': {
          backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
          color: colors.structure.white,
          svg: { opacity: 1 },
        },
      }}
    >
      <div
        css={{
          fontFamily: typography.family.condensed,
          padding: spacing.moderate,
          paddingLeft: spacing.normal,
          paddingRight: spacing.base,
          flex: 1,
        }}
      >
        <span title={`${path.toLowerCase()}.${name}`}>
          {formatDisplayName({ name })}
        </span>
      </div>

      <div
        title={value}
        css={{
          flex: 4,
          fontFamily: typography.family.mono,
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.pebble,
          padding: spacing.moderate,
          wordBreak: name === 'content' ? 'break-word' : 'break-all',
        }}
      >
        {formatDisplayValue({ name, value })}
      </div>

      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          paddingRight: spacing.base,
        }}
      >
        <ButtonCopy title="Value" value={value} offset={100} />
      </div>
    </div>
  )
}

MetadataPrettyRow.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyRow
