import assetShape from '../Asset/shape'

import { colors, constants, spacing } from '../Styles'

import CopySvg from '../Icons/copy.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'

import { formatDisplayName, formatDisplayValue } from './helpers'

const COPY_WIDTH = 20

const MetadataPretty = ({ asset: { metadata } }) => {
  return (
    <div css={{ overflow: 'auto' }}>
      {Object.keys(metadata).map((section) => {
        const title = formatDisplayName({ name: section })

        if (['files', 'metrics', 'analysis'].includes(section)) return null

        return (
          <Accordion
            key={section}
            variant={ACCORDION_VARIANTS.PANEL}
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
                            td: {
                              color: colors.structure.white,
                              svg: {
                                display: 'inline-block',
                              },
                            },
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
                          title={value.length > 300 ? value : ''}
                          css={{
                            fontFamily: 'Roboto Mono',
                            color: colors.structure.pebble,
                            padding: spacing.normal,
                            wordBreak: 'break-all',
                          }}
                        >
                          {formatDisplayValue({ key, value })}
                        </td>
                        <td
                          valign="top"
                          css={{
                            width: COPY_WIDTH + spacing.normal,
                            paddingTop: spacing.normal,
                            paddingRight: spacing.normal,
                          }}
                        >
                          <Button
                            title="Copy to Clipboard"
                            variant={BUTTON_VARIANTS.NEUTRAL}
                            onClick={() => {
                              const dummy = document.createElement('textarea')
                              document.body.appendChild(dummy)
                              dummy.value = value
                              dummy.select()
                              document.execCommand('copy')
                              document.body.removeChild(dummy)
                            }}
                          >
                            <CopySvg
                              width={COPY_WIDTH}
                              color={colors.structure.steel}
                              css={{
                                display: 'none',
                                ':hover': {
                                  color: colors.structure.white,
                                },
                              }}
                            />
                          </Button>
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
