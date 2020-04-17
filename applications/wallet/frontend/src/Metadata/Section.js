import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import Accordion, { VARIANTS } from '../Accordion'

const MetadataSection = ({ title, metadata }) => {
  return (
    <Accordion variant={VARIANTS.PANEL} title={title} isInitiallyOpen>
      <table css={{ borderCollapse: 'collapse', tableLayout: 'fixed' }}>
        <tbody>
          {Object.entries(metadata).map(([key, value], index) => (
            <tr
              key={key}
              css={{
                borderTop: index !== 0 ? constants.borders.divider : '',
              }}
            >
              <td
                css={{
                  fontFamily: 'Roboto Condensed',
                  color: colors.structure.steel,
                  padding: spacing.normal,
                }}
              >
                <span title={`${title.toLowerCase()}.${key}`}>{key}</span>
              </td>
              <td
                css={{
                  wordWrap: 'break-word',
                  fontFamily: 'Roboto Mono',
                  color: colors.structure.pebble,
                  padding: spacing.normal,
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
