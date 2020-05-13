import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'

import { formatDisplayName, formatDisplayValue } from '../Metadata/helpers'

// eslint-disable-next-line import/no-cycle
import MetadataPrettyObject from './Object'

const MetadataPrettyRow = ({ name, value, path }) => {
  if (typeof value === 'object') {
    return <MetadataPrettyObject name={name} value={value} path={path} />
  }

  return (
    <div
      css={{
        display: 'flex',
        '&:not(:first-of-type)': {
          borderTop: constants.borders.divider,
        },
        ':hover': {
          backgroundColor: colors.signal.electricBlue.background,
          div: {
            color: colors.structure.white,
            svg: {
              display: 'inline-block',
            },
          },
        },
      }}
    >
      <div
        css={{
          fontFamily: 'Roboto Condensed',
          color: colors.structure.steel,
          padding: spacing.normal,
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
          fontFamily: 'Roboto Mono',
          color: colors.structure.pebble,
          padding: spacing.normal,
          wordBreak: name === 'content' ? 'break-word' : 'break-all',
        }}
      >
        {formatDisplayValue({ name, value })}
      </div>
      <div
        css={{
          width: COPY_SIZE + spacing.normal,
          paddingTop: spacing.normal,
          paddingRight: spacing.normal,
        }}
      >
        <ButtonCopy value={value} />
      </div>
    </div>
  )
}

MetadataPrettyRow.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.shape({}),
  ]).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettyRow
