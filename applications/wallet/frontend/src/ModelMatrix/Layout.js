import { colors, constants, spacing, typography } from '../Styles'

const ROW_LABELS_WIDTH = 100

const ModelMatrixLayout = () => {
  return (
    <div
      css={{
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
          display: 'flex',
          backgroundColor: colors.structure.lead,
        }}
      >
        <div
          css={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <div
            css={{
              flex: 1,
              display: 'flex',
            }}
          >
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                padding: spacing.normal,
                borderRight: constants.borders.regular.coal,
                borderBottom: constants.borders.regular.coal,
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

            <div
              css={{
                flex: 1,
                display: 'flex',
                borderBottom: constants.borders.regular.coal,
              }}
            >
              [all the rows]
            </div>
          </div>

          <div css={{ display: 'flex' }}>
            {/* begin placeholder for "True Label" column width */}
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                padding: spacing.normal,
              }}
            >
              <div
                css={{
                  writingMode: 'vertical-lr',
                  transform: 'rotate(180deg)',
                }}
              >
                &nbsp;
              </div>
            </div>
            {/* end placeholder for "True Label" column width */}

            {/* begin placeholde for row labels */}
            <div
              css={{
                width: ROW_LABELS_WIDTH,
                borderRight: constants.borders.regular.coal,
              }}
            >
              &nbsp;
            </div>
            {/* end placeholde for row labels */}

            <div css={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
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
          css={{ display: 'flex', borderLeft: constants.borders.regular.coal }}
        >
          [preview]
        </div>
      </div>
    </div>
  )
}

export default ModelMatrixLayout
