import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import Accordion, { VARIANTS } from '../Accordion'

import { UNIQUE_DISPLAY_NAMES } from './helpers'

const MetadataSection = ({ title, metadata }) => {
  return (
    <Accordion variant={VARIANTS.PANEL} title={title} isInitiallyOpen>
      <table
        css={{
          borderCollapse: 'collapse',
          width: '100%',
        }}
      >
        <tbody>
          {Object.entries(metadata).map(([key, value], index) => (
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
                  {UNIQUE_DISPLAY_NAMES[key] ||
                    // Capitalize first letter
                    key.replace(/^\w/, (c) => c.toUpperCase())}
                </span>
              </td>
              <td
                valign="top"
                css={{
                  fontFamily: 'Roboto Mono',
                  color: colors.structure.pebble,
                  padding: spacing.normal,
                  wordBreak: 'break-word',
                }}
              >
                {value}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Accordion>
  )
}

MetadataSection.propTypes = {
  title: PropTypes.string.isRequired,
  metadata: PropTypes.shape({}).isRequired,
}

export default MetadataSection
