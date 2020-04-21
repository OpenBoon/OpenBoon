import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import Accordion, { VARIANTS } from '../Accordion'

import { formatDisplayName } from './helpers'

const MetadataSection = ({ title, children }) => {
  return (
    <Accordion variant={VARIANTS.PANEL} title={title} isInitiallyOpen>
      <table
        css={{
          borderCollapse: 'collapse',
          width: '100%',
        }}
      >
        <tbody>
          {Object.entries(children).map(([key, value], index) => {
            if (typeof value === 'object') return null

            return (
              <tr
                key={key}
                css={{
                  borderTop: index !== 0 ? constants.borders.divider : '',
                  ':hover': {
                    backgroundColor: colors.selection.background,
                  },
                }}
              >
                <td
                  valign="top"
                  css={{
                    fontFamily: 'Roboto Condensed',
                    color: colors.structure.steel,
                    padding: spacing.normal,
                  }}
                >
                  <span title={`${title.toLowerCase()}.${key}`}>
                    {formatDisplayName({ name: key })}
                  </span>
                </td>
                <td
                  valign="top"
                  css={{
                    fontFamily: 'Roboto Mono',
                    color: colors.structure.pebble,
                    padding: spacing.normal,
                    wordBreak: 'break-all',
                  }}
                >
                  {value}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </Accordion>
  )
}

MetadataSection.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.oneOfType([
    PropTypes.shape({}),
    PropTypes.arrayOf(PropTypes.shape({})),
  ]).isRequired,
}

export default MetadataSection
