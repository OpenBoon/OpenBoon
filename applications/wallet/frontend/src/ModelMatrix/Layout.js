import AutoSizer from 'react-virtualized-auto-sizer'

import { colors, constants, spacing, typography } from '../Styles'

import ModelMatrixTable, { LABELS_WIDTH } from './Table'

const ModelMatrixLayout = () => {
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
                  <ModelMatrixTable width={width} height={height} />
                )}
              </AutoSizer>
            </div>
          </div>

          <div
            css={{ display: 'flex', borderTop: constants.borders.regular.coal }}
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

            {/* begin placeholde for row labels */}
            <div
              css={{
                paddingLeft: spacing.normal,
                width: LABELS_WIDTH,
                borderRight: constants.borders.regular.coal,
              }}
            >
              &nbsp;
            </div>
            {/* end placeholde for row labels */}

            <div
              css={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <div css={{ padding: spacing.normal, textAlign: 'center' }}>
                [labels]
              </div>

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
        <div
          css={{
            display: 'flex',
            flexShrink: 0,
            borderLeft: constants.borders.regular.coal,
          }}
        >
          [preview]
        </div>
      </div>
    </div>
  )
}

export default ModelMatrixLayout
