import { useEffect } from 'react'
import PropTypes from 'prop-types'
import { Tooltip } from 'react-tippy'

import { colors, constants, spacing, typography } from '../Styles'

import { useScroller } from '../Scroll/helpers'

import { getColor } from './helpers'

import settingsShape from './settingsShape'

const CONTRAST_THRESHOLD = 69

const ModelMatrixRow = ({ matrix, settings, label, index, dispatch }) => {
  const rowRef = useScroller({
    namespace: 'ModelMatrixHorizontal',
    isWheelEmitter: true,
    isWheelListener: true,
    isScrollEmitter: true,
  })

  /* istanbul ignore next */
  useEffect(() => {
    setTimeout(() => {
      rowRef.current.dispatchEvent(new Event('scroll'))
    }, 0)
  }, [settings.zoom, rowRef])

  const rowTotal = matrix.matrix[index].reduce(
    (previous, current) => previous + current,
    0,
  )

  const cellDimension = (settings.height / matrix.labels.length) * settings.zoom

  return (
    <div
      key={label}
      css={{
        display: 'flex',
        height: cellDimension,
      }}
    >
      <div
        css={{
          width: settings.labelsWidth,
          paddingLeft: spacing.normal,
          paddingRight: spacing.normal,
          display: 'flex',
          alignItems: 'center',
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.bold,
          borderRight: constants.borders.regular.coal,
          borderBottom:
            index === matrix.labels.length - 1
              ? constants.borders.regular.transparent
              : constants.borders.regular.coal,
        }}
      >
        <span
          css={{
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {label}{' '}
        </span>
        <span
          css={{
            color: colors.structure.zinc,
            paddingLeft: spacing.small,
            fontWeight: typography.weight.regular,
          }}
        >
          ({rowTotal})
        </span>
      </div>

      <div
        ref={rowRef}
        css={{
          flex: 1,
          display: 'flex',
          overflow: 'hidden',
        }}
      >
        {matrix.matrix[index].map((value, col) => {
          const percent = rowTotal === 0 ? 0 : (value / rowTotal) * 100
          const isSelected =
            settings.selectedCell[0] === index &&
            settings.selectedCell[1] === col

          return (
            <Tooltip
              key={matrix.labels[col]}
              position="left"
              trigger="mouseenter"
              html={
                <div
                  css={{
                    color: colors.structure.coal,
                    backgroundColor: colors.structure.white,
                    borderRadius: constants.borderRadius.small,
                    boxShadow: constants.boxShadows.default,
                    padding: spacing.moderate,
                  }}
                >
                  <h3>
                    <span
                      css={{
                        fontFamily: typography.family.condensed,
                        fontWeight: typography.weight.regular,
                        color: colors.structure.iron,
                      }}
                    >
                      Predictions:
                    </span>{' '}
                    {value}/{rowTotal}({Math.round(percent)}%)
                  </h3>
                  <h3>
                    <span
                      css={{
                        fontFamily: typography.family.condensed,
                        fontWeight: typography.weight.regular,
                        color: colors.structure.iron,
                      }}
                    >
                      Label True:
                    </span>{' '}
                    {matrix.labels[index]}
                  </h3>
                  <h3>
                    <span
                      css={{
                        fontFamily: typography.family.condensed,
                        fontWeight: typography.weight.regular,
                        color: colors.structure.iron,
                      }}
                    >
                      Label Pred:
                    </span>{' '}
                    {matrix.labels[col]}
                  </h3>
                </div>
              }
            >
              <button
                type="button"
                css={{
                  width: cellDimension,
                  height: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  backgroundColor: getColor({ percent }),
                  border: isSelected ? constants.borders.keyOneLarge : 'none',
                  color:
                    percent > CONTRAST_THRESHOLD
                      ? colors.structure.white
                      : colors.structure.coal,
                  ':hover': {
                    border: constants.borders.keyTwoLarge,
                  },
                }}
                onClick={() =>
                  dispatch({
                    selectedCell: isSelected ? [] : [index, col],
                    isPreviewOpen: !isSelected,
                  })
                }
              >
                <div
                  css={{
                    margin: 'auto',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {settings.isNormalized ? `${Math.round(percent)}%` : value}
                </div>
              </button>
            </Tooltip>
          )
        })}
      </div>
    </div>
  )
}

ModelMatrixRow.propTypes = {
  matrix: PropTypes.shape({
    name: PropTypes.string.isRequired,
    overallAccuracy: PropTypes.number.isRequired,
    labels: PropTypes.arrayOf(PropTypes.string).isRequired,
    matrix: PropTypes.arrayOf(PropTypes.arrayOf(PropTypes.number)).isRequired,
  }).isRequired,
  settings: PropTypes.shape(settingsShape).isRequired,
  label: PropTypes.string.isRequired,
  index: PropTypes.number.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelMatrixRow
