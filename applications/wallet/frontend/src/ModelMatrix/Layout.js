import { useReducer } from 'react'
import AutoSizer from 'react-virtualized-auto-sizer'

// TODO: fetch data
import matrix from './__mocks__/matrix'

import { colors, constants, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import PreviewSvg from '../Icons/preview.svg'

import { INITIAL_STATE, reducer } from './reducer'

import ModelMatrixControls from './Controls'
import ModelMatrixTable from './Table'
import ModelMatrixLabels from './Labels'

const ModelMatrixLayout = () => {
  const [settings, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <div
      css={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          padding: spacing.normal,
          borderBottom: constants.borders.regular.coal,
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          backgroundColor: colors.structure.lead,
        }}
      >
        <span
          css={{
            fontWeight: typography.weight.bold,
            paddingRight: spacing.small,
          }}
        >
          Overall Accuracy:
        </span>
        98%
        <ModelMatrixControls
          matrix={matrix}
          settings={settings}
          dispatch={dispatch}
        />
      </div>

      <div
        css={{
          flex: 1,
          width: '100%',
          display: 'flex',
          backgroundColor: colors.structure.lead,
        }}
      >
        <div
          css={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            width: '0%',
          }}
        >
          <div
            css={{
              flex: 1,
              display: 'flex',
              width: '100%',
            }}
          >
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                padding: spacing.normal,
                borderRight: constants.borders.regular.coal,
              }}
            >
              <div
                css={{
                  writingMode: 'vertical-lr',
                  transform: 'rotate(180deg)',
                }}
              >
                <span
                  css={{
                    fontWeight: typography.weight.bold,
                    fontSize: typography.size.medium,
                    lineHeight: typography.height.medium,
                  }}
                >
                  True Label
                </span>{' '}
                <span css={{ color: colors.structure.zinc }}>
                  (Number in Set)
                </span>
              </div>
            </div>

            <div css={{ width: '100%' }}>
              <AutoSizer defaultWidth={800} defaultHeight={600}>
                {({ width, height }) => (
                  <ModelMatrixTable
                    matrix={matrix}
                    width={width}
                    height={height}
                    settings={settings}
                    dispatch={dispatch}
                  />
                )}
              </AutoSizer>
            </div>
          </div>

          <div
            css={{
              display: 'flex',
              borderTop: constants.borders.regular.coal,
              width: '100%',
            }}
          >
            {/* begin placeholder for "True Label" column width */}
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                padding: spacing.normal,
                borderRight: constants.borders.regular.transparent,
              }}
            >
              <div
                css={{
                  writingMode: 'vertical-lr',
                  transform: 'rotate(180deg)',
                  lineHeight: typography.height.medium,
                }}
              >
                &nbsp;
              </div>
            </div>
            {/* end placeholder for "True Label" column width */}

            <div css={{ display: 'flex', width: settings.width }}>
              {/* begin placeholde for row labels */}
              <div
                css={{
                  paddingLeft: spacing.normal,
                  width: settings.labelsWidth,
                  minWidth: settings.labelsWidth,
                  borderRight: constants.borders.regular.coal,
                }}
              >
                &nbsp;
              </div>
              {/* end placeholde for row labels */}

              <div css={{ flex: 1, width: '0%' }}>
                <div
                  css={{
                    display: 'flex',
                    flexDirection: 'column',
                  }}
                >
                  <ModelMatrixLabels matrix={matrix} settings={settings} />

                  <div
                    css={{
                      padding: spacing.normal,
                      textAlign: 'center',
                      fontWeight: typography.weight.bold,
                      fontSize: typography.size.medium,
                      lineHeight: typography.height.medium,
                    }}
                  >
                    Predictions
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div
          css={{
            display: 'flex',
            flexShrink: 0,
            alignItems: 'flex-start',
            borderLeft: constants.borders.regular.coal,
          }}
        >
          <Button
            aria-label="Preview"
            title="Preview"
            variant={VARIANTS.ICON}
            href="#"
            style={{
              flex: 'none',
              paddingTop: spacing.normal,
              paddingBottom: spacing.normal,
              borderBottom: constants.borders.regular.coal,
              color: /* istanbul ignore next */ settings.isPreviewOpen
                ? colors.key.one
                : colors.structure.steel,
              ':hover': {
                backgroundColor: colors.structure.mattGrey,
              },
              borderRadius: constants.borderRadius.none,
            }}
          >
            <PreviewSvg width={constants.icons.regular} />
          </Button>
        </div>
      </div>
    </div>
  )
}

export default ModelMatrixLayout
