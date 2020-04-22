import assetShape from '../Asset/shape'

import { colors, constants, spacing } from '../Styles'

import Accordion, { VARIANTS } from '../Accordion'

import { formatDisplayName, formatDisplayValue } from './helpers'

const MetadataPretty = ({ asset: { metadata } }) => {
  return (
    <div css={{ overflow: 'auto' }}>
      {Object.keys(metadata).map((section) => {
        const title = formatDisplayName({ name: section })

        if (['files', 'metrics', 'analysis'].includes(section)) return null

        return (
          <Accordion
            key={section}
            variant={VARIANTS.PANEL}
            title={title}
            isInitiallyOpen
          >
            <table
              css={{
                borderCollapse: 'collapse',
                width: '100%',
              }}
            >
              <tbody>
                {Object.entries(metadata[section]).map(
                  ([key, value], index) => {
                    return (
                      <tr
                        key={key}
                        css={{
                          borderTop:
                            index !== 0 ? constants.borders.divider : '',
                          ':hover': {
                            backgroundColor:
                              colors.signal.electricBlue.background,
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
                          {formatDisplayValue({ key, value })}
                        </td>
                      </tr>
                    )
                  },
                )}
              </tbody>
            </table>
          </Accordion>
        )
      })}
    </div>
  )
}

MetadataPretty.propTypes = {
  asset: assetShape.isRequired,
}

export default MetadataPretty
